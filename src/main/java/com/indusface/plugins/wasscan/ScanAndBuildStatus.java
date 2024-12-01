package com.indusface.plugins.wasscan;

import hudson.model.InvisibleAction;
import hudson.util.Secret;

public class ScanAndBuildStatus extends InvisibleAction {
    private final String scanId;
    private final Secret secretKey;
    private String buildStatus;

    public ScanAndBuildStatus(String scanId, String secretKey, String buildStatus) {
        this.scanId = scanId;
        this.secretKey = Secret.fromString(secretKey);
        this.buildStatus = buildStatus;
    }

    public String getScanId() {
        return scanId;
    }

    public String getSecretKey() {
        return Secret.toString(secretKey);
    }

    public String getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }
}
