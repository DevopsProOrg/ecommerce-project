package com.shop.ecommerce.repository;

import com.shop.ecommerce.model.ShopOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
    List<ShopOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ShopOrder> findAllByOrderByCreatedAtDesc();
}
