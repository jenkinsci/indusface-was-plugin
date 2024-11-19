package com.indusface.plugins.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.indusface.plugins.httpclient.HttpClientProvider;
import com.indusface.plugins.wasscan.ScanAndBuildStatus;
import com.indusface.plugins.wasscan.ScanApiResponse;
import hudson.model.Action;
import hudson.model.Run;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(ReportAction.class.getName());

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
    public ScanApiResponse getScanStatus() {
        ScanAndBuildStatus action = run.getAction(ScanAndBuildStatus.class);
        String scanId = action.getScanId();
        String secretKey = action.getSecretKey();
        ScanApiResponse status = new ScanApiResponse();

        try {
            status = callGetStatusAPI(scanId, secretKey);

        } catch (Exception e) {
            logger.info("Exception occurred getScanStatus:" + e.getLocalizedMessage());
        }
        logger.info(
                " Status of Scan for AccessKey :" + secretKey + " is :" + status + " at time :" + LocalDateTime.now());
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
                logger.info("apiUrl for scan Report: " + apiUrl);
                String jsonBody = createJsonBody(secretKey);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(apiUrl))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    JsonObject jsonObject =
                            JsonParser.parseString(response.body()).getAsJsonObject();
                    sr = parseScanData(jsonObject);
                    sr.setJobStatus(jobStatus);

                } else {
                    logger.info("Failed to fetch scan data. HTTP Status: " + response.statusCode());
                }
            }

        } catch (Exception e) {
            logger.info("Failed to fetch scan report. " + e.getMessage());
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
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            logger.info("jsonObject :" + jsonObject);
            scanStatus = jsonObject.getAsJsonObject("result").get("scanStatus").getAsString();
            buildStatus =
                    jsonObject.getAsJsonObject("result").get("buildStatus").getAsString();
            scanApiResponse.setBuildStatus(buildStatus);
            scanApiResponse.setScanStatus(scanStatus);
        } else {
            logger.info("Failed to get scan Status Response: " + response.body());
        }

        return scanApiResponse;
    }

    private static String createJsonBody(String secretKey) {
        if (secretKey == null) {
            throw new IllegalStateException("SECRET_KEY is null");
        }
        JsonObject json = new JsonObject();
        json.addProperty("secret_key", secretKey);

        // Convert the JSON object to a string
        return json.toString();
    }

    /**
     * Parses the scan data from the given JSON object.
     *
     * @param jsonObject the JSON object containing the scan data
     * @return the parsed scan data as a ScanReport object
     */
    private ScanReport parseScanData(JsonObject jsonObject) {
        ScanReport scanReport = new ScanReport();

        JsonObject result = jsonObject.getAsJsonObject("result");

        scanReport.setScanLogId(result.get("scanlogid").getAsLong());
        scanReport.setUrl(result.get("url").getAsString());
        scanReport.setStartTime(result.get("startTime").getAsString());
        scanReport.setEndTime(result.get("endTime").getAsString());
        scanReport.setScanMinutes(result.get("scanminutes").getAsInt());
        scanReport.setScanReportUrl(result.get("scanReport").getAsString());
        scanReport.setTotalVulnerabilities(result.get("totalVulnerabilities").getAsInt());
        scanReport.setScanStatus(result.get("scanStatus").getAsString());
        scanReport.setBuildStatus(result.get("buildStatus").getAsString());

        // Parsing SeverityWiseVulns
        JsonObject severityWiseVulnsJson = result.getAsJsonObject("severityWiseVulns");
        SeverityWiseVulns severityWiseVulns = new SeverityWiseVulns();
        severityWiseVulns.setCritical(severityWiseVulnsJson.get("critical").getAsInt());
        severityWiseVulns.setHigh(severityWiseVulnsJson.get("high").getAsInt());
        severityWiseVulns.setMedium(severityWiseVulnsJson.get("medium").getAsInt());
        severityWiseVulns.setLow(severityWiseVulnsJson.get("low").getAsInt());
        severityWiseVulns.setInfo(severityWiseVulnsJson.get("info").getAsInt());
        scanReport.setSeverityWiseVulns(severityWiseVulns);

        // Parsing BuildStatusConfig
        JsonArray buildStatusArray = result.getAsJsonArray("buildStatusConfig");
        List<BuildStatusConfig> buildStatusConfigList = new ArrayList<>();
        for (int i = 0; i < buildStatusArray.size(); i++) {
            JsonObject buildStatusConfigJson = buildStatusArray.get(i).getAsJsonObject();
            BuildStatusConfig buildStatusConfig = new BuildStatusConfig();
            buildStatusConfig.setSeverity(buildStatusConfigJson.get("severity").getAsString());
            buildStatusConfig.setFoundVulns(
                    buildStatusConfigJson.get("found_vulns").getAsInt());
            buildStatusConfig.setThresholdLimit(
                    buildStatusConfigJson.get("threshold_limit").getAsInt());
            buildStatusConfig.setIsAboveThreshold(
                    buildStatusConfigJson.get("is_above_threshold").getAsString());
            buildStatusConfigList.add(buildStatusConfig);
        }
        scanReport.setBuildStatusConfig(buildStatusConfigList);

        // Parsing Vulnerabilities
        JsonArray vulnerabilitiesArray = result.getAsJsonArray("vulnerabilities");
        List<Vulnerability> vulnerabilityList = new ArrayList<>();
        for (int i = 0; i < vulnerabilitiesArray.size(); i++) {
            JsonObject vulnerabilityJson = vulnerabilitiesArray.get(i).getAsJsonObject();
            Vulnerability vulnerability = new Vulnerability();
            vulnerability.setUniqueId(vulnerabilityJson.get("uniqueid").getAsLong());
            vulnerability.setTitle(vulnerabilityJson.get("title").getAsString());
            vulnerability.setSeverity(vulnerabilityJson.get("severity").getAsString());
            vulnerability.setCvssScore(vulnerabilityJson.get("cvssScore").getAsString());
            vulnerability.setOpenStatus(vulnerabilityJson.get("openStatus").getAsString());
            vulnerability.setFoundOn(vulnerabilityJson.get("foundOn").getAsString());
            vulnerability.setFoundDate(vulnerabilityJson.get("foundDate").getAsString());
            vulnerability.setDescription(vulnerabilityJson.get("description").getAsString());
            vulnerability.setSolution(vulnerabilityJson.get("solution").getAsString());
            vulnerabilityList.add(vulnerability);
        }
        scanReport.setVulnerabilities(vulnerabilityList);
        logger.info(" scan report : " + scanReport);
        return scanReport;
    }

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "Build Summary report";
    }

    @Override
    public String getUrlName() {
        return "SummaryReport.html";
    }
}
