package com.uav.common.websocket;

import java.time.Instant;

public class WebSocketMessage {
    private String topic;
    private Object data;
    private String timestamp;

    public WebSocketMessage() {
        this.timestamp = Instant.now().toString();
    }

    public WebSocketMessage(String topic, Object data) {
        this.topic = topic;
        this.data = data;
        this.timestamp = Instant.now().toString();
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
