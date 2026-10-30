package ca.sfu.cmpt276.nywfareconfig.security;

import ca.sfu.cmpt276.nywfareconfig.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    public CustomUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String id = identifier == null ? "" : identifier.trim();
        AppUser user = repository.findByEmailIgnoreCaseOrUsernameIgnoreCase(id, id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new AppUserDetails(user);
    }
}