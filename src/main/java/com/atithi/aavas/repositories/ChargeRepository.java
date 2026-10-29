package com.atithi.aavas.repositories;

import com.atithi.aavas.enums.BillingMode;
import com.atithi.aavas.enums.ChargeStatus;
import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.models.Charge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChargeRepository extends JpaRepository<Charge, Long> {

    /**
     * Duplicate guard: Only one SECURITY_DEPOSIT charge per customer + hostel.
     */
    boolean existsByHostel_IdAndCustomer_IdAndChargeType(
            Long hostelId,
            Long customerId,
            ChargeType chargeType
    );

    /**
     * Duplicate guard: Only one MONTHLY_RENT charge per allocation + billing period.
     */
    boolean existsByAllocation_IdAndChargeTypeAndPeriodStartAndPeriodEnd(
            Long allocationId,
            ChargeType chargeType,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    /**
     * Duplicate guard: Only one DAILY_RENT charge per allocation + day.
     */
    boolean existsByAllocation_IdAndChargeTypeAndPeriodStart(
            Long allocationId,
            ChargeType chargeType,
            LocalDate periodStart
    );

    /**
     * Full charge history for a customer in a hostel, newest due first.
     */
    List<Charge> findByHostel_IdAndCustomer_IdOrderByDueDateDesc(Long hostelId, Long customerId);

    /**
     * Full charge history for a customer ordered oldest first (for ledger calculations).
     */
    List<Charge> findByHostel_IdAndCustomer_IdOrderByDueDateAsc(Long hostelId, Long customerId);

    /**
     * Outstanding charges for customer ordered oldest due date first (oldest first allocation rule).
     */
    List<Charge> findByHostel_IdAndCustomer_IdAndStatusInOrderByDueDateAsc(
            Long hostelId,
            Long customerId,
            Collection<ChargeStatus> statuses
    );

    List<Charge> findByHostel_IdAndStatusInOrderByDueDateAsc(
            Long hostelId,
            Collection<ChargeStatus> statuses
    );

    List<Charge> findByHostel_IdAndStatusInAndDueDateLessThanEqualOrderByDueDateAsc(
            Long hostelId,
            Collection<ChargeStatus> statuses,
            LocalDate maxDueDate
    );

    @Query("""
            SELECT COALESCE(SUM(c.amount), 0) FROM Charge c
            WHERE c.hostel.id = :hostelId
              AND c.status IN :statuses
            """)
    BigDecimal sumAmountByHostelAndStatusIn(
            @Param("hostelId") Long hostelId,
            @Param("statuses") Collection<ChargeStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(c.amount), 0) FROM Charge c
            JOIN c.allocation ra
            JOIN AllocationFinancialTerms aft ON aft.allocation = ra
            WHERE c.hostel.id = :hostelId
              AND c.status IN :statuses
              AND aft.billingMode = :billingMode
            """)
    BigDecimal sumAmountByHostelAndStatusInAndBillingMode(
            @Param("hostelId") Long hostelId,
            @Param("statuses") Collection<ChargeStatus> statuses,
            @Param("billingMode") BillingMode billingMode
    );

    @Query("""
            SELECT COALESCE(SUM(c.amount), 0) FROM Charge c
            WHERE c.hostel.id = :hostelId
              AND c.dueDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumBilledBetweenDates(
            @Param("hostelId") Long hostelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Charges linked to a specific allocation.
     */
    List<Charge> findByAllocation_IdOrderByDueDateAsc(Long allocationId);

    /**
     * Sums total charge amount ever billed to a customer in a hostel.
     */
    @Query("""
            SELECT COALESCE(SUM(c.amount), 0) FROM Charge c
            WHERE c.hostel.id = :hostelId
              AND c.customer.id = :customerId
            """)
    BigDecimal sumTotalCharges(
            @Param("hostelId") Long hostelId,
            @Param("customerId") Long customerId
    );

    /**
     * Finds the latest charge by period start date for a customer and charge type (e.g. current cycle rent charge).
     */
    Optional<Charge> findTopByHostel_IdAndCustomer_IdAndChargeTypeOrderByPeriodStartDesc(
            Long hostelId,
            Long customerId,
            ChargeType chargeType
    );

    /**
     * Finds speculative charges for an allocation where periodStart is after the checkout date.
     */
    List<Charge> findByAllocation_IdAndPeriodStartGreaterThan(Long allocationId, LocalDate date);

    /**
     * Finds the cycle charge covering a specific date for an allocation.
     */
    Optional<Charge> findByAllocation_IdAndChargeTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            Long allocationId,
            ChargeType chargeType,
            LocalDate checkOutDate1,
            LocalDate checkOutDate2
    );

    /**
     * Finds all charges of a specific type for an allocation ordered by period start.
     */
    List<Charge> findByAllocation_IdAndChargeTypeOrderByPeriodStartAsc(Long allocationId, ChargeType chargeType);
}
