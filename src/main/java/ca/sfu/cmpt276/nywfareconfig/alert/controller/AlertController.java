package ca.sfu.cmpt276.nywfareconfig.alert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlertController {

    // Placeholder page for Epic 4; the warning list and form arrive in Iteration 2
    @GetMapping("/fares/alerts")
    public String alerts() {
        return "alerts/index";
    }
}
