"""Grad-CAM++ localization for SipDetect V20 hybrid model."""

from typing import Optional, Tuple

import cv2
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F


class GradCAMPlusPlus:
    """Grad-CAM++ on the CNN branch of HybridV20 (or EfficientNet backbone)."""

    def __init__(self, model: nn.Module, is_hybrid: bool = True):
        self.model = model
        self.is_hybrid = is_hybrid
        self.gradients = None
        self.activations = None
        self._hooks = []

        target_module = model.cnn if is_hybrid else (
            model.backbone if hasattr(model, "backbone") else model
        )
        self.target_layer = self._find_last_conv(target_module)
        self._register_hooks()

    def _find_last_conv(self, module: nn.Module) -> nn.Conv2d:
        last_conv = None
        for layer in module.modules():
            if isinstance(layer, nn.Conv2d):
                last_conv = layer
        if last_conv is None:
            raise ValueError("Aucune couche Conv2d trouvée pour Grad-CAM++")
        return last_conv

    def _register_hooks(self) -> None:
        def save_activation(_module, _inp, out):
            self.activations = out.detach()

        def save_gradient(_module, _grad_in, grad_out):
            self.gradients = grad_out[0].detach()

        self._hooks.append(self.target_layer.register_forward_hook(save_activation))
        self._hooks.append(self.target_layer.register_full_backward_hook(save_gradient))

    def remove_hooks(self) -> None:
        for hook in self._hooks:
            hook.remove()

    def generate(self, input_tensor: torch.Tensor, target_class: Optional[int] = None, img_size: int = 224):
        self.model.eval()
        tensor = input_tensor.detach().clone().requires_grad_(True)
        output = self.model(tensor)
        if target_class is None:
            target_class = int(output.argmax(1).item())

        self.model.zero_grad()
        one_hot = torch.zeros_like(output)
        one_hot[0, target_class] = 1.0
        output.backward(gradient=one_hot, retain_graph=True)

        grads = self.gradients
        acts = self.activations
        g2 = grads ** 2
        g3 = grads ** 3
        alpha = g2 / (2 * g2 + (acts * g3).sum(dim=(2, 3), keepdim=True) + 1e-8)
        weights = (F.relu(grads) * alpha).sum(dim=(2, 3), keepdim=True)

        cam = F.relu((weights * acts).sum(dim=1, keepdim=True))
        cam = F.interpolate(cam, (img_size, img_size), mode="bilinear", align_corners=False)
        cam = cam.squeeze().detach().cpu().numpy()

        cam_min, cam_max = cam.min(), cam.max()
        if cam_max - cam_min > 1e-8:
            cam = (cam - cam_min) / (cam_max - cam_min)
        return cam, target_class


def get_bbox(cam: np.ndarray, threshold: float = 0.40) -> Tuple[Optional[Tuple[int, int, int, int]], float]:
    binary = (cam > threshold).astype(np.uint8)
    rows = np.any(binary, axis=1)
    cols = np.any(binary, axis=0)
    if not rows.any():
        return None, 0.0
    y0, y1 = np.where(rows)[0][[0, -1]]
    x0, x1 = np.where(cols)[0][[0, -1]]
    confidence = float(binary.sum()) / binary.size
    return (int(x0), int(y0), int(x1), int(y1)), confidence


def overlay_gradcam(patch_gray: np.ndarray, cam: np.ndarray, alpha: float = 0.50) -> np.ndarray:
    """Return RGB uint8 overlay of Grad-CAM on axial patch."""
    base = np.stack([patch_gray] * 3, axis=-1)
    base = (base * 255).clip(0, 255).astype(np.uint8)
    heatmap = cv2.applyColorMap((cam * 255).astype(np.uint8), cv2.COLORMAP_JET)
    heatmap = cv2.cvtColor(heatmap, cv2.COLOR_BGR2RGB)
    blended = (alpha * heatmap + (1 - alpha) * base).astype(np.uint8)
    return blended
