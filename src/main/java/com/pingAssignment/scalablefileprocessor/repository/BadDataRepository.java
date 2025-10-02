package com.pingAssignment.scalablefileprocessor.repository;

import com.pingAssignment.scalablefileprocessor.model.BadData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadDataRepository extends JpaRepository<BadData, Long> {}