package tn.esprit.authservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.authservice.entity.Specialty;

import java.util.Optional;

@Repository
public interface SpecialtyRepository extends MongoRepository<Specialty, String> {
    Optional<Specialty> findByCode(String code);
    boolean existsByCode(String code);
}
