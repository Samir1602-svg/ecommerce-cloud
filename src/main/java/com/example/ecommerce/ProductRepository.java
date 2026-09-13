package com.example.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity - :qty WHERE id = :productId AND stock_quantity >= :qty", nativeQuery = true)
    int decreaseStock(@Param("productId") Long productId, @Param("qty") Integer qty);
}