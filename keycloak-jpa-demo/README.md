# keycloak-jpa-demo

Spring Boot 3.5.9 reference app demonstrating:
1. Spring Data JPA over an ecommerce MySQL 8 schema — 1:1, 1:many, and many:many
   relationships with full annotations.
2. Keycloak 26 client authentication mechanisms — Signed JWT, X509 Certificate,
   Signed JWT with Client Secret, and Capability Config (Authorization Services,
   Device Grant, CIBA).
3. A custom Keycloak Client Authenticator SPI provider.
4. A custom First Broker Login flow that extends "Create User If Unique" to call
   this app's REST API as an external identity source, plus a Post-Login Flow
   step that provisions realm roles/groups.
5. A Bootstrap 5 "Electric Bright Blue" themed site with a Training dropdown —
   one page per concept, written as a YouTube recording script.

## Project layout

```
keycloak-jpa-demo/
  pom.xml                          Spring Boot app
  src/main/java/com/rollingstone/demo/     entities, repositories, controllers, security config
  src/main/resources/
    application.yml
    templates/                     Thymeleaf: navbar fragment, JPA pages, 8 training pages
    static/css/bright-blue.css     your uploaded theme
  db/
    schema.sql                     DBA-owned DDL for ecommerce_demo
    seed-data.sql                  seed rows exercising every relationship
  keycloak/
    realm-export.json              realm with 6 clients + custom flow wiring
  custom-providers/
    client-authenticator/          Keycloak SPI: custom Client Authenticator
    first-broker-login/            Keycloak SPI: extended Create-User-If-Unique + Post-Login provisioning
```

## 1. Database setup

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed-data.sql
```

Default local credentials assumed in `application.yml`: `root` / `localroot`
against `jdbc:mysql://localhost:3306/ecommerce_demo`.

## 2. Keycloak setup

Requires Keycloak 26.x running locally (default `http://localhost:8080`).

```bash
# from the Keycloak distribution directory
bin/kc.sh start-dev
```

Import the realm (admin console: **Create Realm → Browse → select
`keycloak/realm-export.json`**, or via the Admin CLI/REST API). This creates:

- `jpa-demo-web` — the Thymeleaf app's own confidential client (client-secret)
- `signed-jwt-client` — Episode A (`client-jwt`)
- `x509-cert-client` — Episode B (`client-x509`)
- `signed-jwt-secret-client` — Episode C (`client-secret-jwt`)
- `capability-demo-client` — Episode D (Authorization Services + Device Grant + CIBA on)
- `rollingstone-secret-header-client` — Episode E1, authenticated by the custom provider below
- an identity provider (`rollingstone-legacy-broker`) and the `rollingstone-first-broker-login` flow
  definition used in Episodes E2/E3 (placeholder OIDC broker config — point
  `authorizationUrl` / `tokenUrl` / `userInfoUrl` at a real broker before using it live)

**X509 and Signed JWT need real key material** the JSON export can't carry —
generate a keypair/certificate and update `jwks.url` / `x509.subjectdn` after
import, per the steps on the `/training/signed-jwt` and `/training/x509-certificate`
pages.

## 3. Build and run the Spring Boot app

```bash
mvn -f pom.xml clean spring-boot:run
```

App comes up on `http://localhost:8091`. `/` is public; `/jpa/**` requires a
Keycloak login (redirects to `/oauth2/authorization/keycloak`); `/training/**`
is public so it works as a shareable recording reference; `/api/auth/**` is
public (called server-to-server by the Keycloak SPI, see below).

## 4. Build and deploy the two Keycloak SPI provider JARs

Each provider module is **its own Maven project** — Keycloak SPI providers
deploy as standalone JARs dropped into Keycloak's `providers/` directory, never
bundled inside the Spring Boot app.

```bash
cd custom-providers/client-authenticator
mvn clean package
cp target/rollingstone-custom-client-authenticator.jar $KEYCLOAK_HOME/providers/

cd ../first-broker-login
mvn clean package
cp target/rollingstone-first-broker-login-ext.jar $KEYCLOAK_HOME/providers/

cd $KEYCLOAK_HOME
bin/kc.sh build
bin/kc.sh start-dev
```

After the rebuild:
- **Rollingstone Secret Header** appears in every client's Credentials → Client
  Authenticator dropdown (used by `rollingstone-secret-header-client` in the realm export).
- **rollingstone Create User If Unique (Ext)** and **Rollingstone Post-Login Role/Group
  Provisioning** appear as swappable executions when you duplicate the
  built-in *First broker login* flow (Keycloak's built-in flows are read-only,
  so duplicate it first, then swap executions — the realm export already
  defines the resulting flow as `rollingstone-first-broker-login`).

> The Maven POMs for both SPI modules compile against
> `org.keycloak:keycloak-server-spi*` / `keycloak-services` 26.0.7 with
> `provided` scope (these classes ship inside the Keycloak server itself).
> A couple of `AbstractIdpAuthenticator` hook method names have shifted
> across Keycloak minor versions — if `mvn package` reports a signature
> mismatch, check the installed Keycloak version's javadoc for the exact
> `authenticateImpl` / `actionImpl` signatures and adjust the override.

## 5. Walk through the training site

Start at `http://localhost:8091/training` — each of the 8 pages is a
self-contained script (goal, prerequisites, on-screen steps, wrap-up line)
matching the episode list above, cross-linked next/previous.

## 6. Walk through the JPA pages

`http://localhost:8091/jpa/customers` and `http://localhost:8091/jpa/products`
render the live entity graph: 1:1 profile/payment, 1:many orders/items,
many:many product tags — each page links back to the matching annotation
in `/training/jpa-relationships`.

``` 
curl -X POST http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=capability-demo-client" \
  -d "client_secret=88x3Wm0K9me6fEGCwRzMgypc4bsNL4EE"
  
 curl -X POST http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/auth/device \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=capability-demo-client" \
  -d "client_secret=88x3Wm0K9me6fEGCwRzMgypc4bsNL4EE"
  
 Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ curl -X POST http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/auth/device \
>   -H "Content-Type: application/x-www-form-urlencoded" \
>   -d "client_id=capability-demo-client" \
>   -d "client_secret=88x3Wm0K9me6fEGCwRzMgypc4bsNL4EE"
{"device_code":"HYuO4dhusIu8SQnhpOHSqXuXd4UFpKCwTYTZp6zeO-c","user_code":"YDQB-WFPT","verification_uri":"http://localhost:8080/realms/jpa-demo-realm/device","verification_uri_complete":"http://localhost:8080/realms/jpa-demo-realm/device?user_code=YDQB-WFPT","expires_in":600,"interval":5}Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ 

curl -X POST http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
  -d "device_code=HYuO4dhusIu8SQnhpOHSqXuXd4UFpKCwTYTZp6zeO-c" \
  -d "client_id=capability-demo-client" \
  -d "client_secret=88x3Wm0K9me6fEGCwRzMgypc4bsNL4EE"
  
  Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ curl -X POST http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/auth/device \
>   -H "Content-Type: application/x-www-form-urlencoded" \
>   -d "client_id=capability-demo-client" \
>   -d "client_secret=88x3Wm0K9me6fEGCwRzMgypc4bsNL4EE"
{"device_code":"HYuO4dhusIu8SQnhpOHSqXuXd4UFpKCwTYTZp6zeO-c","user_code":"YDQB-WFPT","verification_uri":"http://localhost:8080/realms/jpa-demo-realm/device","verification_uri_complete":"http://localhost:8080/realms/jpa-demo-realm/device?user_code=YDQB-WFPT","expires_in":600,"interval":5}Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ 
Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ 
Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ curl -X POST http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/token \
>   -H "Content-Type: application/x-www-form-urlencoded" \
>   -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
>   -d "device_code=HYuO4dhusIu8SQnhpOHSqXuXd4UFpKCwTYTZp6zeO-c" \
>   -d "client_id=capability-demo-client" \
>   -d "client_secret=88x3Wm0K9me6fEGCwRzMgypc4bsNL4EE"
{"access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI3SC10VGIwOW82X3QyQ2FyZUdLRXh0Y0ZoRkNRYmxVcVpVcTZEYzc4RDc0In0.eyJleHAiOjE3ODk5MTE2MDksImlhdCI6MTc4OTkxMTMwOSwiYXV0aF90aW1lIjoxNzg5OTExMjczLCJqdGkiOiJvbnJ0ZGc6MjIxNzhlZDctODk5ZS00NjBmLTgwM2QtYmRhY2ZhZDYxNWUxIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9qcGEtZGVtby1yZWFsbSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJmYzhjNDgyMC03ZWRhLTQzOGItYjFjOC0zNWExYTlhNjEyYTQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJjYXBhYmlsaXR5LWRlbW8tY2xpZW50Iiwic2lkIjoiZDVjNWEwNTktNGFkYy00YTY2LTk3YjgtNzE2MThkZjY2Yjc1IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsImFkbWluIiwidW1hX2F1dGhvcml6YXRpb24iLCJkZWZhdWx0LXJvbGVzLWpwYS1kZW1vLXJlYWxtIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiQWxpY2UgQmVudGxleSIsInByZWZlcnJlZF91c2VybmFtZSI6ImFsaWNlIiwiZ2l2ZW5fbmFtZSI6IkFsaWNlIiwiZmFtaWx5X25hbWUiOiJCZW50bGV5IiwiZW1haWwiOiJhbGljZS5iZW50bGV5QGdtYWlsLmNvbSJ9.qvTHW6QeqdSZ-ygYdGdrxfDpQzj7cUr5hWGR5kXNhxZucqOCwPOHUjr90GRCmhgw5YiOh_cjjHJtgfEQjeS6bw9-Avd_Naef1KDJFIqIXQ9gO-MzRKY4aafR63qL6ahssFu5U82vbtJaP-6k0MY1wivHxTOZbXkteH2I-1XipHpMPmimQB3qk5wifFOuIkiie4WsQactdXIGfuqhREALCG_AS--_GTZbHDNo_M4T_BW0S2tr_qb79G-e3_eVWqD4sLn11KFclPIwji6zMLi4r79mYenVppSVTR38zvRwVD9juHH6Auywx_j8B7W8mP_g8aEJ-MfXCxdQBeNnrPMIWQ","expires_in":300,"refresh_expires_in":1800,"refresh_token":"eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI0YjY2Y2M3MC1jODVlLTQ0NjEtYTNiYy05ZjlhODY2MDU1NjQifQ.eyJleHAiOjE3ODk5MTMxMDksImlhdCI6MTc4OTkxMTMwOSwianRpIjoiOTQzZGExZDgtY2EyMy00ZWE4LTg1NTAtZDhiMmE2YzllMTFlIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9qcGEtZGVtby1yZWFsbSIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvanBhLWRlbW8tcmVhbG0iLCJzdWIiOiJmYzhjNDgyMC03ZWRhLTQzOGItYjFjOC0zNWExYTlhNjEyYTQiLCJ0eXAiOiJSZWZyZXNoIiwiYXpwIjoiY2FwYWJpbGl0eS1kZW1vLWNsaWVudCIsInNpZCI6ImQ1YzVhMDU5LTRhZGMtNGE2Ni05N2I4LTcxNjE4ZGY2NmI3NSIsInNjb3BlIjoiYmFzaWMgYWNyIGVtYWlsIHNlcnZpY2VfYWNjb3VudCB3ZWItb3JpZ2lucyByb2xlcyBwcm9maWxlIn0.MFxQOc9TBFuthqCRaWyYUzwVKsCgvqjCQtaUY-PLuyRcy0JcZvvq4_fT8aVbss0RMYG_ZUF5NIDQth2HMu16Kg","token_type":"Bearer","not-before-policy":0,"session_state":"d5c5a059-4adc-4a66-97b8-71618df66b75","scope":"email profile"}Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ 

curl -X POST http://localhost:8080/realms/jpa-demo-realm/protocol/openid-connect/ext/ciba/auth \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=capability-demo-client" \
  -d "client_secret=88x3Wm0K9me6fEGCwRzMgypc4bsNL4EE" \
  -d "login_hint=alice" \
  -d "scope=openid"
 
 Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ cd /Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo
Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ mkdir -p mtls
Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ cd mtls
Binits-MacBook-Pro:mtls binitdatta$ pwd
/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls
Binits-MacBook-Pro:mtls binitdatta$ openssl genrsa -out ca-key.pem 4096
Binits-MacBook-Pro:mtls binitdatta$ openssl req -x509 -new -nodes -key ca-key.pem -sha256 -days 3650 -out ca-cert.pem -subj "/CN=jpa-demo-lab-ca"
Binits-MacBook-Pro:mtls binitdatta$ openssl genrsa -out kc-server-key.pem 2048
Binits-MacBook-Pro:mtls binitdatta$ openssl req -new -key kc-server-key.pem -out kc-server.csr -subj "/CN=localhost"
Binits-MacBook-Pro:mtls binitdatta$ openssl x509 -req -in kc-server.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
>   -out kc-server-cert.pem -days 825 -sha256 \
>   -extfile <(printf "subjectAltName=DNS:localhost,IP:127.0.0.1")
Certificate request self-signature ok
subject=CN=localhost
Binits-MacBook-Pro:mtls binitdatta$ openssl genrsa -out x509-demo-client-key.pem 2048
Binits-MacBook-Pro:mtls binitdatta$ openssl req -new -key x509-demo-client-key.pem -out x509-demo-client.csr -subj "/CN=x509-demo-client"
Binits-MacBook-Pro:mtls binitdatta$ openssl x509 -req -in x509-demo-client.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
>   -out x509-demo-client-cert.pem -days 825 -sha256
Certificate request self-signature ok
subject=CN=x509-demo-client
Binits-MacBook-Pro:mtls binitdatta$ openssl pkcs12 -export -inkey x509-demo-client-key.pem -in x509-demo-client-cert.pem \
>   -certfile ca-cert.pem -name x509-demo-client -out x509-demo-client-keystore.p12 -passout pass:changeit
Binits-MacBook-Pro:mtls binitdatta$ 
Binits-MacBook-Pro:mtls binitdatta$ keytool -importcert -alias jpa-demo-lab-ca -file ca-cert.pem \
>   -keystore truststore.p12 -storetype PKCS12 -storepass changeit -noprompt
Certificate was added to keystore
Binits-MacBook-Pro:mtls binitdatta$ 



 bin/kc.sh start-dev --spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri=http://localhost:8092/ciba/notify
 
 cd /Users/binitdatta/KeyCloak_Home/keycloak-26.2.5
bin/kc.sh start-dev \
  --spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri=http://localhost:8092/ciba/notify \
  --https-certificate-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/kc-server-cert.pem \
  --https-certificate-key-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/kc-server-key.pem \
  --https-client-auth=request
  
Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ curl -kv https://localhost:8443/realms/jpa-demo-realm 2>&1 | grep -E "SSL connection|subject|HTTP/"
* SSL connection using TLSv1.3 / AEAD-AES256-GCM-SHA384 / [blank] / UNDEF
*  subject: CN=localhost
* using HTTP/2
* [HTTP/2] [1] OPENED stream for https://localhost:8443/realms/jpa-demo-realm
* [HTTP/2] [1] [:method: GET]
* [HTTP/2] [1] [:scheme: https]
* [HTTP/2] [1] [:authority: localhost:8443]
* [HTTP/2] [1] [:path: /realms/jpa-demo-realm]
* [HTTP/2] [1] [user-agent: curl/8.7.1]
* [HTTP/2] [1] [accept: */*]
> GET /realms/jpa-demo-realm HTTP/2
< HTTP/2 200 
Binits-MacBook-Pro:keycloak-jpa-demo binitdatta$ 

cd /Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls

openssl genrsa -out x509-cert-client-key.pem 2048
openssl req -new -key x509-cert-client-key.pem -out x509-cert-client.csr -subj "/CN=x509-cert-client"
openssl x509 -req -in x509-cert-client.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial \
  -out x509-cert-client-cert.pem -days 825 -sha256

openssl pkcs12 -export -inkey x509-cert-client-key.pem -in x509-cert-client-cert.pem \
  -certfile ca-cert.pem -name x509-cert-client -out x509-cert-client-keystore.p12 -passout pass:changeit

cd /Users/binitdatta/KeyCloak_Home/keycloak-26.2.5
bin/kc.sh start-dev \
  --spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri=http://localhost:8092/ciba/notify \
  --https-certificate-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/kc-server-cert.pem \
  --https-certificate-key-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/kc-server-key.pem \
  --https-client-auth=request \
  --https-trust-store-file=/Users/binitdatta/Development/KeyCloak-JPA-Showcase/keycloak-jpa-demo/mtls/truststore.p12 \
  --https-trust-store-password=changeit
  
``` 

java -Djavax.net.ssl.trustStore=mtls/truststore.p12      -Djavax.net.ssl.trustStorePassword=changeit      -Djavax.net.ssl.trustStoreType=PKCS12      -jar target/keycloak-jpa-demo-1.0.0.jar

git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin https://github.com/binitdatta/KeyCloak-JPA-Showcase.git
git push -u origin main

```