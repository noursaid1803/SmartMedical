package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.entity.Patient;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 👨‍⚕️ CREATE DOCTOR
    @PostMapping("/doctors")
    public User createDoctor(@RequestBody User user) {
        return userService.createDoctor(user);
    }

    // 🧑 CREATE PATIENT
    @PostMapping("/patients")
    public Patient createPatient(@RequestBody Patient patient) {
        return userService.createPatient(patient);
    }

    // GET ALL USERS
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    // GET ALL PATIENTS
    @GetMapping("/patients")
    public List<Patient> getPatients() {
        return userService.getAllPatients();
    }

    // DELETE USER
    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return "User deleted";
    }

    // DELETE PATIENT
    @DeleteMapping("/patients/{id}")
    public String deletePatient(@PathVariable String id) {
        userService.deletePatient(id);
        return "Patient deleted";
    }
}
