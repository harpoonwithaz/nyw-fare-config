package ca.sfu.cmpt276.nywfareconfig.controller;

import ca.sfu.cmpt276.nywfareconfig.model.Role;
import ca.sfu.cmpt276.nywfareconfig.security.ActorService;
import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ActorService actorService;

    public DashboardController(ActorService actorService) {
        this.actorService = actorService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails user, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("role", user.getRole());
        model.addAttribute("actor", actorService.currentActorHeader());
        model.addAttribute("isAdmin", user.getRole() == Role.CLIENT_SERVICES);
        return "dashboard";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}