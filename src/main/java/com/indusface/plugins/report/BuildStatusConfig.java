package com.indusface.plugins.report;

public class BuildStatusConfig {
    private String severity;
    private int foundVulns;
    private int thresholdLimit;
    private String isAboveThreshold;

    // Getters and Setters
    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getFoundVulns() {
        return foundVulns;
    }

    public void setFoundVulns(int foundVulns) {
        this.foundVulns = foundVulns;
    }

    public int getThresholdLimit() {
        return thresholdLimit;
    }

    public void setThresholdLimit(int thresholdLimit) {
        this.thresholdLimit = thresholdLimit;
    }

    public String getIsAboveThreshold() {
        return isAboveThreshold;
    }

    public void setIsAboveThreshold(String isAboveThreshold) {
        this.isAboveThreshold = isAboveThreshold;
    }
}
