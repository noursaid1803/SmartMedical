package tn.esprit.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.authservice.entity.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String message;
    private boolean success;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String email;
        private String firstName;
        private String lastName;
        private String[] roles;
        private boolean verified;
        @Builder.Default
        private boolean needsVerification = false;
        @Builder.Default
        private boolean needsPasswordChange = false;
    }
}
