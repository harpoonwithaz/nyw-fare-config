package ca.sfu.cmpt276.nywfareconfig.alert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AlertController {

    // Epic 4 TVM previewer. Everything on the page runs in the browser, nothing is saved yet
    @GetMapping("/fares/alerts")
    public String alerts() {
        return "alerts/index";
    }
}
