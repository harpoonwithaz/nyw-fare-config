package ca.sfu.cmpt276.nywfareconfig.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);
}