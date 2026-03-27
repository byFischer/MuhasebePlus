package com.MuhasebePlus.demo.user.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.MuhasebePlus.demo.user.entity.User;



public interface UserRepository extends JpaRepository<User, Long> {
    
}
