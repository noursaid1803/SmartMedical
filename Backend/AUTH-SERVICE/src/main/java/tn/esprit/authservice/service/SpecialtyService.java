package tn.esprit.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.authservice.entity.Specialty;
import tn.esprit.authservice.repository.SpecialtyRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    /**
     * Initialize all medical specialties with their AI model information
     */
    public void initializeSpecialties() {
        if (specialtyRepository.count() > 0) {
            log.info("Specialties already initialized, skipping");
            return;
        }

        List<Specialty> specialties = Arrays.asList(
            // Cerveau (2D/3D Tumeurs)
            Specialty.builder()
                .code("CERVEAU")
                .name("Cerveau (2D/3D Tumeurs)")
                .icon("🧠")
                .description("Segmentation volumétrique de tumeurs cérébrales basée sur UNet 3D avec post-traitement CRF. Permet la détection de gliomes et métastases sur IRM multi-séquences.")
                .modelName("3D U-Net for Brain Tumor Segmentation")
                .modelArchitecture("UNet 3D")
                .dataset("BraTS (Brain Tumor Segmentation Challenge)")
                .referencePaper("3D U-Net for Brain Tumor Segmentation — BrainLesion Workshop (MICCAI 2019)")
                .doi("10.1007/978-3-030-46640-4_12")
                .acceptedFileTypes(Arrays.asList("DICOM", "NIfTI", "NII", "PNG", "JPEG"))
                .acceptedOrgans(Arrays.asList("cerveau", "brain"))
                .minImageWidth(128)
                .minImageHeight(128)
                .maxImageWidth(512)
                .maxImageHeight(512)
                .maxFileSizeBytes(50L * 1024 * 1024) // 50MB
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),

            // Sein / Mammographie
            Specialty.builder()
                .code("SEIN")
                .name("Sein / Mammographie")
                .icon("🎀")
                .description("Réseau profond pour la classification de lésions mammaires BI-RADS, intégrant des cartes d'attention pour les microcalcifications.")
                .modelName("Deep Learning for Breast Cancer Detection")
                .modelArchitecture("Deep CNN with Attention Maps")
                .dataset("DDSM / MIAS")
                .referencePaper("Deep Learning for Breast Cancer Detection on Mammography — Radiology: Artificial Intelligence")
                .doi("10.1148/ryai.2020190227")
                .acceptedFileTypes(Arrays.asList("DICOM", "PNG", "JPEG"))
                .acceptedOrgans(Arrays.asList("sein", "breast", "mammography"))
                .minImageWidth(256)
                .minImageHeight(256)
                .maxImageWidth(4096)
                .maxImageHeight(4096)
                .maxFileSizeBytes(30L * 1024 * 1024) // 30MB
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),

            // Lésions cutanées
            Specialty.builder()
                .code("PEAU")
                .name("Lésions cutanées")
                .icon("🩺")
                .description("Architecture EfficientNet-B4 fine-tunée sur ISIC 2020 pour la classification et segmentation de lésions pigmentées (mélanome, naevus).")
                .modelName("EfficientNet-B4 Skin Lesion Classifier")
                .modelArchitecture("EfficientNet-B4")
                .dataset("ISIC 2020")
                .referencePaper("Dermatologist-level classification of skin cancer with deep neural networks — Nature")
                .doi("10.1038/nature21056")
                .acceptedFileTypes(Arrays.asList("PNG", "JPEG", "JPG"))
                .acceptedOrgans(Arrays.asList("peau", "skin", "lesion"))
                .minImageWidth(224)
                .minImageHeight(224)
                .maxImageWidth(1024)
                .maxImageHeight(1024)
                .maxFileSizeBytes(10L * 1024 * 1024) // 10MB
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),

            // Rétinopathie diabétique
            Specialty.builder()
                .code("OEIL")
                .name("Rétinopathie diabétique")
                .icon("🩸")
                .description("Réseau ResNet-50 pour la détection de rétinopathie diabétique à partir de fonds d'œil (grading 0-4).")
                .modelName("ResNet-50 Diabetic Retinopathy Detector")
                .modelArchitecture("ResNet-50")
                .dataset("EyePACS / APTOS")
                .referencePaper("Development and Validation of a Deep Learning Algorithm for Detection of Diabetic Retinopathy — JAMA")
                .doi("10.1001/jama.2016.17216")
                .acceptedFileTypes(Arrays.asList("PNG", "JPEG", "JPG", "DICOM"))
                .acceptedOrgans(Arrays.asList("oeil", "eye", "retina", "fundus"))
                .minImageWidth(512)
                .minImageHeight(512)
                .maxImageWidth(2048)
                .maxImageHeight(2048)
                .maxFileSizeBytes(15L * 1024 * 1024) // 15MB
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),

            // Alzheimer (IRM)
            Specialty.builder()
                .code("ALZHEIMER")
                .name("Alzheimer (IRM)")
                .icon("🧠")
                .description("CNN 3D avec couches denses pour la prédiction de la maladie d'Alzheimer à partir d'IRM structurelles (atrophie hippocampique).")
                .modelName("3D Deep Learning for Alzheimer's Disease Diagnosis")
                .modelArchitecture("3D CNN with Dense Layers")
                .dataset("ADNI / OASIS")
                .referencePaper("3D Deep Learning for Alzheimer's Disease Diagnosis — NeuroImage")
                .doi("10.1016/j.neuroimage.2019.02.048")
                .acceptedFileTypes(Arrays.asList("DICOM", "NIfTI", "NII"))
                .acceptedOrgans(Arrays.asList("cerveau", "brain", "hippocampus"))
                .minImageWidth(128)
                .minImageHeight(128)
                .maxImageWidth(256)
                .maxImageHeight(256)
                .maxFileSizeBytes(100L * 1024 * 1024) // 100MB
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),

            // Poumon / Nodules
            Specialty.builder()
                .code("POUMON")
                .name("Poumon / Nodules")
                .icon("🫁")
                .description("UNet 2D sur coupes CT pour la segmentation de nodules pulmonaires et classification de malignité (LIDC dataset).")
                .modelName("UNet 2D Pulmonary Nodule Segmentation")
                .modelArchitecture("UNet 2D")
                .dataset("LIDC-IDRI")
                .referencePaper("Pulmonary Nodule Detection in CT Scans using a 3D Convolutional Neural Network — Medical Physics")
                .doi("10.1002/mp.13945")
                .acceptedFileTypes(Arrays.asList("DICOM", "NIfTI", "NII", "PNG", "JPEG"))
                .acceptedOrgans(Arrays.asList("poumon", "lung", "chest", "thorax"))
                .minImageWidth(256)
                .minImageHeight(256)
                .maxImageWidth(1024)
                .maxImageHeight(1024)
                .maxFileSizeBytes(50L * 1024 * 1024) // 50MB
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );

        specialtyRepository.saveAll(specialties);
        log.info("Initialized {} medical specialties", specialties.size());
    }

    public List<Specialty> getAllSpecialties() {
        return specialtyRepository.findAll();
    }

    public List<Specialty> getActiveSpecialties() {
        return specialtyRepository.findAll().stream()
                .filter(Specialty::isActive)
                .toList();
    }

    public Specialty getSpecialtyByCode(String code) {
        return specialtyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Specialty not found: " + code));
    }

    public Specialty getSpecialtyById(String id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Specialty not found: " + id));
    }
}
