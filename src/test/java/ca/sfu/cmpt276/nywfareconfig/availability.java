package com.nyw.fareconfig.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AvailabilityController {

    @GetMapping("/availability")
    public String showAvailability(Model model) {
        // Pass a mock user object if your sidebar expects one
        return "availability/availability"; 
    }
}