package com.indusface.plugins.wasscan;

public class ScanApiResponse {
    private String scanStatus;
    private String buildStatus;

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

    @Override
    public String toString() {
        return "ScanApiResponse [scanStatus=" + scanStatus + ", buildStatus=" + buildStatus + "]";
    }
}
