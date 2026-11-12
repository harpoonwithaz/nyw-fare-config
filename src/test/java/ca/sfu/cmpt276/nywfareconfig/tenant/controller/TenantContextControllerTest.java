package ca.sfu.cmpt276.nywfareconfig.tenant.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.TenantContext;
import ca.sfu.cmpt276.nywfareconfig.tenant.service.TenantService;

@ExtendWith(MockitoExtension.class)
class TenantContextControllerTest {

    @Mock
    private TenantService tenantService;

    private TenantContext tenantContext;
    private TenantContextController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tenantContext = new TenantContext();
        controller = new TenantContextController(tenantContext, tenantService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static Tenant createTenant(String code, String displayName) {
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setDisplayName(displayName);
        tenant.setActive(true);
        return tenant;
    }

    @Test
    void switchTenant_WithNewTenant_UpdatesContextAndClearsSecondaryFilters() throws Exception {
        Tenant nyw = createTenant("NYW_SANDBOX", "NY Waterway");
        Tenant metra = createTenant("METRA", "Metra");

        tenantContext.setSelectedTenant(nyw);
        tenantContext.setSelectedOrigin("EDGEWTR");
        tenantContext.setSelectedProduct("ADULT_ONEWAY");

        when(tenantService.getTenantByCode("METRA")).thenReturn(metra);

        mockMvc.perform(post("/context/tenant")
                .param("tenant", "METRA")
                .param("returnUrl", "/fares"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/fares"));

        assertSame(metra, tenantContext.getSelectedTenant());
        assertNull(tenantContext.getSelectedOrigin());
        assertNull(tenantContext.getSelectedProduct());
        verify(tenantService).getTenantByCode("METRA");
    }

    @Test
    void switchTenant_WithSameTenantCaseInsensitive_PreservesSecondaryFilters() throws Exception {
        Tenant nyw = createTenant("NYW_SANDBOX", "NY Waterway");
        tenantContext.setSelectedTenant(nyw);
        tenantContext.setSelectedOrigin("EDGEWTR");
        tenantContext.setSelectedProduct("ADULT_ONEWAY");

        mockMvc.perform(post("/context/tenant")
                .param("tenant", "nyw_sandbox")
                .param("returnUrl", "/availability"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/availability"));

        assertSame(nyw, tenantContext.getSelectedTenant());
        assertEquals("EDGEWTR", tenantContext.getSelectedOrigin());
        assertEquals("ADULT_ONEWAY", tenantContext.getSelectedProduct());
        verify(tenantService, never()).getTenantByCode(anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void switchTenant_WithNullOrBlankTenant_ResetsEntireContext(String blankTenant) {
        Tenant nyw = createTenant("NYW_SANDBOX", "NY Waterway");
        tenantContext.setSelectedTenant(nyw);
        tenantContext.setSelectedOrigin("EDGEWTR");
        tenantContext.setSelectedProduct("ADULT_ONEWAY");

        String view = controller.switchTenant(blankTenant, "/fares");

        assertEquals("redirect:/fares", view);
        assertNull(tenantContext.getSelectedTenant());
        assertNull(tenantContext.getSelectedOrigin());
        assertNull(tenantContext.getSelectedProduct());
        verify(tenantService, never()).getTenantByCode(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/fares",
        "/fares/grid",
        "/availability",
        "/fares/alerts",
        "/review",
        "/publishing",
        "/history",
        "/dashboard",
        "/admin",
        "/admin/users",
        "/account",
        "/account/password"
    })
    void switchTenant_WithAllowedReturnUrls_RedirectsToTargetUrl(String allowedUrl) throws Exception {
        mockMvc.perform(post("/context/tenant")
                .param("returnUrl", allowedUrl))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(allowedUrl));
    }

    @Test
    void switchTenant_WithNormalizedAllowedPath_RedirectsToNormalizedPath() throws Exception {
        mockMvc.perform(post("/context/tenant")
                .param("returnUrl", "/admin/users/../settings"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/settings"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "   ",
        "/\\evil.example",
        "/fares\\evil",
        "//evil.example",
        "///evil.example",
        "/%5cevil.example",
        "/fares%2f..",
        "https://evil.example",
        "http://evil.example/fares",
        "javascript:alert(1)",
        "/unlisted-path",
        "/fares-malicious",
        "/fares?tenant=METRA",
        "/fares#section",
        "/fares/../../etc/passwd",
        "relative/without/leading/slash",
        "http://[::1"
    })
    void switchTenant_WithUnsafeOrDisallowedReturnUrl_FallsBackToDefaultFaresUrl(String unsafeUrl) {
        String result = controller.switchTenant(null, unsafeUrl);
        assertEquals("redirect:/fares", result);
    }
}