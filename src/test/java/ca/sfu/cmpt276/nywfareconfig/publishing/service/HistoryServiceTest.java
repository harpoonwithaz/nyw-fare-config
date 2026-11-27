package ca.sfu.cmpt276.nywfareconfig.publishing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ca.sfu.cmpt276.nywfareconfig.caracal.CaracalApiClient;

class HistoryServiceTest {

    @Mock
    private CaracalApiClient caracalApiClient;

    private HistoryService historyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        historyService = new HistoryService(caracalApiClient);
    }

    @Test
    void getPublicationHistoryReturnsCaracalResponse() throws Exception {
        JsonNode expected = objectMapper.readTree("""
            {"entries": [{"id": "pub-001"}]}
            """);

        when(caracalApiClient.fetchPublicationHistory("NYW_SANDBOX"))
                .thenReturn(expected);

        JsonNode actual = historyService.getPublicationHistory("NYW_SANDBOX");

        assertEquals(expected, actual);
        verify(caracalApiClient).fetchPublicationHistory("NYW_SANDBOX");
    }

    @Test
    void getAuditHistoryRequestsTwentyEntries() throws Exception {
        JsonNode expected = objectMapper.readTree("""
            {"entries": [{"function": "catalog-publish"}]}
            """);

        when(caracalApiClient.fetchAuditHistory("NYW_SANDBOX", 20))
                .thenReturn(expected);

        JsonNode actual = historyService.getAuditHistory("NYW_SANDBOX");

        assertEquals(expected, actual);
        verify(caracalApiClient).fetchAuditHistory("NYW_SANDBOX", 20);
    }
}
