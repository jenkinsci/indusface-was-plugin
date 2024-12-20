package com.indusface.plugins.wasscan;

import com.indusface.plugins.report.BuildStatus;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.Secret;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * WasPostBuildAction is a Jenkins post-build action that performs specific tasks after a build is completed.
 * It extends the Notifier class and implements the SimpleBuildStep interface.
 */
public class WasPostBuildAction extends Notifier implements SimpleBuildStep {

    private Secret accessKey;
    private String buildName;

    /**
     * Constructor to initialize the WasPostBuildAction with the specified access key and build name.
     *
     * @param accessKey the access key required for the post-build action
     * @param buildName the name of the build
     */
    @DataBoundConstructor
    public WasPostBuildAction(String accessKey, String buildName) {
        this.accessKey = Secret.fromString(accessKey);
        this.buildName = buildName;
    }

    /**
     * Gets the access key.
     *
     * @return the access key
     */
    public String getAccessKey() {
        return Secret.toString(accessKey);
    }

    /**
     * Sets the access key.
     *
     * @param accessKey the new access key
     */
    @DataBoundSetter
    public void setAccessKey(String accessKey) {
        this.accessKey = Secret.fromString(accessKey);
    }

    /**
     * Gets the build name.
     *
     * @return the build name
     */
    public String getBuildName() {
        return buildName;
    }

    /**
     * Sets the build name.
     *
     * @param buildName the new build name
     */
    @DataBoundSetter
    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    /**
     * Performs the post-build action. If the build was successful, it executes additional tasks.
     *
     * @param run the build run
     * @param workspace the workspace
     * @param launcher the launcher
     * @param listener the task listener
     * @throws InterruptedException if the build is interrupted
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        try {
            cancelPreviousRunningBuilds(run, listener);
            Result result = run.getResult();
            if (result != null && result.equals(Result.SUCCESS)) {
                listener.getLogger().println("Build was successful! Executing post-build action...");
                ScanApiLaunch sc = new ScanApiLaunch();
                boolean isBuildFail = sc.startScan(listener, Secret.toString(accessKey), run);
                if (isBuildFail) {
                    listener.getLogger().println("Build failed.");
                    run.setResult(Result.FAILURE);
                } else {
                    run.setResult(Result.SUCCESS);
                }

                ScanAndBuildStatus statusDetail = run.getAction(ScanAndBuildStatus.class);
                if (statusDetail != null) {
                    statusDetail.setBuildStatus(BuildStatus.COMPLETED.toString());
                }
                listener.getLogger().println("Build result: " + run.getResult());
            } else {
                listener.getLogger().println("Build was not successful. Skipping post-build action.");
            }

        } catch (Exception e) {
            if ("sleep interrupted".equalsIgnoreCase(e.getMessage())) {
                listener.getLogger().println("Aborted/Interrupted execution of the build.");
                run.setResult(Result.ABORTED);
                // throw new InterruptedException("Aborted/Interrupted execution of the build.");
            } else {
                run.setResult(Result.FAILURE);
                listener.getLogger().println("Exception triggered");
            }
        } finally {
            ScanAndBuildStatus statusDetail = run.getAction(ScanAndBuildStatus.class);
            if (statusDetail != null && statusDetail.getBuildStatus().equals(BuildStatus.INPROGRESS.toString())) {
                statusDetail.setBuildStatus(BuildStatus.ABORTED.toString());
            }
        }
    }

    private void cancelPreviousRunningBuilds(Run<?, ?> run, TaskListener listener) {
        // Get the job associated with the build
        Job<?, ?> job = run.getParent();

        // Check if there are any other builds running
        for (Run<?, ?> previousBuild : job.getBuilds()) {
            // Skip the current build
            if (previousBuild == run) continue;

            // If the previous build is still running, cancel it
            if (previousBuild.isBuilding()) {
                listener.getLogger().println("Cancelling previous build #" + previousBuild.getNumber());
                previousBuild.setResult(Result.ABORTED);
                Executor executor = previousBuild.getExecutor();
                if (executor != null) {
                    executor.interrupt(Result.ABORTED); // Abort the previous build
                }
                ScanAndBuildStatus getScanAction = previousBuild.getAction(ScanAndBuildStatus.class);
                getScanAction.setBuildStatus(BuildStatus.ABORTED.toString());
                previousBuild.setResult(Result.ABORTED);
            }
        }
    }

    /**
     * Specifies the required monitor service for this build step.
     *
     * @return the build step monitor
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Descriptor for the WasPostBuildAction.
     */
    @Symbol("indusfaceWasScan")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /**
         * Constructor for DescriptorImpl.
         */
        public DescriptorImpl() {
            super(WasPostBuildAction.class);
        }

        /**
         * Checks if this build step is applicable to the given project type.
         *
         * @param aClass the project class
         * @return true if applicable, false otherwise
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * Gets the display name of this build step.
         *
         * @return the display name
         */
        @Override
        public String getDisplayName() {
            return "Indusface Was Scan";
        }
    }
}
