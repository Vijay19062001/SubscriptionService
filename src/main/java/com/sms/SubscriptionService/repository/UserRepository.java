package com.sms.SubscriptionService.repository;
import com.sms.SubscriptionService.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findById(Integer userId);
    Optional<Users>findByuserName(String userName);
}
