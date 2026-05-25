package com.example.mpct.repository;

import com.example.mpct.model.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByRuc(String ruc);
    Optional<UserProfile> findByUserId(UUID userId);
}
