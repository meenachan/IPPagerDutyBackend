package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
