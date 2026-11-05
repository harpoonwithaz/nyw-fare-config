package ca.sfu.cmpt276.nywfareconfig.fare.model;

import java.util.Map;

// Represents an origin-to-destination connection in NYW mode.
public class Route {
    private String destSiteCode;
    private String destName;
    private Map<String, FareProduct> fares;
    
    public Route(String destSiteCode, String destName, Map<String, FareProduct> fares) {
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

    public Map<String, FareProduct> getFares() {
        return fares;
    }

    
}
