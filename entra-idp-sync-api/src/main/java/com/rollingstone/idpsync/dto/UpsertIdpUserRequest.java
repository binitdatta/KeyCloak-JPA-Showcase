package com.rollingstone.idpsync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Body for PUT /api/v1/idp-users/by-entra-object-id/{entraObjectId}.
 * entraObjectId itself is the path variable (it identifies the resource),
 * not part of the body -- matches REST PUT semantics for an upsert-by-key.
 *
 * Maps 1:1 onto the fields EntraUserSyncDao.upsertUser(...) currently takes
 * from ExtendedEntraCreateUserAuthenticator.persistProfile(...).
 */
public record UpsertIdpUserRequest(
        @NotBlank String keycloakUserId,
        @NotBlank @Email String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phoneNumber,
        String streetAddress,
        String city,
        String state,
        String postalCode,
        String country
) {
}
