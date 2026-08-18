package com.patroltracker.repository;

import com.patroltracker.model.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckpointRepository extends JpaRepository<Checkpoint, String> {
    Optional<Checkpoint> findByQrCodeData(String qrCodeData);
}
