package com.smartparking.service;

import com.smartparking.dto.AnprScanResponse;
import com.smartparking.dto.AnprStatsResponse;
import com.smartparking.model.NumberPlateScan;
import com.smartparking.repository.NumberPlateScanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ANPRService — orchestrates OCR scanning, persists results, and provides analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ANPRService {

    private final OCRService ocrService;
    private final NumberPlateScanRepository scanRepository;

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    /**
     * Receives a base64 image, runs OCR, saves result to DB, returns response.
     */
    public AnprScanResponse processScan(String base64Image) {
        // Run OCR
        OCRService.OcrResult ocrResult = ocrService.processBase64Image(base64Image);

        // Persist scan record
        NumberPlateScan scan = NumberPlateScan.builder()
                .vehicleNumber(ocrResult.vehicleNumber)
                .confidence(ocrResult.confidence)
                .imagePath(ocrResult.imagePath)
                .scanTime(LocalDateTime.now())
                .scanSuccess(ocrResult.success)
                .confirmedByUser(false)
                .build();

        scan = scanRepository.save(scan);

        return AnprScanResponse.builder()
                .scanId(scan.getId())
                .success(ocrResult.success)
                .vehicleNumber(ocrResult.vehicleNumber)
                .confidence(ocrResult.confidence)
                .message(ocrResult.message)
                .imagePath(ocrResult.imagePath)
                .build();
    }

    /**
     * User has confirmed (and possibly edited) the vehicle number.
     * Update the record in DB.
     */
    public void confirmScan(Long scanId, String finalVehicleNumber) {
        scanRepository.findById(scanId).ifPresent(scan -> {
            scan.setFinalVehicleNumber(finalVehicleNumber.trim().toUpperCase());
            scan.setConfirmedByUser(true);
            scanRepository.save(scan);
        });
    }

    /**
     * Aggregated ANPR statistics for the owner dashboard.
     */
    public AnprStatsResponse getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long todayScans = scanRepository.countByScanTimeBetweenAndScanSuccess(startOfDay, endOfDay, true)
                + scanRepository.countByScanTimeBetweenAndScanSuccess(startOfDay, endOfDay, false);
        long todaySuccessful = scanRepository.countByScanTimeBetweenAndScanSuccess(startOfDay, endOfDay, true);

        double successRate = todayScans > 0 ? (double) todaySuccessful / todayScans * 100 : 0;

        Double avgConf = scanRepository.avgConfidenceBetween(startOfDay, endOfDay);
        double avgConfidence = avgConf != null ? avgConf : 0.0;

        NumberPlateScan lastScan = scanRepository.findTopByOrderByScanTimeDesc();
        String lastVehicle = (lastScan != null && lastScan.getVehicleNumber() != null) ? lastScan.getVehicleNumber() : "—";
        String lastTime = (lastScan != null && lastScan.getScanTime() != null) ? lastScan.getScanTime().format(DISPLAY_FORMAT) : "—";

        return AnprStatsResponse.builder()
                .todayScans(todayScans)
                .todaySuccessful(todaySuccessful)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .avgConfidence(Math.round(avgConfidence * 10.0) / 10.0)
                .lastScannedVehicle(lastVehicle)
                .lastScanTime(lastTime)
                .build();
    }

    /**
     * Get last 50 scan records for history table.
     */
    public List<NumberPlateScan> getHistory() {
        return scanRepository.findTop50ByOrderByScanTimeDesc();
    }
}
