package com.indusface.plugins.wasscan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.indusface.plugins.httpclient.HttpClientProvider;
import com.indusface.plugins.report.BuildStatus;
import com.indusface.plugins.report.ReportAction;
import com.indusface.plugins.report.ServiceUrls;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

/**
 * The ScanApiLaunch class is responsible for initiating and monitoring a scan
 * process through an external API. It handles the communication with the API,
 * processes the responses, and updates the Jenkins build status based on the
 * scan results.
 */
public class ScanApiLaunch {
    private static final Logger logger = Logger.getLogger(ScanApiLaunch.class.getName());

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
        HttpResponse<String> response = null;
        try {
            response = startScan(accessKey);

            // Print the response status code and body
            logger.info("Response Code: " + response.statusCode());
            logger.info("Response Body: " + response.body());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                logger.info("Scan start Failed .... " + response.body());
                listener.getLogger().println("Scan start Failed .... " + response.body());
                isBuildFailed = true;
                return isBuildFailed;
            }

            listener.getLogger().println("Scan Started ");
            scanlogid = getScanLogId(response);
            run.addAction(new ScanAndBuildStatus(scanlogid, accessKey, BuildStatus.INPROGRESS.toString()));
            listener.getLogger().println("Scan Logid :" + scanlogid);
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
                        listener.getLogger()
                                .println("Scan complete. View report now.Current Build Status: "
                                        + scanApiResponse.getBuildStatus());
                        isScanRunning = false;
                        if (scanApiResponse.getBuildStatus().equals("fail")) {
                            isBuildFailed = true;
                        }
                        return isBuildFailed;
                    }
                    listener.getLogger()
                            .println("Scanning in progress. Checking status in 5 minutes.: "
                                    + scanApiResponse.getScanStatus());

                } catch (Exception e) {
                    isBuildFailed = true;
                    listener.getLogger().println("Unable to retrieve status of scan.");
                    return isBuildFailed;
                }
            }
        } catch (IOException | URISyntaxException e) {
            run.setResult(Result.FAILURE);
            logger.info("I/O error occurred: " + e.getMessage());
            listener.getLogger().println("I/O error occurred . Jenkins Build  Failed.");
            isBuildFailed = true;
        }
        return isBuildFailed;
    }

    /**
     * Extracts the scan log ID from the API response.
     *
     * @param response the HTTP response from the scan API
     * @return the scan log ID
     */
    private String getScanLogId(HttpResponse<String> response) {
        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        logger.info("jsonObject :" + jsonObject);
        return jsonObject.getAsJsonObject("result").get("scanlogid").getAsString();
    }

    /**
     * Initiates the scan by sending a POST request to the scan API with the
     * provided access key.
     *
     * @param accessKey the access key required for the scan
     * @return the HTTP response from the scan API
     * @throws URISyntaxException   if the URI syntax is incorrect
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the request is interrupted
     */
    private HttpResponse<String> startScan(String accessKey)
            throws URISyntaxException, IOException, InterruptedException {
        HttpClient client = HttpClientProvider.getHttpClient();

        String scan_start_api = ServiceUrls.SCAN_START_API;
        URI scanUri = new URI(scan_start_api);

        String secretKey = "secret_key=" + accessKey;

        // Create a POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(scanUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(secretKey))
                .build();
        logger.info("Invoked Start scan API");
        // Send the request and get the response
        return client.send(request, HttpResponse.BodyHandlers.ofString());
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
