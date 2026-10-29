package com.atithi.aavas.repositories;

import com.atithi.aavas.enums.BillingMode;
import com.atithi.aavas.enums.PaymentStatus;
import com.atithi.aavas.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByHostel_IdAndPaymentStatusAndPaymentDateBetween(
            Long hostelId,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Payment> findByHostel_IdAndPaymentStatusAndPaymentDate(
            Long hostelId,
            PaymentStatus paymentStatus,
            LocalDate date
    );

    Optional<Payment> findByIdAndHostel_Id(Long id, Long hostelId);

    List<Payment> findByHostel_IdOrderByPaymentDateDescCreatedAtDesc(Long hostelId);

    List<Payment> findByHostel_IdAndCustomer_IdOrderByPaymentDateDesc(Long hostelId, Long customerId);

    List<Payment> findByHostel_IdAndAllocation_IdOrderByPaymentDateDesc(Long hostelId, Long allocationId);

    Optional<Payment> findTopByHostel_IdAndCustomer_IdOrderByPaymentDateDescCreatedAtDesc(Long hostelId, Long customerId);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.hostel.id = :hostelId
              AND p.paymentStatus = :paymentStatus
              AND p.paymentDate = :date
            """)
    BigDecimal sumAmountByHostelAndStatusAndPaymentDate(
            @Param("hostelId") Long hostelId,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("date") LocalDate date
    );

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.hostel.id = :hostelId
              AND p.paymentStatus = :paymentStatus
              AND p.paymentDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumAmountByHostelAndStatusAndPaymentDateBetween(
            @Param("hostelId") Long hostelId,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT MONTH(p.paymentDate), COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.hostel.id = :hostelId
              AND p.paymentStatus = :paymentStatus
              AND p.paymentDate BETWEEN :startDate AND :endDate
            GROUP BY MONTH(p.paymentDate)
            """)
    List<Object[]> sumMonthlyRevenueForYear(
            @Param("hostelId") Long hostelId,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            JOIN p.allocation ra
            JOIN AllocationFinancialTerms aft ON aft.allocation = ra
            WHERE p.hostel.id = :hostelId
              AND p.paymentStatus = :paymentStatus
              AND aft.billingMode = :billingMode
              AND p.paymentDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumAmountByHostelAndStatusAndBillingModeAndPaymentDateBetween(
            @Param("hostelId") Long hostelId,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("billingMode") BillingMode billingMode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
