package com.rollingstone.demo.repository;

import com.rollingstone.demo.entity.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // EntityGraph avoids N+1 when the list page also needs the 1:1 profile.
    @EntityGraph(attributePaths = {"profile"})
    List<Customer> findAll();

    Optional<Customer> findByEmail(String email);

    @Query("select distinct c from Customer c left join fetch c.orders where c.customerId = :id")
    Optional<Customer> findWithOrders(Long id);
}
