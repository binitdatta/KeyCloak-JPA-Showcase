package com.rollingstone.demo.repository;

import com.rollingstone.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer_CustomerId(Long customerId);

    @Query("select distinct o from Order o left join fetch o.items i left join fetch i.product where o.orderId = :id")
    Optional<Order> findWithItems(Long id);
}
