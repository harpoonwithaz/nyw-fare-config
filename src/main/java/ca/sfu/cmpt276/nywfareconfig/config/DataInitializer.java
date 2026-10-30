package ca.sfu.cmpt276.nywfareconfig.config;

import ca.sfu.cmpt276.nywfareconfig.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.model.Role;
import ca.sfu.cmpt276.nywfareconfig.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final String PW = "1234";

    @Bean
    CommandLineRunner seedUsers(AppUserRepository repo, PasswordEncoder encoder) {
        return args -> {
            // The list below is the ONLY set of valid logins.
            // Format: email, username, password, role, enabled
            List<AppUser> users = List.of(
                    new AppUser("admin@caracal.com", "admin", encoder.encode(PW), Role.CLIENT_SERVICES, true),
                    new AppUser("ceo@caracal.com", "ceo", encoder.encode(PW), Role.CLIENT_SERVICES, true),
                    new AppUser("operator@nywaterway.com", "operator", encoder.encode(PW), Role.OPERATOR, true),
                    new AppUser("user@nywaterway.com", "user", encoder.encode(PW), Role.OPERATOR, true),
                    new AppUser("eldon@caracal.com", "eldon", encoder.encode(PW), Role.CLIENT_SERVICES, true));

            repo.deleteAll(); // wipes old users (including the earlier test accounts)
            repo.saveAll(users); // re-creates exactly the list above
        };
    }
}