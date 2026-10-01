import easyocr
import cv2
import os

def test():
    img_path = "backend/anpr/images/scan_e4e54e92-3a09-4e0c-ba04-a8a669e62573.jpg"
    if not os.path.exists(img_path):
        print(f"Error: {img_path} not found")
        return
        
    reader = easyocr.Reader(['en'], gpu=False, verbose=False)
    
    # 1. As is
    img_normal = cv2.imread(img_path)
    res_normal = reader.readtext(img_path)
    print("=== As is Detections ===")
    for bbox, text, conf in res_normal:
        print(f"  - '{text}' (conf: {conf:.2f})")
        
    # 2. Horizontally flipped
    flipped = cv2.flip(img_normal, 1)
    flip_path = "backend/anpr/images/temp_flip.jpg"
    cv2.imwrite(flip_path, flipped)
    res_flipped = reader.readtext(flip_path)
    print("\n=== Horizontally Flipped Detections ===")
    for bbox, text, conf in res_flipped:
        print(f"  - '{text}' (conf: {conf:.2f})")
        
    if os.path.exists(flip_path):
        os.remove(flip_path)

if __name__ == "__main__":
    test()
