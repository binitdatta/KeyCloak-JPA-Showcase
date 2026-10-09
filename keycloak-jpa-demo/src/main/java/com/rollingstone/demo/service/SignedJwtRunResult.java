package com.rollingstone.demo.dto;

import java.util.Map;

/**
 * Everything the "Try it live" panel on /training/signed-jwt needs to render
 * the private_key_jwt flow in the browser. Populated incrementally by
 * SignedJwtDemoService as each step completes, so a failure partway through
 * still returns whatever was gathered plus which step broke, rather than a
 * bare 500 with no context.
 */
public class SignedJwtRunResult {

    private boolean success;
    private String errorStep;
    private String errorMessage;

    private String signedAssertion;
    private Map<String, Object> assertionClaims;

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

    public String getSignedAssertion() {
        return signedAssertion;
    }

    public void setSignedAssertion(String signedAssertion) {
        this.signedAssertion = signedAssertion;
    }

    public Map<String, Object> getAssertionClaims() {
        return assertionClaims;
    }

    public void setAssertionClaims(Map<String, Object> assertionClaims) {
        this.assertionClaims = assertionClaims;
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