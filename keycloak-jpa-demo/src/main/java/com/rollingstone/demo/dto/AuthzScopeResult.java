package com.rollingstone.demo.dto;

import java.util.Map;

public class AuthzScopeResult {

    private String scope;
    private String permissionRequested;
    private boolean granted;
    private int httpStatus;
    private String rawResponseBody;
    private String rptToken;
    private Map<String, Object> authorizationClaims;
    private String errorMessage;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getPermissionRequested() {
        return permissionRequested;
    }

    public void setPermissionRequested(String permissionRequested) {
        this.permissionRequested = permissionRequested;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getRawResponseBody() {
        return rawResponseBody;
    }

    public void setRawResponseBody(String rawResponseBody) {
        this.rawResponseBody = rawResponseBody;
    }

    public String getRptToken() {
        return rptToken;
    }

    public void setRptToken(String rptToken) {
        this.rptToken = rptToken;
    }

    public Map<String, Object> getAuthorizationClaims() {
        return authorizationClaims;
    }

    public void setAuthorizationClaims(Map<String, Object> authorizationClaims) {
        this.authorizationClaims = authorizationClaims;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}