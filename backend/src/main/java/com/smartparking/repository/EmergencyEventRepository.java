package com.smartparking.repository;

import com.smartparking.model.EmergencyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {
    List<EmergencyEvent> findByStatus(String status);
}
