package com.pingAssignment.scalablefileprocessor.repository;

import com.pingAssignment.scalablefileprocessor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}