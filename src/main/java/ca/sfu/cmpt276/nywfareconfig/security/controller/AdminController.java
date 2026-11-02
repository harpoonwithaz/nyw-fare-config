package ca.sfu.cmpt276.nywfareconfig.security.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import ca.sfu.cmpt276.nywfareconfig.security.service.UserService;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String users(@AuthenticationPrincipal AppUserDetails user, Model model) {
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("roles", Role.values());
        model.addAttribute("currentUserId", user.getId());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@AuthenticationPrincipal AppUserDetails user,
            @PathVariable("id") Long id,
            @RequestParam(name = "role", defaultValue = "") String role) {
        String error = userService.changeRole(user.getId(), id, role);
        if (error != null) {
            return "redirect:/admin/users?error=" + error;
        }
        return "redirect:/admin/users?roleChanged";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("roles", Role.values());
        return "admin/create-account";
    }

    @PostMapping("/users/new")
    public String createUser(@RequestParam(name = "email", defaultValue = "") String email,
            @RequestParam(name = "username", defaultValue = "") String username,
            @RequestParam(name = "role", defaultValue = "") String role,
            @RequestParam(name = "password", defaultValue = "") String password,
            @RequestParam(name = "confirmPassword", defaultValue = "") String confirmPassword,
            Model model) {
        List<String> errors = userService.createUser(email, username, password, confirmPassword, role);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("selectedRole", role);
            model.addAttribute("roles", Role.values());
            return "admin/create-account";
        }
        return "redirect:/admin/users?created";
    }
}