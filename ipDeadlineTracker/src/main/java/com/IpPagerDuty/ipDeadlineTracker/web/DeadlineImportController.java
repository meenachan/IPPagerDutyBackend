package com.IpPagerDuty.ipDeadlineTracker.web;

import com.IpPagerDuty.ipDeadlineTracker.security.AuthContext;
import com.IpPagerDuty.ipDeadlineTracker.security.AuthInterceptor;
import com.IpPagerDuty.ipDeadlineTracker.service.CsvImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matters/{matterId}/deadlines")
public class DeadlineImportController {
    private final CsvImportService csvImportService;

    public DeadlineImportController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCsv(@PathVariable UUID matterId,
                                                        @RequestParam("file") MultipartFile file,
                                                        HttpServletRequest req) {
        AuthContext ctx = AuthInterceptor.require(req);
        CsvImportService.CsvImportResult result = csvImportService.importDeadlines(matterId, file, ctx.user());
        return ResponseEntity.status(201).body(Map.of(
            "importedCount", result.importedCount(),
            "deadlineIds", result.deadlineIds()
        ));
    }
}
