package ca.sfu.cmpt276.nywfareconfig.alert.service;

import java.util.List;

import org.springframework.stereotype.Component;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.ProductOption;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.SiteOption;
import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;

// Sites and tickets for the alert form, read live from Caracal's site-list and product-list
@Component
public class CaracalCatalogLookup implements CatalogLookup {

    private final CaracalApiClient caracal;

    public CaracalCatalogLookup(CaracalApiClient caracal) {
        this.caracal = caracal;
    }

    @Override
    public List<SiteOption> sites(String tenantCode) {
        try {
            return orEmpty(caracal.fetchSites(tenantCode)).stream()
                    .map(site -> new SiteOption(site.siteCode(), site.name()))
                    .toList();
        } catch (CaracalApiException ex) {
            throw new CatalogUnavailableException("Could not load sites for " + tenantCode, ex);
        }
    }

    // Inactive tickets can't be sold, so they aren't offered on an alert
    @Override
    public List<ProductOption> products(String tenantCode) {
        try {
            return orEmpty(caracal.fetchProducts(tenantCode)).stream()
                    .filter(product -> product.isActive())
                    .map(product -> new ProductOption(product.code(), product.name()))
                    .toList();
        } catch (CaracalApiException ex) {
            throw new CatalogUnavailableException("Could not load tickets for " + tenantCode, ex);
        }
    }

    // Caracal can send a response with no list in it
    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
