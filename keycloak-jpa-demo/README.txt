KEYCLOAK + ENTRA SAML BROKERED SSO — TRAINING PAGE

Route: http://localhost:8080/training/keycloak-entra-brokered-sso

FILES:
- src/main/resources/templates/training/keycloak-entra-brokered-sso.html
- src/main/resources/static/images/azure-saml/oidc_and_saml_brokered_sso_flow.png
- src/main/java/com/example/keycloakjpa/controller/KeycloakEntraBrokeredSsoController.java
- src/main/resources/templates/fragments/navbar.html (example navbar from supplied project sample)
- NAVBAR-ADD.html (safe insertion snippet)

IMPORTANT: The package com.example.keycloakjpa.controller is a placeholder. Copy the Java controller to your existing Spring Boot controller package and EDIT its package declaration accordingly. Spring must component-scan it.

IMPORTANT: Your current navbar may be newer than the supplied navbar example. DO NOT overwrite your current navbar blindly. Merge the single menu entry in NAVBAR-ADD.html into your current Training dropdown. The packaged navbar is an optional reference only.

INSTALL RECOMMENDATION:
1. Extract HTML + PNG using commands supplied in the chat (not the sample navbar).
2. Copy/edit the Java controller package.
3. Merge NAVBAR-ADD.html into the CURRENT navbar.
4. Start Spring Boot and open the route above.

The image is a conceptual diagram and includes example endpoints; adapt interpretations to your deployed environment.
