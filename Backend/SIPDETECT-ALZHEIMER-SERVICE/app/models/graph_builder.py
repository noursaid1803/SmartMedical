"""Graph construction (SLIC + k-NN) for SipDetect inference."""

import io
from typing import Tuple

import numpy as np
import torch
from PIL import Image
from skimage.measure import regionprops
from skimage.segmentation import slic
from sklearn.neighbors import kneighbors_graph
from torch_geometric.data import Batch, Data
from torchvision import transforms

from app.models.config import INPUT_SIZE, K_NN, N_SEGMENTS, PH_CHANNELS
from app.models.ph_features import extract_ph_from_cnn


def get_image_transform() -> transforms.Compose:
    return transforms.Compose([
        transforms.Resize((INPUT_SIZE, INPUT_SIZE)),
        transforms.ToTensor(),
        transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
    ])


def build_graph_from_tensor(
    img_tensor: torch.Tensor,
    cnn_model: torch.nn.Module,
    n_segments: int = N_SEGMENTS,
    k: int = K_NN,
    n_ph_channels: int = PH_CHANNELS,
) -> Tuple[Data, np.ndarray]:
    with torch.no_grad():
        feat_map = cnn_model.features(img_tensor)

    ph_features = extract_ph_from_cnn(img_tensor, cnn_model, n_ph_channels)

    img_np = img_tensor.squeeze(0).permute(1, 2, 0).cpu().numpy()
    img_np = (img_np - img_np.min()) / (img_np.max() - img_np.min() + 1e-8)
    segments = slic(img_np, n_segments=n_segments, compactness=10, sigma=1, channel_axis=-1)
    regions = regionprops(segments + 1)

    feat_h, feat_w = feat_map.shape[2], feat_map.shape[3]
    node_features = []
    centroids = []

    for region in regions:
        cy, cx = region.centroid
        fy = min(int(cy / INPUT_SIZE * feat_h), feat_h - 1)
        fx = min(int(cx / INPUT_SIZE * feat_w), feat_w - 1)
        node_feat = feat_map[0, :, fy, fx].cpu().numpy()
        coords = np.array([cx / INPUT_SIZE, cy / INPUT_SIZE], dtype=np.float32)
        node_features.append(np.concatenate([node_feat, coords, ph_features]))
        centroids.append([cx / INPUT_SIZE, cy / INPUT_SIZE])

    node_features = np.array(node_features, dtype=np.float32)
    centroids = np.array(centroids, dtype=np.float32)

    if len(centroids) <= 1:
        edge_index = torch.zeros((2, 0), dtype=torch.long)
        edge_attr = torch.zeros((0, 1), dtype=torch.float32)
    else:
        k_eff = min(k, len(centroids) - 1)
        adjacency = kneighbors_graph(centroids, k_eff, mode="connectivity", include_self=False)
        rows, cols = adjacency.nonzero()
        edge_index = torch.tensor(np.stack([rows, cols], axis=0), dtype=torch.long)
        edge_dist = np.linalg.norm(centroids[rows] - centroids[cols], axis=1, keepdims=True)
        edge_attr = torch.tensor(edge_dist, dtype=torch.float32)

    graph = Data(
        x=torch.tensor(node_features, dtype=torch.float32),
        edge_index=edge_index,
        edge_attr=edge_attr,
    )
    return graph, segments


def preprocess_image(image_bytes: bytes) -> Tuple[torch.Tensor, Image.Image]:
    transform = get_image_transform()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    return transform(image).unsqueeze(0), image


def batch_graph(graph: Data) -> Batch:
    return Batch.from_data_list([graph])
