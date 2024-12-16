package com.indusface.plugins.report;

import com.indusface.plugins.httpclient.HttpClientProvider;
import com.indusface.plugins.wasscan.ScanAndBuildStatus;
import com.indusface.plugins.wasscan.ScanApiResponse;
import hudson.model.Action;
import hudson.model.Run;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * The ReportAction class is responsible for fetching and parsing scan reports
 * and statuses from an external API. It implements the Action interface to
 * integrate with Jenkins.
 */
public class ReportAction implements Action {
    private Run<?, ?> run; // The Jenkins build

    private static final String SCAN_STATUS_API = ServiceUrls.SCAN_STATUS_API;
    private static final String SCAN_REPORT_API = ServiceUrls.SCAN_REPORT_API;
    // Get the singleton HttpClient instance from HttpClientProvider
    private static final HttpClient client = HttpClientProvider.getHttpClient();

    /**
     * Constructor to initialize the ReportAction with the specified Jenkins build.
     *
     * @param run the Jenkins build run
     */
    public ReportAction(Run<?, ?> run) {
        this.run = run;
    }

    /**
     * Fetches the scan status from the external API.
     *
     * @return the scan status as a ScanApiResponse object
     */
    public ScanApiResponse getScanStatus() throws Exception {
        ScanAndBuildStatus action = run.getAction(ScanAndBuildStatus.class);
        ScanApiResponse status = callGetStatusAPI(action.getScanId(), action.getSecretKey());

        return status;
    }

    /**
     * Fetches and parses the scan report data from the external API.
     *
     * @return the scan report as a ScanReport object
     */
    public ScanReport scanReportData() {
        ScanReport sr = new ScanReport();

        try {
            ScanAndBuildStatus action = run.getAction(ScanAndBuildStatus.class);

            if (action == null) {
                throw new IllegalArgumentException("ScanId and access key  is missing from the build.");
            }

            String scanId = action.getScanId();
            String secretKey = action.getSecretKey();
            String jobStatus = action.getBuildStatus();
            sr.setJobStatus(jobStatus);
            if (jobStatus.equals(BuildStatus.COMPLETED.toString())) {
                String apiUrl = String.format(SCAN_REPORT_API, scanId);
                String jsonBody = createJsonBody(secretKey);
                HttpRequest request;

                request = HttpRequest.newBuilder()
                        .uri(new URI(apiUrl))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = null;

                response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == HttpURLConnection.HTTP_OK) {

                    JSONObject jsonObject = JSONObject.fromObject(response.body());
                    sr = parseScanData(jsonObject);
                    sr.setJobStatus(jobStatus);

                } else {
                    sr.setJobStatus("ERROR");
                }
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            sr.setJobStatus("ERROR");
        }

        return sr;
    }

    /**
     * Calls the external API to get the scan status.
     *
     * @param secretKey the secret key for authentication
     * @param scanlogid the scan log ID
     * @return the scan status as a ScanApiResponse object
     * @throws Exception if an error occurs during the API call
     */
    public ScanApiResponse callGetStatusAPI(String scanlogid, String secretKey) throws Exception {
        ScanApiResponse scanApiResponse = new ScanApiResponse();
        String scanStatus = null;
        String buildStatus = null;
        String apiUrl = String.format(SCAN_STATUS_API, scanlogid);
        String jsonBody = createJsonBody(secretKey);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(apiUrl))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            JSONObject jsonObject = JSONObject.fromObject(response.body());
            scanStatus = jsonObject.getJSONObject("result").get("scanStatus").toString();
            buildStatus = jsonObject.getJSONObject("result").get("buildStatus").toString();
            scanApiResponse.setBuildStatus(buildStatus);
            scanApiResponse.setScanStatus(scanStatus);
        }
        return scanApiResponse;
    }

    private static String createJsonBody(String secretKey) {
        if (secretKey == null) {
            throw new IllegalStateException("SECRET_KEY is null");
        }
        JSONObject json = new JSONObject();
        json.put("secret_key", secretKey);

        // Convert the JSON object to a string
        return json.toString();
    }

    /**
     * Parses the scan data from the given JSON object.
     *
     * @param jsonObject the JSON object containing the scan data
     * @return the parsed scan data as a ScanReport object
     */
    private ScanReport parseScanData(JSONObject jsonObject) {
        ScanReport scanReport = new ScanReport();

        JSONObject result = jsonObject.getJSONObject("result");

        scanReport.setScanLogId(result.getLong("scanlogid"));
        scanReport.setUrl(result.get("url").toString());
        scanReport.setStartTime(result.get("startTime").toString());
        scanReport.setEndTime(result.get("endTime").toString());
        scanReport.setScanMinutes(result.getInt("scanminutes"));
        scanReport.setScanReportUrl(result.get("scanReport").toString());
        scanReport.setTotalVulnerabilities(result.getInt("totalVulnerabilities"));
        scanReport.setScanStatus(result.get("scanStatus").toString());
        scanReport.setBuildStatus(result.get("buildStatus").toString());

        // Parsing SeverityWiseVulns
        JSONObject severityWiseVulnsJson = result.getJSONObject("severityWiseVulns");
        SeverityWiseVulns severityWiseVulns = new SeverityWiseVulns();
        severityWiseVulns.setCritical(severityWiseVulnsJson.getInt("critical"));
        severityWiseVulns.setHigh(severityWiseVulnsJson.getInt("high"));
        severityWiseVulns.setMedium(severityWiseVulnsJson.getInt("medium"));
        severityWiseVulns.setLow(severityWiseVulnsJson.getInt("low"));
        severityWiseVulns.setInfo(severityWiseVulnsJson.getInt("info"));
        scanReport.setSeverityWiseVulns(severityWiseVulns);

        // Parsing BuildStatusConfig
        JSONArray buildStatusArray = result.getJSONArray("buildStatusConfig");
        List<BuildStatusConfig> buildStatusConfigList = new ArrayList<>();
        for (int i = 0; i < buildStatusArray.size(); i++) {
            JSONObject buildStatusConfigJson = (JSONObject) buildStatusArray.get(i);
            BuildStatusConfig buildStatusConfig = new BuildStatusConfig();
            buildStatusConfig.setSeverity(buildStatusConfigJson.get("severity").toString());
            buildStatusConfig.setFoundVulns(buildStatusConfigJson.getInt("found_vulns"));
            buildStatusConfig.setThresholdLimit(buildStatusConfigJson.getInt("threshold_limit"));
            buildStatusConfig.setIsAboveThreshold(
                    buildStatusConfigJson.get("is_above_threshold").toString());
            buildStatusConfigList.add(buildStatusConfig);
        }
        scanReport.setBuildStatusConfig(buildStatusConfigList);

        JSONArray vulnerabilitiesArray = result.getJSONArray("vulnerabilities");

        List<Vulnerability> vulnerabilityList = new ArrayList<>();

        for (int i = 0; i < vulnerabilitiesArray.size(); i++) {
            JSONObject vulnerabilityJson = vulnerabilitiesArray.getJSONObject(i);
            Vulnerability vulnerability = new Vulnerability();
            vulnerability.setUniqueId(vulnerabilityJson.getLong("uniqueid"));
            vulnerability.setTitle(vulnerabilityJson.get("title").toString());
            vulnerability.setSeverity(vulnerabilityJson.get("severity").toString());
            vulnerability.setCvssScore(vulnerabilityJson.get("cvssScore").toString());
            vulnerability.setOpenStatus(vulnerabilityJson.get("openStatus").toString());
            vulnerability.setFoundOn(vulnerabilityJson.get("foundOn").toString());
            vulnerability.setFoundDate(vulnerabilityJson.get("foundDate").toString());
            vulnerability.setDescription(vulnerabilityJson.get("description").toString());
            vulnerability.setSolution(vulnerabilityJson.get("solution").toString());
            vulnerabilityList.add(vulnerability);
        }
        scanReport.setVulnerabilities(vulnerabilityList);
        return scanReport;
    }

    @Override
    public String getIconFileName() {
        return "symbol-reader-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return "WAS Scan Report";
    }

    @Override
    public String getUrlName() {
        return "WasScanReport";
    }
}
