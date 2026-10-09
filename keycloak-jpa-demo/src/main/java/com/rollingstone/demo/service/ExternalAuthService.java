package com.rollingstone.demo.service;

import com.rollingstone.demo.dto.ExternalAuthRequest;
import com.rollingstone.demo.dto.ExternalAuthResponse;
import com.rollingstone.demo.entity.Customer;
import com.rollingstone.demo.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Stand-in "legacy identity store" check for the First-Broker-Login demo.
 *
 * In a real system this would call an existing user directory (rollingstone's own
 * user table, an LDAP bind, etc). For the POC we treat "any customer whose
 * email exists in the customers table, with password == 'demo1234'" as
 * authenticated, purely so the Keycloak SPI flow has something real to call.
 * Swap validatePassword() for a real check (BCrypt against a stored hash,
 * an LDAP bind, a call to another service) before using this outside a demo.
 */
@Service
public class ExternalAuthService {

    private static final String DEMO_PASSWORD = "demo1234";

    private final CustomerRepository customerRepository;

    public ExternalAuthService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public ExternalAuthResponse authenticate(ExternalAuthRequest request) {
        Optional<Customer> maybeCustomer = customerRepository.findByEmail(request.getEmail());

        if (maybeCustomer.isEmpty() || !validatePassword(request.getPassword())) {
            return new ExternalAuthResponse(false, null, request.getEmail(), null, null, List.of(), List.of());
        }

        Customer customer = maybeCustomer.get();
        List<String> roles = defaultRolesFor(customer);
        List<String> groups = defaultGroupsFor(customer);

        return new ExternalAuthResponse(
                true,
                String.valueOf(customer.getCustomerId()),
                customer.getEmail(),
                customer.getFirstName(),
                customer.getLastName(),
                roles,
                groups
        );
    }

    private boolean validatePassword(String rawPassword) {
        return DEMO_PASSWORD.equals(rawPassword);
    }

    private List<String> defaultRolesFor(Customer customer) {
        // Every migrated customer gets the baseline "customer" realm role;
        // the Post-Login-Flow authenticator (Keycloak SPI) applies this.
        return List.of("customer");
    }

    private List<String> defaultGroupsFor(Customer customer) {
        return List.of("/customers/migrated");
    }
}
