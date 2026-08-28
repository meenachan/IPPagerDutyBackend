package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.DeadlineWatcher;
import com.IpPagerDuty.ipDeadlineTracker.domain.DeadlineWatcherId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeadlineWatcherRepository extends JpaRepository<DeadlineWatcher, DeadlineWatcherId> {
}
