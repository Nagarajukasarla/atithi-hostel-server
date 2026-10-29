package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.ChargeStatus;
import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.models.Charge;
import com.atithi.aavas.models.Customer;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.Payment;
import com.atithi.aavas.models.PaymentAllocation;
import com.atithi.aavas.repositories.ChargeRepository;
import com.atithi.aavas.repositories.PaymentAllocationRepository;
import com.atithi.aavas.responses.PaymentAllocationResponse;
import com.atithi.aavas.services.PaymentAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAllocationServiceImpl implements PaymentAllocationService {

    private final PaymentAllocationRepository allocationRepository;
    private final ChargeRepository chargeRepository;

    @Override
    @Transactional
    public List<PaymentAllocation> allocatePaymentAgainstOldestCharges(
            Hostel hostel,
            Customer customer,
            Payment payment
    ) {
        if (payment == null || payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Payment amount is zero or null. Skipping allocation.");
            return List.of();
        }

        // Find all outstanding charges for this customer in this hostel, ordered by due date ASC (oldest first)
        List<Charge> outstandingCharges = chargeRepository.findByHostel_IdAndCustomer_IdAndStatusInOrderByDueDateAsc(
                hostel.getId(),
                customer.getId(),
                List.of(ChargeStatus.OPEN, ChargeStatus.PARTIAL)
        );

        if (outstandingCharges.isEmpty()) {
            log.info("No open or partial charges found for customer {}. Payment #{} remains unallocated.",
                    customer.getId(), payment.getId());
            return List.of();
        }

        BigDecimal remainingToAllocate = payment.getAmount();
        List<PaymentAllocation> createdAllocations = new ArrayList<>();

        for (Charge charge : outstandingCharges) {
            if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Calculate already paid on this charge
            BigDecimal alreadyPaid = allocationRepository.sumAllocatedAmountByChargeId(charge.getId());
            BigDecimal unpaid = charge.getAmount().subtract(alreadyPaid);

            if (unpaid.compareTo(BigDecimal.ZERO) <= 0) {
                charge.setStatus(ChargeStatus.PAID);
                chargeRepository.save(charge);
                continue;
            }

            BigDecimal toAllocate = remainingToAllocate.min(unpaid);

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .payment(payment)
                    .charge(charge)
                    .allocatedAmount(toAllocate)
                    .build();

            PaymentAllocation saved = allocationRepository.save(allocation);
            createdAllocations.add(saved);

            // Update charge status
            BigDecimal newTotalPaid = alreadyPaid.add(toAllocate);
            if (newTotalPaid.compareTo(charge.getAmount()) >= 0) {
                charge.setStatus(ChargeStatus.PAID);
            } else {
                charge.setStatus(ChargeStatus.PARTIAL);
            }
            chargeRepository.save(charge);

            remainingToAllocate = remainingToAllocate.subtract(toAllocate);

            log.info("Allocated {} from payment #{} to charge #{} ({}), remaining unallocated: {}",
                    toAllocate, payment.getId(), charge.getId(), charge.getChargeType(), remainingToAllocate);
        }

        return createdAllocations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAllocationResponse> getPaymentAllocationsByPayment(Long paymentId) {
        return allocationRepository.findByPayment_Id(paymentId).stream()
                .map(PaymentAllocationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAllocationResponse> getPaymentAllocationsByCharge(Long chargeId) {
        return allocationRepository.findByCharge_Id(chargeId).stream()
                .map(PaymentAllocationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAllocationResponse> getPaymentAllocationsByCustomer(Long hostelId, Long customerId) {
        return allocationRepository.findByPayment_Hostel_IdAndPayment_Customer_IdOrderByCreatedAtDesc(hostelId, customerId).stream()
                .map(PaymentAllocationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalAllocatedForCustomer(Long hostelId, Long customerId) {
        return allocationRepository.sumAllocatedAmountByCustomerId(hostelId, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getSecurityDepositPaidForCustomer(Long hostelId, Long customerId) {
        return allocationRepository.sumAllocatedAmountByCustomerIdAndChargeType(
                hostelId,
                customerId,
                ChargeType.SECURITY_DEPOSIT
        );
    }
}
