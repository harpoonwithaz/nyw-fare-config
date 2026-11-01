package ca.sfu.cmpt276.nywfareconfig.config;

import ca.sfu.cmpt276.nywfareconfig.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.model.Role;
import ca.sfu.cmpt276.nywfareconfig.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final String STARTER_PASSWORD = "1234";

    @Bean
    CommandLineRunner seedUsers(AppUserRepository repo, PasswordEncoder encoder) {
        return args -> {
            seed(repo, encoder, "admin@caracal.com", "admin", Role.CLIENT_SERVICES);
            seed(repo, encoder, "ceo@caracal.com", "ceo", Role.CLIENT_SERVICES);
            seed(repo, encoder, "operator@nywaterway.com", "operator", Role.OPERATOR);
            seed(repo, encoder, "user@nywaterway.com", "user", Role.OPERATOR);
            seed(repo, encoder, "eldon@caracal.com", "eldon", Role.CLIENT_SERVICES);
        };
    }

    private static void seed(AppUserRepository repo, PasswordEncoder encoder,
            String email, String username, Role role) {
        if (!repo.existsByUsernameIgnoreCase(username) && !repo.existsByEmailIgnoreCase(email)) {
            repo.save(new AppUser(email, username, encoder.encode(STARTER_PASSWORD), role, true));
        }
    }
}