package tn.esprit.userservice.entity;


import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    private String id;

    private String firstName;
    private String lastName;
    private int age;
    private String gender;

    private String address;

    private String medicalHistory; // maladies
}