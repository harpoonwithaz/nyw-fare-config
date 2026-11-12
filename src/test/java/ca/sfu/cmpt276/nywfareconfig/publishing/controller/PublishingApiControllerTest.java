package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;

class PublishingApiControllerTest {

    @Test
    void shouldReturnConfigurationChanges() {

        // STORY 5.1: Mock the staging service.
        StagingService stagingService = mock(StagingService.class);

        ConfigurationChangeView change = new ConfigurationChangeView(
            "NYW_SANDBOX",
            "OD",
            "EDGEWTR",
            "Midtown / W. 39th St",
            "Adult One-Way",
            "Adult One-Way",
            new BigDecimal("13.25"),
            new BigDecimal("14.00")
        );

        when(stagingService.getConfigurationChanges())
            .thenReturn(List.of(change));

        // STORY 5.1: Create the controller with the mocked service.
        PublishingApiController controller =
            new PublishingApiController(stagingService);

        // Call the controller method directly.
        List<ConfigurationChangeView> result =
            controller.getPendingChanges();

        // Verify the returned configuration.
        assertEquals(1, result.size());
        assertEquals("NYW_SANDBOX", result.get(0).tenantCode());
        assertEquals("OD", result.get(0).gridMode());
        assertEquals("EDGEWTR", result.get(0).selectedContext());
        assertTrue(result.get(0).isChanged());
    }
}

