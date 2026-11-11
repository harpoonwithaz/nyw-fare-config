package ca.sfu.cmpt276.nywfareconfig.availability.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller 
public class AvailabilityController {
    
    @GetMapping("availability")
    public String getAvailability() {
        return "availability/availability";
    }
    
}
