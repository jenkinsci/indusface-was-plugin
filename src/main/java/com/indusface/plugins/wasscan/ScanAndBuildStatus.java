package com.indusface.plugins.wasscan;

import hudson.model.InvisibleAction;

public class ScanAndBuildStatus extends InvisibleAction {
    private final String scanId;
    private final String secretKey;
    private String buildStatus;

    public ScanAndBuildStatus(String scanId, String secretKey, String buildStatus) {
        this.scanId = scanId;
        this.secretKey = secretKey;
        this.buildStatus = buildStatus;
    }

    public String getScanId() {
        return scanId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }
}
