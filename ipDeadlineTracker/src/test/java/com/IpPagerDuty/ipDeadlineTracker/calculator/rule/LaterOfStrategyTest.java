package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LaterOfStrategyTest {
    private final LaterOfStrategy strategy = new LaterOfStrategy();

    @Test
    void article19UsesLaterOfPriorityAndIsrCandidates() {
        DeadlineRule rule = TestRules.laterOf("PCT_ART19", 16, 2);
        CalculationContext ctx = new CalculationContext();
        ctx.setPriorityDates(List.of(LocalDate.of(2025, 2, 14)));
        ctx.setIsrTransmittalDate(LocalDate.of(2026, 4, 20));

        CalculationResult result = strategy.calculate(rule, ctx);

        // candidateA = 2025-02-14 + 16m = 2026-06-14; candidateB = 2026-04-20 + 2m = 2026-06-20 -> later
        assertEquals(LocalDate.of(2026, 6, 20), result.getCalculatedDate());
        assertFalse(result.isEstimated());
    }

    @Test
    void article19WhenIsrEarlierThanPriorityPlus14Months() {
        DeadlineRule rule = TestRules.laterOf("PCT_ART19", 16, 2);
        CalculationContext ctx = new CalculationContext();
        ctx.setPriorityDates(List.of(LocalDate.of(2025, 2, 14)));
        ctx.setIsrTransmittalDate(LocalDate.of(2026, 1, 1));

        CalculationResult result = strategy.calculate(rule, ctx);

        // candidateA = 2026-06-14; candidateB = 2026-03-01 -> A wins
        assertEquals(LocalDate.of(2026, 6, 14), result.getCalculatedDate());
    }

    @Test
    void demandUsesLaterOfPriorityAndIsrWoCandidates() {
        DeadlineRule rule = TestRules.laterOf("PCT_DEMAND", 22, 3);
        CalculationContext ctx = new CalculationContext();
        ctx.setPriorityDates(List.of(LocalDate.of(2025, 2, 14)));
        ctx.setIsrWoTransmittalDate(LocalDate.of(2026, 5, 20));

        CalculationResult result = strategy.calculate(rule, ctx);

        // candidateA = 2025-02-14 + 22m = 2026-12-14; candidateB = 2026-05-20 + 3m = 2026-08-20 -> A wins
        assertEquals(LocalDate.of(2026, 12, 14), result.getCalculatedDate());
    }

    @Test
    void missingIsrDateInExactModeThrowsRequiresAdditionalInput() {
        DeadlineRule rule = TestRules.laterOf("PCT_ART19", 16, 2);
        CalculationContext ctx = new CalculationContext();
        ctx.setPriorityDates(List.of(LocalDate.of(2025, 2, 14)));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> strategy.calculate(rule, ctx));
        assertEquals("REQUIRES_ADDITIONAL_INPUT", ex.getDetails().get("type"));
    }

    @Test
    void missingIsrDateWithEstimateAllowedProducesEstimatedResult() {
        DeadlineRule rule = TestRules.laterOf("PCT_ART19", 16, 2);
        CalculationContext ctx = new CalculationContext();
        ctx.setPriorityDates(List.of(LocalDate.of(2025, 2, 14)));
        ctx.setAllowEstimate(true);

        CalculationResult result = strategy.calculate(rule, ctx);

        assertTrue(result.isEstimated());
        assertFalse(result.getWarnings().isEmpty());
    }

    @Test
    void missingPriorityThrowsBadRequest() {
        DeadlineRule rule = TestRules.laterOf("PCT_ART19", 16, 2);
        CalculationContext ctx = new CalculationContext();

        assertThrows(BadRequestException.class, () -> strategy.calculate(rule, ctx));
    }
}
