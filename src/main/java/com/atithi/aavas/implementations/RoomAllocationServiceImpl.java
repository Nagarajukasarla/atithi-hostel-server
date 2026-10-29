package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.AllocationStatus;
import com.atithi.aavas.enums.RoomStatus;
import com.atithi.aavas.models.Customer;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.Room;
import com.atithi.aavas.models.RoomAllocation;
import com.atithi.aavas.repositories.CustomerRepository;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.RoomAllocationRepository;
import com.atithi.aavas.repositories.RoomRepository;
import com.atithi.aavas.requests.CreateRoomAllocationRequest;
import com.atithi.aavas.responses.RoomAllocationResponse;
import com.atithi.aavas.services.RoomAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomAllocationServiceImpl implements RoomAllocationService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final HostelRepository hostelRepository;
    private final com.atithi.aavas.repositories.AllocationFinancialTermsRepository financialTermsRepository;
    private final com.atithi.aavas.repositories.ChargeRepository chargeRepository;
    private final com.atithi.aavas.repositories.PaymentRepository paymentRepository;
    private final com.atithi.aavas.repositories.PaymentAllocationRepository paymentAllocationRepository;
    private final com.atithi.aavas.services.PaymentAllocationService paymentAllocationService;

    @Override
    @Transactional
    public RoomAllocationResponse createAllocation(Long hostelId, CreateRoomAllocationRequest request) {
        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found with ID: " + hostelId));

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.customerId()));

        // One customer -> One ACTIVE room allocation
        boolean hasActive = roomAllocationRepository.existsByHostel_IdAndCustomer_IdAndStatus(
                hostelId, customer.getId(), AllocationStatus.ACTIVE
        );
        if (hasActive) {
            throw new IllegalArgumentException("Customer " + customer.getFullName() +
                    " already has an active room allocation. Please vacate or end the existing allocation before reallocating.");
        }

        // Pessimistic write lock for concurrency safety and room capacity
        Room room = roomRepository.findByIdAndHostel_IdForUpdate(request.roomId(), hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + request.roomId() + " in current hostel"));

        int currentOccupied = room.getOccupied() != null ? room.getOccupied() : 0;
        int maxCapacity = room.getMaxCapacity();

        if (currentOccupied >= maxCapacity) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " is already fully occupied (" +
                    currentOccupied + "/" + maxCapacity + " beds).");
        }

        String bedNumber = request.bedNumber() != null && !request.bedNumber().isBlank()
                ? request.bedNumber().trim()
                : null;

        if (bedNumber != null) {
            boolean bedOccupied = roomAllocationRepository.existsActiveAllocationForRoomAndBed(
                    hostelId, request.roomId(), bedNumber, AllocationStatus.ACTIVE);
            if (bedOccupied) {
                throw new IllegalArgumentException("Bed " + bedNumber + " in room " + room.getRoomNumber() +
                        " is already occupied. Please select an available bed.");
            }
        }

        // Create Room Allocation
        RoomAllocation allocation = RoomAllocation.builder()
                .hostel(hostel)
                .customer(customer)
                .room(room)
                .bedNumber(bedNumber)
                .checkInDate(request.checkInDate())
                .checkOutDate(request.checkOutDate())
                .status(AllocationStatus.ACTIVE)
                .build();

        RoomAllocation savedAllocation = roomAllocationRepository.save(allocation);

        // Update Room Occupancy & Status atomically
        int newOccupied = currentOccupied + 1;
        room.setOccupied(newOccupied);
        if (newOccupied >= maxCapacity) {
            room.setRoomStatus(RoomStatus.FILLED);
        } else {
            room.setRoomStatus(RoomStatus.PARTIALLY_FILLED);
        }
        roomRepository.save(room);

        log.info("Allocated room {} (bed {}) to customer {} in hostel {}",
                room.getRoomNumber(), allocation.getBedNumber(), customer.getFullName(), hostelId);

        return RoomAllocationResponse.fromEntity(savedAllocation);
    }

    @Override
    @Transactional
    public RoomAllocationResponse endAllocation(Long hostelId, Long allocationId) {
        return endAllocation(hostelId, allocationId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public com.atithi.aavas.responses.CheckoutPreviewResponse getCheckoutPreview(Long hostelId, Long allocationId, LocalDate checkOutDate) {
        RoomAllocation allocation = roomAllocationRepository.findByIdAndHostel_Id(allocationId, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room allocation not found with ID: " + allocationId));

        if (allocation.getStatus() != AllocationStatus.ACTIVE) {
            throw new IllegalStateException("Room allocation is already " + allocation.getStatus());
        }

        LocalDate effectiveCheckOut = checkOutDate != null ? checkOutDate : LocalDate.now();
        LocalDate checkInDate = allocation.getCheckInDate();

        if (effectiveCheckOut.isBefore(checkInDate)) {
            throw new IllegalArgumentException("Checkout date (" + effectiveCheckOut + ") cannot be before check-in date (" + checkInDate + ")");
        }
        if (effectiveCheckOut.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Checkout date cannot be in the future");
        }

        Customer customer = allocation.getCustomer();
        com.atithi.aavas.models.AllocationFinancialTerms terms = financialTermsRepository.findByAllocation_Id(allocation.getId()).orElse(null);
        com.atithi.aavas.enums.BillingMode billingMode = terms != null && terms.getBillingMode() != null
                ? terms.getBillingMode()
                : com.atithi.aavas.enums.BillingMode.MONTHLY;

        BigDecimal totalCharges = chargeRepository.sumTotalCharges(hostelId, customer.getId());
        BigDecimal totalPaid = paymentAllocationRepository.sumAllocatedAmountByCustomerId(hostelId, customer.getId());
        BigDecimal outstandingBalance = totalCharges.subtract(totalPaid).max(BigDecimal.ZERO);

        BigDecimal securityDepositRequired = terms != null && terms.getSecurityDepositRequired() != null
                ? terms.getSecurityDepositRequired()
                : BigDecimal.ZERO;
        BigDecimal securityDepositPaid = paymentAllocationRepository.sumAllocatedAmountByCustomerIdAndChargeType(
                hostelId, customer.getId(), com.atithi.aavas.enums.ChargeType.SECURITY_DEPOSIT
        );

        String roomNumber = allocation.getRoom() != null ? allocation.getRoom().getRoomNumber() : "";
        Long roomId = allocation.getRoom() != null ? allocation.getRoom().getId() : null;

        if (billingMode == com.atithi.aavas.enums.BillingMode.DAILY) {
            int stayDurationDays = (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, effectiveCheckOut) + 1;
            stayDurationDays = Math.max(1, stayDurationDays);

            BigDecimal dailyRate = terms != null && terms.getDailyRate() != null ? terms.getDailyRate() : BigDecimal.ZERO;

            List<com.atithi.aavas.models.Charge> futureCharges = chargeRepository.findByAllocation_IdAndPeriodStartGreaterThan(
                    allocation.getId(), effectiveCheckOut
            );
            int cancelCount = (int) futureCharges.stream()
                    .filter(c -> c.getStatus() == com.atithi.aavas.enums.ChargeStatus.OPEN && c.getChargeType() == com.atithi.aavas.enums.ChargeType.DAILY_RENT)
                    .count();
            BigDecimal cancelAmount = futureCharges.stream()
                    .filter(c -> c.getStatus() == com.atithi.aavas.enums.ChargeStatus.OPEN && c.getChargeType() == com.atithi.aavas.enums.ChargeType.DAILY_RENT)
                    .map(com.atithi.aavas.models.Charge::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int missingCount = 0;
            for (LocalDate d = checkInDate; !d.isAfter(effectiveCheckOut); d = d.plusDays(1)) {
                boolean exists = chargeRepository.existsByAllocation_IdAndChargeTypeAndPeriodStart(
                        allocation.getId(), com.atithi.aavas.enums.ChargeType.DAILY_RENT, d
                );
                if (!exists) missingCount++;
            }
            BigDecimal missingAmount = dailyRate.multiply(BigDecimal.valueOf(missingCount));

            BigDecimal adjustedTotalCharges = totalCharges.subtract(cancelAmount).add(missingAmount);
            BigDecimal expectedTariff = dailyRate.multiply(BigDecimal.valueOf(stayDurationDays)).add(securityDepositRequired);
            BigDecimal netSettlementAmount = expectedTariff.subtract(totalPaid);

            return new com.atithi.aavas.responses.CheckoutPreviewResponse(
                    allocation.getId(),
                    customer.getId(),
                    customer.getFullName(),
                    customer.getMobileNumber(),
                    roomId,
                    roomNumber,
                    allocation.getBedNumber(),
                    billingMode,
                    checkInDate,
                    effectiveCheckOut,
                    stayDurationDays,
                    dailyRate,
                    null, // monthlyRent
                    null, // currentCycleStart
                    null, // currentCycleEnd
                    null, // daysInCycle
                    null, // daysStayedInCycle
                    null, // dailyProRatedRate
                    null, // proRatedRentAmount
                    null, // rentPaidForCurrentCycle
                    null, // unpaidRentDues
                    securityDepositRequired,
                    securityDepositPaid,
                    adjustedTotalCharges,
                    totalPaid,
                    outstandingBalance,
                    netSettlementAmount,
                    cancelCount
            );
        } else {
            // MONTHLY
            BigDecimal monthlyRent = terms != null && terms.getMonthlyRent() != null ? terms.getMonthlyRent() : BigDecimal.ZERO;
            LocalDate anchor = terms != null && terms.getBillingAnchorDate() != null ? terms.getBillingAnchorDate() : checkInDate;

            // Find current cycle [cStart, cEnd] containing effectiveCheckOut
            int k = 0;
            LocalDate cStart = anchor;
            LocalDate cEnd = anchor.plusMonths(1).minusDays(1);
            while (effectiveCheckOut.isAfter(cEnd) && k < 1200) {
                k++;
                cStart = anchor.plusMonths(k);
                cEnd = anchor.plusMonths(k + 1).minusDays(1);
            }
            if (effectiveCheckOut.isBefore(cStart)) {
                cStart = checkInDate;
                cEnd = checkInDate.plusMonths(1).minusDays(1);
            }

            int daysInCycle = (int) java.time.temporal.ChronoUnit.DAYS.between(cStart, cEnd) + 1;
            int daysStayedInCycle = (int) java.time.temporal.ChronoUnit.DAYS.between(cStart, effectiveCheckOut) + 1;
            daysStayedInCycle = Math.max(1, Math.min(daysStayedInCycle, daysInCycle));

            BigDecimal dailyProRatedRate = daysInCycle > 0
                    ? monthlyRent.divide(BigDecimal.valueOf(daysInCycle), 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal proRatedRentAmount = dailyProRatedRate.multiply(BigDecimal.valueOf(daysStayedInCycle))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            java.util.Optional<com.atithi.aavas.models.Charge> currentCycleChargeOpt = chargeRepository
                    .findByAllocation_IdAndChargeTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                            allocation.getId(), com.atithi.aavas.enums.ChargeType.MONTHLY_RENT, effectiveCheckOut, effectiveCheckOut
                    );

            BigDecimal rentPaidForCurrentCycle = BigDecimal.ZERO;
            if (currentCycleChargeOpt.isPresent()) {
                rentPaidForCurrentCycle = paymentAllocationRepository.sumAllocatedAmountByChargeId(currentCycleChargeOpt.get().getId());
            }

            List<com.atithi.aavas.models.Charge> futureCharges = chargeRepository.findByAllocation_IdAndPeriodStartGreaterThan(
                    allocation.getId(), effectiveCheckOut
            );
            int cancelCount = (int) futureCharges.stream()
                    .filter(c -> c.getStatus() == com.atithi.aavas.enums.ChargeStatus.OPEN)
                    .count();

            // Calculate unpaid rent dues across all cycles
            BigDecimal pastUnpaidRent = BigDecimal.ZERO;
            List<com.atithi.aavas.models.Charge> allMonthly = chargeRepository.findByAllocation_IdAndChargeTypeOrderByPeriodStartAsc(
                    allocation.getId(), com.atithi.aavas.enums.ChargeType.MONTHLY_RENT
            );
            for (com.atithi.aavas.models.Charge ch : allMonthly) {
                if (currentCycleChargeOpt.isPresent() && ch.getId().equals(currentCycleChargeOpt.get().getId())) continue;
                if (ch.getPeriodEnd() != null && ch.getPeriodEnd().isBefore(cStart)) {
                    BigDecimal pd = paymentAllocationRepository.sumAllocatedAmountByChargeId(ch.getId());
                    BigDecimal unp = ch.getAmount().subtract(pd);
                    if (unp.compareTo(BigDecimal.ZERO) > 0) pastUnpaidRent = pastUnpaidRent.add(unp);
                }
            }

            BigDecimal currentCycleUnpaid = proRatedRentAmount.subtract(rentPaidForCurrentCycle).max(BigDecimal.ZERO);
            BigDecimal unpaidRentDues = pastUnpaidRent.add(currentCycleUnpaid);

            // Net settlement amount before deductions: unpaidRent - refundableDeposit
            BigDecimal netSettlementAmount = unpaidRentDues.subtract(securityDepositPaid);

            return new com.atithi.aavas.responses.CheckoutPreviewResponse(
                    allocation.getId(),
                    customer.getId(),
                    customer.getFullName(),
                    customer.getMobileNumber(),
                    roomId,
                    roomNumber,
                    allocation.getBedNumber(),
                    billingMode,
                    checkInDate,
                    effectiveCheckOut,
                    null, // stayDurationDays
                    null, // dailyRate
                    monthlyRent,
                    cStart,
                    cEnd,
                    daysInCycle,
                    daysStayedInCycle,
                    dailyProRatedRate.setScale(2, java.math.RoundingMode.HALF_UP),
                    proRatedRentAmount,
                    rentPaidForCurrentCycle,
                    unpaidRentDues,
                    securityDepositRequired,
                    securityDepositPaid,
                    totalCharges,
                    totalPaid,
                    outstandingBalance,
                    netSettlementAmount,
                    cancelCount
            );
        }
    }

    @Override
    @Transactional
    public RoomAllocationResponse endAllocation(Long hostelId, Long allocationId, com.atithi.aavas.requests.EndRoomAllocationRequest request) {
        RoomAllocation allocation = roomAllocationRepository.findByIdAndHostel_Id(allocationId, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Room allocation not found with ID: " + allocationId));

        if (allocation.getStatus() != AllocationStatus.ACTIVE) {
            throw new IllegalStateException("Room allocation is already " + allocation.getStatus());
        }

        LocalDate checkOutDate = request != null && request.checkOutDate() != null ? request.checkOutDate() : LocalDate.now();
        LocalDate checkInDate = allocation.getCheckInDate();

        if (checkOutDate.isBefore(checkInDate)) {
            throw new IllegalArgumentException("Checkout date (" + checkOutDate + ") cannot be before check-in date (" + checkInDate + ")");
        }
        if (checkOutDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Checkout date cannot be in the future");
        }

        Room room = roomRepository.findByIdAndHostel_IdForUpdate(allocation.getRoom().getId(), hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Allocated room not found"));

        Customer customer = allocation.getCustomer();
        com.atithi.aavas.models.AllocationFinancialTerms terms = financialTermsRepository.findByAllocation_Id(allocation.getId()).orElse(null);
        com.atithi.aavas.enums.BillingMode billingMode = terms != null && terms.getBillingMode() != null
                ? terms.getBillingMode()
                : com.atithi.aavas.enums.BillingMode.MONTHLY;

        if (billingMode == com.atithi.aavas.enums.BillingMode.DAILY) {
            BigDecimal dailyRate = terms != null && terms.getDailyRate() != null ? terms.getDailyRate() : BigDecimal.ZERO;

            // 1. Generate missing daily charges up to checkOutDate
            for (LocalDate d = checkInDate; !d.isAfter(checkOutDate); d = d.plusDays(1)) {
                boolean exists = chargeRepository.existsByAllocation_IdAndChargeTypeAndPeriodStart(
                        allocation.getId(), com.atithi.aavas.enums.ChargeType.DAILY_RENT, d
                );
                if (!exists && dailyRate.compareTo(BigDecimal.ZERO) > 0) {
                    com.atithi.aavas.models.Charge dailyCharge = com.atithi.aavas.models.Charge.builder()
                            .hostel(room.getHostel())
                            .customer(customer)
                            .allocation(allocation)
                            .chargeType(com.atithi.aavas.enums.ChargeType.DAILY_RENT)
                            .amount(dailyRate)
                            .dueDate(d)
                            .periodStart(d)
                            .periodEnd(d)
                            .status(com.atithi.aavas.enums.ChargeStatus.OPEN)
                            .notes("Daily rent for " + d)
                            .build();
                    chargeRepository.save(dailyCharge);
                }
            }

            // 2. Delete speculative unallocated daily charges beyond checkOutDate
            List<com.atithi.aavas.models.Charge> futureCharges = chargeRepository.findByAllocation_IdAndPeriodStartGreaterThan(
                    allocation.getId(), checkOutDate
            );
            for (com.atithi.aavas.models.Charge ch : futureCharges) {
                if (ch.getStatus() == com.atithi.aavas.enums.ChargeStatus.OPEN && ch.getChargeType() == com.atithi.aavas.enums.ChargeType.DAILY_RENT) {
                    chargeRepository.delete(ch);
                }
            }

            // 3. Process settlement payment if provided
            if (request != null && request.payment() != null && request.payment().amount() != null && request.payment().amount().compareTo(BigDecimal.ZERO) > 0) {
                String receiptNumber = "RCP-DAILY-" + System.currentTimeMillis();
                com.atithi.aavas.models.Payment payment = com.atithi.aavas.models.Payment.builder()
                        .hostel(room.getHostel())
                        .customer(customer)
                        .allocation(allocation)
                        .paymentType(com.atithi.aavas.enums.PaymentType.DAILY_RENT)
                        .amount(request.payment().amount())
                        .paymentMethod(request.payment().paymentMethod())
                        .paymentStatus(com.atithi.aavas.enums.PaymentStatus.SUCCESS)
                        .paymentDate(checkOutDate)
                        .receiptNumber(receiptNumber)
                        .transactionReference(request.payment().transactionReference())
                        .notes(request.payment().notes() != null ? request.payment().notes() : "Daily checkout settlement payment")
                        .build();

                com.atithi.aavas.models.Payment savedPayment = paymentRepository.save(payment);
                paymentAllocationService.allocatePaymentAgainstOldestCharges(room.getHostel(), customer, savedPayment);
            }
        } else {
            // MONTHLY
            BigDecimal monthlyRent = terms != null && terms.getMonthlyRent() != null ? terms.getMonthlyRent() : BigDecimal.ZERO;
            LocalDate anchor = terms != null && terms.getBillingAnchorDate() != null ? terms.getBillingAnchorDate() : checkInDate;

            // Find exit cycle
            int k = 0;
            LocalDate cStart = anchor;
            LocalDate cEnd = anchor.plusMonths(1).minusDays(1);
            while (checkOutDate.isAfter(cEnd) && k < 1200) {
                k++;
                cStart = anchor.plusMonths(k);
                cEnd = anchor.plusMonths(k + 1).minusDays(1);
            }
            if (checkOutDate.isBefore(cStart)) {
                cStart = checkInDate;
                cEnd = checkInDate.plusMonths(1).minusDays(1);
            }

            int daysInCycle = (int) java.time.temporal.ChronoUnit.DAYS.between(cStart, cEnd) + 1;
            int daysStayedInCycle = (int) java.time.temporal.ChronoUnit.DAYS.between(cStart, checkOutDate) + 1;
            daysStayedInCycle = Math.max(1, Math.min(daysStayedInCycle, daysInCycle));

            BigDecimal dailyProRatedRate = daysInCycle > 0
                    ? monthlyRent.divide(BigDecimal.valueOf(daysInCycle), 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal proRatedRentAmount = dailyProRatedRate.multiply(BigDecimal.valueOf(daysStayedInCycle))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            // 1. Reconcile exit cycle charge
            java.util.Optional<com.atithi.aavas.models.Charge> currentChargeOpt = chargeRepository
                    .findByAllocation_IdAndChargeTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                            allocation.getId(), com.atithi.aavas.enums.ChargeType.MONTHLY_RENT, checkOutDate, checkOutDate
                    );

            if (currentChargeOpt.isPresent()) {
                com.atithi.aavas.models.Charge ch = currentChargeOpt.get();
                BigDecimal paidOnCh = paymentAllocationRepository.sumAllocatedAmountByChargeId(ch.getId());
                ch.setAmount(proRatedRentAmount);
                ch.setPeriodEnd(checkOutDate);
                ch.setNotes("Pro-rated monthly rent for " + daysStayedInCycle + "/" + daysInCycle + " days (" + cStart + " to " + checkOutDate + ")");
                if (paidOnCh.compareTo(proRatedRentAmount) >= 0) {
                    ch.setStatus(com.atithi.aavas.enums.ChargeStatus.PAID);
                } else if (paidOnCh.compareTo(BigDecimal.ZERO) > 0) {
                    ch.setStatus(com.atithi.aavas.enums.ChargeStatus.PARTIAL);
                } else {
                    ch.setStatus(com.atithi.aavas.enums.ChargeStatus.OPEN);
                }
                chargeRepository.save(ch);
            } else if (proRatedRentAmount.compareTo(BigDecimal.ZERO) > 0) {
                com.atithi.aavas.models.Charge newProRated = com.atithi.aavas.models.Charge.builder()
                        .hostel(room.getHostel())
                        .customer(customer)
                        .allocation(allocation)
                        .chargeType(com.atithi.aavas.enums.ChargeType.MONTHLY_RENT)
                        .amount(proRatedRentAmount)
                        .dueDate(cStart)
                        .periodStart(cStart)
                        .periodEnd(checkOutDate)
                        .status(com.atithi.aavas.enums.ChargeStatus.OPEN)
                        .notes("Pro-rated monthly rent for " + daysStayedInCycle + "/" + daysInCycle + " days (" + cStart + " to " + checkOutDate + ")")
                        .build();
                chargeRepository.save(newProRated);
            }

            // 2. Delete speculative future unallocated charges beyond checkOutDate
            List<com.atithi.aavas.models.Charge> futureCharges = chargeRepository.findByAllocation_IdAndPeriodStartGreaterThan(
                    allocation.getId(), checkOutDate
            );
            for (com.atithi.aavas.models.Charge ch : futureCharges) {
                if (ch.getStatus() == com.atithi.aavas.enums.ChargeStatus.OPEN) {
                    chargeRepository.delete(ch);
                }
            }

            // 3. Damage / Maintenance deductions
            if (request != null && request.depositDeduction() != null && request.depositDeduction().compareTo(BigDecimal.ZERO) > 0) {
                com.atithi.aavas.models.Charge deductionCharge = com.atithi.aavas.models.Charge.builder()
                        .hostel(room.getHostel())
                        .customer(customer)
                        .allocation(allocation)
                        .chargeType(com.atithi.aavas.enums.ChargeType.MAINTENANCE)
                        .amount(request.depositDeduction())
                        .dueDate(checkOutDate)
                        .status(com.atithi.aavas.enums.ChargeStatus.OPEN)
                        .notes("Checkout deduction: " + (request.depositDeductionReason() != null ? request.depositDeductionReason() : "Damages / Maintenance"))
                        .build();
                chargeRepository.save(deductionCharge);
            }

            // 4. Adjust security deposit against open charges if available
            BigDecimal depositPaid = paymentAllocationRepository.sumAllocatedAmountByCustomerIdAndChargeType(
                    hostelId, customer.getId(), com.atithi.aavas.enums.ChargeType.SECURITY_DEPOSIT
            );

            List<com.atithi.aavas.models.Charge> openCharges = chargeRepository.findByHostel_IdAndCustomer_IdAndStatusInOrderByDueDateAsc(
                    hostelId, customer.getId(), List.of(com.atithi.aavas.enums.ChargeStatus.OPEN, com.atithi.aavas.enums.ChargeStatus.PARTIAL)
            );

            BigDecimal totalUnpaid = BigDecimal.ZERO;
            for (com.atithi.aavas.models.Charge oc : openCharges) {
                BigDecimal pd = paymentAllocationRepository.sumAllocatedAmountByChargeId(oc.getId());
                totalUnpaid = totalUnpaid.add(oc.getAmount().subtract(pd).max(BigDecimal.ZERO));
            }

            if (depositPaid.compareTo(BigDecimal.ZERO) > 0 && totalUnpaid.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal depositToAdjust = depositPaid.min(totalUnpaid);
                com.atithi.aavas.models.Payment adjPayment = com.atithi.aavas.models.Payment.builder()
                        .hostel(room.getHostel())
                        .customer(customer)
                        .allocation(allocation)
                        .paymentType(com.atithi.aavas.enums.PaymentType.SECURITY_DEPOSIT)
                        .amount(depositToAdjust)
                        .paymentMethod(com.atithi.aavas.enums.PaymentMethod.CASH)
                        .paymentStatus(com.atithi.aavas.enums.PaymentStatus.SUCCESS)
                        .paymentDate(checkOutDate)
                        .receiptNumber("ADJ-" + System.currentTimeMillis())
                        .notes("Adjusted from security deposit on checkout to settle open dues")
                        .build();

                com.atithi.aavas.models.Payment savedAdj = paymentRepository.save(adjPayment);
                paymentAllocationService.allocatePaymentAgainstOldestCharges(room.getHostel(), customer, savedAdj);
            }

            // 5. Customer settlement payment (if guest still owes net balance)
            if (request != null && request.payment() != null && request.payment().amount() != null && request.payment().amount().compareTo(BigDecimal.ZERO) > 0) {
                com.atithi.aavas.models.Payment payment = com.atithi.aavas.models.Payment.builder()
                        .hostel(room.getHostel())
                        .customer(customer)
                        .allocation(allocation)
                        .paymentType(com.atithi.aavas.enums.PaymentType.MONTHLY_RENT)
                        .amount(request.payment().amount())
                        .paymentMethod(request.payment().paymentMethod())
                        .paymentStatus(com.atithi.aavas.enums.PaymentStatus.SUCCESS)
                        .paymentDate(checkOutDate)
                        .receiptNumber("RCP-MTH-" + System.currentTimeMillis())
                        .transactionReference(request.payment().transactionReference())
                        .notes(request.payment().notes() != null ? request.payment().notes() : "Monthly checkout settlement payment")
                        .build();

                com.atithi.aavas.models.Payment savedPayment = paymentRepository.save(payment);
                paymentAllocationService.allocatePaymentAgainstOldestCharges(room.getHostel(), customer, savedPayment);
            }

            // 6. Record refund to resident (if hostel returned deposit)
            if (request != null && request.refund() != null && request.refund().amount() != null && request.refund().amount().compareTo(BigDecimal.ZERO) > 0) {
                com.atithi.aavas.models.Payment refundPayment = com.atithi.aavas.models.Payment.builder()
                        .hostel(room.getHostel())
                        .customer(customer)
                        .allocation(allocation)
                        .paymentType(com.atithi.aavas.enums.PaymentType.SECURITY_DEPOSIT)
                        .amount(request.refund().amount())
                        .paymentMethod(request.refund().paymentMethod())
                        .paymentStatus(com.atithi.aavas.enums.PaymentStatus.SUCCESS)
                        .paymentDate(checkOutDate)
                        .receiptNumber("REF-" + System.currentTimeMillis())
                        .transactionReference(request.refund().transactionReference())
                        .notes("Deposit refund to resident: " + (request.refund().notes() != null ? request.refund().notes() : ""))
                        .build();

                paymentRepository.save(refundPayment);
            }
        }

        // 7. Update allocation to ENDED
        allocation.setStatus(AllocationStatus.ENDED);
        allocation.setCheckOutDate(checkOutDate);
        RoomAllocation updatedAllocation = roomAllocationRepository.save(allocation);

        // 8. Decrement room occupied count and update status
        int currentOccupied = room.getOccupied() != null ? room.getOccupied() : 1;
        int newOccupied = Math.max(0, currentOccupied - 1);
        room.setOccupied(newOccupied);
        if (newOccupied == 0) {
            room.setRoomStatus(RoomStatus.VACANT);
        } else {
            room.setRoomStatus(RoomStatus.PARTIALLY_FILLED);
        }
        roomRepository.save(room);

        log.info("Successfully checked out and ended allocation {} for customer {} in room {}. Occupancy now {}",
                allocationId, customer.getFullName(), room.getRoomNumber(), newOccupied);

        return RoomAllocationResponse.fromEntity(updatedAllocation);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomAllocationResponse getActiveAllocationForCustomer(Long hostelId, Long customerId) {
        return roomAllocationRepository.findByHostel_IdAndCustomer_IdAndStatus(hostelId, customerId, AllocationStatus.ACTIVE)
                .map(RoomAllocationResponse::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomAllocationResponse> getAllocations(Long hostelId) {
        return roomAllocationRepository.findByHostel_IdOrderByCreatedAtDesc(hostelId).stream()
                .map(RoomAllocationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomAllocationResponse> getAllocationsForCustomer(Long hostelId, Long customerId) {
        return roomAllocationRepository.findByHostel_IdAndCustomer_IdOrderByCreatedAtDesc(hostelId, customerId).stream()
                .map(RoomAllocationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomAllocationResponse> getActiveAllocationsForRoom(Long hostelId, Long roomId) {
        return roomAllocationRepository.findByHostel_IdAndRoom_IdAndStatus(hostelId, roomId, AllocationStatus.ACTIVE).stream()
                .map(RoomAllocationResponse::fromEntity)
                .toList();
    }
}
