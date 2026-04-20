package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.Enum.Role;
import tn.esprit.userservice.entity.Patient;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.PatientRepository;
import tn.esprit.userservice.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    // 🔥 CREATE DOCTOR
    public User createDoctor(User user) {
        user.setRole(Role.DOCTOR);
        return userRepository.save(user);
    }

    // 🔥 CREATE PATIENT
    public Patient createPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    // GET ALL USERS
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // GET ALL PATIENTS
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    // DELETE USER
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    // DELETE PATIENT
    public void deletePatient(String id) {
        patientRepository.deleteById(id);
    }
}