package com.atithi.aavas.repositories;

import com.atithi.aavas.models.AllocationFinancialTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationFinancialTermsRepository extends JpaRepository<AllocationFinancialTerms, Long> {

    Optional<AllocationFinancialTerms> findByAllocation_Id(Long allocationId);

    boolean existsByAllocation_Id(Long allocationId);

    @Query("""
            SELECT aft.allocation.id, aft.billingMode
            FROM AllocationFinancialTerms aft
            WHERE aft.allocation.id IN :allocationIds
            """)
    List<Object[]> findBillingModesByAllocationIds(@Param("allocationIds") Collection<Long> allocationIds);

    /**
     * Fetches all financial terms whose linked allocation is currently ACTIVE.
     * Eagerly joins allocation, hostel, and customer to prevent N+1 queries
     * when the scheduler iterates over results.
     */
    @Query("""
            SELECT aft FROM AllocationFinancialTerms aft
            JOIN FETCH aft.allocation a
            JOIN FETCH aft.hostel
            JOIN FETCH a.customer
            WHERE a.status = com.atithi.aavas.enums.AllocationStatus.ACTIVE
              AND (aft.billingMode = com.atithi.aavas.enums.BillingMode.MONTHLY OR aft.billingMode IS NULL)
            """)
    List<AllocationFinancialTerms> findAllActiveTerms();

    /**
     * Fetches all active financial terms with DAILY billing mode.
     */
    @Query("""
            SELECT aft FROM AllocationFinancialTerms aft
            JOIN FETCH aft.allocation a
            JOIN FETCH aft.hostel
            JOIN FETCH a.customer
            WHERE a.status = com.atithi.aavas.enums.AllocationStatus.ACTIVE
              AND aft.billingMode = com.atithi.aavas.enums.BillingMode.DAILY
            """)
    List<AllocationFinancialTerms> findActiveDailyAllocations();

    /**
     * Fetches all active financial terms with MONTHLY billing mode.
     */
    @Query("""
            SELECT aft FROM AllocationFinancialTerms aft
            JOIN FETCH aft.allocation a
            JOIN FETCH aft.hostel
            JOIN FETCH a.customer
            WHERE a.status = com.atithi.aavas.enums.AllocationStatus.ACTIVE
              AND (aft.billingMode = com.atithi.aavas.enums.BillingMode.MONTHLY OR aft.billingMode IS NULL)
            """)
    List<AllocationFinancialTerms> findActiveMonthlyAllocations();
}
