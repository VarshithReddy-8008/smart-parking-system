package com.smartparking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherStatus {

    @Id
    private Integer id;

    @Column(name = "current_weather", nullable = false, length = 30)
    private String currentWeather;

    @Column(name = "temperature", nullable = false)
    private Double temperature;

    @Column(name = "weather_alert", length = 255)
    private String weatherAlert;

    @Column(name = "last_updated")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
