package com.template.usermanagement.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByIdAndDeletedFalse(Long id);

    boolean existsByUsernameAndDeletedFalse(String username);

    boolean existsByEmailAndDeletedFalse(String email);

    @Query("SELECT u FROM User u WHERE u.deleted = false " +
            "AND (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<User> findAllActive(@Param("search") String search, Pageable pageable);

    long countByDeletedFalse();

    long countByIsActiveTrueAndDeletedFalse();
}
