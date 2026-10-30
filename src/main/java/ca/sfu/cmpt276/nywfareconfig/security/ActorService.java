package ca.sfu.cmpt276.nywfareconfig.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorService {

    /** Value to send in the X-Actor header on Caracal API calls. */
    public String currentActorHeader() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails user) {
            return "operator:" + user.getEmail();
        }
        throw new IllegalStateException("No authenticated operator");
    }
}