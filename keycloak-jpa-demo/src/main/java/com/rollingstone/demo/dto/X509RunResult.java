package com.rollingstone.demo.dto;

import java.util.Map;

public class X509RunResult {

    private boolean success;
    private String errorStep;
    private String errorMessage;

    private String certSubjectDn;
    private String certIssuerDn;
    private String certSerialNumber;
    private String certNotBefore;
    private String certNotAfter;

    private String accessToken;
    private Map<String, Object> accessTokenClaims;

    private String userinfo;

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

    public String getCertSubjectDn() {
        return certSubjectDn;
    }

    public void setCertSubjectDn(String certSubjectDn) {
        this.certSubjectDn = certSubjectDn;
    }

    public String getCertIssuerDn() {
        return certIssuerDn;
    }

    public void setCertIssuerDn(String certIssuerDn) {
        this.certIssuerDn = certIssuerDn;
    }

    public String getCertSerialNumber() {
        return certSerialNumber;
    }

    public void setCertSerialNumber(String certSerialNumber) {
        this.certSerialNumber = certSerialNumber;
    }

    public String getCertNotBefore() {
        return certNotBefore;
    }

    public void setCertNotBefore(String certNotBefore) {
        this.certNotBefore = certNotBefore;
    }

    public String getCertNotAfter() {
        return certNotAfter;
    }

    public void setCertNotAfter(String certNotAfter) {
        this.certNotAfter = certNotAfter;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Map<String, Object> getAccessTokenClaims() {
        return accessTokenClaims;
    }

    public void setAccessTokenClaims(Map<String, Object> accessTokenClaims) {
        this.accessTokenClaims = accessTokenClaims;
    }

    public String getUserinfo() {
        return userinfo;
    }

    public void setUserinfo(String userinfo) {
        this.userinfo = userinfo;
    }
}