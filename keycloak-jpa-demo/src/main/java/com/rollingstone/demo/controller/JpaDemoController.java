package com.rollingstone.demo.controller;

import com.rollingstone.demo.entity.Customer;
import com.rollingstone.demo.entity.Order;
import com.rollingstone.demo.entity.Product;
import com.rollingstone.demo.repository.CustomerRepository;
import com.rollingstone.demo.repository.OrderRepository;
import com.rollingstone.demo.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Renders the ecommerce entity graph so the relationship types are visible
 * on screen, not just in the annotations:
 *   /jpa/customers          -> list (each row shows its 1:1 profile)
 *   /jpa/customers/{id}     -> detail (1:many orders)
 *   /jpa/orders/{id}        -> detail (1:many items, 1:1 payment)
 *   /jpa/products           -> list (many:one category, many:many tags)
 */
@Controller
public class JpaDemoController {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public JpaDemoController(CustomerRepository customerRepository,
                              OrderRepository orderRepository,
                              ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/jpa/customers")
    public String listCustomers(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        return "jpa/customers";
    }

    @GetMapping("/jpa/customers/{id}")
    public String customerDetail(@PathVariable Long id, Model model) {
        Customer customer = customerRepository.findWithOrders(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        model.addAttribute("customer", customer);
        return "jpa/customer-detail";
    }

    @GetMapping("/jpa/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        Order order = orderRepository.findWithItems(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        model.addAttribute("order", order);
        return "jpa/order-detail";
    }

    @GetMapping("/jpa/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "jpa/products";
    }

    @GetMapping("/jpa/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findWithTags(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        model.addAttribute("product", product);
        return "jpa/product-detail";
    }
}
