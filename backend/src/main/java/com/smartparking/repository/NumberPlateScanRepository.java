package com.smartparking.repository;

import com.smartparking.model.NumberPlateScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NumberPlateScanRepository extends JpaRepository<NumberPlateScan, Long> {

    // All scans today
    List<NumberPlateScan> findByScanTimeAfterOrderByScanTimeDesc(LocalDateTime after);

    // Most recent scan
    NumberPlateScan findTopByOrderByScanTimeDesc();

    // Count successful scans today
    long countByScanTimeBetweenAndScanSuccess(LocalDateTime from, LocalDateTime to, Boolean success);

    // Average confidence for successful scans today
    @Query("SELECT AVG(n.confidence) FROM NumberPlateScan n WHERE n.scanTime >= :from AND n.scanTime <= :to AND n.scanSuccess = true")
    Double avgConfidenceBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // History (last 50)
    List<NumberPlateScan> findTop50ByOrderByScanTimeDesc();
}
