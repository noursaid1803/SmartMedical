"""SipDetect V20 model architectures (EfficientNet-B4 + Swin-Small hybrid)."""

import torch
import torch.nn as nn
import timm

from app.models.lung_cancer.config import N_CLASSES


class CNNBaselineV20(nn.Module):
    """Sprint 1 — EfficientNet-B4 baseline."""

    def __init__(self):
        super().__init__()
        self._model_name = "efficientnet_b4"
        for model_name in ["efficientnet_b4", "efficientnet_b3", "resnet50"]:
            try:
                self.backbone = timm.create_model(
                    model_name,
                    pretrained=False,
                    num_classes=0,
                    global_pool="avg",
                    drop_rate=0.20,
                )
                self._model_name = model_name
                break
            except Exception:
                continue

        dim = self.backbone.num_features
        self.head = nn.Sequential(
            nn.BatchNorm1d(dim),
            nn.Dropout(0.40),
            nn.Linear(dim, 512),
            nn.GELU(),
            nn.BatchNorm1d(512),
            nn.Dropout(0.25),
            nn.Linear(512, 256),
            nn.GELU(),
            nn.BatchNorm1d(256),
            nn.Dropout(0.12),
            nn.Linear(256, N_CLASSES),
        )
        self.skip = nn.Linear(dim, N_CLASSES)

    def forward(self, x):
        feat = self.backbone(x)
        return self.head(feat) + 0.1 * self.skip(feat)


class HybridV20(nn.Module):
    """Sprint 2 — EfficientNet-B4 + Swin-Small with dynamic gate."""

    def __init__(self):
        super().__init__()
        self._cnn_name = "efficientnet_b4"
        self._vit_name = "swin_small_patch4_window7_224"

        for cnn_name in ["efficientnet_b4", "efficientnet_b3"]:
            try:
                self.cnn = timm.create_model(
                    cnn_name,
                    pretrained=False,
                    num_classes=0,
                    global_pool="avg",
                    drop_rate=0.15,
                )
                self._cnn_name = cnn_name
                break
            except Exception:
                continue

        for vit_name in ["swin_small_patch4_window7_224", "swin_tiny_patch4_window7_224"]:
            try:
                self.vit = timm.create_model(vit_name, pretrained=False, num_classes=0)
                self._vit_name = vit_name
                break
            except Exception:
                continue

        dim = 512
        self.cnn_proj = nn.Sequential(
            nn.Linear(self.cnn.num_features, dim),
            nn.LayerNorm(dim),
            nn.GELU(),
            nn.Dropout(0.08),
        )
        self.vit_proj = nn.Sequential(
            nn.Linear(self.vit.num_features, dim),
            nn.LayerNorm(dim),
            nn.GELU(),
            nn.Dropout(0.08),
        )
        self.gate = nn.Sequential(
            nn.Linear(dim * 2, dim // 2),
            nn.ReLU(),
            nn.Linear(dim // 2, 2),
        )
        nn.init.zeros_(self.gate[-1].weight)
        with torch.no_grad():
            self.gate[-1].bias[0] = 0.85
            self.gate[-1].bias[1] = -0.85

        self.res_cnn = nn.Linear(dim, dim)
        self.res_vit = nn.Linear(dim, dim)
        self.res_norm = nn.LayerNorm(dim)
        self.se = nn.Sequential(
            nn.Linear(dim, dim // 8),
            nn.ReLU(),
            nn.Linear(dim // 8, dim),
            nn.Sigmoid(),
        )
        self.classifier = nn.Sequential(
            nn.LayerNorm(dim),
            nn.Dropout(0.22),
            nn.Linear(dim, 384),
            nn.GELU(),
            nn.Dropout(0.12),
            nn.Linear(384, 256),
            nn.GELU(),
            nn.Dropout(0.08),
            nn.Linear(256, N_CLASSES),
        )
        self.skip = nn.Linear(dim, N_CLASSES)

    def forward(self, x):
        fc = self.cnn_proj(self.cnn(x))
        fv = self.vit_proj(self.vit(x))
        weights = torch.softmax(self.gate(torch.cat([fc, fv], dim=1)), dim=-1)
        fused = weights[:, 0:1] * fc + weights[:, 1:2] * fv
        fused = self.res_norm(fused + self.res_cnn(fc) + self.res_vit(fv) * 0.3)
        fused = fused * self.se(fused)
        return self.classifier(fused) + 0.1 * self.skip(fused)
