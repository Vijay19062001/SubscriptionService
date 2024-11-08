package com.sms.SubscriptionService.repository;
import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findById(Integer userId);
    Optional<Users> findByIdAndDbstatus(Integer userId, Status status);
    Optional<Users> findByUserNameIgnoreCase(String userName);
    boolean existsById(Integer userId);
    Optional<Users>findByAccountNumberAndDbstatus(String accountNumber, Status status);
}
