package com.IpPagerDuty.ipDeadlineTracker.calculator.validation;

import com.IpPagerDuty.ipDeadlineTracker.calculator.rule.CalculationContext;
import com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto.CalculationRequest;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses and validates raw calculation inputs (spec section 15).
 * Never silently falls back to a default/guessed date.
 */
@Component
public class CalculationInputValidator {

    public CalculationContext validate(CalculationRequest request) {
        if (request.ruleCode() == null || request.ruleCode().isBlank()) {
            throw new BadRequestException("ruleCode is required", Map.of("field", "ruleCode"));
        }
        CalculationRequest.CalculationInputs inputs = request.inputs();
        if (inputs == null) {
            throw new BadRequestException("inputs is required", Map.of("field", "inputs"));
        }

        CalculationContext context = new CalculationContext();
        context.setAllowEstimate(Boolean.TRUE.equals(inputs.allowEstimate()));

        List<LocalDate> priorityDates = new ArrayList<>();
        if (inputs.priorityDates() != null) {
            if (inputs.priorityDates().isEmpty()) {
                throw new BadRequestException("priorityDates must not be empty when supplied", Map.of("field", "priorityDates"));
            }
            for (String raw : inputs.priorityDates()) {
                priorityDates.add(parseDate(raw, "priorityDates"));
            }
        }
        if (inputs.earliestPriorityDate() != null) {
            priorityDates.add(parseDate(inputs.earliestPriorityDate(), "earliestPriorityDate"));
        }
        if (!priorityDates.isEmpty()) {
            context.setPriorityDates(priorityDates);
        }

        LocalDate internationalFilingDate = parseOptional(inputs.internationalFilingDate(), "internationalFilingDate");
        context.setInternationalFilingDate(internationalFilingDate);
        context.setFirstFilingDate(parseOptional(inputs.firstFilingDate(), "firstFilingDate"));
        context.setIsrTransmittalDate(parseOptional(inputs.isrTransmittalDate(), "isrTransmittalDate"));
        context.setIsrWoTransmittalDate(parseOptional(inputs.isrWoTransmittalDate(), "isrWoTransmittalDate"));

        if (context.hasPriority() && internationalFilingDate != null
            && internationalFilingDate.isBefore(context.earliestPriorityDate())) {
            throw new BadRequestException(
                "internationalFilingDate must not be before the earliest claimed priority date",
                Map.of("field", "internationalFilingDate"));
        }

        return context;
    }

    private LocalDate parseOptional(String raw, String field) {
        return raw == null ? null : parseDate(raw, field);
    }

    private LocalDate parseDate(String raw, String field) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("invalid date for " + field + " (expected ISO YYYY-MM-DD)",
                Map.of("field", field, "value", raw));
        }
    }
}
