package tn.esprit.authservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.authservice.entity.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
}
