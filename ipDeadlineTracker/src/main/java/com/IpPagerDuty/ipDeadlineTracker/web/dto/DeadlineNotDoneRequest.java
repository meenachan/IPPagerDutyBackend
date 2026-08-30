package com.IpPagerDuty.ipDeadlineTracker.web.dto;

import com.IpPagerDuty.ipDeadlineTracker.domain.Deadline;
import jakarta.validation.constraints.NotNull;

public record DeadlineNotDoneRequest(@NotNull Deadline.NotDoneReason reason) {
}
