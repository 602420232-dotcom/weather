package com.uav.common.exception;

public class PythonExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private final String scriptName;

    public PythonExecutionException(String scriptName, String message) {
        super(message);
        this.scriptName = scriptName;
    }

    public PythonExecutionException(String scriptName, String message, Throwable cause) {
        super(message, cause);
        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return scriptName;
    }
}
