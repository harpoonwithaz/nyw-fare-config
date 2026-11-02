package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import ca.sfu.cmpt276.nywfareconfig.fare.model.Terminal;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
import jakarta.servlet.http.HttpServletResponse;


@Controller 
public class FareController {
    

    // Empty fair view
    @GetMapping("fares")
    public String getAllTerminals(Model model) {

        // Get list of terminals
        List<Terminal> originTerminals = FareService.getOriginTerminals(); // only returns terminal name and id

        // Add origin terminals attribute to model
        model.addAttribute("originTerminals", originTerminals);

        return "fare/fare";
    }

    // Fair view for origin terminal
    @GetMapping("fares/")
    public String getOriginTerminal(@RequestParam("terminal") String id, Model model) {
        // TODO: Make the service method return an optional and check for errors
        // Terminal terminal = FareService.getTerminalById(id);
        
        // model.addAttribute("terminal", terminal);
        // System.out.println(terminal.getRoutes().get(0).getFares().get("CHILD_ONEWAY"));
        return "fare/terminal-fare";
    }
    
    
}
