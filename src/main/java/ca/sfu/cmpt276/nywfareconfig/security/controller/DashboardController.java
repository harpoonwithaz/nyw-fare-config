package ca.sfu.cmpt276.nywfareconfig.security.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails user, Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", user.getRole() == Role.CLIENT_SERVICES);
        return "dashboard/home";
    }

    @GetMapping("usage-guide")
    public String getUsageGuide() {
        return "dashboard/usage-guide";
    }
}