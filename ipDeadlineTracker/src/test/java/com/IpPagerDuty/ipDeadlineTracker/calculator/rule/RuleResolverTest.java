package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.repositories.DeadlineRuleRepository;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleResolverTest {
    private final DeadlineRuleRepository repository = mock(DeadlineRuleRepository.class);
    private final RuleResolver resolver = new RuleResolver(repository);

    @Test
    void unknownOfficeIsRejected() {
        when(repository.findByCodeAndOfficeCodeAndActiveTrue("PCT_NATIONAL_PHASE", "ZZ"))
            .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
            () -> resolver.resolve("PCT_NATIONAL_PHASE", "ZZ", LocalDate.now()));
    }

    @Test
    void ruleOutsideEffectiveWindowIsRejected() {
        DeadlineRule rule = TestRules.addMonths("PCT_NATIONAL_PHASE", "EP",
            DeadlineRule.TriggerType.EARLIEST_PRIORITY_OR_IFD, 31, null);
        rule.setEffectiveFrom(LocalDate.of(2030, 1, 1));
        when(repository.findByCodeAndOfficeCodeAndActiveTrue("PCT_NATIONAL_PHASE", "EP"))
            .thenReturn(Optional.of(rule));

        assertThrows(BadRequestException.class,
            () -> resolver.resolve("PCT_NATIONAL_PHASE", "EP", LocalDate.of(2026, 8, 30)));
    }
}
