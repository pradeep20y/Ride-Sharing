package com.ridesharing.project.repository;

import com.ridesharing.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Find all users by type
    List<User> findByUserType(String userType);

    // Check if email exists
    boolean existsByEmail(String email);
}
