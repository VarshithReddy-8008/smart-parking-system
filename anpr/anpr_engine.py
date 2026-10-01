#!/usr/bin/env python3
"""
anpr_engine.py — Automatic Number Plate Recognition Engine
Uses EasyOCR + OpenCV to detect and read number plates.
Called by Spring Boot via ProcessBuilder.
Usage: python -u anpr_engine.py <image_path>
Output: JSON to stdout
"""

import sys
import os
import re
import json
import time

def load_dependencies():
    try:
        import cv2
        import easyocr
        import numpy as np
        return cv2, easyocr, np
    except ImportError as e:
        print(json.dumps({
            "success": False,
            "vehicleNumber": "",
            "confidence": 0.0,
            "message": f"Missing dependency: {str(e)}. Run: pip install easyocr opencv-python"
        }))
        sys.exit(0)

def preprocess_image(cv2, np, image_path):
    """Apply OpenCV preprocessing to improve OCR accuracy."""
    img = cv2.imread(image_path)
    if img is None:
        return None, None

    # Resize for consistency
    scale = 2
    img_resized = cv2.resize(img, (img.shape[1] * scale, img.shape[0] * scale))

    # Convert to grayscale
    gray = cv2.cvtColor(img_resized, cv2.COLOR_BGR2GRAY)

    # Apply bilateral filter to reduce noise while keeping edges
    denoised = cv2.bilateralFilter(gray, 11, 17, 17)

    # Apply adaptive threshold
    thresh = cv2.adaptiveThreshold(
        denoised, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY, 11, 2
    )

    return img_resized, thresh

def clean_plate_text(raw_text):
    """Clean, filter, and standardize the detected text."""
    # Remove everything that is not alphanumeric
    cleaned = re.sub(r'[^A-Za-z0-9]', '', raw_text)
    # Convert to uppercase
    cleaned = cleaned.upper()
    return cleaned

def is_valid_plate(cleaned_text):
    """
    Validate if the cleaned text looks like a valid vehicle number plate.
    Must have a length between 5 and 12, at least one digit, and at least two letters.
    """
    if not cleaned_text:
        return False
    if len(cleaned_text) < 5 or len(cleaned_text) > 12:
        return False
    # Must contain at least one digit (number plates always contain numbers)
    if not any(char.isdigit() for char in cleaned_text):
        return False
    # Must contain at least two letters (for state/country codes)
    letters_count = sum(1 for char in cleaned_text if char.isalpha())
    if letters_count < 2:
        return False
    return True

def score_plate(text, conf):
    """
    Ranks candidate plates by matching them against typical Indian registration formats.
    """
    score = conf
    
    # Format 1: State(2) + RTO(2) + Serial(1-2) + Number(4) e.g., TS08UF6789
    if re.match(r'^[A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4}$', text):
        score += 2.0
    # Format 2: State(2) + RTO(1-2) + Serial(0-2) + Number(4) e.g., AP28T1234
    elif re.match(r'^[A-Z]{2}[0-9]{1,2}[A-Z]{0,2}[0-9]{4}$', text):
        score += 1.5
    # Format 3: BH Series e.g., 22BH1234AA
    elif re.match(r'^[0-9]{2}BH[0-9]{4}[A-Z]{1,2}$', text):
        score += 2.0
    # Format 4: Generic pattern ending with 3-4 digits
    elif re.match(r'^[A-Z]{2}.*[0-9]{3,4}$', text):
        score += 0.8
        
    # Completeness length bonus
    if len(text) >= 9:
        score += 0.5
        
    return score

def merge_detections(results):
    """
    Groups and merges text detections that are horizontally aligned and adjacent.
    Handles split bounding boxes for license plates.
    """
    if not results:
        return []
    
    processed = []
    for bbox, text, conf in results:
        xs = [p[0] for p in bbox]
        ys = [p[1] for p in bbox]
        x_min, x_max = min(xs), max(xs)
        y_min, y_max = min(ys), max(ys)
        height = y_max - y_min
        processed.append({
            'text': text,
            'conf': conf,
            'x_min': x_min,
            'x_max': x_max,
            'y_min': y_min,
            'y_max': y_max,
            'height': height,
            'center_y': (y_min + y_max) / 2
        })
        
    # Group detections into lines by vertical center overlap
    lines = []
    for det in processed:
        placed = False
        for line in lines:
            avg_height = sum(d['height'] for d in line) / len(line)
            avg_center_y = sum(d['center_y'] for d in line) / len(line)
            
            # If vertical centers are close (within 60% of average height)
            if abs(det['center_y'] - avg_center_y) < (0.6 * avg_height):
                line.append(det)
                placed = True
                break
        if not placed:
            lines.append([det])
            
    merged_results = []
    for line in lines:
        # Sort left to right
        line.sort(key=lambda d: d['x_min'])
        
        current = line[0]
        merged_line = []
        for next_det in line[1:]:
            dist = next_det['x_min'] - current['x_max']
            max_allowed_dist = 1.5 * max(current['height'], next_det['height'])
            
            # Merge if horizontal distance is reasonable
            if dist < max_allowed_dist:
                current['text'] = current['text'] + " " + next_det['text']
                current['x_max'] = next_det['x_max']
                current['y_min'] = min(current['y_min'], next_det['y_min'])
                current['y_max'] = max(current['y_max'], next_det['y_max'])
                current['height'] = current['y_max'] - current['y_min']
                current['center_y'] = (current['y_min'] + current['y_max']) / 2
                current['conf'] = (current['conf'] + next_det['conf']) / 2
            else:
                merged_line.append(current)
                current = next_det
        merged_line.append(current)
        merged_results.extend(merged_line)
        
    return merged_results

def run_ocr(image_path):
    cv2, easyocr, np = load_dependencies()

    if not os.path.exists(image_path):
        print(json.dumps({
            "success": False,
            "vehicleNumber": "",
            "confidence": 0.0,
            "message": "Image file not found"
        }))
        return

    # Initialize EasyOCR reader (English)
    reader = easyocr.Reader(['en'], gpu=False, verbose=False)

    # Preprocess image
    original, processed = preprocess_image(cv2, np, image_path)
    if original is None:
        print(json.dumps({
            "success": False,
            "vehicleNumber": "",
            "confidence": 0.0,
            "message": "Could not read image file"
        }))
        return

    # Save processed image temporarily for EasyOCR
    proc_path = image_path.replace('.', '_proc.')
    cv2.imwrite(proc_path, processed)

    # Save horizontally flipped original
    img_orig = cv2.imread(image_path)
    orig_flip_path = image_path.replace('.', '_flip.')
    if img_orig is not None:
        img_flipped = cv2.flip(img_orig, 1)
        cv2.imwrite(orig_flip_path, img_flipped)
    else:
        orig_flip_path = None

    # Run OCR on original, processed, and flipped original
    candidates = []
    paths_to_run = [proc_path, image_path]
    if orig_flip_path:
        paths_to_run.append(orig_flip_path)

    for path in paths_to_run:
        try:
            results = reader.readtext(path)
            merged = merge_detections(results)
            for item in merged:
                cleaned = clean_plate_text(item['text'])
                if is_valid_plate(cleaned):
                    candidates.append((cleaned, item['conf']))
        except Exception as e:
            continue

    # Cleanup temp files
    for path in [proc_path, orig_flip_path]:
        if path and os.path.exists(path):
            try:
                os.remove(path)
            except Exception:
                pass

    # Select the best plate using the scoring function
    best_plate = ""
    best_score = -1.0
    best_conf = 0.0

    for cleaned, conf in candidates:
        score = score_plate(cleaned, conf)
        if score > best_score:
            best_score = score
            best_plate = cleaned
            best_conf = conf

    if best_plate:
        print(json.dumps({
            "success": True,
            "vehicleNumber": best_plate,
            "confidence": round(best_conf * 100, 2),
            "message": "OK"
        }))
    else:
        print(json.dumps({
            "success": False,
            "vehicleNumber": "",
            "confidence": 0.0,
            "message": "Number plate could not be detected"
        }))

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({
            "success": False,
            "vehicleNumber": "",
            "confidence": 0.0,
            "message": "Usage: python anpr_engine.py <image_path>"
        }))
        sys.exit(1)

    run_ocr(sys.argv[1])
