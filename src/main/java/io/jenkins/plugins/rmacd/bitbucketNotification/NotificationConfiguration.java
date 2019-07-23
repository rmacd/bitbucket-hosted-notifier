package io.jenkins.plugins.rmacd.bitbucketNotification;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class NotificationConfiguration extends GlobalConfiguration {

    /** @return the singleton instance */
    public static NotificationConfiguration get() {
        return GlobalConfiguration.all().get(NotificationConfiguration.class);
    }

    private String restEndpoint;
    private String credentialsId;
    private NotificationUtils notificationUtils;

    public NotificationConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
        notificationUtils = new NotificationUtils();
    }

    public String getRestEndpoint() {
        return restEndpoint;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Together with {@link #getRestEndpoint}, binds to entry in {@code config.jelly}.
     * @param restEndpoint the new value of this field
     */
    @DataBoundSetter
    public void setRestEndpoint(String restEndpoint) {
        this.restEndpoint = restEndpoint;
        save();
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        save();
    }

    public FormValidation doCheckRestEndpoint(@QueryParameter String endpointURL) {
        if (StringUtils.isEmpty(endpointURL)) {
            return FormValidation.warning("Please specify a REST endpoint.");
        }
        try {
            if (notificationUtils.getURL(endpointURL) == 200) {
                return FormValidation.ok();
            }
            else {
                return FormValidation.warning("Non-200 response returned from endpoint");
            }
        } catch (NotificationCheckedException e) {
            return FormValidation.error(e.getMessage());
        }
    }

    public ListBoxModel doFillCredentialsIdItems(
            @AncestorInPath Jenkins context,
            @QueryParameter String remoteBase) {
        if (context == null || !context.hasPermission(Item.CONFIGURE)) {
            return new StandardListBoxModel();
        }

        return new StandardListBoxModel().includeEmptyValue().includeMatchingAs(ACL.SYSTEM, context, StandardCertificateCredentials.class,
                URIRequirementBuilder.create().build(),
                CredentialsMatchers.anyOf(
                        CredentialsMatchers.instanceOf(StandardCertificateCredentials.class),
                        CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)
                )
        );
    }

}
