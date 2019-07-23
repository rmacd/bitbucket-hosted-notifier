package io.jenkins.plugins.rmacd.bitbucketNotification.models;

import net.sf.json.JSONSerializer;

import javax.annotation.Nonnull;

public class NotificationModel {

    private State state;
    private String url;
    private String key;
    private String name;

    public State getState() {
        return state;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public enum State {
        INPROGRESS, SUCCESSFUL, FAILED
    }

    public NotificationModel(@Nonnull State state, @Nonnull String url, @Nonnull String key, String name) {
        this.state = state;
        this.url = url;
        this.key = key;
        this.name = name;
    }

    @Override
    public String toString() {
        return JSONSerializer.toJSON(this).toString();
    }
}
