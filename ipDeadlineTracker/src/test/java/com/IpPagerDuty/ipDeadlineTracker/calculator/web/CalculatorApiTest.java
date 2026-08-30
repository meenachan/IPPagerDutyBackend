package com.IpPagerDuty.ipDeadlineTracker.calculator.web;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.repositories.CalculationRepository;
import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.repositories.DeadlineRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:calctestdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never"
})
class CalculatorApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired DeadlineRuleRepository ruleRepository;
    @Autowired CalculationRepository calculationRepository;

    @BeforeEach
    void seedRules() {
        calculationRepository.deleteAll();
        ruleRepository.deleteAll();

        ruleRepository.save(rule("PCT_NATIONAL_PHASE", "EP", DeadlineRule.TriggerType.EARLIEST_PRIORITY_OR_IFD,
            DeadlineRule.CalculationType.ADD_MONTHS, Map.of("months", 31), "EPO", "EPC Rule 159(1)"));
        ruleRepository.save(rule("PRIORITY_PERIOD", null, DeadlineRule.TriggerType.FIRST_FILING,
            DeadlineRule.CalculationType.ADD_MONTHS, Map.of("months", 12), "WIPO", "PCT Rule 2.4"));
        ruleRepository.save(rule("PCT_ART19", null, DeadlineRule.TriggerType.EARLIEST_PRIORITY,
            DeadlineRule.CalculationType.LATER_OF, Map.of("priorityMonths", 16, "isrMonths", 2), "WIPO", "PCT Rule 46.1"));
    }

    private DeadlineRule rule(String code, String officeCode, DeadlineRule.TriggerType triggerType,
                               DeadlineRule.CalculationType calculationType, Map<String, Object> params,
                               String sourceAuthority, String sourceReference) {
        DeadlineRule rule = new DeadlineRule();
        rule.setCode(code);
        rule.setOfficeCode(officeCode);
        rule.setVersion("2026.08");
        rule.setTriggerType(triggerType);
        rule.setCalculationType(calculationType);
        rule.setParameters(params);
        rule.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        rule.setSourceAuthority(sourceAuthority);
        rule.setSourceReference(sourceReference);
        rule.setActive(true);
        return rule;
    }

    @Test
    void calculatesEpNationalPhaseDeadline() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ruleCode":"PCT_NATIONAL_PHASE","officeCode":"EP","inputs":{"earliestPriorityDate":"2025-02-14","internationalFilingDate":"2025-02-14"}}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.calculatedDate").value("2027-09-14"))
            .andExpect(jsonPath("$.ruleCode").value("PCT_NATIONAL_PHASE"))
            .andExpect(jsonPath("$.source.authority").value("EPO"))
            .andExpect(jsonPath("$.estimated").value(false));
    }

    @Test
    void unknownOfficeReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ruleCode":"PCT_NATIONAL_PHASE","officeCode":"ZZ","inputs":{"earliestPriorityDate":"2025-02-14"}}
                    """))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void malformedDateReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ruleCode":"PRIORITY_PERIOD","inputs":{"firstFilingDate":"not-a-date"}}
                    """))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void missingRequiredDateReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ruleCode":"PRIORITY_PERIOD","inputs":{}}
                    """))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void internationalFilingDateBeforePriorityIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ruleCode":"PCT_NATIONAL_PHASE","officeCode":"EP","inputs":{"earliestPriorityDate":"2025-02-14","internationalFilingDate":"2024-01-01"}}
                    """))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void missingIsrDateForArticle19RequiresAdditionalInput() throws Exception {
        mockMvc.perform(post("/api/v1/calculator/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ruleCode":"PCT_ART19","inputs":{"earliestPriorityDate":"2025-02-14"}}
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.details.type").value("REQUIRES_ADDITIONAL_INPUT"));
    }

    @Test
    void listRulesReturnsSeededRules() throws Exception {
        mockMvc.perform(get("/api/v1/calculator/rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    void calculatorEndpointsDoNotRequireAuthentication() throws Exception {
        // No session cookie supplied at all - should still succeed (free public utility).
        mockMvc.perform(get("/api/v1/calculator/rules"))
            .andExpect(status().isOk());
    }
}
