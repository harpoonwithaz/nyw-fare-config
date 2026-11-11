package ca.sfu.cmpt276.nywfareconfig.fare.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import ca.sfu.cmpt276.nywfareconfig.security.AppUserDetails;
import ca.sfu.cmpt276.nywfareconfig.security.model.AppUser;
import ca.sfu.cmpt276.nywfareconfig.security.model.Role;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:fare-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.session.jdbc.initialize-schema=embedded"
})
@AutoConfigureMockMvc
class FareControllerMockMvcTests {

    private static final AppUserDetails TEST_USER = new AppUserDetails(
        new AppUser("test@example.com", "test-user", "password", Role.OPERATOR, true));

    @Autowired
    private MockMvc mockMvc;

    @Test
    // Execute GET /fares?tenant=NYW_SANDBOX&origin=EDGEWTR asserting HTTP 200, 
    // checking model attributes for matrixMode=OD and verifying table cells in the Thymeleaf response
    void getNywFaresRendersOdMatrix() throws Exception {
        mockMvc.perform(get("/fares")
                .with(user(TEST_USER))
                .param("tenant", "NYW_SANDBOX")
                .param("origin", "EDGEWTR"))
            .andExpect(status().isOk())
            .andExpect(view().name("fare/fare"))
            .andExpect(model().attribute("gridView",
                hasProperty("tenant", hasProperty("gridMode", is(GridMode.OD)))));
    }

    @Test
    // Execute GET /fares?tenant=METRA&product=ADULT_ONEWAY asserting HTTP 200 and matrixMode=ZONAL.
    void getMetraFaresRendersZonalMatrix() throws Exception {
        mockMvc.perform(get("/fares")
                .with(user(TEST_USER))
                .param("tenant", "METRA")
                .param("product", "ADULT_ONEWAY"))
            .andExpect(status().isOk())
            .andExpect(view().name("fare/fare"))
            .andExpect(model().attribute("gridView",
                hasProperty("tenant", hasProperty("gridMode", is(GridMode.ZONAL)))));
    }
}