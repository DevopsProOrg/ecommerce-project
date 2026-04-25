package com.shop.ecommerce.repository;

import com.shop.ecommerce.model.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
}
