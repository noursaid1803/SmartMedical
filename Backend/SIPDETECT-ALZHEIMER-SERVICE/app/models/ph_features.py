"""Persistent Homology features (TDA) for SipDetect v6."""

from typing import TYPE_CHECKING

import gudhi
import numpy as np
import torch

if TYPE_CHECKING:
    import torch.nn as nn


def extract_ph_features(feature_map_2d: np.ndarray) -> np.ndarray:
    fmap = feature_map_2d.astype(np.float64)
    fmin, fmax = fmap.min(), fmap.max()
    if fmax <= fmin:
        return np.zeros(12, dtype=np.float32)

    fmap = (fmap - fmin) / (fmax - fmin)
    cc = gudhi.CubicalComplex(
        dimensions=list(fmap.shape),
        top_dimensional_cells=fmap.flatten().tolist(),
    )
    cc.compute_persistence()
    pairs = cc.persistence()

    features: list[float] = []
    for dim in (0, 1):
        intervals = [
            (birth, death)
            for dim_val, (birth, death) in pairs
            if dim_val == dim and death != float("inf")
        ]
        if intervals:
            pers = np.array([death - birth for birth, death in intervals])
            features.extend([
                float(np.mean(pers)),
                float(np.std(pers)),
                float(np.max(pers)),
                float(np.sum(pers)),
                float(len(pers)),
                float(np.percentile(pers, 75)),
            ])
        else:
            features.extend([0.0] * 6)

    return np.array(features, dtype=np.float32)


def extract_ph_from_cnn(img_tensor: torch.Tensor, cnn_model: "nn.Module", n_channels: int = 8) -> np.ndarray:
    with torch.no_grad():
        feat_map = cnn_model.features(img_tensor)

    channel_energy = feat_map[0].abs().mean(dim=(1, 2))
    top_channels = channel_energy.argsort(descending=True)[:n_channels]
    all_ph = [extract_ph_features(feat_map[0, ch].cpu().numpy()) for ch in top_channels]
    return np.mean(all_ph, axis=0)
