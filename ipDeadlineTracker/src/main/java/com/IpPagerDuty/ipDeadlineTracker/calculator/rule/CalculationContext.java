package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import java.time.LocalDate;
import java.util.List;

/**
 * Raw, already-parsed calculation inputs supplied by the caller. Not every
 * field is required by every rule — the strategy/validator for a given rule
 * determines which fields are mandatory.
 */
public class CalculationContext {
    private List<LocalDate> priorityDates;
    private LocalDate internationalFilingDate;
    private LocalDate firstFilingDate;
    private LocalDate isrTransmittalDate;
    private LocalDate isrWoTransmittalDate;
    private boolean allowEstimate;

    public List<LocalDate> getPriorityDates() { return priorityDates; }
    public void setPriorityDates(List<LocalDate> priorityDates) { this.priorityDates = priorityDates; }

    public boolean hasPriority() { return priorityDates != null && !priorityDates.isEmpty(); }

    public LocalDate earliestPriorityDate() {
        return priorityDates.stream().min(LocalDate::compareTo).orElseThrow();
    }

    public LocalDate getInternationalFilingDate() { return internationalFilingDate; }
    public void setInternationalFilingDate(LocalDate internationalFilingDate) { this.internationalFilingDate = internationalFilingDate; }

    public LocalDate getFirstFilingDate() { return firstFilingDate; }
    public void setFirstFilingDate(LocalDate firstFilingDate) { this.firstFilingDate = firstFilingDate; }

    public LocalDate getIsrTransmittalDate() { return isrTransmittalDate; }
    public void setIsrTransmittalDate(LocalDate isrTransmittalDate) { this.isrTransmittalDate = isrTransmittalDate; }

    public LocalDate getIsrWoTransmittalDate() { return isrWoTransmittalDate; }
    public void setIsrWoTransmittalDate(LocalDate isrWoTransmittalDate) { this.isrWoTransmittalDate = isrWoTransmittalDate; }

    public boolean isAllowEstimate() { return allowEstimate; }
    public void setAllowEstimate(boolean allowEstimate) { this.allowEstimate = allowEstimate; }
}
