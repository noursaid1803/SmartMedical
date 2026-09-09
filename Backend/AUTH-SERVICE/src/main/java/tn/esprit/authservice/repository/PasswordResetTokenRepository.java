package tn.esprit.authservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.authservice.entity.PasswordResetToken;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    
    Optional<PasswordResetToken> findByEmail(String email);
    
    Optional<PasswordResetToken> findByEmailAndCode(String email, String code);
    
    void deleteByEmail(String email);
}
