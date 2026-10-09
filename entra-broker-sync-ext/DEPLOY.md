# Deploying entra-broker-sync-ext

## 1. Database
Run db/entra-sync-schema.sql against your keycloak_jpa_demo MySQL 8 schema
before any Entra login test.

## 2. Build
    cd entra-broker-sync-ext
    mvn clean package
    export KEYCLOAK_HOME=/Users/binitdatta/KeyCloak_Home/keycloak-26.2.5

## 3. Deploy to Keycloak
    cp target/rollingstone-entra-broker-sync-ext.jar $KEYCLOAK_HOME/providers/
    cp ~/.m2/repository/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar $KEYCLOAK_HOME/providers/
    cd $KEYCLOAK_HOME
    bin/kc.sh build

#  4. Start KeyCloak

    bin/kc.sh start-dev \
    --spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri=http://localhost:8092/ciba/notify \
    --https-certificate-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/kc-server-cert.pem \
    --https-certificate-key-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/kc-server-key.pem \
    --https-client-auth=request \
    --https-trust-store-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/truststore.p12 \
    --https-trust-store-password=changeit

## 4. Start Keycloak, then confirm registration
Authentication -> Flows -> any flow -> Add execution. The dropdown should
list "rollingstone Entra Create User (Ext)" and "rollingstone Entra Post-Login Role/Group
Sync". If either is missing, the META-INF/services file didn't make it into
the JAR -- verify with:
    jar tf target/rollingstone-entra-broker-sync-ext.jar | grep META-INF/services

## 5. Wire the flows
- First Login Flow: duplicate the built-in "first broker login" flow, name it
  rollingstone-entra-first-broker-login, swap its "Create User If Unique" step for
  "rollingstone Entra Create User (Ext)".
- Post Login Flow: create a new top-level flow rollingstone-entra-post-login with a
  single REQUIRED execution: "rollingstone Entra Post-Login Role/Group Sync".
- On the entra-saml Identity Provider's Advanced tab, set First Login Flow
  and Post Login Flow to the two flows above.

## 6. DB credentials
EntraFblConstants.java hardcodes JDBC_URL / JDBC_USER / JDBC_PASSWORD for
this training lab. Edit that file and rebuild if your MySQL credentials
differ from root/changeit on localhost:3306.

Temp. Password : Yuno0468
susan.smith@binitdattagmail.onmicrosoft.com