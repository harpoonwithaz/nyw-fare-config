package ca.sfu.cmpt276.nywfareconfig.security.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;

@ControllerAdvice
public class GlobalUserModelAdvice {

    @ModelAttribute("currentUser")
    public AppUserDetails currentUser(@AuthenticationPrincipal AppUserDetails user) {
        return user;
    }
}
