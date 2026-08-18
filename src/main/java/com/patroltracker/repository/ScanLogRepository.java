package com.patroltracker.repository;

import com.patroltracker.model.ScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanLogRepository extends JpaRepository<ScanLog, String> {
    List<ScanLog> findByUserIdOrderByScanTimeDesc(String userId);
    List<ScanLog> findByDutyIdOrderByScanTimeDesc(String dutyId);
    List<ScanLog> findByCheckpointIdOrderByScanTimeDesc(String checkpointId);
    List<ScanLog> findAllByOrderByScanTimeDesc();
}
