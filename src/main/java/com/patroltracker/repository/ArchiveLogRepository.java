package com.patroltracker.repository;

import com.patroltracker.model.ArchiveLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveLogRepository extends JpaRepository<ArchiveLog, String> {
    List<ArchiveLog> findAllByOrderByArchivedAtDesc();
}
