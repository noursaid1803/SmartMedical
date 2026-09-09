package tn.esprit.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.authservice.dto.CreateAdminRequest;
import tn.esprit.authservice.entity.Patient;
import tn.esprit.authservice.repository.PatientRepository;
import tn.esprit.authservice.service.AdminService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdminService adminService;
    private final PatientRepository patientRepository;

    @Override
    public void run(String... args) {
        log.info("🚀 Initialisation des données...");

        try {
            CreateAdminRequest primaryAdmin = CreateAdminRequest.builder()
                    .name("Administrateur SmartMedical")
                    .email("arijhedhri4@gmail.com")
                    .password("admin123")
                    .build();

            adminService.createBootstrapAdmin(primaryAdmin);
            log.info("✅ Admin principal créé !");
            log.info("📧 Email: arijhedhri4@gmail.com");
            log.info("🔑 Mot de passe: admin123");
            log.info("ℹ️  Accès complet : patients, médecins, ajout d'admins et médecins.");

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("existe déjà")) {
                log.info("ℹ️  Admin principal existe déjà (arijhedhri4@gmail.com).");
            } else {
                log.error("❌ Erreur lors de la création de l'admin: {}", e.getMessage());
            }
        }

        // Créer des patients avec leurs dossiers médicaux
        createSamplePatients();

        log.info("🚀 Initialisation terminée!");
    }

    private void createSamplePatients() {
        log.info("🏥 Création des patients et dossiers médicaux...");

        List<Patient> samplePatients = Arrays.asList(
            Patient.builder()
                .firstName("Ahmed")
                .lastName("Ben Ali")
                .email("ahmed.benali@email.com")
                .phone("+216 98 123 456")
                .dateOfBirth("15/03/1985")
                .age(39)
                .gender("M")
                .address("12 Rue de la Liberté")
                .city("Tunis")
                .region("Tunis")
                .postalCode("1000")
                .country("Tunisie")
                .bloodType("A+")
                .height(175.0)
                .weight(75.0)
                .allergies(Arrays.asList("Pénicilline"))
                .chronicDiseases(Arrays.asList("Hypertension"))
                .currentMedications(Arrays.asList("Amlor 5mg"))
                .occupation("Ingénieur")
                .createdAt(LocalDateTime.now().minusDays(10))
                .build(),

            Patient.builder()
                .firstName("Fatima")
                .lastName("Hassan")
                .email("fatima.hassan@email.com")
                .phone("+216 97 234 567")
                .dateOfBirth("22/07/1990")
                .age(34)
                .gender("F")
                .address("25 Avenue Habib Bourguiba")
                .city("Sfax")
                .region("Sfax")
                .postalCode("3000")
                .country("Tunisie")
                .bloodType("O+")
                .height(165.0)
                .weight(62.0)
                .allergies(Arrays.asList())
                .chronicDiseases(Arrays.asList())
                .currentMedications(Arrays.asList("Vitamine D"))
                .occupation("Enseignante")
                .createdAt(LocalDateTime.now().minusDays(8))
                .build(),

            Patient.builder()
                .firstName("Mohamed")
                .lastName("Khalil")
                .email("mohamed.khalil@email.com")
                .phone("+216 96 345 678")
                .dateOfBirth("10/11/1975")
                .age(49)
                .gender("M")
                .address("8 Rue du Commerce")
                .city("Sousse")
                .region("Sousse")
                .postalCode("4000")
                .country("Tunisie")
                .bloodType("B+")
                .height(180.0)
                .weight(85.0)
                .allergies(Arrays.asList("Lactose"))
                .chronicDiseases(Arrays.asList("Diabète type 2"))
                .currentMedications(Arrays.asList("Metformine 500mg", "Insuline"))
                .occupation("Commerçant")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build(),

            Patient.builder()
                .firstName("Sonia")
                .lastName("Mansour")
                .email("sonia.mansour@email.com")
                .phone("+216 95 456 789")
                .dateOfBirth("05/01/1988")
                .age(36)
                .gender("F")
                .address("42 Rue des Jasmins")
                .city("Ariana")
                .region("Ariana")
                .postalCode("2000")
                .country("Tunisie")
                .bloodType("AB+")
                .height(168.0)
                .weight(58.0)
                .allergies(Arrays.asList("Pollen", "Acariens"))
                .chronicDiseases(Arrays.asList("Asthme"))
                .currentMedications(Arrays.asList("Ventoline", "Corticoïdes"))
                .occupation("Comptable")
                .createdAt(LocalDateTime.now().minusDays(3))
                .build(),

            Patient.builder()
                .firstName("Karim")
                .lastName("Gharbi")
                .email("karim.gharbi@email.com")
                .phone("+216 94 567 890")
                .dateOfBirth("18/09/1992")
                .age(32)
                .gender("M")
                .address("15 Avenue de la République")
                .city("Gabès")
                .region("Gabès")
                .postalCode("6000")
                .country("Tunisie")
                .bloodType("O-")
                .height(178.0)
                .weight(70.0)
                .allergies(Arrays.asList())
                .chronicDiseases(Arrays.asList())
                .currentMedications(Arrays.asList())
                .occupation("Développeur")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build()
        );

        int createdCount = 0;
        for (Patient patient : samplePatients) {
            try {
                if (!patientRepository.existsByEmail(patient.getEmail())) {
                    patient.setCreatedAt(LocalDateTime.now());
                    patient.setUpdatedAt(LocalDateTime.now());
                    patientRepository.save(patient);
                    createdCount++;
                    log.info("✅ Patient créé: {} {}", patient.getFirstName(), patient.getLastName());
                } else {
                    log.info("ℹ️  Patient {} {} existe déjà", patient.getFirstName(), patient.getLastName());
                }
            } catch (Exception e) {
                log.error("❌ Erreur lors de la création du patient {} {}: {}", 
                    patient.getFirstName(), patient.getLastName(), e.getMessage());
            }
        }

        log.info("✅ {} patients créés avec succès!", createdCount);
        log.info("⚠️  Note: Les dossiers médicaux seront créés automatiquement lors de la première consultation.");
        log.info("📋 Chaque patient aura un seul dossier médical par médecin.");
    }
}
