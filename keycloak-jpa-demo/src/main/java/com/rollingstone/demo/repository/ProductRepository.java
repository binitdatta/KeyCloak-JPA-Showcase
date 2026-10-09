package com.rollingstone.demo.repository;

import com.rollingstone.demo.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAll();

    // Fetch both tags AND category eagerly — product-detail.html dereferences
    // product.category.categoryName as well as product.tags, and open-in-view
    // is false, so anything the view touches must be loaded here, in the query.
    @Query("select distinct p from Product p left join fetch p.tags left join fetch p.category where p.productId = :id")
    Optional<Product> findWithTags(Long id);

    List<Product> findByCategory_CategoryId(Long categoryId);

    @Query("select distinct p from Product p join p.tags t where t.tagName = :tagName")
    List<Product> findByTagName(String tagName);
}