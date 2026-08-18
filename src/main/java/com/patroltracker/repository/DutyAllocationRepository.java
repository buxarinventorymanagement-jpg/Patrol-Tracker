package com.patroltracker.repository;

import com.patroltracker.model.DutyAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DutyAllocationRepository extends JpaRepository<DutyAllocation, String> {
    List<DutyAllocation> findByUserId(String userId);
    List<DutyAllocation> findByStatus(String status);
}
