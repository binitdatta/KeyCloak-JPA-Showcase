package com.rollingstone.idpsync.service;

import com.rollingstone.idpsync.domain.IdpUser;

public record UpsertResult(IdpUser user, boolean created) {
}
