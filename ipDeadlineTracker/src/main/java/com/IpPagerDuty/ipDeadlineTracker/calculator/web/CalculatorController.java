package com.IpPagerDuty.ipDeadlineTracker.calculator.web;

import com.IpPagerDuty.ipDeadlineTracker.calculator.service.DeadlineCalculatorService;
import com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto.CalculationRequest;
import com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto.CalculationResponse;
import com.IpPagerDuty.ipDeadlineTracker.calculator.web.dto.RuleSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Free, public deadline calculator endpoints (spec: "free public calculator"
 * acquisition utility) — intentionally excluded from session auth, see
 * SecurityConfig. Converting a result into a monitored deadline still goes
 * through the authenticated /api/v1/deadlines endpoints.
 */
@RestController
@RequestMapping("/api/v1/calculator")
public class CalculatorController {
    private final DeadlineCalculatorService calculatorService;

    public CalculatorController(DeadlineCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @GetMapping("/rules")
    public ResponseEntity<List<RuleSummaryResponse>> listRules() {
        return ResponseEntity.ok(calculatorService.listRules());
    }

    @PostMapping("/calculate")
    public ResponseEntity<CalculationResponse> calculate(@RequestBody CalculationRequest request) {
        return ResponseEntity.ok(calculatorService.calculate(request));
    }
}
