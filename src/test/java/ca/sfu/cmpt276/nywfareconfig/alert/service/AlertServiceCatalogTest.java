package ca.sfu.cmpt276.nywfareconfig.alert.service;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertForm;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.ProductOption;
import ca.sfu.cmpt276.nywfareconfig.alert.dto.SiteOption;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.RouteScheduleRepository;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.ServiceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertServiceCatalogTest {

    private static final String TENANT = "NYW_SANDBOX";

    private ServiceAlertRepository alerts;
    private CatalogLookup catalog;
    private AlertService service;

    @BeforeEach
    void setUp() {
        alerts = mock(ServiceAlertRepository.class);
        catalog = mock(CatalogLookup.class);
        service = new AlertService(alerts, mock(RouteScheduleRepository.class), catalog);
        when(catalog.sites(TENANT)).thenReturn(List.of(
                new SiteOption("PIER11", "Pier 11"),
                new SiteOption("HOB14ST", "Hoboken 14th St")));
        when(catalog.products(TENANT)).thenReturn(List.of(new ProductOption("ADULT_ONEWAY", "Adult One-Way")));
    }

    private AlertForm form(String routeKey, String productCode) {
        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of(routeKey));
        form.setProductCode(productCode);
        form.setMessageEn("No service on weekends.");
        return form;
    }

    @Test
    void savesWhenSitesAndTicketAreInTheCatalog() {
        List<String> errors = service.save(TENANT, null, form("pier11:hob14st", "adult_oneway"), "admin@caracal.com");

        assertTrue(errors.isEmpty());
        ArgumentCaptor<ServiceAlert> captor = ArgumentCaptor.forClass(ServiceAlert.class);
        verify(alerts).save(captor.capture());
        assertEquals("ADULT_ONEWAY", captor.getValue().getProductCode());
        assertTrue(captor.getValue().getRouteKeys().contains("PIER11:HOB14ST"));
    }

    @Test
    void rejectsAnUnknownSite() {
        List<String> errors = service.save(TENANT, null, form("PIER11:NOWHERE", null), "admin@caracal.com");

        assertTrue(errors.contains("Unknown site code: NOWHERE."));
        verify(alerts, never()).save(any(ServiceAlert.class));
    }

    @Test
    void rejectsAnUnknownTicket() {
        List<String> errors = service.save(TENANT, null, form("PIER11:HOB14ST", "MYSTERY_PASS"), "admin@caracal.com");

        assertTrue(errors.contains("Unknown ticket: MYSTERY_PASS."));
        verify(alerts, never()).save(any(ServiceAlert.class));
    }

    @Test
    void refusesToSaveWhenTheCatalogIsDown() {
        when(catalog.sites(TENANT)).thenThrow(new CatalogUnavailableException("down"));

        List<String> errors = service.save(TENANT, null, form("PIER11:HOB14ST", null), "admin@caracal.com");

        assertTrue(errors.contains("The Caracal catalog is not available right now. Try again in a moment."));
        verify(alerts, never()).save(any(ServiceAlert.class));
    }

    @Test
    void routeOptionsAreBuiltFromTheSiteList() {
        var options = service.routeOptionsByTerminal(TENANT);

        assertEquals(2, options.size());
        assertEquals("HOB14ST", options.get("PIER11").get(0).destinationCode());
        assertEquals("PIER11:HOB14ST", options.get("PIER11").get(0).key());
    }
}