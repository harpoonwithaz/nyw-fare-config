package ca.sfu.cmpt276.nywfareconfig.security.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import ca.sfu.cmpt276.nywfareconfig.security.PasswordPolicy;
import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import ca.sfu.cmpt276.nywfareconfig.security.repository.AppUserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");

    private final AppUserRepository repository;
    private final PasswordEncoder encoder;
    private final ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessions;

    public UserService(AppUserRepository repository,
            PasswordEncoder encoder,
            ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessions) {
        this.repository = repository;
        this.encoder = encoder;
        this.sessions = sessions;
    }

    public List<AppUser> listUsers() {
        return repository.findAll(Sort.by("username").ascending());
    }

    public List<String> createUser(String email, String username, String password,
            String confirmPassword, String roleName) {
        List<String> errors = new ArrayList<>();

        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        String cleanUsername = username == null ? "" : username.trim().toLowerCase();
        Role role = parseRole(roleName);

        boolean emailValid = EMAIL.matcher(cleanEmail).matches();
        boolean usernameValid = USERNAME.matcher(cleanUsername).matches();

        if (!emailValid) {
            errors.add("Enter a valid email address.");
        } else if (repository.existsByEmailIgnoreCase(cleanEmail)) {
            errors.add("That email is already registered.");
        }

        if (!usernameValid) {
            errors.add("Username must be 3 to 30 characters using letters, numbers, dots, underscores or hyphens.");
        } else if (repository.existsByUsernameIgnoreCase(cleanUsername)) {
            errors.add("That username is already taken.");
        }

        if (role == null) {
            errors.add("Choose a valid role.");
        }

        if (password == null || !password.equals(confirmPassword)) {
            errors.add("Passwords do not match.");
        }
        errors.addAll(PasswordPolicy.validate(password));

        if (!errors.isEmpty()) {
            return errors;
        }

        try {
            repository.saveAndFlush(new AppUser(cleanEmail, cleanUsername, encoder.encode(password), role, true));
        } catch (DataIntegrityViolationException ex) {
            errors.add("That username or email is already in use.");
        }
        return errors;
    }

    public List<String> changePassword(Long userId, String currentPassword,
            String newPassword, String confirmPassword) {
        List<String> errors = new ArrayList<>();

        AppUser user = repository.findById(userId).orElse(null);
        if (user == null) {
            errors.add("Account not found.");
            return errors;
        }

        if (currentPassword == null
                || PasswordPolicy.exceedsLimit(currentPassword)
                || !encoder.matches(currentPassword, user.getPasswordHash())) {
            errors.add("Current password is incorrect.");
        }

        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            errors.add("New passwords do not match.");
        }
        errors.addAll(PasswordPolicy.validate(newPassword));

        if (currentPassword != null && currentPassword.equals(newPassword)) {
            errors.add("New password must be different from the current password.");
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        user.setPasswordHash(encoder.encode(newPassword));
        repository.save(user);
        return errors;
    }

    public String changeRole(Long actorId, Long targetId, String roleName) {
        Role role = parseRole(roleName);
        if (role == null) {
            return "invalid";
        }
        if (actorId != null && actorId.equals(targetId)) {
            return "self";
        }

        AppUser target = repository.findById(targetId).orElse(null);
        if (target == null) {
            return "notfound";
        }
        if (target.getRole() == role) {
            return null;
        }

        target.setRole(role);
        repository.save(target);

        String username = target.getUsername();
        sessions.ifAvailable(repo -> endSessions(repo, username));
        return null;
    }

    private void endSessions(FindByIndexNameSessionRepository<? extends Session> repo, String username) {
        for (String sessionId : repo.findByPrincipalName(username).keySet()) {
            repo.deleteById(sessionId);
        }
    }

    private Role parseRole(String name) {
        try {
            return Role.valueOf(name == null ? "" : name.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}