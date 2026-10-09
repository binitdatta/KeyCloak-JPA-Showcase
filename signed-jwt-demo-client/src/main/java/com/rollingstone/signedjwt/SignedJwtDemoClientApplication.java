package com.rollingstone.signedjwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. spring.main.web-application-type=none (see application.yml)
 * means this starts no embedded server — it runs DemoRunner's
 * CommandLineRunner logic once and exits, which is all a "generate an
 * assertion, get a token, use the token" demo needs.
 */
@SpringBootApplication
public class SignedJwtDemoClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignedJwtDemoClientApplication.class, args);
    }
}
