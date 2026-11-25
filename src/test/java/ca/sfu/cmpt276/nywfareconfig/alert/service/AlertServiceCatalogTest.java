package ca.sfu.cmpt276.nywfareconfig.alert.service;

import ca.sfu.cmpt276.nywfareconfig.alert.dto.AlertForm;
import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.RouteScheduleRepository;
import ca.sfu.cmpt276.nywfareconfig.alert.repository.ServiceAlertRepository;
import ca.sfu.cmpt276.nywfareconfig.caracal.exception.CaracalApiException;
import ca.sfu.cmpt276.nywfareconfig.fare.dto.CatalogOption;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import ca.sfu.cmpt276.nywfareconfig.fare.service.FareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertServiceCatalogTest {

    private static final String TENANT = "NYW_SANDBOX";
    private static final List<String> BOTH = AlertLimits.LANGUAGES;

    private ServiceAlertRepository alerts;
    private FareService fares;
    private AlertService service;

    @BeforeEach
    void setUp() {
        alerts = mock(ServiceAlertRepository.class);
        fares = mock(FareService.class);
        service = new AlertService(alerts, mock(RouteScheduleRepository.class), fares);
        when(fares.getSecondaryFilterOptions(TENANT, GridMode.OD)).thenReturn(List.of(
                new CatalogOption("PIER11", "Pier 11"),
                new CatalogOption("HOB14ST", "Hoboken 14th St")));
        when(fares.getOdProducts(TENANT)).thenReturn(List.of(new CatalogOption("ADULT_ONEWAY", "Adult One-Way")));
    }

    private AlertForm form(String routeKey, String productCode) {
        AlertForm form = new AlertForm();
        form.setRouteKeys(List.of(routeKey));
        form.setProductCode(productCode);
        form.setMessageEn("No service on weekends.");
        return form;
    }

    private ServiceAlert savedAlert() {
        ArgumentCaptor<ServiceAlert> captor = ArgumentCaptor.forClass(ServiceAlert.class);
        verify(alerts).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void savesWhenSitesAndTicketAreInTheCatalog() {
        List<String> errors = service.save(TENANT, null, form("pier11:hob14st", "adult_oneway"), "admin@caracal.com", BOTH);

        assertTrue(errors.isEmpty());
        assertEquals("ADULT_ONEWAY", savedAlert().getProductCode());
        assertTrue(savedAlert().getRouteKeys().contains("PIER11:HOB14ST"));
    }

    @Test
    void rejectsAnUnknownSite() {
        List<String> errors = service.save(TENANT, null, form("PIER11:NOWHERE", null), "admin@caracal.com", BOTH);

        assertTrue(errors.contains("Unknown site code: NOWHERE."));
        verify(alerts, never()).save(any(ServiceAlert.class));
    }

    @Test
    void rejectsAnUnknownTicket() {
        List<String> errors = service.save(TENANT, null, form("PIER11:HOB14ST", "MYSTERY_PASS"), "admin@caracal.com", BOTH);

        assertTrue(errors.contains("Unknown ticket: MYSTERY_PASS."));
        verify(alerts, never()).save(any(ServiceAlert.class));
    }

    @Test
    void keepsARetiredTicketThatWasAlreadySaved() {
        ServiceAlert existing = new ServiceAlert(TENANT);
        existing.setProductCode("OLD_PASS");
        when(alerts.lockByIdAndTenantCode(7L, TENANT)).thenReturn(Optional.of(existing));

        List<String> errors = service.save(TENANT, 7L, form("PIER11:HOB14ST", "OLD_PASS"), "admin@caracal.com", BOTH);

        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals("OLD_PASS", savedAlert().getProductCode());
    }

    @Test
    void refusesToSaveWhenTheCatalogIsDown() {
        when(fares.getSecondaryFilterOptions(TENANT, GridMode.OD)).thenThrow(new CaracalApiException("down"));

        List<String> errors = service.save(TENANT, null, form("PIER11:HOB14ST", null), "admin@caracal.com", BOTH);

        assertTrue(errors.contains("The Caracal catalog is not available right now. Try again in a moment."));
        verify(alerts, never()).save(any(ServiceAlert.class));
    }

    @Test
    void rejectsALabelWithItsPlaceholderLeftIn() {
        AlertForm form = form("PIER11:HOB14ST", null);
        form.setConfirmLabelEn("Buy for [Day]");

        List<String> errors = service.save(TENANT, null, form, "admin@caracal.com", BOTH);

        assertTrue(errors.contains("Replace the placeholder in brackets, like [Day], in the button label before saving."));
        verify(alerts, never()).save(any(ServiceAlert.class));
    }

    @Test
    void englishOnlyTenantGetsNoSpanishDefaults() {
        assertTrue(service.save(TENANT, null, form("PIER11:HOB14ST", null), "admin@caracal.com", List.of("en")).isEmpty());

        assertEquals("Continue", savedAlert().getConfirmLabelEn());
        assertNull(savedAlert().getConfirmLabelEs());
        assertNull(savedAlert().getDismissLabelEs());
    }

    @Test
    void routeOptionsAreGroupedByOriginWithNames() {
        var options = service.routeOptionsByTerminal(TENANT);

        assertEquals(2, options.size());
        var fromPier = options.get("Pier 11 (PIER11)");
        assertEquals("HOB14ST", fromPier.get(0).destinationCode());
        assertEquals("PIER11:HOB14ST", fromPier.get(0).key());
    }

    @Test
    void productFailureMarksTheCatalogNotLoaded() {
        when(fares.getOdProducts(TENANT)).thenThrow(new CaracalApiException("down"));

        var catalog = service.formCatalog(TENANT);

        assertFalse(catalog.loaded());
        assertEquals(2, catalog.routes().size());
    }

    @Test
    void singleSiteTenantHasNoRoutes() {
        when(fares.getSecondaryFilterOptions(TENANT, GridMode.OD)).thenReturn(List.of(new CatalogOption("PIER11", "Pier 11")));

        var catalog = service.formCatalog(TENANT);

        assertTrue(catalog.loaded());
        assertTrue(catalog.routes().isEmpty());
    }

    @Test
    void languagesFollowTenantLocales() {
        assertEquals(List.of("en", "es"), AlertLimits.languagesFor(List.of("en-US", "es-US")));
        assertEquals(List.of("en"), AlertLimits.languagesFor(List.of("en-US")));
        assertEquals(List.of("en", "es"), AlertLimits.languagesFor(List.of()));
    }
}
