"""Grad-CAM visualization for SipDetect v6 CNN backbone."""

from typing import Optional, Tuple

import cv2
import numpy as np
import torch
import torch.nn.functional as F
from PIL import Image


def _denormalize(tensor: torch.Tensor) -> np.ndarray:
    mean = torch.tensor([0.485, 0.456, 0.406]).view(3, 1, 1)
    std = torch.tensor([0.229, 0.224, 0.225]).view(3, 1, 1)
    img = tensor.detach().cpu().squeeze(0) * std + mean
    img = img.clamp(0, 1).permute(1, 2, 0).numpy()
    return (img * 255).astype(np.uint8)


def generate_gradcam(
    model: torch.nn.Module,
    img_tensor: torch.Tensor,
    graph_batch,
    target_class: int,
) -> Tuple[np.ndarray, np.ndarray, float]:
    model.eval()
    activations = []
    gradients = []

    def forward_hook(_module, _input, output):
        activations.append(output)

    def backward_hook(_module, _grad_input, grad_output):
        gradients.append(grad_output[0])

    target_layer = model.cnn.features[-1]
    handle_f = target_layer.register_forward_hook(forward_hook)
    handle_b = target_layer.register_full_backward_hook(backward_hook)

    try:
        model.zero_grad(set_to_none=True)
        logits = model(img_tensor, graph_batch)
        score = logits[0, target_class]
        score.backward()

        acts = activations[0].detach()
        grads = gradients[0].detach()
        weights = grads.mean(dim=(2, 3), keepdim=True)
        cam = F.relu((weights * acts).sum(dim=1, keepdim=True))
        cam = F.interpolate(cam, size=img_tensor.shape[-2:], mode="bilinear", align_corners=False)
        cam = cam.squeeze().cpu().numpy()
        cam = (cam - cam.min()) / (cam.max() - cam.min() + 1e-8)

        original = _denormalize(img_tensor)
        heatmap = cv2.applyColorMap(np.uint8(255 * cam), cv2.COLORMAP_JET)
        heatmap = cv2.cvtColor(heatmap, cv2.COLOR_BGR2RGB)
        overlay = cv2.addWeighted(original, 0.55, heatmap, 0.45, 0)
        intensity = float(cam.mean() * 100)
        return original, overlay, cam, intensity
    finally:
        handle_f.remove()
        handle_b.remove()


def locate_affected_zone(cam: np.ndarray, img_size: int) -> Tuple[str, float]:
    h, w = cam.shape
    cy, cx = np.unravel_index(np.argmax(cam), cam.shape)

    x_ratio = cx / max(w, 1)
    y_ratio = cy / max(h, 1)

    if y_ratio < 0.35:
        zone = "Cortex frontal"
    elif y_ratio > 0.65:
        zone = "Région occipitale / tronc cérébral"
    elif x_ratio < 0.4:
        zone = "Cortex temporal gauche"
    elif x_ratio > 0.6:
        zone = "Cortex temporal droit"
    elif 0.4 <= x_ratio <= 0.6 and 0.35 <= y_ratio <= 0.65:
        zone = "Région hippocampique / ventriculaire"
    else:
        zone = "Cortex pariétal"

    confidence = float(cam.max() * 100)
    return zone, confidence


def encode_image_base64(rgb_array: np.ndarray) -> str:
    import base64
    import io

    image = Image.fromarray(rgb_array)
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    encoded = base64.b64encode(buffer.getvalue()).decode("ascii")
    return f"data:image/png;base64,{encoded}"
