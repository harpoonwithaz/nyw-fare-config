package ca.sfu.cmpt276.nywfareconfig.repository;

import ca.sfu.cmpt276.nywfareconfig.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
}