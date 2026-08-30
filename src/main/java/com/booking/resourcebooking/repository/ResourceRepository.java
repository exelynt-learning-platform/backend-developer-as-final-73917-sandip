package com.booking.resourcebooking.repository;

import com.booking.resourcebooking.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
