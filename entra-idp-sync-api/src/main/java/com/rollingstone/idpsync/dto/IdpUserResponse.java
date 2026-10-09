package com.rollingstone.idpsync.dto;

import com.rollingstone.idpsync.domain.IdpUser;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record IdpUserResponse(
        Long id,
        String keycloakUserId,
        String entraObjectId,
        String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phoneNumber,
        String streetAddress,
        String city,
        String state,
        String postalCode,
        String country,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {
    public static IdpUserResponse from(IdpUser u) {
        return new IdpUserResponse(
                u.getId(), u.getKeycloakUserId(), u.getEntraObjectId(), u.getEmail(),
                u.getFirstName(), u.getLastName(), u.getDateOfBirth(), u.getPhoneNumber(),
                u.getStreetAddress(), u.getCity(), u.getState(), u.getPostalCode(), u.getCountry(),
                u.getStatus(), u.getCreatedAt(), u.getUpdatedAt(), u.getLastLoginAt()
        );
    }
}
