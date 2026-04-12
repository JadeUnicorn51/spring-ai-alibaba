package com.alibaba.cloud.ai.studio.multitenant.shared.security;

import java.util.Optional;

public interface AuthTokenVerifier {

    Optional<AuthClaims> verify(String bearerToken);

}
