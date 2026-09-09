"""Preprocess uploaded lung scans into SipDetect V20 patch format."""

import io
from typing import Tuple

import numpy as np
from PIL import Image


def _crop_resize(slice_arr: np.ndarray, out_size: int) -> np.ndarray:
    height, width = slice_arr.shape
    side = min(height, width)
    center_y, center_x = height // 2, width // 2
    half = side // 2
    crop = slice_arr[
        max(0, center_y - half) : min(height, center_y + half),
        max(0, center_x - half) : min(width, center_x + half),
    ]
    image = Image.fromarray((crop * 255).astype(np.uint8))
    resized = np.array(image.resize((out_size, out_size), Image.BILINEAR)).astype(np.float32) / 255.0
    return resized


def normalize_grayscale(arr: np.ndarray) -> np.ndarray:
    """Map grayscale medical image to [0, 1] (CT-like dynamic range)."""
    arr = arr.astype(np.float32)
    low, high = np.percentile(arr, (1, 99))
    if high - low < 1e-6:
        low, high = float(arr.min()), float(arr.max())
    if high - low < 1e-6:
        return np.zeros_like(arr, dtype=np.float32)
    return np.clip((arr - low) / (high - low), 0.0, 1.0)


def image_bytes_to_patch(image_bytes: bytes, patch_size: int = 96) -> np.ndarray:
    """
    Convert an uploaded lung scan (JPG/PNG) to a (3, patch_size, patch_size) patch.

    For 2D uploads, pseudo axial/coronal/sagittal views are derived from the same slice,
    matching the notebook's 3-channel patch layout used by HybridV20.
    """
    image = Image.open(io.BytesIO(image_bytes)).convert("L")
    gray = normalize_grayscale(np.array(image, dtype=np.float32))

    axial = _crop_resize(gray, patch_size)
    coronal = _crop_resize(np.flipud(gray), patch_size)
    sagittal = _crop_resize(np.fliplr(gray), patch_size)
    patch = np.stack([axial, coronal, sagittal], axis=0)

    if np.isnan(patch).any() or patch.std() < 1e-4:
        raise ValueError("Image trop uniforme ou invalide pour l'analyse pulmonaire.")

    return patch.astype(np.float32)


def denorm_axial(normalized_tensor, ct_mean, ct_std) -> np.ndarray:
    """Recover axial channel for visualization (channel 0)."""
    channel = normalized_tensor[0].detach().cpu().numpy()
    channel = channel * float(ct_std[0]) + float(ct_mean[0])
    return np.clip(channel, 0.0, 1.0)


def bbox_to_location_label(bbox: Tuple[int, int, int, int], image_size: int) -> str:
    x0, y0, x1, y1 = bbox
    cx = ((x0 + x1) / 2) / image_size
    cy = ((y0 + y1) / 2) / image_size

    horizontal = "centre"
    if cx < 0.35:
        horizontal = "gauche"
    elif cx > 0.65:
        horizontal = "droite"

    vertical = "milieu"
    if cy < 0.35:
        vertical = "supérieur"
    elif cy > 0.65:
        vertical = "inférieur"

    return f"Lobe {vertical} {horizontal} (Grad-CAM)"
