package com.smartparking.repository;

import com.smartparking.model.WeatherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherStatusRepository extends JpaRepository<WeatherStatus, Integer> {
}
