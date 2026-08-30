package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Generic ADD_MONTHS calculation: resolves the applicable trigger date from
 * the rule's triggerType, then adds a fixed number of calendar months using
 * LocalDate.plusMonths (never fixed-day arithmetic, per spec section 5).
 *
 * This single strategy backs PRIORITY_PERIOD, PCT_PUBLICATION and every
 * PCT_NATIONAL_PHASE office row — office differences are pure configuration.
 */
@Component
public class AddMonthsStrategy implements CalculationStrategy {

    @Override
    public DeadlineRule.CalculationType supports() {
        return DeadlineRule.CalculationType.ADD_MONTHS;
    }

    @Override
    public CalculationResult calculate(DeadlineRule rule, CalculationContext context) {
        LocalDate triggerDate = resolveTriggerDate(rule, context);
        int months = readMonths(rule);

        LocalDate calculatedDate = triggerDate.plusMonths(months);

        CalculationResult result = new CalculationResult(triggerDate, calculatedDate, false);
        result.addTrace("trigger = " + rule.getTriggerType());
        result.addTrace(triggerDate + " + " + months + " calendar months = " + calculatedDate);

        Object warningNote = rule.getParameters().get("warningNote");
        if (warningNote != null) {
            result.addWarning(warningNote.toString());
        }
        return result;
    }

    public static LocalDate resolveTriggerDate(DeadlineRule rule, CalculationContext context) {
        return switch (rule.getTriggerType()) {
            case FIRST_FILING -> requireDate(context.getFirstFilingDate(), "firstFilingDate");
            case EARLIEST_PRIORITY -> requirePriority(context);
            case EARLIEST_PRIORITY_OR_IFD -> context.hasPriority()
                ? context.earliestPriorityDate()
                : requireDate(context.getInternationalFilingDate(), "internationalFilingDate");
            case ISR_TRANSMITTAL -> requireDate(context.getIsrTransmittalDate(), "isrTransmittalDate");
        };
    }

    private static LocalDate requirePriority(CalculationContext context) {
        if (!context.hasPriority()) {
            throw new BadRequestException("earliestPriorityDate is required for this rule",
                Map.of("field", "earliestPriorityDate"));
        }
        return context.earliestPriorityDate();
    }

    private static LocalDate requireDate(LocalDate date, String field) {
        if (date == null) {
            throw new BadRequestException(field + " is required for this rule", Map.of("field", field));
        }
        return date;
    }

    private static int readMonths(DeadlineRule rule) {
        Object months = rule.getParameters().get("months");
        if (months instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(months.toString());
    }
}
