package com.IpPagerDuty.ipDeadlineTracker;

import com.IpPagerDuty.ipDeadlineTracker.domain.Organization;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never"
})
class AuthFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired OrganizationRepository organizationRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User();
        user.setEmail("owner@example.com");
        userRepository.save(user);
    }

    @Test
    void requestMagicLinkForExistingUserReturns202() throws Exception {
        mockMvc.perform(post("/api/v1/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"owner@example.com\"}"))
            .andExpect(status().isAccepted());
    }

    @Test
    void requestMagicLinkForUnknownUserReturns202() throws Exception {
        mockMvc.perform(post("/api/v1/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\"}"))
            .andExpect(status().isAccepted());
    }
}
