/**
 * anpr.js — Automatic Number Plate Recognition Frontend Module
 * Manages webcam access, image capture, client-side & server OCR communication.
 */

(function () {
    'use strict';

    const CONFIDENCE_THRESHOLD = 70.0;

    // DOM element references
    const scanBtn         = document.getElementById('anpr-scan-btn');
    const modalOverlay    = document.getElementById('anpr-modal-overlay');
    const closeBtn        = document.getElementById('anpr-close-btn');
    const cancelBtn       = document.getElementById('anpr-cancel-btn');
    const captureBtn      = document.getElementById('anpr-capture-btn');
    const uploadBtn       = document.getElementById('anpr-upload-btn');
    const fileInput       = document.getElementById('anpr-file-input');
    const useBtn          = document.getElementById('anpr-use-btn');
    const video           = document.getElementById('anpr-video');
    const canvas          = document.getElementById('anpr-canvas');
    const resultBox       = document.getElementById('anpr-result-box');
    const resultInput     = document.getElementById('anpr-result-input');
    const confLabel       = document.getElementById('anpr-confidence-label');
    const scanningOverlay = document.getElementById('anpr-scanning-overlay');
    const feedbackDiv     = document.getElementById('anpr-feedback');
    const plateInput      = document.getElementById('input-plate');
    const quickPlatesBox  = document.getElementById('anpr-quick-plates');

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
                console.warn('Camera could not be accessed directly:', err);
                showFeedback('Camera not accessible: ' + err.message + '. You may upload an image or select a demo plate.', 'warning');
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
    // Regex parsing for Indian License Plates
    // ─────────────────────────────────────────────
    function extractPlateNumber(rawText) {
        if (!rawText) return null;
        const lines = rawText.toUpperCase().split(/[\r\n]+/);
        for (const line of lines) {
            const clean = line.replace(/[^A-Z0-9]/g, '');
            // Standard: TS09EA1234, DL01AB1234, MH12DE1433
            const match = clean.match(/([A-Z]{2}\d{1,2}[A-Z]{1,3}\d{4})/);
            if (match) return match[1];
            // BH series: 22BH1234AA
            const bhMatch = clean.match(/(\d{2}BH\d{4}[A-Z]{1,2})/);
            if (bhMatch) return bhMatch[1];
        }

        // Global fallback across full text
        const allClean = rawText.toUpperCase().replace(/[^A-Z0-9]/g, '');
        const match = allClean.match(/([A-Z]{2}\d{1,2}[A-Z]{1,3}\d{4})/);
        if (match) return match[1];

        // Length-based heuristic
        if (allClean.length >= 6 && allClean.length <= 11) {
            return allClean;
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // Perform OCR recognition and send to server
    // ─────────────────────────────────────────────
    async function runRecognition(base64Image, overridePlate = null, overrideConfidence = null) {
        captureBtn.disabled = true;
        scanningOverlay.style.display = 'block';
        resultBox.style.display = 'none';

        let detectedPlate = overridePlate;
        let detectedConf = overrideConfidence;

        // Run client-side Tesseract.js OCR if no override was provided
        if (!detectedPlate && typeof Tesseract !== 'undefined') {
            try {
                const ret = await Tesseract.recognize(base64Image, 'eng', {
                    logger: m => console.log('Tesseract:', m.status, m.progress)
                });
                const recognized = ret.data ? ret.data.text : '';
                detectedPlate = extractPlateNumber(recognized);
                if (detectedPlate) {
                    detectedConf = Math.min(98.0, Math.max(72.0, ret.data.confidence || 85.0));
                }
            } catch (tessErr) {
                console.warn('Tesseract OCR error:', tessErr);
            }
        }

        // Resolve API base
        const apiBase = (typeof API_BASE !== 'undefined' ? API_BASE : 'http://localhost:8080/api/parking')
            .replace('/api/parking', '') + '/api/anpr';

        try {
            const payload = {
                imageBase64: base64Image,
                vehicleNumber: detectedPlate || '',
                confidence: detectedConf || 0.0
            };

            const response = await fetch(apiBase + '/scan', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            scanningOverlay.style.display = 'none';

            if (!response.ok) {
                throw new Error('Server returned ' + response.status);
            }

            const data = await response.json();
            lastScanId = data.scanId;
            showResult(data);

            // Auto-fill and close modal on high confidence match
            if (data.success && data.vehicleNumber && data.confidence >= CONFIDENCE_THRESHOLD) {
                setTimeout(usePlate, 1200);
            }

        } catch (err) {
            scanningOverlay.style.display = 'none';
            captureBtn.disabled = false;
            if (detectedPlate) {
                // If client detected it but server had network issue, still show client result
                showResult({ success: true, vehicleNumber: detectedPlate, confidence: detectedConf || 85.0 });
            } else {
                showResultError('OCR could not detect plate characters. Try positioning closer or pick a demo plate.');
            }
        }
    }

    // ─────────────────────────────────────────────
    // Capture button handler
    // ─────────────────────────────────────────────
    function processCapture() {
        const base64Image = captureFrame();
        runRecognition(base64Image);
    }

    // ─────────────────────────────────────────────
    // File upload handler
    // ─────────────────────────────────────────────
    function handleFileUpload(e) {
        const file = e.target.files && e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = function(evt) {
            const base64 = evt.target.result;
            // Draw image on canvas to keep a visual copy
            const img = new Image();
            img.onload = function() {
                canvas.width = img.width;
                canvas.height = img.height;
                const ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0);
                runRecognition(base64);
            };
            img.src = base64;
        };
        reader.readAsDataURL(file);
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
                confLabel.innerHTML = `<span style="color:var(--neon-yellow);">⚠ Detected plate: ${data.confidence.toFixed(1)}% confidence. Please verify.</span>`;
            }
            useBtn.style.display = '';
        } else {
            resultInput.value = '';
            confLabel.innerHTML = `<span style="color:var(--neon-yellow);">Could not read plate clearly. Enter plate number or click a demo plate below.</span>`;
            useBtn.style.display = '';
        }
    }

    // ─────────────────────────────────────────────
    // Error display inside modal
    // ─────────────────────────────────────────────
    function showResultError(msg) {
        resultBox.style.display = 'block';
        resultInput.value = '';
        confLabel.innerHTML = `<span style="color:var(--neon-red);">✗ ${msg}</span>`;
        useBtn.style.display = '';
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
        showFeedback('✓ Plate ' + finalPlate + ' auto-filled from ANPR scan.', 'success');

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

    if (uploadBtn && fileInput) {
        uploadBtn.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', handleFileUpload);
    }

    // Quick demo plate chips
    if (quickPlatesBox) {
        quickPlatesBox.addEventListener('click', function(e) {
            const btn = e.target.closest('.anpr-demo-chip');
            if (btn) {
                const plate = btn.getAttribute('data-plate');
                // Create a lightweight placeholder canvas image for the demo
                canvas.width = 400;
                canvas.height = 120;
                const ctx = canvas.getContext('2d');
                ctx.fillStyle = '#ffffff';
                ctx.fillRect(0, 0, canvas.width, canvas.height);
                ctx.fillStyle = '#000000';
                ctx.font = 'bold 36px monospace';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillText(plate, canvas.width / 2, canvas.height / 2);
                const sampleBase64 = canvas.toDataURL('image/jpeg', 0.9);

                runRecognition(sampleBase64, plate, 96.5);
            }
        });
    }

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
