package ca.sfu.cmpt276.nywfareconfig.alert.service;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.ProductOption;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.SiteOption;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TemporaryCatalog implements CatalogLookup {

    @Override
    public List<SiteOption> sites(String tenantCode) {
        return List.of(new SiteOption("PIER11", "PIER11"), new SiteOption("HOB14ST", "HOB14ST"));
    }

    @Override
    public List<ProductOption> products(String tenantCode) {
        return List.of(new ProductOption("ADULT_ONEWAY", "Adult One-Way"));
    }
}