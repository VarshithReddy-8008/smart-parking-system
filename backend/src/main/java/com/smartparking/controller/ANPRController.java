package com.smartparking.controller;

import com.smartparking.dto.AnprScanRequest;
import com.smartparking.dto.AnprScanResponse;
import com.smartparking.dto.AnprStatsResponse;
import com.smartparking.model.NumberPlateScan;
import com.smartparking.service.ANPRService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ANPRController — REST endpoints for Automatic Number Plate Recognition.
 * All existing /api/parking endpoints remain untouched.
 */
@RestController
@RequestMapping("/api/anpr")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ANPRController {

    private final ANPRService anprService;

    /**
     * POST /api/anpr/scan
     * Accepts a base64 image and runs OCR. Returns detected vehicle number and confidence.
     */
    @PostMapping("/scan")
    public ResponseEntity<AnprScanResponse> scan(@RequestBody AnprScanRequest request) {
        if (request.getImageBase64() == null || request.getImageBase64().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    AnprScanResponse.builder()
                            .success(false)
                            .message("No image provided")
                            .build()
            );
        }
        AnprScanResponse response = anprService.processScan(request.getImageBase64());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/anpr/confirm
     * User has confirmed the plate number (possibly edited). Updates the DB record.
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@RequestBody Map<String, Object> payload) {
        Long scanId = payload.containsKey("scanId") ? Long.parseLong(payload.get("scanId").toString()) : null;
        String vehicleNumber = payload.containsKey("vehicleNumber") ? payload.get("vehicleNumber").toString() : "";
        if (scanId != null && !vehicleNumber.isEmpty()) {
            anprService.confirmScan(scanId, vehicleNumber);
        }
        return ResponseEntity.ok(Map.of("status", "confirmed"));
    }

    /**
     * GET /api/anpr/stats
     * Returns today's scan statistics for the owner dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<AnprStatsResponse> getStats() {
        return ResponseEntity.ok(anprService.getStats());
    }

    /**
     * GET /api/anpr/history
     * Returns the last 50 scan records.
     */
    @GetMapping("/history")
    public ResponseEntity<List<NumberPlateScan>> getHistory() {
        return ResponseEntity.ok(anprService.getHistory());
    }
}
