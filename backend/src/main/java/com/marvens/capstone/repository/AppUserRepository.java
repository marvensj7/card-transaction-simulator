package com.marvens.capstone.repository;

import java.util.Optional;

import com.marvens.capstone.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

    // A role check does not load the user's account before its balance lock.
    @Query("select u.role from AppUser u where u.id = :userId")
    Optional<AppUser.Role> findRoleById(@Param("userId") Long userId);
}
