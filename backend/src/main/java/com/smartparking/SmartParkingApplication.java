package com.smartparking;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SmartParkingApplication {

    @PostConstruct
    public void init() {
        // Enforce Asia/Kolkata (IST, UTC+5:30) timezone globally across JVM and database
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(SmartParkingApplication.class, args);
    }
}
