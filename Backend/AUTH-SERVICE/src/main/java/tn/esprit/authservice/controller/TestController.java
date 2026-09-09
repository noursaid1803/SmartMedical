package tn.esprit.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.authservice.entity.User;
import tn.esprit.authservice.repository.UserRepository;
import tn.esprit.authservice.service.AuthService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login-debug")
    public ResponseEntity<?> loginDebug(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            String token = authService.login(user);
            response.put("token", token);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/user-exists")
    public ResponseEntity<?> userExists(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            response.put("exists", true);
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("verified", user.isVerified());
            response.put("hasPassword", user.getPassword() != null && !user.getPassword().isEmpty());
            response.put("passwordLength", user.getPassword() != null ? user.getPassword().length() : 0);
        } else {
            response.put("exists", false);
        }
        return ResponseEntity.ok(response);
    }
}
