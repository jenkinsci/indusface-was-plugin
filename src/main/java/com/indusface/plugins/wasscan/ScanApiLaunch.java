package com.indusface.plugins.wasscan;

import static com.indusface.plugins.httpclient.HttpClientProvider.getHttpClient;
import static com.indusface.plugins.report.ReportAction.createJsonBody;

import com.indusface.plugins.entity.StartScanResponse;
import com.indusface.plugins.report.BuildStatus;
import com.indusface.plugins.report.ReportAction;
import com.indusface.plugins.report.ServiceUrls;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

/**
 * The ScanApiLaunch class is responsible for initiating and monitoring a scan
 * process through an external API. It handles the communication with the API,
 * processes the responses, and updates the Jenkins build status based on the
 * scan results.
 */
public class ScanApiLaunch {

    /**
     * Starts the scan process using the provided access key and updates the Jenkins
     * build status based on the scan results.
     *
     * @param listener  the task listener for logging
     * @param accessKey the access key required for the scan
     * @param run       the Jenkins build run
     * @return true if the build failed, false otherwise
     */
    public boolean startScan(TaskListener listener, String accessKey, Run<?, ?> run) throws InterruptedException {
        String scanlogid = null;
        boolean isBuildFailed = false;
        StartScanResponse response = null;
        try {
            response = startScan(accessKey);

            if (!response.isSuccess()) {
                listener.getLogger().println("Scan start Failed .... ");
                isBuildFailed = true;
                return isBuildFailed;
            }

            listener.getLogger().println("Scan Started ");
            scanlogid = response.getScanLogId();
            run.addAction(new ScanAndBuildStatus(scanlogid, accessKey, BuildStatus.INPROGRESS.toString()));
            ReportAction action = new ReportAction(run);
            run.addAction(action);
            boolean isScanRunning = true;
            while (isScanRunning) {
                boolean isBuildAborted = monitorAndAbortIfCancelled(run, listener);
                if (isBuildAborted) {
                    run.setResult(Result.ABORTED);
                    ScanAndBuildStatus statusDetail = run.getAction(ScanAndBuildStatus.class);
                    statusDetail.setBuildStatus(BuildStatus.ABORTED.toString());
                    return isBuildFailed;
                }
                try {
                    ScanApiResponse scanApiResponse = action.getScanStatus();
                    if (scanApiResponse.getScanStatus().equals("Completed")) {
                        ScanAndBuildStatus statusDetail = run.getAction(ScanAndBuildStatus.class);
                        statusDetail.setBuildStatus(BuildStatus.COMPLETED.toString());
                        listener.getLogger()
                                .println("Scan completed. View report now . Build Status: "
                                        + scanApiResponse.getBuildStatus());
                        isScanRunning = false;
                        if (scanApiResponse.getBuildStatus().equals("fail")) {
                            isBuildFailed = true;
                        }
                        return isBuildFailed;
                    }
                    listener.getLogger().println("Scanning in progress. Checking status in 5 minutes. ");

                } catch (Exception e) {
                    isBuildFailed = true;
                    listener.getLogger().println("Unable to retrieve status of scan.");
                    ScanAndBuildStatus statusDetail = run.getAction(ScanAndBuildStatus.class);
                    if (statusDetail != null
                            && statusDetail.getBuildStatus().equals(BuildStatus.INPROGRESS.toString())) {
                        System.out.println();
                        statusDetail.setBuildStatus(BuildStatus.UNAVAILABLE.toString());
                    }
                    return isBuildFailed;
                }
            }
        } catch (IOException | URISyntaxException e) {
            run.setResult(Result.FAILURE);
            listener.getLogger().println("I/O error occurred . Jenkins Build  Failed.");
            isBuildFailed = true;
        }
        return isBuildFailed;
    }

    /**
     * Initiates the scan by sending a POST request to the scan API with the
     * provided access key.
     *
     * @param accessKey the access key required for the scan
     * @return the HTTP response from the scan API
     * @throws URISyntaxException if the URI syntax is incorrect
     * @throws IOException        if an I/O error occurs
     */
    private StartScanResponse startScan(String accessKey) throws URISyntaxException, IOException {
        String scan_start_api = ServiceUrls.SCAN_START_API;
        URI scanUri = new URI(scan_start_api);

        String jsonBody = createJsonBody(accessKey);

        try (CloseableHttpClient client = getHttpClient()) {
            HttpUriRequestBase request = new HttpPost(scanUri);
            request.setHeader("Accept", "application/x-www-form-urlencoded");
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println("Response Code: " + response.getCode());
                return new StartScanResponse(response.getCode(), EntityUtils.toString(response.getEntity()));
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return new StartScanResponse();
    }

    public static boolean monitorAndAbortIfCancelled(Run<?, ?> build, TaskListener listener)
            throws InterruptedException {
        long timeoutMillis = 5 * 60 * 1000; // 5 minutes in milliseconds
        long endTime = System.currentTimeMillis() + timeoutMillis;

        // Continuously check the build's status every second
        while (System.currentTimeMillis() < endTime) {
            // If the build is no longer building, break out of the loop
            if (!build.isBuilding()) {
                listener.getLogger().println("Build #" + build.getNumber() + " has finished.");
                return false;
            }

            Executor executor = build.getExecutor();

            if (executor == null) {
                listener.getLogger().println("No executor found; unable to monitor build cancellation.");
                return false;
            }

            // Check if the build is interrupted (i.e., canceled)
            if (executor.isInterrupted()) {
                listener.getLogger()
                        .println("Build #" + build.getNumber() + " was canceled after checking for 5 minutes.");
                build.setResult(Result.ABORTED); // Mark the build as aborted
                return true; // Indicate that the build was canceled
            }

            // Sleep for 1 second before checking again
            Thread.sleep(1000);
        }
        return false;
    }
}
