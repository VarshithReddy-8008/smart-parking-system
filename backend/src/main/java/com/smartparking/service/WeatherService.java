package com.smartparking.service;

import com.smartparking.model.WeatherStatus;
import com.smartparking.repository.WeatherStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherStatusRepository weatherStatusRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WeatherStatus getWeather() {
        WeatherStatus status = weatherStatusRepository.findById(1)
                .orElse(null);

        // Fetch live weather if no status or if older than 15 minutes
        if (status == null || status.getLastUpdated() == null ||
                ChronoUnit.MINUTES.between(status.getLastUpdated(), LocalDateTime.now()) > 15) {
            
            try {
                // Hyderabad coordinates
                String url = "https://api.open-meteo.com/v1/forecast?latitude=17.3850&longitude=78.4867&current_weather=true";
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null && response.containsKey("current_weather")) {
                    Map<String, Object> currentWeather = (Map<String, Object>) response.get("current_weather");
                    double temperature = Double.parseDouble(currentWeather.get("temperature").toString());
                    int weathercode = Integer.parseInt(currentWeather.get("weathercode").toString());
                    
                    String weatherCondition = mapWeatherCode(weathercode);
                    String alert = determineAlert(weatherCondition);

                    if (status == null) {
                        status = WeatherStatus.builder().id(1).build();
                    }
                    status.setCurrentWeather(weatherCondition);
                    status.setTemperature(temperature);
                    status.setWeatherAlert(alert);
                    status.setLastUpdated(LocalDateTime.now());
                    
                    return weatherStatusRepository.save(status);
                }
            } catch (Exception e) {
                // Fallback to existing or default on error
                e.printStackTrace();
            }
        }

        if (status == null) {
            status = WeatherStatus.builder()
                    .id(1)
                    .currentWeather("Sunny")
                    .temperature(32.5)
                    .weatherAlert("Normal Conditions")
                    .lastUpdated(LocalDateTime.now())
                    .build();
            return weatherStatusRepository.save(status);
        }

        return status;
    }

    private String mapWeatherCode(int code) {
        if (code == 0) return "Sunny";
        if (code >= 1 && code <= 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 67) return "Rain";
        if (code >= 80 && code <= 82) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 85 && code <= 86) return "Snow";
        if (code >= 95 && code <= 99) return "Storm";
        return "Sunny";
    }

    private String determineAlert(String condition) {
        if (condition.equals("Rain") || condition.equals("Storm")) {
            return "Adverse Weather Alert";
        }
        return "Normal Conditions";
    }

    public WeatherStatus setWeather(String weather, Double temp, String alert) {
        WeatherStatus status = weatherStatusRepository.findById(1)
                .orElse(WeatherStatus.builder().id(1).build());
        
        status.setCurrentWeather(weather);
        if (temp != null) status.setTemperature(temp);
        status.setWeatherAlert(alert != null ? alert : "No active alerts");
        status.setLastUpdated(LocalDateTime.now());
        
        return weatherStatusRepository.save(status);
    }

    public String getRecommendationAdvice(String currentWeather) {
        if (currentWeather == null) return "Nearest available slot.";
        switch (currentWeather.toUpperCase()) {
            case "RAIN":
                return "Recommend Covered/Indoor parking to protect vehicle from rain.";
            case "STORM":
                return "Recommend Covered/Indoor parking to avoid storm hazards.";
            case "EXTREME HEAT":
                return "Recommend Shaded/Covered parking to reduce cabin heat build-up.";
            case "FOG":
                return "Recommend Covered/Indoor parking for maximum visibility and safety.";
            case "SUNNY":
            case "CLOUDY":
            default:
                return "Recommend nearest parking slots to reduce walking distance.";
        }
    }
}
