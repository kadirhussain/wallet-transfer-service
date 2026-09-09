package com.wallet.transfer.repository;

import com.wallet.transfer.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {


    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    Optional<User> findById(UUID id);

    Optional<User> findByEmailOrMobile(String email, String mobile);
}
