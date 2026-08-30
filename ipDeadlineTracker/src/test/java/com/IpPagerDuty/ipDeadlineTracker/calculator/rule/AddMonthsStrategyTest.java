package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddMonthsStrategyTest {
    private final AddMonthsStrategy strategy = new AddMonthsStrategy();

    private CalculationContext contextWithPriority(LocalDate priority) {
        CalculationContext ctx = new CalculationContext();
        ctx.setPriorityDates(List.of(priority));
        return ctx;
    }

    @ParameterizedTest(name = "{0} national phase: {1} months from {2}-{3}-{4} => {5}-{6}-{7}")
    @CsvSource({
        "EP,31,2025,2,14,2027,9,14",
        "US,30,2025,2,14,2027,8,14",
        "JP,30,2025,2,14,2027,8,14",
        "CN,30,2025,2,14,2027,8,14",
        "IN,31,2025,2,14,2027,9,14",
        "GB,31,2025,2,14,2027,9,14",
        "AU,31,2025,2,14,2027,9,14",
        "CA,30,2025,2,14,2027,8,14"
    })
    void calculatesNationalPhaseDeadlinesForEachOffice(String office, int months,
                                                        int py, int pm, int pd,
                                                        int ey, int em, int ed) {
        DeadlineRule rule = TestRules.addMonths("PCT_NATIONAL_PHASE", office,
            DeadlineRule.TriggerType.EARLIEST_PRIORITY_OR_IFD, months, null);
        CalculationContext ctx = contextWithPriority(LocalDate.of(py, pm, pd));

        CalculationResult result = strategy.calculate(rule, ctx);

        assertEquals(LocalDate.of(ey, em, ed), result.getCalculatedDate());
        assertFalse(result.isEstimated());
    }

    @Test
    void cnRuleAddsWarningAboutUnmodeledException() {
        DeadlineRule rule = TestRules.addMonths("PCT_NATIONAL_PHASE", "CN",
            DeadlineRule.TriggerType.EARLIEST_PRIORITY_OR_IFD, 30,
            Map.of("warningNote", "32-month exception not modeled"));
        CalculationContext ctx = contextWithPriority(LocalDate.of(2025, 2, 14));

        CalculationResult result = strategy.calculate(rule, ctx);

        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("32-month"));
    }

    @Test
    void noPriorityUsesInternationalFilingDate() {
        DeadlineRule rule = TestRules.addMonths("PCT_NATIONAL_PHASE", "EP",
            DeadlineRule.TriggerType.EARLIEST_PRIORITY_OR_IFD, 31, null);
        CalculationContext ctx = new CalculationContext();
        ctx.setInternationalFilingDate(LocalDate.of(2025, 2, 14));

        CalculationResult result = strategy.calculate(rule, ctx);

        assertEquals(LocalDate.of(2027, 9, 14), result.getCalculatedDate());
        assertEquals(LocalDate.of(2025, 2, 14), result.getTriggerDate());
    }

    @Test
    void multiplePriorityClaimsEarliestWins() {
        DeadlineRule rule = TestRules.addMonths("PCT_NATIONAL_PHASE", "EP",
            DeadlineRule.TriggerType.EARLIEST_PRIORITY_OR_IFD, 31, null);
        CalculationContext ctx = new CalculationContext();
        ctx.setPriorityDates(List.of(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 2, 14), LocalDate.of(2025, 3, 1)));

        CalculationResult result = strategy.calculate(rule, ctx);

        assertEquals(LocalDate.of(2025, 2, 14), result.getTriggerDate());
        assertEquals(LocalDate.of(2027, 9, 14), result.getCalculatedDate());
    }

    @Test
    void missingRequiredDateThrowsBadRequest() {
        DeadlineRule rule = TestRules.addMonths("PCT_NATIONAL_PHASE", "EP",
            DeadlineRule.TriggerType.EARLIEST_PRIORITY_OR_IFD, 31, null);
        CalculationContext ctx = new CalculationContext();

        assertThrows(BadRequestException.class, () -> strategy.calculate(rule, ctx));
    }

    @Test
    void priorityPeriodTwelveMonths() {
        DeadlineRule rule = TestRules.addMonths("PRIORITY_PERIOD", null,
            DeadlineRule.TriggerType.FIRST_FILING, 12, null);
        CalculationContext ctx = new CalculationContext();
        ctx.setFirstFilingDate(LocalDate.of(2025, 2, 14));

        CalculationResult result = strategy.calculate(rule, ctx);

        assertEquals(LocalDate.of(2026, 2, 14), result.getCalculatedDate());
    }

    @Test
    void pctPublicationEighteenMonths() {
        DeadlineRule rule = TestRules.addMonths("PCT_PUBLICATION", null,
            DeadlineRule.TriggerType.EARLIEST_PRIORITY, 18, null);
        CalculationContext ctx = contextWithPriority(LocalDate.of(2025, 2, 14));

        CalculationResult result = strategy.calculate(rule, ctx);

        assertEquals(LocalDate.of(2026, 8, 14), result.getCalculatedDate());
    }

    @Test
    void monthEndBoundaryJan31PlusOneMonth() {
        DeadlineRule rule = TestRules.addMonths("PRIORITY_PERIOD", null,
            DeadlineRule.TriggerType.FIRST_FILING, 1, null);
        CalculationContext ctx = new CalculationContext();
        ctx.setFirstFilingDate(LocalDate.of(2025, 1, 31));

        CalculationResult result = strategy.calculate(rule, ctx);

        // LocalDate.plusMonths clamps to the last valid day of the shorter month.
        assertEquals(LocalDate.of(2025, 2, 28), result.getCalculatedDate());
    }

    @Test
    void leapYearFeb29PlusTwelveMonths() {
        DeadlineRule rule = TestRules.addMonths("PRIORITY_PERIOD", null,
            DeadlineRule.TriggerType.FIRST_FILING, 12, null);
        CalculationContext ctx = new CalculationContext();
        ctx.setFirstFilingDate(LocalDate.of(2024, 2, 29));

        CalculationResult result = strategy.calculate(rule, ctx);

        assertEquals(LocalDate.of(2025, 2, 28), result.getCalculatedDate());
    }
}
