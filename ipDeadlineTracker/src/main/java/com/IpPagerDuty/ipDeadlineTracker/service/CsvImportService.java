package com.IpPagerDuty.ipDeadlineTracker.service;

import com.IpPagerDuty.ipDeadlineTracker.domain.*;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.*;
import com.IpPagerDuty.ipDeadlineTracker.security.BadRequestException;
import com.IpPagerDuty.ipDeadlineTracker.security.ForbiddenException;
import com.IpPagerDuty.ipDeadlineTracker.security.NotFoundException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsvImportService {
    private static final long MAX_SIZE = 2 * 1024 * 1024;
    private static final int MAX_ROWS = 500;

    private final MatterRepository matterRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final DeadlineRepository deadlineRepository;
    private final NotificationRepository notificationRepository;
    private final DeadlineService deadlineService;
    private final AuditService auditService;

    public CsvImportService(MatterRepository matterRepository,
                            OrganizationMemberRepository memberRepository,
                            UserRepository userRepository,
                            DeadlineRepository deadlineRepository,
                            NotificationRepository notificationRepository,
                            DeadlineService deadlineService,
                            AuditService auditService) {
        this.matterRepository = matterRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.deadlineRepository = deadlineRepository;
        this.notificationRepository = notificationRepository;
        this.deadlineService = deadlineService;
        this.auditService = auditService;
    }

    @Transactional
    public CsvImportResult importDeadlines(UUID matterId, MultipartFile file, User user) {
        if (file.getSize() > MAX_SIZE) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "file too large"
            );
        }

        Matter matter = matterRepository.findById(matterId).orElseThrow(() -> new NotFoundException("matter not found"));
        OrganizationMember member = memberRepository.findByUserId(user.getId())
            .orElseThrow(() -> new NotFoundException("organization not found"));
        if (!member.getOrganization().getId().equals(matter.getOrganization().getId())) {
            throw new NotFoundException("matter not found");
        }
        if (member.getRole() == OrganizationMember.Role.CLIENT && !matter.getOwner().getId().equals(user.getId())) {
            throw new NotFoundException("matter not found");
        }

        List<RowError> errors = new ArrayList<>();
        List<ParsedRow> rows = parse(file, member.getOrganization().getId(), user, errors);
        if (!errors.isEmpty()) {
            throw new BadRequestException("validation failed", Map.of("errors", errors));
        }

        List<UUID> deadlineIds = new ArrayList<>();
        for (ParsedRow row : rows) {
            Deadline d = new Deadline();
            d.setMatter(matter);
            d.setDueDate(row.dueDate);
            d.setType(row.type);
            d.setResponsibleUser(row.responsible);
            d.setNotes(row.notes);
            d.setCreatedBy(user);
            deadlineRepository.save(d);
            deadlineService.materializeReminders(d);
            deadlineIds.add(d.getId());
            auditService.record(matter.getOrganization(), user, "DEADLINE", d.getId(), "created_via_csv", Map.of(
                "dueDate", d.getDueDate().toString(),
                "type", d.getType()
            ));
        }
        auditService.record(matter.getOrganization(), user, "MATTER", matter.getId(), "csv_import_completed", Map.of(
            "importedCount", rows.size()
        ));
        return new CsvImportResult(rows.size(), deadlineIds);
    }

    private List<ParsedRow> parse(MultipartFile file, UUID orgId, User defaultUser, List<RowError> errors) {
        List<ParsedRow> rows = new ArrayList<>();
        Map<String, User> orgUsersByEmail = memberRepository.findByOrganizationId(orgId).stream()
            .collect(Collectors.toMap(m -> m.getUser().getEmail().toLowerCase(), OrganizationMember::getUser));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                 .setHeader()
                 .setIgnoreHeaderCase(true)
                 .setTrim(true)
                 .build()
                 .parse(reader)) {

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                if (rowNum > MAX_ROWS + 1) {
                    throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "too many rows"
                    );
                }
                String dueDateStr = record.get("due_date");
                String type = record.get("type");
                String responsibleEmail = record.isSet("responsible_email") ? record.get("responsible_email") : null;
                String notes = record.isSet("notes") ? record.get("notes") : null;

                LocalDate dueDate;
                try {
                    dueDate = LocalDate.parse(dueDateStr);
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    errors.add(new RowError(rowNum, "invalid due_date"));
                    continue;
                }
                if (type == null || type.isBlank()) {
                    errors.add(new RowError(rowNum, "type is required"));
                }
                User responsible = defaultUser;
                if (responsibleEmail != null && !responsibleEmail.isBlank()) {
                    responsible = orgUsersByEmail.get(responsibleEmail.toLowerCase().trim());
                    if (responsible == null) {
                        errors.add(new RowError(rowNum, "responsible_email is not an organization member"));
                    }
                }
                if (errors.isEmpty()) {
                    rows.add(new ParsedRow(dueDate, type, responsible, notes));
                }
            }
        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("failed to parse CSV", Map.of("reason", e.getMessage()));
        }
        return rows;
    }

    private record ParsedRow(LocalDate dueDate, String type, User responsible, String notes) {}
    public record RowError(int row, String reason) {}
    public record CsvImportResult(int importedCount, List<UUID> deadlineIds) {}
}
