package ca.sfu.cmpt276.nywfareconfig.fare.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.sfu.cmpt276.nywfareconfig.fare.model.*;

public class FareService {
    private static List<String> headers;



    public static List<Terminal> getOriginTerminals() {
        // HARD CODED MOCK DATA
        // TODO: Call database
        
        
        List<Terminal> originTerminals = new ArrayList<Terminal>();

        originTerminals.add(new Terminal("201", "Edgewater"));
        originTerminals.add(new Terminal("100", "Belford"));

        return originTerminals;
    }
}
