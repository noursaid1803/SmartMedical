package tn.esprit.userservice.repository;



import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.userservice.entity.User;


public interface UserRepository extends MongoRepository<User, String> {
}
