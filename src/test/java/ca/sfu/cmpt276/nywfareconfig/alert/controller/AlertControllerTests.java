package ca.sfu.cmpt276.nywfareconfig.alert.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;

// Full context like FareControllerMockMvcTests, since TenantContext is session scoped
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:alert-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.session.jdbc.initialize-schema=embedded"
})
@AutoConfigureMockMvc
class AlertControllerTests {

    private static final AppUserDetails TEST_USER = new AppUserDetails(
        new AppUser("test@example.com", "test-user", "password", Role.OPERATOR, true));

    private static final String NO_TENANT = "No Tenant Selected";

    @Autowired
    private MockMvc mockMvc;

    private MockHttpServletRequestBuilder withSession(MockHttpServletRequestBuilder builder,
            MockHttpSession session, Cookie cookie) {
        builder.session(session).with(user(TEST_USER));
        if (cookie != null) {
            builder.cookie(cookie);
        }
        return builder;
    }

    @Test
    void loggedOutUserIsSentToLogin() throws Exception {
        mockMvc.perform(get("/fares/alerts"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void noTenantShowsSelectTenantPrompt() throws Exception {
        mockMvc.perform(get("/fares/alerts").with(user(TEST_USER)))
            .andExpect(status().isOk())
            .andExpect(view().name("alerts/alerts"))
            .andExpect(model().attributeDoesNotExist("alerts"))
            .andExpect(content().string(containsString(NO_TENANT)));
    }

    @Test
    void selectedTenantShowsAlertList() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cookie cookie = mockMvc.perform(withSession(post("/context/tenant"), session, null)
                .with(csrf())
                .param("tenant", "NYW_SANDBOX")
                .param("returnUrl", "/fares/alerts"))
            .andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getCookie("SESSION");

        mockMvc.perform(withSession(get("/fares/alerts"), session, cookie).param("status", "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(view().name("alerts/alerts"))
            .andExpect(model().attributeExists("alerts", "routeOptions"))
            .andExpect(model().attribute("status", "DRAFT"))
            .andExpect(content().string(not(containsString(NO_TENANT))));
    }
}
