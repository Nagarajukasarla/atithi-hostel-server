package com.atithi.aavas.repositories;

import com.atithi.aavas.enums.BillingMode;
import com.atithi.aavas.enums.ChargeStatus;
import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.models.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    List<PaymentAllocation> findByPayment_Id(Long paymentId);

    List<PaymentAllocation> findByCharge_Id(Long chargeId);

    List<PaymentAllocation> findByPayment_Hostel_IdAndPayment_Customer_IdOrderByCreatedAtDesc(
            Long hostelId,
            Long customerId
    );

    @Query("""
            SELECT COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.charge.id = :chargeId
            """)
    BigDecimal sumAllocatedAmountByChargeId(@Param("chargeId") Long chargeId);

    @Query("""
            SELECT COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.payment.id = :paymentId
            """)
    BigDecimal sumAllocatedAmountByPaymentId(@Param("paymentId") Long paymentId);

    @Query("""
            SELECT COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.payment.hostel.id = :hostelId
              AND pa.payment.customer.id = :customerId
            """)
    BigDecimal sumAllocatedAmountByCustomerId(
            @Param("hostelId") Long hostelId,
            @Param("customerId") Long customerId
    );

    @Query("""
            SELECT COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.payment.hostel.id = :hostelId
              AND pa.payment.customer.id = :customerId
              AND pa.charge.chargeType = :chargeType
            """)
    BigDecimal sumAllocatedAmountByCustomerIdAndChargeType(
            @Param("hostelId") Long hostelId,
            @Param("customerId") Long customerId,
            @Param("chargeType") ChargeType chargeType
    );

    @Query("""
            SELECT pa.charge.id, COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.charge.customer.id = :customerId
              AND pa.charge.hostel.id = :hostelId
            GROUP BY pa.charge.id
            """)
    List<Object[]> sumAllocationsGroupedByChargeForCustomer(
            @Param("hostelId") Long hostelId,
            @Param("customerId") Long customerId
    );

    @Query("""
            SELECT pa.charge.id, COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.charge.hostel.id = :hostelId
            GROUP BY pa.charge.id
            """)
    List<Object[]> sumAllocationsGroupedByChargeForHostel(@Param("hostelId") Long hostelId);

    @Query("""
            SELECT COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.charge.hostel.id = :hostelId
              AND pa.charge.status IN :statuses
            """)
    BigDecimal sumAllocatedAmountByHostelAndChargeStatusIn(
            @Param("hostelId") Long hostelId,
            @Param("statuses") Collection<ChargeStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            JOIN pa.charge c
            JOIN c.allocation ra
            JOIN AllocationFinancialTerms aft ON aft.allocation = ra
            WHERE c.hostel.id = :hostelId
              AND c.status IN :statuses
              AND aft.billingMode = :billingMode
            """)
    BigDecimal sumAllocatedAmountByHostelAndChargeStatusInAndBillingMode(
            @Param("hostelId") Long hostelId,
            @Param("statuses") Collection<ChargeStatus> statuses,
            @Param("billingMode") BillingMode billingMode
    );

    @Query("""
            SELECT pa.charge.id, COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.charge.id IN :chargeIds
            GROUP BY pa.charge.id
            """)
    List<Object[]> sumAllocationsGroupedByChargeIds(@Param("chargeIds") Collection<Long> chargeIds);
}
