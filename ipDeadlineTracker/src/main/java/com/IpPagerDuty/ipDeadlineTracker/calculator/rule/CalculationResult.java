package com.IpPagerDuty.ipDeadlineTracker.calculator.rule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of a single calculation, including the human-readable trace
 * required by the engineering spec so every result is explainable.
 */
public class CalculationResult {
    private final LocalDate triggerDate;
    private final LocalDate calculatedDate;
    private final boolean estimated;
    private final List<String> trace = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public CalculationResult(LocalDate triggerDate, LocalDate calculatedDate, boolean estimated) {
        this.triggerDate = triggerDate;
        this.calculatedDate = calculatedDate;
        this.estimated = estimated;
    }

    public void addTrace(String line) { trace.add(line); }
    public void addWarning(String warning) { warnings.add(warning); }

    public LocalDate getTriggerDate() { return triggerDate; }
    public LocalDate getCalculatedDate() { return calculatedDate; }
    public boolean isEstimated() { return estimated; }
    public List<String> getTrace() { return trace; }
    public List<String> getWarnings() { return warnings; }
}
