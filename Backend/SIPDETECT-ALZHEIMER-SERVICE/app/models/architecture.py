"""SipDetect v6 — EfficientNet-B3 + GATv2 + PH hybrid architecture."""

import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GATv2Conv, global_mean_pool
from torch_geometric.utils import to_dense_batch
from torchvision.models import efficientnet_b3


def safe_global_max_pool(x: torch.Tensor, batch: torch.Tensor) -> torch.Tensor:
    dense, mask = to_dense_batch(x, batch)
    dense = dense.clone()
    dense[~mask] = float("-inf")
    return dense.max(dim=1).values


class GATEncoder(nn.Module):
    def __init__(
        self,
        in_channels: int = 1550,
        hidden: int = 256,
        out_channels: int = 128,
        heads: int = 4,
        dropout: float = 0.3,
        edge_dim: int = 1,
    ):
        super().__init__()
        self.dropout = dropout

        self.conv1 = GATv2Conv(
            in_channels, hidden, heads=heads, dropout=dropout,
            concat=True, add_self_loops=True, edge_dim=edge_dim,
        )
        self.bn1 = nn.BatchNorm1d(hidden * heads)

        self.conv2 = GATv2Conv(
            hidden * heads, hidden, heads=heads, dropout=dropout,
            concat=True, add_self_loops=True, edge_dim=edge_dim,
        )
        self.bn2 = nn.BatchNorm1d(hidden * heads)

        self.conv3 = GATv2Conv(
            hidden * heads, out_channels, heads=heads, dropout=dropout,
            concat=False, add_self_loops=True, edge_dim=edge_dim,
        )
        self.bn3 = nn.BatchNorm1d(out_channels)

    def forward(self, x, edge_index, edge_attr, batch):
        x = F.dropout(x, p=self.dropout, training=self.training)
        x = F.elu(self.bn1(self.conv1(x, edge_index, edge_attr)))

        x = F.dropout(x, p=self.dropout, training=self.training)
        x = F.elu(self.bn2(self.conv2(x, edge_index, edge_attr)))

        x = F.dropout(x, p=self.dropout, training=self.training)
        x = F.elu(self.bn3(self.conv3(x, edge_index, edge_attr)))

        mean_pool = global_mean_pool(x, batch)
        max_pool = safe_global_max_pool(x, batch)
        return torch.cat([mean_pool, max_pool], dim=-1)


class CNNGATHybrid(nn.Module):
    """CNN (EfficientNet-B3) + GATv2 + PH fusion classifier."""

    def __init__(self, num_classes: int = 4, edge_dim: int = 1):
        super().__init__()
        base = efficientnet_b3(weights=None)
        self.cnn = nn.Module()
        self.cnn.features = base.features
        self.cnn.avgpool = base.avgpool
        self.cnn.classifier = nn.Sequential(
            nn.Identity(),
            nn.Linear(1536, 256),
            nn.Identity(),
            nn.Identity(),
            nn.Linear(256, num_classes),
        )

        self.gat = GATEncoder(edge_dim=edge_dim)
        self.classifier = nn.Sequential(
            nn.Linear(1792, 768),
            nn.BatchNorm1d(768),
            nn.Identity(),
            nn.Identity(),
            nn.Linear(768, 256),
            nn.BatchNorm1d(256),
            nn.Identity(),
            nn.Identity(),
            nn.Linear(256, num_classes),
        )

    def forward(self, imgs, graph_data):
        cnn_feat = self.cnn.features(imgs)
        cnn_feat = self.cnn.avgpool(cnn_feat).flatten(1)

        gat_feat = self.gat(
            graph_data.x,
            graph_data.edge_index,
            graph_data.edge_attr,
            graph_data.batch,
        )

        return self.classifier(torch.cat([cnn_feat, gat_feat], dim=-1))

    def forward_logits(self, imgs, graph_data):
        return self.forward(imgs, graph_data)
