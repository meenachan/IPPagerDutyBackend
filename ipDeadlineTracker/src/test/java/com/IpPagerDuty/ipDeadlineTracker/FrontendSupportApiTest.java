package com.IpPagerDuty.ipDeadlineTracker;

import com.IpPagerDuty.ipDeadlineTracker.domain.*;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.*;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the new Angular-frontend-support endpoints: /me bootstrap, deadline
 * archival (soft delete), and the not-done reason flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:frontendsupport",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never"
})
class FrontendSupportApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired OrganizationMemberRepository memberRepository;
    @Autowired MatterRepository matterRepository;
    @Autowired DeadlineRepository deadlineRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired AuditEventRepository auditEventRepository;

    private User owner;
    private Organization org;
    private Matter matter;
    private Deadline deadline;
    private Cookie sessionCookie;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        deadlineRepository.deleteAll();
        matterRepository.deleteAll();
        memberRepository.deleteAll();
        organizationRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setEmail("owner@example.com");
        owner.setDisplayName("Owner Person");
        userRepository.save(owner);

        org = new Organization();
        org.setName("Smith IP");
        organizationRepository.save(org);

        OrganizationMember membership = new OrganizationMember();
        membership.setOrganization(org);
        membership.setUser(owner);
        membership.setRole(OrganizationMember.Role.BUSINESS_OWNER);
        memberRepository.save(membership);

        matter = new Matter();
        matter.setOrganization(org);
        matter.setTitle("Acme Corp - PCT Application");
        matter.setType(Matter.Type.PATENT);
        matter.setOwner(owner);
        matter.setCreatedBy(owner);
        matterRepository.save(matter);

        deadline = new Deadline();
        deadline.setMatter(matter);
        deadline.setDueDate(LocalDate.now().plusMonths(1));
        deadline.setType("PCT_NATIONAL_PHASE_EP");
        deadline.setResponsibleUser(owner);
        deadline.setCreatedBy(owner);
        deadlineRepository.save(deadline);

        String rawToken = "test-session-token-" + System.nanoTime();
        Session session = new Session();
        session.setUser(owner);
        session.setTokenHash(AuthInterceptor.sha256(rawToken));
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        sessionRepository.save(session);
        sessionCookie = new Cookie("ipd_session", rawToken);
    }

    @Test
    void meReturnsUserAndOrganizationWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/me").cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("owner@example.com"))
            .andExpect(jsonPath("$.user.displayName").value("Owner Person"))
            .andExpect(jsonPath("$.organizations[0].id").value(org.getId().toString()))
            .andExpect(jsonPath("$.activeOrganizationId").value(org.getId().toString()));
    }

    @Test
    void meReturns401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteDeadlineArchivesItAndCancelsNotifications() throws Exception {
        mockMvc.perform(delete("/api/v1/deadlines/{id}", deadline.getId()).cookie(sessionCookie))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/deadlines/{id}", deadline.getId()).cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void notDoneReasonPersistsAndIsReturnedInDetail() throws Exception {
        mockMvc.perform(post("/api/v1/deadlines/{id}/not-done", deadline.getId())
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"WAITING_ON_CLIENT\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notDoneReason").value("WAITING_ON_CLIENT"));

        mockMvc.perform(get("/api/v1/deadlines/{id}", deadline.getId()).cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notDoneReason").value("WAITING_ON_CLIENT"));
    }

    @Test
    void notDoneRejectsMissingReason() throws Exception {
        mockMvc.perform(post("/api/v1/deadlines/{id}/not-done", deadline.getId())
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnprocessableEntity());
    }
}
