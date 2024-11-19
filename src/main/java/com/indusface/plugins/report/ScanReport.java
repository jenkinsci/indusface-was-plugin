package com.indusface.plugins.report;

import java.util.List;

public class ScanReport {
    private String jobStatus;
    private long scanLogId;
    private String url;
    private String status;
    private String startTime;
    private String endTime;
    private int scanMinutes;
    private String scanReportUrl;
    private int totalVulnerabilities;
    private SeverityWiseVulns severityWiseVulns;
    private String scanStatus;
    private String buildStatus;
    private List<BuildStatusConfig> buildStatusConfig;
    private List<Vulnerability> vulnerabilities;

    // Getters and Setters
    public long getScanLogId() {
        return scanLogId;
    }

    public void setScanLogId(long scanLogId) {
        this.scanLogId = scanLogId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getScanMinutes() {
        return scanMinutes;
    }

    public void setScanMinutes(int scanMinutes) {
        this.scanMinutes = scanMinutes;
    }

    public String getScanReportUrl() {
        return scanReportUrl;
    }

    public void setScanReportUrl(String scanReportUrl) {
        this.scanReportUrl = scanReportUrl;
    }

    public int getTotalVulnerabilities() {
        return totalVulnerabilities;
    }

    public void setTotalVulnerabilities(int totalVulnerabilities) {
        this.totalVulnerabilities = totalVulnerabilities;
    }

    public SeverityWiseVulns getSeverityWiseVulns() {
        return severityWiseVulns;
    }

    public void setSeverityWiseVulns(SeverityWiseVulns severityWiseVulns) {
        this.severityWiseVulns = severityWiseVulns;
    }

    public String getScanStatus() {
        return scanStatus;
    }

    public void setScanStatus(String scanStatus) {
        this.scanStatus = scanStatus;
    }

    public String getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }

    public List<BuildStatusConfig> getBuildStatusConfig() {
        return buildStatusConfig;
    }

    public void setBuildStatusConfig(List<BuildStatusConfig> buildStatusConfig) {
        this.buildStatusConfig = buildStatusConfig;
    }

    public List<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(List<Vulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    @Override
    public String toString() {
        return "ScanReport [jobStatus=" + jobStatus + ", scanLogId=" + scanLogId + ", url=" + url + ", status=" + status
                + ", startTime=" + startTime + ", endTime=" + endTime + ", scanMinutes=" + scanMinutes
                + ", scanReportUrl=" + scanReportUrl + ", totalVulnerabilities=" + totalVulnerabilities
                + ", severityWiseVulns=" + severityWiseVulns + ", scanStatus=" + scanStatus + ", buildStatus="
                + buildStatus + ", buildStatusConfig=" + buildStatusConfig + ", vulnerabilities=" + vulnerabilities
                + "]";
    }
}
