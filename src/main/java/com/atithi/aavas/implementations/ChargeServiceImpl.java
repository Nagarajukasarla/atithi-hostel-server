package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.AllocationStatus;
import com.atithi.aavas.enums.ChargeStatus;
import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.models.*;
import com.atithi.aavas.repositories.*;
import com.atithi.aavas.responses.*;
import com.atithi.aavas.services.ChargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeServiceImpl implements ChargeService {

    private final ChargeRepository chargeRepository;
    private final CustomerRepository customerRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final AllocationFinancialTermsRepository financialTermsRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Charge createSecurityDepositCharge(
            Hostel hostel,
            Customer customer,
            RoomAllocation allocation,
            BigDecimal amount,
            LocalDate dueDate
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Security deposit amount is zero or null for customer {}. Skipping deposit charge.", customer.getId());
            return null;
        }

        // Duplicate guard: Only one SECURITY_DEPOSIT charge per customer + hostel
        boolean exists = chargeRepository.existsByHostel_IdAndCustomer_IdAndChargeType(
                hostel.getId(),
                customer.getId(),
                ChargeType.SECURITY_DEPOSIT
        );
        if (exists) {
            log.warn("Security deposit charge already exists for customer {} in hostel {}. Skipping duplicate.",
                    customer.getId(), hostel.getId());
            return null;
        }

        Charge charge = Charge.builder()
                .hostel(hostel)
                .customer(customer)
                .allocation(allocation)
                .chargeType(ChargeType.SECURITY_DEPOSIT)
                .amount(amount)
                .dueDate(dueDate)
                .status(ChargeStatus.OPEN)
                .notes("Security deposit required upon room registration")
                .build();

        Charge saved = chargeRepository.save(charge);
        log.info("Created SECURITY_DEPOSIT charge #{} of {} for customer {} (due {})",
                saved.getId(), amount, customer.getFullName(), dueDate);
        return saved;
    }

    @Override
    @Transactional
    public Charge createMonthlyRentCharge(
            Hostel hostel,
            Customer customer,
            RoomAllocation allocation,
            BigDecimal amount,
            LocalDate dueDate,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Monthly rent amount is zero or null for customer {}. Skipping rent charge.", customer.getId());
            return null;
        }

        // Duplicate guard: Only one MONTHLY_RENT charge per allocation + billing period
        if (allocation != null && periodStart != null && periodEnd != null) {
            boolean exists = chargeRepository.existsByAllocation_IdAndChargeTypeAndPeriodStartAndPeriodEnd(
                    allocation.getId(),
                    ChargeType.MONTHLY_RENT,
                    periodStart,
                    periodEnd
            );
            if (exists) {
                log.warn("Monthly rent charge already exists for allocation {} for period {} to {}. Skipping duplicate.",
                        allocation.getId(), periodStart, periodEnd);
                return null;
            }
        }

        Charge charge = Charge.builder()
                .hostel(hostel)
                .customer(customer)
                .allocation(allocation)
                .chargeType(ChargeType.MONTHLY_RENT)
                .amount(amount)
                .dueDate(dueDate)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status(ChargeStatus.OPEN)
                .notes("Monthly rent for period " + periodStart + " to " + periodEnd)
                .build();

        Charge saved = chargeRepository.save(charge);
        log.info("Created MONTHLY_RENT charge #{} of {} for customer {} (period {} to {}, due {})",
                saved.getId(), amount, customer.getFullName(), periodStart, periodEnd, dueDate);
        return saved;
    }

    @Override
    @Transactional
    public Charge createDailyRentCharge(
            Hostel hostel,
            Customer customer,
            RoomAllocation allocation,
            BigDecimal amount,
            LocalDate date
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Daily rent amount is zero or null for customer {}. Skipping daily charge.", customer.getId());
            return null;
        }

        // Duplicate guard: Only one DAILY_RENT charge per allocation + day
        if (allocation != null && date != null) {
            boolean exists = chargeRepository.existsByAllocation_IdAndChargeTypeAndPeriodStart(
                    allocation.getId(),
                    ChargeType.DAILY_RENT,
                    date
            );
            if (exists) {
                log.warn("Daily rent charge already exists for allocation {} for date {}. Skipping duplicate.",
                        allocation.getId(), date);
                return null;
            }
        }

        Charge charge = Charge.builder()
                .hostel(hostel)
                .customer(customer)
                .allocation(allocation)
                .chargeType(ChargeType.DAILY_RENT)
                .amount(amount)
                .dueDate(date)
                .periodStart(date)
                .periodEnd(date)
                .status(ChargeStatus.OPEN)
                .notes("Daily rent for " + date)
                .build();

        Charge saved = chargeRepository.save(charge);
        log.info("Created DAILY_RENT charge #{} of {} for customer {} (date {}, due {})",
                saved.getId(), amount, customer.getFullName(), date, date);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargeResponse> getCustomerCharges(Long hostelId, Long customerId) {
        List<Charge> charges = chargeRepository.findByHostel_IdAndCustomer_IdOrderByDueDateDesc(hostelId, customerId);
        List<Object[]> allocationsGrouped = paymentAllocationRepository.sumAllocationsGroupedByChargeForCustomer(hostelId, customerId);
        java.util.Map<Long, BigDecimal> paidMap = new java.util.HashMap<>();
        for (Object[] row : allocationsGrouped) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                paidMap.put((Long) row[0], (BigDecimal) row[1]);
            }
        }

        return charges.stream()
                .map(c -> ChargeResponse.fromEntity(c, paidMap.getOrDefault(c.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargeResponse> getOutstandingCharges(Long hostelId, Long customerId) {
        return chargeRepository.findByHostel_IdAndCustomer_IdAndStatusInOrderByDueDateAsc(
                hostelId,
                customerId,
                List.of(ChargeStatus.OPEN, ChargeStatus.PARTIAL)
        ).stream().map(ChargeResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateOutstandingBalance(Long hostelId, Long customerId) {
        BigDecimal totalCharges = chargeRepository.sumTotalCharges(hostelId, customerId);
        BigDecimal totalPaid = paymentAllocationRepository.sumAllocatedAmountByCustomerId(hostelId, customerId);
        BigDecimal balance = totalCharges.subtract(totalPaid);
        return balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balance;
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialSummaryResponse getCustomerFinancialSummary(Long hostelId, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        RoomAllocation activeAllocation = roomAllocationRepository
                .findByHostel_IdAndCustomer_IdAndStatus(hostelId, customerId, AllocationStatus.ACTIVE)
                .orElse(null);

        com.atithi.aavas.enums.BillingMode billingMode = com.atithi.aavas.enums.BillingMode.MONTHLY;
        BigDecimal monthlyRent = BigDecimal.ZERO;
        BigDecimal dailyRate = null;
        BigDecimal securityDepositRequired = BigDecimal.ZERO;
        Long activeAllocationId = null;
        String roomNumber = null;

        if (activeAllocation != null) {
            activeAllocationId = activeAllocation.getId();
            roomNumber = activeAllocation.getRoom() != null ? activeAllocation.getRoom().getRoomNumber() : null;

            AllocationFinancialTerms terms = financialTermsRepository
                    .findByAllocation_Id(activeAllocation.getId())
                    .orElse(null);

            if (terms != null) {
                billingMode = terms.getBillingMode() != null ? terms.getBillingMode() : com.atithi.aavas.enums.BillingMode.MONTHLY;
                monthlyRent = terms.getMonthlyRent() != null ? terms.getMonthlyRent() : BigDecimal.ZERO;
                dailyRate = terms.getDailyRate();
                securityDepositRequired = terms.getSecurityDepositRequired() != null
                        ? terms.getSecurityDepositRequired()
                        : BigDecimal.ZERO;
            }
        }

        BigDecimal totalCharges = chargeRepository.sumTotalCharges(hostelId, customerId);
        BigDecimal outstandingBalance = calculateOutstandingBalance(hostelId, customerId);

        // Plan 3: calculate real totalPaid and securityDepositPaid from payment allocations
        BigDecimal totalPaid = paymentAllocationRepository.sumAllocatedAmountByCustomerId(hostelId, customerId);
        BigDecimal securityDepositPaid = paymentAllocationRepository.sumAllocatedAmountByCustomerIdAndChargeType(
                hostelId,
                customerId,
                ChargeType.SECURITY_DEPOSIT
        );

        return new FinancialSummaryResponse(
                customerId,
                customer.getFullName(),
                activeAllocationId,
                roomNumber,
                billingMode,
                monthlyRent,
                dailyRate,
                securityDepositRequired,
                securityDepositPaid,
                totalCharges,
                totalPaid,
                outstandingBalance
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerFinancialDetailsResponse getCustomerFinancialDetails(Long hostelId, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        RoomAllocation activeAllocation = roomAllocationRepository
                .findByHostel_IdAndCustomer_IdAndStatus(hostelId, customerId, AllocationStatus.ACTIVE)
                .orElse(null);

        AllocationFinancialTerms terms = activeAllocation != null
                ? financialTermsRepository.findByAllocation_Id(activeAllocation.getId()).orElse(null)
                : null;

        FinancialSummaryResponse summary = getCustomerFinancialSummary(hostelId, customerId);

        List<ChargeResponse> charges = getCustomerCharges(hostelId, customerId);

        List<PaymentResponse> payments = paymentRepository
                .findByHostel_IdAndCustomer_IdOrderByPaymentDateDesc(hostelId, customerId)
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();

        List<PaymentAllocationResponse> allocations = paymentAllocationRepository
                .findByPayment_Hostel_IdAndPayment_Customer_IdOrderByCreatedAtDesc(hostelId, customerId)
                .stream()
                .map(PaymentAllocationResponse::fromEntity)
                .toList();

        return new CustomerFinancialDetailsResponse(
                CustomerResponse.fromEntity(customer),
                activeAllocation != null ? RoomAllocationResponse.fromEntity(activeAllocation) : null,
                terms != null ? AllocationFinancialTermsResponse.fromEntity(terms) : null,
                summary,
                charges,
                payments,
                allocations
        );
    }
}
