package com.shop.ecommerce.repository;

import com.shop.ecommerce.model.MonitoringAlertEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringAlertEventRepository extends JpaRepository<MonitoringAlertEvent, Long> {

    Page<MonitoringAlertEvent> findAllByOrderByReceivedAtDesc(Pageable pageable);
}
