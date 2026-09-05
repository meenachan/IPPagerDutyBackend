package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'PENDING' AND n.scheduledFor <= :now ORDER BY n.scheduledFor")
    List<Notification> findPendingDue(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.deliveryStatus = 'CANCELLED' WHERE n.deadline.id = :deadlineId AND n.deliveryStatus = 'PENDING'")
    int cancelPendingByDeadline(@Param("deadlineId") UUID deadlineId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.deadline.id = :deadlineId AND n.deliveryStatus = 'PENDING'")
    int deletePendingByDeadline(@Param("deadlineId") UUID deadlineId);

    @Modifying
    @Query("UPDATE Notification n SET n.deliveryStatus = 'SENT', n.sentAt = :now WHERE n.id = :id AND n.deliveryStatus = 'PENDING'")
    int claimAndMarkSent(@Param("id") UUID id, @Param("now") Instant now);
}
