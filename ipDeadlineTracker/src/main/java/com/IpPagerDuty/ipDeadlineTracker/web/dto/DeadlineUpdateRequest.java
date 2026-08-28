package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.Deadline;

import java.time.LocalDate;
import java.util.UUID;

public record DeadlineUpdateRequest(LocalDate dueDate,
                                    UUID responsibleUserId,
                                    Deadline.Status status) {
}
