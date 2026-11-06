package ca.sfu.cmpt276.nywfareconfig.security.repository;

import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    List<AppUser> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email,
            Sort sort);

    long countByRole(Role role);
}