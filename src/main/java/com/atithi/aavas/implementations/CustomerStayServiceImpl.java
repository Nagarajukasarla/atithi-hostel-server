package com.atithi.aavas.implementations;

import com.atithi.aavas.models.*;
import com.atithi.aavas.repositories.*;
import com.atithi.aavas.requests.CreateCustomerStayRequest;
import com.atithi.aavas.requests.CreateRoomAllocationRequest;
import com.atithi.aavas.responses.CustomerStayResponse;
import com.atithi.aavas.responses.RoomAllocationResponse;
import com.atithi.aavas.services.CustomerStayService;
import com.atithi.aavas.services.RoomAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atithi.aavas.enums.PaymentMethod;
import com.atithi.aavas.enums.PaymentStatus;
import com.atithi.aavas.enums.PaymentType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Orchestrates the full customer stay creation in a single transaction.
 *
 * Plan 1 (this version): validates customer + room + tariff, creates
 *   RoomAllocation (via existing RoomAllocationService) + AllocationFinancialTerms.
 * Plan 2 will extend this to create Charges.
 * Plan 3 will extend this to create Payment + PaymentAllocations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerStayServiceImpl implements CustomerStayService {

    private final RoomAllocationService roomAllocationService;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTariffRepository roomTariffRepository;
    private final AllocationFinancialTermsRepository financialTermsRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final com.atithi.aavas.services.ChargeService chargeService;
    private final PaymentRepository paymentRepository;
    private final com.atithi.aavas.services.PaymentAllocationService paymentAllocationService;

    @Override
    @Transactional
    public CustomerStayResponse createCustomerStay(Long hostelId, CreateCustomerStayRequest request) {

        // ── 1. Validate customer exists (any hostel — customers are currently not hostel-scoped by ID) ──
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found with ID: " + request.customerId()));

        // ── 2. Validate room belongs to this hostel ──
        Room room = roomRepository.findByIdAndHostel_Id(request.roomId(), hostelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found with ID: " + request.roomId() + " in this hostel."));

        // ── 3. Validate room type (needed for tariff cross-check) ──
        if (room.getRoomType() == null) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " has no room type assigned.");
        }
        Long roomTypeId = room.getRoomType().getId();

        // ── 4. Load and validate tariff ──
        LocalDate checkInDate = request.checkInDate();
        Long tariffId = request.financialTerms().roomTariffId();
        RoomTariff tariff;

        if (tariffId != null) {
            tariff = roomTariffRepository.findByIdAndHostel_Id(tariffId, hostelId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Room tariff not found with ID: " + tariffId + " in this hostel."));

            // Tariff must belong to the same room type as the selected room
            if (!tariff.getRoomType().getId().equals(roomTypeId)) {
                throw new IllegalArgumentException(
                        "The selected tariff does not belong to the room type of room " + room.getRoomNumber() + ".");
            }

            // Tariff must be active and effective for the check-in date
            if (!tariff.isActive()) {
                throw new IllegalArgumentException("The selected tariff is no longer active.");
            }
            if (tariff.getEffectiveFrom().isAfter(checkInDate)) {
                throw new IllegalArgumentException(
                        "The selected tariff is not yet effective on the check-in date " + checkInDate + ".");
            }
            if (tariff.getEffectiveTo() != null && tariff.getEffectiveTo().isBefore(checkInDate)) {
                throw new IllegalArgumentException(
                        "The selected tariff expired before the check-in date " + checkInDate + ".");
            }
        } else {
            // Fallback: look up current active tariff for this room type
            tariff = roomTariffRepository.findCurrentTariff(hostelId, roomTypeId, checkInDate)
                    .orElseGet(() -> {
                        // Create a default tariff for this room type
                        RoomTariff newTariff = RoomTariff.builder()
                                .hostel(room.getHostel())
                                .roomType(room.getRoomType())
                                .monthlyRent(request.financialTerms().monthlyRent())
                                .securityDepositRequired(request.financialTerms().securityDepositRequired())
                                .effectiveFrom(checkInDate)
                                .active(true)
                                .build();
                        return roomTariffRepository.save(newTariff);
                    });
        }

        // ── 5. Call existing RoomAllocationService — do NOT reimplement allocation logic ──
        RoomAllocationResponse allocationResponse = roomAllocationService.createAllocation(
                hostelId,
                new CreateRoomAllocationRequest(
                        request.customerId(),
                        request.roomId(),
                        request.bedNumber(),
                        checkInDate,
                        request.checkOutDate()
                )
        );

        // Load the saved allocation entity for financial terms FK
        RoomAllocation allocation = roomAllocationRepository.findById(allocationResponse.id())
                .orElseThrow(() -> new IllegalStateException("Allocation not found after creation — unexpected error."));

        // ── 6. Create AllocationFinancialTerms ──
        LocalDate billingAnchorDate = request.financialTerms().billingAnchorDate() != null
                ? request.financialTerms().billingAnchorDate()
                : checkInDate;

        com.atithi.aavas.enums.BillingMode billingMode = request.financialTerms().billingMode() != null
                ? request.financialTerms().billingMode()
                : com.atithi.aavas.enums.BillingMode.MONTHLY;

        java.math.BigDecimal agreedDailyRate = billingMode == com.atithi.aavas.enums.BillingMode.DAILY
                ? (request.financialTerms().dailyRate() != null
                        ? request.financialTerms().dailyRate()
                        : (tariff.getDailyRate() != null ? tariff.getDailyRate() : java.math.BigDecimal.ZERO))
                : null;

        java.math.BigDecimal agreedMonthlyRent = billingMode == com.atithi.aavas.enums.BillingMode.MONTHLY
                ? (request.financialTerms().monthlyRent() != null
                        ? request.financialTerms().monthlyRent()
                        : (tariff.getMonthlyRent() != null ? tariff.getMonthlyRent() : java.math.BigDecimal.ZERO))
                : null;

        AllocationFinancialTerms terms = AllocationFinancialTerms.builder()
                .hostel(room.getHostel())
                .allocation(allocation)
                .roomTariff(tariff)
                .billingMode(billingMode)
                .monthlyRent(agreedMonthlyRent)
                .dailyRate(agreedDailyRate)
                .securityDepositRequired(request.financialTerms().securityDepositRequired())
                .billingAnchorDate(billingAnchorDate)
                .build();

        AllocationFinancialTerms savedTerms = financialTermsRepository.save(terms);

        // ── 7. Create Charges ──
        java.util.List<Charge> createdCharges = new java.util.ArrayList<>();

        // 7a. SECURITY_DEPOSIT charge
        Charge depositCharge = chargeService.createSecurityDepositCharge(
                room.getHostel(),
                customer,
                allocation,
                savedTerms.getSecurityDepositRequired(),
                checkInDate
        );
        if (depositCharge != null) {
            createdCharges.add(depositCharge);
        }

        // 7b. Initial rent charge (DAILY or MONTHLY)
        if (billingMode == com.atithi.aavas.enums.BillingMode.DAILY) {
            LocalDate today = LocalDate.now();
            if (!checkInDate.isAfter(today)) {
                Charge dailyCharge = chargeService.createDailyRentCharge(
                        room.getHostel(),
                        customer,
                        allocation,
                        savedTerms.getDailyRate(),
                        checkInDate
                );
                if (dailyCharge != null) {
                    createdCharges.add(dailyCharge);
                }
            }
        } else {
            LocalDate periodStart = checkInDate;
            LocalDate periodEnd = checkInDate.plusMonths(1).minusDays(1);
            Charge rentCharge = chargeService.createMonthlyRentCharge(
                    room.getHostel(),
                    customer,
                    allocation,
                    savedTerms.getMonthlyRent(),
                    checkInDate,
                    periodStart,
                    periodEnd
            );
            if (rentCharge != null) {
                createdCharges.add(rentCharge);
            }
        }

        // ── 8. Initial Payment & Allocation (Plan 3) ──
        Payment savedPayment = null;
        java.util.List<PaymentAllocation> createdAllocations = java.util.List.of();

        if (request.initialPayment() != null
                && request.initialPayment().amount() != null
                && request.initialPayment().amount().compareTo(java.math.BigDecimal.ZERO) > 0) {

            String receiptNumber = "RCP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            PaymentMethod method = request.initialPayment().paymentMode() != null
                    ? request.initialPayment().paymentMode()
                    : PaymentMethod.CASH;

            String notes = request.initialPayment().notes() != null && !request.initialPayment().notes().isBlank()
                    ? request.initialPayment().notes()
                    : "Initial registration payment for Room " + room.getRoomNumber()
                    + (allocation.getBedNumber() != null ? " Bed " + allocation.getBedNumber() : "");

            Payment payment = Payment.builder()
                    .hostel(room.getHostel())
                    .customer(customer)
                    .allocation(allocation)
                    .paymentType(PaymentType.INITIAL_REGISTRATION)
                    .amount(request.initialPayment().amount())
                    .paymentMethod(method)
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .paymentDate(checkInDate)
                    .receiptNumber(receiptNumber)
                    .notes(notes)
                    .build();

            savedPayment = paymentRepository.save(payment);

            // Allocate payment against charges oldest-first
            createdAllocations = paymentAllocationService.allocatePaymentAgainstOldestCharges(
                    room.getHostel(),
                    customer,
                    savedPayment
            );
        }

        // ── 9. Calculate authoritative outstanding balance ──
        java.math.BigDecimal outstandingBalance = chargeService.calculateOutstandingBalance(hostelId, customer.getId());

        log.info("Customer stay registered for customer {} in room {} (hostel {}): rent={}, deposit={}, chargesCount={}, paid={}, balance={}",
                customer.getFullName(), room.getRoomNumber(), hostelId,
                savedTerms.getMonthlyRent(), savedTerms.getSecurityDepositRequired(),
                createdCharges.size(),
                savedPayment != null ? savedPayment.getAmount() : java.math.BigDecimal.ZERO,
                outstandingBalance);

        return CustomerStayResponse.fromEntities(
                allocation,
                savedTerms,
                createdCharges,
                savedPayment,
                createdAllocations,
                outstandingBalance
        );
    }
}
