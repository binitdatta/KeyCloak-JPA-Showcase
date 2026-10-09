package com.rollingstone.idpsync.dto;

import java.time.LocalDate;

/**
 * Body for PATCH /api/v1/idp-users/{id} -- every field optional, only the
 * fields actually present are applied. Used for admin/manual corrections,
 * not by the Entra sync authenticators (they always go through the
 * upsert-by-entraObjectId endpoint instead).
 */
public record UpdateIdpUserProfileRequest(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phoneNumber,
        String streetAddress,
        String city,
        String state,
        String postalCode,
        String country,
        String status
) {
}
