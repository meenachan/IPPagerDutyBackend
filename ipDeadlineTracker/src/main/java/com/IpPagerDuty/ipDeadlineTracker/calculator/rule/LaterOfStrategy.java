package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * LATER_OF calculation used by PCT_ART19 and PCT_DEMAND: the deadline is the
 * later of (a) N months from the earliest priority date and (b) M months
 * from the relevant ISR/WO transmittal date (spec sections 7 and 8).
 *
 * If the transmittal date is missing, exact calculation is refused unless
 * the caller explicitly opts into an estimate (spec section 16) — WIPO's
 * assumed four-month ISR timing is only ever applied as a disclosed
 * estimate, never silently.
 */
@Component
public class LaterOfStrategy implements CalculationStrategy {
    private static final int ASSUMED_ISR_MONTHS_FROM_PRIORITY = 4;

    @Override
    public DeadlineRule.CalculationType supports() {
        return DeadlineRule.CalculationType.LATER_OF;
    }

    @Override
    public CalculationResult calculate(DeadlineRule rule, CalculationContext context) {
        if (!context.hasPriority()) {
            throw new BadRequestException("earliestPriorityDate is required for this rule",
                Map.of("field", "earliestPriorityDate"));
        }
        LocalDate priorityDate = context.earliestPriorityDate();
        int priorityMonths = readInt(rule, "priorityMonths");
        int isrMonths = readInt(rule, "isrMonths");

        LocalDate candidateA = priorityDate.plusMonths(priorityMonths);

        LocalDate isrDate = resolveIsrDate(rule, context);
        boolean estimated = false;
        String isrField = isrFieldName(rule);
        if (isrDate == null) {
            if (!context.isAllowEstimate()) {
                throw new BadRequestException("REQUIRES_ADDITIONAL_INPUT: " + isrField + " is required for an exact calculation",
                    Map.of("type", "REQUIRES_ADDITIONAL_INPUT", "field", isrField));
            }
            isrDate = priorityDate.plusMonths(ASSUMED_ISR_MONTHS_FROM_PRIORITY);
            estimated = true;
        }
        LocalDate candidateB = isrDate.plusMonths(isrMonths);

        LocalDate calculatedDate = candidateA.isAfter(candidateB) ? candidateA : candidateB;
        CalculationResult result = new CalculationResult(priorityDate, calculatedDate, estimated);
        result.addTrace("candidateA = " + priorityDate + " + " + priorityMonths + " months = " + candidateA);
        if (estimated) {
            result.addTrace(isrField + " missing; estimated as priority + " + ASSUMED_ISR_MONTHS_FROM_PRIORITY + " months = " + isrDate);
            result.addWarning("ISR transmittal date was estimated; do not convert to a monitored deadline without confirming the real transmittal date");
        }
        result.addTrace("candidateB = " + isrDate + " + " + isrMonths + " months = " + candidateB);
        result.addTrace("result = later of candidateA/candidateB = " + calculatedDate);
        return result;
    }

    private LocalDate resolveIsrDate(DeadlineRule rule, CalculationContext context) {
        return "PCT_ART19".equals(rule.getCode()) ? context.getIsrTransmittalDate() : context.getIsrWoTransmittalDate();
    }

    private String isrFieldName(DeadlineRule rule) {
        return "PCT_ART19".equals(rule.getCode()) ? "isrTransmittalDate" : "isrWoTransmittalDate";
    }

    private int readInt(DeadlineRule rule, String key) {
        Object v = rule.getParameters().get(key);
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }
}
