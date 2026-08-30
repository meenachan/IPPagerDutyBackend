package com.IpPagerDuty.ipDeadlineTracker.calculator.service;

import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.Calculation;
import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.DeadlineRule;
import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.repositories.CalculationRepository;
import com.IpPagerDuty.ipDeadlineTracker.calculator.domain.repositories.DeadlineRuleRepository;
import com.IpPagerDuty.ipDeadlineTracker.calculator.rule.CalculationResult;
import com.IpPagerDuty.ipDeadlineTracker.calculator.rule.RuleResolver;
import com.IpPagerDuty.ipDeadlineTracker.calculator.rule.StrategyRegistry;
import com.IpPagerDuty.ipDeadlineTracker.calculator.validation.CalculationInputValidator;
import com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto.CalculationRequest;
import com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto.CalculationResponse;
import com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto.RuleSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeadlineCalculatorService {
    private final RuleResolver ruleResolver;
    private final StrategyRegistry strategyRegistry;
    private final CalculationInputValidator validator;
    private final CalculationRepository calculationRepository;
    private final DeadlineRuleRepository ruleRepository;

    public DeadlineCalculatorService(RuleResolver ruleResolver,
                                     StrategyRegistry strategyRegistry,
                                     CalculationInputValidator validator,
                                     CalculationRepository calculationRepository,
                                     DeadlineRuleRepository ruleRepository) {
        this.ruleResolver = ruleResolver;
        this.strategyRegistry = strategyRegistry;
        this.validator = validator;
        this.calculationRepository = calculationRepository;
        this.ruleRepository = ruleRepository;
    }

    public List<RuleSummaryResponse> listRules() {
        return ruleRepository.findByActiveTrue().stream()
            .map(r -> new RuleSummaryResponse(
                r.getCode(),
                r.getOfficeCode(),
                r.getOfficeCode(),
                r.getVersion(),
                requiredInputsFor(r),
                r.getSourceAuthority(),
                r.getSourceReference()))
            .collect(Collectors.toList());
    }

    private List<String> requiredInputsFor(DeadlineRule rule) {
        List<String> fields = new ArrayList<>();
        switch (rule.getTriggerType()) {
            case FIRST_FILING -> fields.add("firstFilingDate");
            case EARLIEST_PRIORITY -> fields.add("earliestPriorityDate");
            case EARLIEST_PRIORITY_OR_IFD -> {
                fields.add("earliestPriorityDate");
                fields.add("internationalFilingDate");
            }
            case ISR_TRANSMITTAL -> fields.add("isrTransmittalDate");
        }
        if (rule.getCalculationType() == DeadlineRule.CalculationType.LATER_OF) {
            fields.add("PCT_ART19".equals(rule.getCode()) ? "isrTransmittalDate" : "isrWoTransmittalDate");
        }
        return fields;
    }

    @Transactional
    public CalculationResponse calculate(CalculationRequest request) {
        var context = validator.validate(request);
        DeadlineRule rule = ruleResolver.resolve(request.ruleCode(), request.officeCode(), LocalDate.now());
        var strategy = strategyRegistry.resolve(rule.getCalculationType());
        CalculationResult result = strategy.calculate(rule, context);

        Calculation calculation = new Calculation();
        calculation.setRuleCode(rule.getCode());
        calculation.setRuleVersion(rule.getVersion());
        calculation.setOfficeCode(rule.getOfficeCode());
        calculation.setInputs(toInputsMap(request));
        calculation.setTriggerDate(result.getTriggerDate());
        calculation.setCalculatedDate(result.getCalculatedDate());
        calculation.setEstimated(result.isEstimated());
        calculation.setTrace(result.getTrace());
        calculation.setSourceAuthority(rule.getSourceAuthority());
        calculation.setSourceReference(rule.getSourceReference());
        calculationRepository.save(calculation);

        return new CalculationResponse(
            calculation.getId(),
            rule.getCode(),
            rule.getVersion(),
            rule.getOfficeCode(),
            result.getTriggerDate(),
            result.getCalculatedDate(),
            result.isEstimated(),
            result.getTrace(),
            result.getWarnings(),
            new CalculationResponse.Source(rule.getSourceAuthority(), rule.getSourceReference(), rule.getSourceUrl())
        );
    }

    private Map<String, Object> toInputsMap(CalculationRequest request) {
        Map<String, Object> map = new java.util.HashMap<>();
        var inputs = request.inputs();
        if (inputs.priorityDates() != null) map.put("priorityDates", inputs.priorityDates());
        if (inputs.earliestPriorityDate() != null) map.put("earliestPriorityDate", inputs.earliestPriorityDate());
        if (inputs.internationalFilingDate() != null) map.put("internationalFilingDate", inputs.internationalFilingDate());
        if (inputs.firstFilingDate() != null) map.put("firstFilingDate", inputs.firstFilingDate());
        if (inputs.isrTransmittalDate() != null) map.put("isrTransmittalDate", inputs.isrTransmittalDate());
        if (inputs.isrWoTransmittalDate() != null) map.put("isrWoTransmittalDate", inputs.isrWoTransmittalDate());
        return map;
    }
}
