package tn.esprit.userservice.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.userservice.Enum.Role;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    private String email;
    private String password;

    private Role role;

    // infos communes
    private String firstName;
    private String lastName;
    private String phone;
}