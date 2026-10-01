/**
 * anpr.js — Automatic Number Plate Recognition Frontend Module
 * Manages webcam access, image capture, and OCR API communication.
 * Works alongside the existing app.js without modifying it.
 */

(function () {
    'use strict';

    const CONFIDENCE_THRESHOLD = 70.0;

    // DOM element references
    const scanBtn       = document.getElementById('anpr-scan-btn');
    const modalOverlay  = document.getElementById('anpr-modal-overlay');
    const closeBtn      = document.getElementById('anpr-close-btn');
    const cancelBtn     = document.getElementById('anpr-cancel-btn');
    const captureBtn    = document.getElementById('anpr-capture-btn');
    const useBtn        = document.getElementById('anpr-use-btn');
    const video         = document.getElementById('anpr-video');
    const canvas        = document.getElementById('anpr-canvas');
    const resultBox     = document.getElementById('anpr-result-box');
    const resultInput   = document.getElementById('anpr-result-input');
    const confLabel     = document.getElementById('anpr-confidence-label');
    const scanningOverlay = document.getElementById('anpr-scanning-overlay');
    const feedbackDiv   = document.getElementById('anpr-feedback');
    const plateInput    = document.getElementById('input-plate');

    let mediaStream = null;
    let lastScanId  = null;

    // ─────────────────────────────────────────────
    // Open the camera modal
    // ─────────────────────────────────────────────
    function openModal() {
        modalOverlay.style.display = 'flex';
        resultBox.style.display = 'none';
        scanningOverlay.style.display = 'none';
        captureBtn.disabled = false;

        navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } } })
            .then(stream => {
                mediaStream = stream;
                video.srcObject = stream;
            })
            .catch(err => {
                closeModal();
                showFeedback('Camera unavailable: ' + err.message + '. Please enter the plate manually.', 'warning');
            });
    }

    // ─────────────────────────────────────────────
    // Close and stop the camera
    // ─────────────────────────────────────────────
    function closeModal() {
        modalOverlay.style.display = 'none';
        if (mediaStream) {
            mediaStream.getTracks().forEach(t => t.stop());
            mediaStream = null;
        }
        video.srcObject = null;
    }

    // ─────────────────────────────────────────────
    // Capture current video frame as JPEG base64
    // ─────────────────────────────────────────────
    function captureFrame() {
        canvas.width  = video.videoWidth  || 640;
        canvas.height = video.videoHeight || 480;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        return canvas.toDataURL('image/jpeg', 0.9);
    }

    // ─────────────────────────────────────────────
    // Send captured image to backend OCR API
    // ─────────────────────────────────────────────
    async function processCapture() {
        captureBtn.disabled = true;
        scanningOverlay.style.display = 'block';
        resultBox.style.display = 'none';

        const base64Image = captureFrame();

        // Resolve API base (reuse API_BASE from app.js if available)
        const apiBase = (typeof API_BASE !== 'undefined' ? API_BASE : 'http://localhost:8080/api/parking')
            .replace('/api/parking', '') + '/api/anpr';

        try {
            const response = await fetch(apiBase + '/scan', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ imageBase64: base64Image })
            });

            scanningOverlay.style.display = 'none';

            if (!response.ok) {
                throw new Error('Server returned ' + response.status);
            }

            const data = await response.json();
            lastScanId = data.scanId;
            showResult(data);

            // Auto-fill and close modal on successful detection
            if (data.success && data.vehicleNumber) {
                usePlate();
            }

        } catch (err) {
            scanningOverlay.style.display = 'none';
            captureBtn.disabled = false;
            showResultError('OCR failed: ' + err.message + '. Please enter plate manually.');
        }
    }

    // ─────────────────────────────────────────────
    // Display OCR result in modal
    // ─────────────────────────────────────────────
    function showResult(data) {
        resultBox.style.display = 'block';
        captureBtn.disabled = false;

        if (data.success && data.vehicleNumber) {
            resultInput.value = data.vehicleNumber;

            if (data.confidence >= CONFIDENCE_THRESHOLD) {
                confLabel.innerHTML = `<span style="color:var(--neon-green);">✓ Confidence: ${data.confidence.toFixed(1)}% — High accuracy</span>`;
            } else {
                confLabel.innerHTML = `<span style="color:var(--neon-yellow);">⚠ Low confidence: ${data.confidence.toFixed(1)}%. Please verify the plate number.</span>`;
            }
        } else {
            resultInput.value = '';
            confLabel.innerHTML = `<span style="color:var(--neon-red);">✗ Number plate could not be detected. Please enter manually.</span>`;
            useBtn.style.display = 'none';
            return;
        }

        useBtn.style.display = '';
    }

    // ─────────────────────────────────────────────
    // Error display inside modal
    // ─────────────────────────────────────────────
    function showResultError(msg) {
        resultBox.style.display = 'block';
        resultInput.value = '';
        confLabel.innerHTML = `<span style="color:var(--neon-red);">✗ ${msg}</span>`;
        useBtn.style.display = 'none';
    }

    // ─────────────────────────────────────────────
    // Confirm and populate the main form's plate field
    // ─────────────────────────────────────────────
    async function usePlate() {
        const finalPlate = resultInput.value.trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
        if (!finalPlate) {
            confLabel.innerHTML = '<span style="color:var(--neon-red);">Please enter a valid plate number.</span>';
            return;
        }

        // Populate the main form input
        plateInput.value = finalPlate;
        plateInput.dispatchEvent(new Event('input'));

        // Show feedback on main form
        showFeedback('✓ Plate auto-filled from camera scan. You may edit before submitting.', 'success');

        // Confirm scan record in backend (fire-and-forget)
        if (lastScanId) {
            const apiBase = (typeof API_BASE !== 'undefined' ? API_BASE : 'http://localhost:8080/api/parking')
                .replace('/api/parking', '') + '/api/anpr';
            fetch(apiBase + '/confirm', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ scanId: lastScanId, vehicleNumber: finalPlate })
            }).catch(() => {/* non-critical, ignore */});
        }

        closeModal();
    }

    // ─────────────────────────────────────────────
    // Show feedback below plate input on main form
    // ─────────────────────────────────────────────
    function showFeedback(msg, type) {
        if (!feedbackDiv) return;
        const colors = {
            success: 'var(--neon-green)',
            warning: 'var(--neon-yellow)',
            error:   'var(--neon-red)'
        };
        feedbackDiv.style.color = colors[type] || 'var(--text-secondary)';
        feedbackDiv.textContent = msg;
        feedbackDiv.style.display = 'block';

        setTimeout(() => { feedbackDiv.style.display = 'none'; }, 8000);
    }

    // ─────────────────────────────────────────────
    // Event bindings
    // ─────────────────────────────────────────────
    if (scanBtn)    scanBtn.addEventListener('click', openModal);
    if (closeBtn)   closeBtn.addEventListener('click', closeModal);
    if (cancelBtn)  cancelBtn.addEventListener('click', closeModal);
    if (captureBtn) captureBtn.addEventListener('click', processCapture);
    if (useBtn)     useBtn.addEventListener('click', usePlate);

    // Close on overlay click (outside modal box)
    if (modalOverlay) {
        modalOverlay.addEventListener('click', function(e) {
            if (e.target === modalOverlay) closeModal();
        });
    }

    // Allow pressing Enter inside result input to confirm
    if (resultInput) {
        resultInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') { e.preventDefault(); usePlate(); }
        });
    }

})();
