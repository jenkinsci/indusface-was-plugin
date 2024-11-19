package com.indusface.plugins.report;

public enum BuildStatus {
    COMPLETED("COMPLETED"),
    ABORTED("ABORTED"),
    INPROGRESS("INPROGRESS");

    private final String description;

    BuildStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == ABORTED || this == INPROGRESS;
    }

    @Override
    public String toString() {
        return description;
    }
}
