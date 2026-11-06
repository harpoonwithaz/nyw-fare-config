package ca.sfu.cmpt276.nywfareconfig.fare.model;

import java.util.Map;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.FareCell;

// Represents an origin-to-destination connection in NYW mode.
public class Route {
    private String destSiteCode;
    private String destName;
    private Map<String, FareCell> fares;
    
    public Route(String destSiteCode, String destName, Map<String, FareCell> fares) {
        this.destSiteCode = destSiteCode;
        this.destName = destName;
        this.fares = fares;
    }

    public String getDestSiteCode() {
        return destSiteCode;
    }

    public String getDestName() {
        return destName;
    }

    public Map<String, FareCell> getFares() {
        return fares;
    }

    
}
