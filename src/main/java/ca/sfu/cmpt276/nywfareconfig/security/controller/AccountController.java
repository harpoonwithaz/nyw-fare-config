package ca.sfu.cmpt276.nywfareconfig.security.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.service.UserService;

import java.util.List;

@Controller
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/account/password")
    public String passwordForm() {
        return "auth/change-password";
    }

    @PostMapping("/account/password")
    public String changePassword(@AuthenticationPrincipal AppUserDetails user,
            @RequestParam(name = "currentPassword", defaultValue = "") String currentPassword,
            @RequestParam(name = "newPassword", defaultValue = "") String newPassword,
            @RequestParam(name = "confirmPassword", defaultValue = "") String confirmPassword,
            Model model) {
        List<String> errors = userService.changePassword(user.getId(), currentPassword, newPassword, confirmPassword);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return "auth/change-password";
        }
        return "redirect:/dashboard?passwordChanged";
    }
}