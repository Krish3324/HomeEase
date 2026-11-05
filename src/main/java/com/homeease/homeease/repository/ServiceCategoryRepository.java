package com.homeease.homeease.repository;

import com.homeease.homeease.model.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    Optional<ServiceCategory> findByNameIgnoreCase(String name);
}
