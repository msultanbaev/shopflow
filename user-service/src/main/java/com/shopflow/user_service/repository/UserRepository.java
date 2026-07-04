package com.shopflow.user_service.repository;

import com.shopflow.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
    boolean existsByEmail(String email);
}
