package ca.sfu.cmpt276.nywfareconfig.publishing.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.sfu.cmpt276.nywfareconfig.publishing.dto.ConfigurationChangeView;
import ca.sfu.cmpt276.nywfareconfig.publishing.service.StagingService;

/**
 * STORY 5.1: Configuration Review REST API.
 *
 * Returns mock configuration changes from StagingService.
 * Database integration will replace the mock data later.
 */
@RestController
public class PublishingApiController {

    private final StagingService stagingService;

    public PublishingApiController(StagingService stagingService) {
        this.stagingService = stagingService;
    }

    /**
     * STORY 5.1: Retrieve configuration comparisons.
     *
     * Includes O-D and zonal changes.
     */
    @GetMapping("/api/publishing/pending-changes")
    public List<ConfigurationChangeView> getPendingChanges() {
        return stagingService.getConfigurationChanges();
    }
}
