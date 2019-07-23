package io.jenkins.plugins.rmacd.bitbucketNotification;

public class NotificationCheckedException extends Exception {
    public NotificationCheckedException(String message) {
        super(message);
    }

    public NotificationCheckedException(String message, Throwable e) {
        super(message, e);
    }
}
