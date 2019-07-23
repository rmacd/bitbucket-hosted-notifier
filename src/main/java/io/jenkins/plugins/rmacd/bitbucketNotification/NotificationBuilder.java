package io.jenkins.plugins.rmacd.bitbucketNotification;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.rmacd.bitbucketNotification.models.NotificationModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class NotificationBuilder extends Builder implements SimpleBuildStep {

    private final String action;

    private final NotificationUtils notificationUtils;

    @DataBoundConstructor
    public NotificationBuilder(String action) {
        if (action.isEmpty() ||
                (!action.equalsIgnoreCase("inprogress") &&
                        !action.equalsIgnoreCase("successful") &&
                        !action.equalsIgnoreCase("failed"))) {
            throw new NotificationRuntimeException("Invalid action");
        }
        this.action = action;
        this.notificationUtils = null;
    }

    public String getAction() {
        return action;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath filePath,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener taskListener
    ) {
        AbstractProject project = ((AbstractBuild) run).getProject();
        notificationUtils.setLogger(taskListener.getLogger());

        // construct JSON
        NotificationModel notification = new NotificationModel(
            NotificationModel.State.valueOf(action),
                Objects.requireNonNull(Jenkins.get().getRootUrl()) + run.getUrl(),
                project.getName(),
                (null == run.getTruncatedDescription() || run.getTruncatedDescription().isEmpty()) ?
                        project.getName() : run.getTruncatedDescription()
        );

        String commitID;
        try {
            commitID = run.getEnvironment(taskListener).get("git_commit");
            // in case above returns null
            if (null == commitID) throw new IOException();
        } catch (IOException | InterruptedException e) {
            throw new NotificationRuntimeException("Unable to resolve commit ID");
        }

        NotificationConfiguration config = NotificationConfiguration.get();
        taskListener.getLogger().println("Got rest endpoint " + config.getRestEndpoint() + "/" + commitID);
        taskListener.getLogger().println("Got credentials ID " + config.getCredentialsId());

        Map<String, String> buildvars = ((AbstractBuild) run).getBuildVariables();
        for (Iterator<String> iterator = buildvars.keySet().iterator(); iterator.hasNext(); ) {
            String next = iterator.next();
            taskListener.getLogger().printf("got entry: %s:%s%n", next, buildvars.get(next));
        }

        try {
            notificationUtils.postJSON(config.getRestEndpoint() + commitID, notification.toString());
        } catch (NotificationCheckedException e) {
            taskListener.fatalError(e.getMessage());
        }
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Notify hosted Bitbucket instance";
        }
    }
}
