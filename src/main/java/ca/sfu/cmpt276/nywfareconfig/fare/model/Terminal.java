package ca.sfu.cmpt276.nywfareconfig.fare.model;

import java.util.List;

public class Terminal {
    // Identification
    private String originId;
    private String siteCode;
    private String name;
    private String timeZone;

    // Matrix values
    private List<String> productHeaders;
    private List<Route> routes;

    // For just populating the origin terminal dropdown
    public Terminal(String originId, String name) {
        this.originId = originId;
        this.name = name;
    }

    public Terminal(String originId, String siteCode, String name, String timeZone, List<String>  productHeaders, List<Route> routes) {
        this.originId = originId;
        this.siteCode = siteCode;
        this.name = name;
        this.timeZone = timeZone;
        this.productHeaders = productHeaders;
        this.routes = routes;
    }


    public String getOriginId() {
        return originId;
    }


    public String getSiteCode() {
        return siteCode;
    }


    public String getName() {
        return name;
    }


    public String getTimeZone() {
        return timeZone;
    }


    public List<String> getProductHeaders() {
        return productHeaders;
    }


    public List<Route> getRoutes() {
        return routes;
    }


    
}
