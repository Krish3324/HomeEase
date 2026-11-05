package com.homeease.homeease.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.homeease.homeease.model.User;
import com.homeease.homeease.model.Role;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.state) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> searchUsers(@Param("role") Role role, @Param("search") String search);

    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String token);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    long countByEnabled(boolean enabled);
    boolean existsByPublicUserId(String publicUserId);
}
