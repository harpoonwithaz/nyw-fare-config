package ca.sfu.cmpt276.nywfareconfig.alert.dto;

import java.util.List;
import java.util.Map;

import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;

// Everything the alert form needs from Caracal. loaded is false when either the
// site list or the product list could not be fetched.
public record AlertCatalog(Map<String, List<RouteOption>> routes, List<CatalogOption> products, boolean loaded) {
}
