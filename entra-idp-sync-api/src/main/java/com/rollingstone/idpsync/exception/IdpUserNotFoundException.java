package com.rollingstone.idpsync.exception;

public class IdpUserNotFoundException extends RuntimeException {

    public IdpUserNotFoundException(long idpUserId) {
        super("idp_user not found: id=" + idpUserId);
    }

    public IdpUserNotFoundException(String entraObjectId) {
        super("idp_user not found for entra_object_id=" + entraObjectId);
    }
}
