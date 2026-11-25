package ca.sfu.cmpt276.nywfareconfig.alert.service;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.ProductOption;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.SiteOption;

import java.util.List;

public interface CatalogLookup {
    List<SiteOption> sites(String tenantCode);

    List<ProductOption> products(String tenantCode);
}