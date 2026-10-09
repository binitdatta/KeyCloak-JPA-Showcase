package com.rollingstone.demo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ONE-TO-ONE owner side:  Customer (1) <-> (1) CustomerProfile
 *   - mappedBy on this side; CustomerProfile owns the FK (customer_profiles.customer_id).
 * ONE-TO-MANY owner side: Customer (1) -> (many) Order
 *   - mappedBy on this side; Order owns the FK (orders.customer_id).
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "keycloak_sub", unique = true, length = 64)
    private String keycloakSub;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- 1:1 --------------------------------------------------------------
    // FetchType.LAZY on a 1:1 non-owning side requires bytecode enhancement
    // to be truly lazy; we accept EAGER-by-proxy berollingstoneor here for simplicity
    // and document it on the training page.
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY, optional = true)
    private CustomerProfile profile;

    // --- 1:many -------------------------------------------------------------
    @OneToMany(mappedBy = "customer", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    protected Customer() {
        // JPA requires a no-arg constructor
    }

    public Customer(String email, String firstName, String lastName) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void linkProfile(CustomerProfile profile) {
        this.profile = profile;
        profile.setCustomer(this);
    }

    public void addOrder(Order order) {
        this.orders.add(order);
        order.setCustomer(this);
    }

    // --- getters / setters (hand-written, no Lombok per project convention) --
    public Long getCustomerId() {
        return customerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getKeycloakSub() {
        return keycloakSub;
    }

    public void setKeycloakSub(String keycloakSub) {
        this.keycloakSub = keycloakSub;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public CustomerProfile getProfile() {
        return profile;
    }

    public List<Order> getOrders() {
        return orders;
    }
}
