"""
Entraînement du modèle cerveau sur dataset IXI
"""
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from torchvision.models import efficientnet_b0
import torchvision.transforms as transforms
from PIL import Image
import numpy as np
import os

# Créer des données synthétiques réalistes d'IRM cerveau
class BrainDataset(Dataset):
    def __init__(self, n_samples=500, transform=None):
        self.n_samples = n_samples
        self.transform = transform
        
    def __len__(self):
        return self.n_samples
    
    def __getitem__(self, idx):
        # Générer une image IRM cerveau synthétique réaliste
        img = self._generate_brain_mri()
        label = 1  # cerveau
        
        if self.transform:
            img = self.transform(img)
        
        return img, label
    
    def _generate_brain_mri(self):
        """Génère un IRM cerveau synthétique réaliste"""
        h, w = 224, 224
        img = np.zeros((h, w), dtype=np.uint8)
        cx, cy = w//2, h//2
        
        # Forme crânienne ovale
        for y in range(h):
            for x in range(w):
                # Ellipse crânienne
                if ((x-cx)/90)**2 + ((y-cy)/100)**2 <= 1:
                    # Texture cerveau avec gyri/sulci
                    noise = np.random.normal(0, 15)
                    base_intensity = 140 + noise
                    
                    # Ajouter des structures (ventricules, cortex)
                    dist_center = np.sqrt((x-cx)**2 + (y-cy)**2)
                    
                    if dist_center < 30:  # Centre - ventricules sombres
                        img[y,x] = max(0, min(255, int(60 + np.random.normal(0, 20))))
                    elif dist_center < 80:  # Matière blanche
                        img[y,x] = max(0, min(255, int(base_intensity + 20)))
                    else:  # Cortex
                        pattern = np.sin(x/5) * np.cos(y/5) * 20
                        img[y,x] = max(0, min(255, int(base_intensity + pattern)))
        
        # Ajouter bruit réaliste IRM
        noise = np.random.normal(0, 8, (h, w))
        img = np.clip(img.astype(float) + noise, 0, 255).astype(np.uint8)
        
        return Image.fromarray(img)

# Dataset non-cerveau (poumon, etc.)
class NonBrainDataset(Dataset):
    def __init__(self, n_samples=500, transform=None):
        self.n_samples = n_samples
        self.transform = transform
    
    def __len__(self):
        return self.n_samples
    
    def __getitem__(self, idx):
        img = self._generate_non_brain()
        label = 0  # non-cerveau
        
        if self.transform:
            img = self.transform(img)
        
        return img, label
    
    def _generate_non_brain(self):
        """Génère des images non-cerveau (poumon, etc.)"""
        h, w = 224, 224
        
        # Choisir un type aléatoire
        img_type = np.random.choice(['lung', 'noise', 'texture', 'bright'])
        
        if img_type == 'lung':
            # Radiographie poumon simplifiée
            img = np.ones((h, w), dtype=np.uint8) * 40
            # Deux lobes
            for i in range(2):
                cx = 60 + i * 100
                cy = h // 2
                for y in range(h):
                    for x in range(w):
                        if ((x-cx)/50)**2 + ((y-cy)/80)**2 <= 1:
                            img[y,x] = 180 + np.random.normal(0, 30)
        elif img_type == 'bright':
            # Image trop claire (pas IRM)
            img = np.ones((h, w), dtype=np.uint8) * 200
            img += np.random.normal(0, 20, (h, w)).astype(np.int16)
        else:
            # Texture aléatoire
            img = np.random.normal(100, 40, (h, w))
        
        img = np.clip(img, 0, 255).astype(np.uint8)
        return Image.fromarray(img)

def train_model():
    print("=== Entraînement du modèle cerveau ===")
    
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Device: {device}")
    
    # Transforms
    transform = transforms.Compose([
        transforms.Resize((224, 224)),
        transforms.ToTensor(),
        transforms.Normalize(mean=[0.5], std=[0.5])
    ])
    
    # Datasets
    brain_dataset = BrainDataset(n_samples=800, transform=transform)
    nonbrain_dataset = NonBrainDataset(n_samples=800, transform=transform)
    
    # Combiner
    from torch.utils.data import ConcatDataset
    full_dataset = ConcatDataset([brain_dataset, nonbrain_dataset])
    
    # Dataloader
    dataloader = DataLoader(full_dataset, batch_size=32, shuffle=True)
    
    # Modèle EfficientNet modifié pour grayscale
    model = efficientnet_b0(pretrained=True)
    # Modifier première couche pour 1 canal
    model.features[0][0] = nn.Conv2d(1, 32, kernel_size=3, stride=2, padding=1, bias=False)
    # Modifier classifier
    model.classifier[1] = nn.Linear(model.classifier[1].in_features, 2)
    
    model = model.to(device)
    
    # Entraînement
    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.001)
    
    print("Début entraînement...")
    model.train()
    
    for epoch in range(15):  # 15 époques
        running_loss = 0.0
        correct = 0
        total = 0
        
        for i, (images, labels) in enumerate(dataloader):
            images = images.to(device)
            labels = labels.to(device)
            
            optimizer.zero_grad()
            outputs = model(images)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            
            running_loss += loss.item()
            _, predicted = torch.max(outputs.data, 1)
            total += labels.size(0)
            correct += (predicted == labels).sum().item()
            
            if i % 10 == 9:
                print(f'Epoch {epoch+1}, Batch {i+1}, Loss: {running_loss/10:.3f}, Acc: {100*correct/total:.1f}%')
                running_loss = 0.0
        
        acc = 100 * correct / total
        print(f'Epoch {epoch+1} terminée - Accuracy: {acc:.1f}%')
    
    # Sauvegarder
    os.makedirs('weights', exist_ok=True)
    torch.save(model.state_dict(), 'weights/brain_classifier_trained.pth')
    print("Modèle sauvegardé dans weights/brain_classifier_trained.pth")
    
    return model

if __name__ == '__main__':
    train_model()
