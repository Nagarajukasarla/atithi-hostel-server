package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.PaymentStatus;
import com.atithi.aavas.models.Customer;
import com.atithi.aavas.models.Hostel;
import com.atithi.aavas.models.Payment;
import com.atithi.aavas.models.RoomAllocation;
import com.atithi.aavas.repositories.CustomerRepository;
import com.atithi.aavas.repositories.HostelRepository;
import com.atithi.aavas.repositories.PaymentRepository;
import com.atithi.aavas.repositories.RoomAllocationRepository;
import com.atithi.aavas.requests.CreatePaymentRequest;
import com.atithi.aavas.responses.PaymentResponse;
import com.atithi.aavas.services.PaymentAllocationService;
import com.atithi.aavas.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final HostelRepository hostelRepository;
    private final CustomerRepository customerRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final PaymentAllocationService paymentAllocationService;

    @Override
    @Transactional
    public PaymentResponse createPayment(Long hostelId, CreatePaymentRequest request) {
        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Hostel not found with ID: " + hostelId));

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.customerId()));

        RoomAllocation allocation = null;
        if (request.allocationId() != null) {
            allocation = roomAllocationRepository.findByIdAndHostel_Id(request.allocationId(), hostelId)
                    .orElseThrow(() -> new IllegalArgumentException("Room allocation not found with ID: " + request.allocationId()));
        }

        String receiptNumber = request.receiptNumber();
        if (receiptNumber == null || receiptNumber.isBlank()) {
            receiptNumber = "RCP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        LocalDate paymentDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();
        PaymentStatus status = request.paymentStatus() != null ? request.paymentStatus() : PaymentStatus.SUCCESS;

        Payment payment = Payment.builder()
                .hostel(hostel)
                .customer(customer)
                .allocation(allocation)
                .paymentType(request.paymentType())
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .paymentStatus(status)
                .transactionReference(request.transactionReference())
                .receiptNumber(receiptNumber)
                .paymentDate(paymentDate)
                .notes(request.notes())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Allocate payment against outstanding charges (oldest-first) — same logic as initial registration
        if (status == PaymentStatus.SUCCESS) {
            paymentAllocationService.allocatePaymentAgainstOldestCharges(
                    hostel,
                    customer,
                    savedPayment
            );
        }

        log.info("Recorded payment {} of {} via {} for customer {} — allocated against outstanding charges",
                savedPayment.getId(), savedPayment.getAmount(), savedPayment.getPaymentMethod(), customer.getFullName());

        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(Long hostelId) {
        return paymentRepository.findByHostel_IdOrderByPaymentDateDescCreatedAtDesc(hostelId).stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long hostelId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndHostel_Id(paymentId, hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));
        return PaymentResponse.fromEntity(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCustomer(Long hostelId, Long customerId) {
        return paymentRepository.findByHostel_IdAndCustomer_IdOrderByPaymentDateDesc(hostelId, customerId).stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByAllocation(Long hostelId, Long allocationId) {
        return paymentRepository.findByHostel_IdAndAllocation_IdOrderByPaymentDateDesc(hostelId, allocationId).stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }
}
