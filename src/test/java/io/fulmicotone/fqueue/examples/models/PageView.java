package io.fulmicotone.fqueue.examples.models;

public class PageView {

    private String url;
    private String userId;

    public PageView(String url, String userId) {
        this.url = url;
        this.userId = userId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    @Override
    public String toString() {
        return "PageView: domain:"+url+", userId:"+userId;
    }
}
