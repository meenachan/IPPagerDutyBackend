package com.IpPagerDuty.ipDeadlineTracker.domain.repositories;

import com.IpPagerDuty.ipDeadlineTracker.domain.Deadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeadlineRepository extends JpaRepository<Deadline, UUID> {
    @Query("SELECT d FROM Deadline d LEFT JOIN FETCH d.watchers WHERE d.matter.id = :matterId ORDER BY d.dueDate")
    List<Deadline> findByMatterId(@Param("matterId") UUID matterId);

    @Query("SELECT d FROM Deadline d LEFT JOIN FETCH d.watchers WHERE d.id = :id")
    Optional<Deadline> findByIdWithWatchers(@Param("id") UUID id);

    @Query("SELECT d FROM Deadline d WHERE d.matter.organization.id = :orgId AND d.responsibleUser.id = :userId AND d.status = 'OPEN'")
    List<Deadline> findOpenByResponsibleUser(@Param("orgId") UUID orgId, @Param("userId") UUID userId);

    @Query("SELECT d FROM Deadline d WHERE d.matter.organization.id = :orgId AND d.matter.owner.id = :userId")
    List<Deadline> findByMatterOwner(@Param("orgId") UUID orgId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Deadline d SET d.responsibleUser.id = :newUserId WHERE d.responsibleUser.id = :oldUserId AND d.status = 'OPEN' AND d.matter.organization.id = :orgId")
    int reassignResponsibilities(@Param("orgId") UUID orgId, @Param("oldUserId") UUID oldUserId, @Param("newUserId") UUID newUserId);

    @Query("SELECT d FROM Deadline d WHERE d.status = 'OPEN' AND d.dueDate < :today")
    List<Deadline> findOpenPastDue(@Param("today") LocalDate today);
}
