package io.jenkins.plugins.rmacd.bitbucketNotification;

public class NotificationRuntimeException extends RuntimeException {
    NotificationRuntimeException(String invalid_action) {
        super(invalid_action);
    }
}
