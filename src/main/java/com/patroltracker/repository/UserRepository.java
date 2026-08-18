package com.patroltracker.repository;

import com.patroltracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    List<User> findByRole(String role);
    List<User> findByStatus(String status);
}
