package io.fulmicotone.fqueue.examples.models;

public class Intent {

    private String userId;
    private String intentId;

    public Intent(String userId, String intentId) {
        this.userId = userId;
        this.intentId = intentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIntentId() {
        return intentId;
    }

    public void setIntentId(String intentId) {
        this.intentId = intentId;
    }

    @Override
    public String toString() {
        return "Intent: userId:"+userId+", intentId:"+intentId;
    }
}
