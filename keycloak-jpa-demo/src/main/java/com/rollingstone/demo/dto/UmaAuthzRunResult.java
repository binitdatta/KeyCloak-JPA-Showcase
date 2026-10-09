package com.rollingstone.demo.dto;

import java.util.List;

public class UmaAuthzRunResult {

    private boolean success;
    private String errorStep;
    private String errorMessage;
    private List<AuthzScopeResult> scopeResults;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorStep() {
        return errorStep;
    }

    public void setErrorStep(String errorStep) {
        this.errorStep = errorStep;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<AuthzScopeResult> getScopeResults() {
        return scopeResults;
    }

    public void setScopeResults(List<AuthzScopeResult> scopeResults) {
        this.scopeResults = scopeResults;
    }
}