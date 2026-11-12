package ca.sfu.cmpt276.nywfareconfig.alert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlertController {

    // Service alerts page (Epic 4)
    @GetMapping("/fares/alerts")
    public String alerts() {
        return "alerts/index";
    }
}
