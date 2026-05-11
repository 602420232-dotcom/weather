package com.uav.common.audit;

public class AuditEntry {

    private String timestamp;
    private String username;
    private String operation;
    private String details;
    private String status;

    public AuditEntry() {
    }

    public AuditEntry(String timestamp, String username, String operation, String details, String status) {
        this.timestamp = timestamp;
        this.username = username;
        this.operation = operation;
        this.details = details;
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return timestamp + " | " + username + " | " + operation + " | " + details + " | " + status;
    }
}
