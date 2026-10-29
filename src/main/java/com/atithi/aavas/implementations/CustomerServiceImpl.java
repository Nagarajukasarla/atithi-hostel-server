package com.atithi.aavas.implementations;

import com.atithi.aavas.enums.AllocationStatus;
import com.atithi.aavas.enums.ChargeType;
import com.atithi.aavas.models.*;
import com.atithi.aavas.repositories.*;
import com.atithi.aavas.requests.CustomerRequest;
import com.atithi.aavas.responses.CustomerDirectoryItemResponse;
import com.atithi.aavas.responses.CustomerResponse;
import com.atithi.aavas.services.ChargeService;
import com.atithi.aavas.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final AllocationFinancialTermsRepository financialTermsRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentRepository paymentRepository;
    private final ChargeService chargeService;

    @Override
    public CustomerResponse getCustomerByAadhaar(String aadhaarNumber) {
        Customer customer = customerRepository.findByAadhaarNumber(aadhaarNumber)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with Aadhaar: " + aadhaarNumber));
        return CustomerResponse.fromEntity(customer);
    }

    private String trimToNull(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    public CustomerResponse saveOrUpdateCustomer(CustomerRequest request) {
        String aadhaar = trimToNull(request.aadhaarNumber());
        Customer customer = aadhaar != null
                ? customerRepository.findByAadhaarNumber(aadhaar).orElse(null)
                : null;

        if (customer == null) {
            customer = Customer.builder()
                    .firstName(request.firstName().trim())
                    .lastName(request.lastName().trim())
                    .gender(request.gender())
                    .dateOfBirth(request.dateOfBirth())
                    .mobileNumber(request.mobileNumber().trim())
                    .altMobileNumber(trimToNull(request.altMobileNumber()))
                    .email(trimToNull(request.email()))
                    .aadhaarNumber(aadhaar)
                    .occupation(trimToNull(request.occupation()))
                    .emergencyContactName(trimToNull(request.emergencyContactName()))
                    .emergencyContactPhone(trimToNull(request.emergencyContactPhone()))
                    .permanentAddress(trimToNull(request.permanentAddress()))
                    .build();
            log.info("Creating new customer (Aadhaar: {})", aadhaar);
        } else {
            customer.setFirstName(request.firstName().trim());
            customer.setLastName(request.lastName().trim());
            customer.setGender(request.gender());
            customer.setDateOfBirth(request.dateOfBirth());
            customer.setMobileNumber(request.mobileNumber().trim());
            customer.setAltMobileNumber(trimToNull(request.altMobileNumber()));
            customer.setEmail(trimToNull(request.email()));
            customer.setOccupation(trimToNull(request.occupation()));
            customer.setEmergencyContactName(trimToNull(request.emergencyContactName()));
            customer.setEmergencyContactPhone(trimToNull(request.emergencyContactPhone()));
            customer.setPermanentAddress(trimToNull(request.permanentAddress()));
            log.info("Updating existing customer with Aadhaar: {}", aadhaar);
        }

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));
        return CustomerResponse.fromEntity(customer);
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));

        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setGender(request.gender());
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setMobileNumber(request.mobileNumber().trim());
        customer.setAltMobileNumber(trimToNull(request.altMobileNumber()));
        customer.setEmail(trimToNull(request.email()));
        customer.setAadhaarNumber(trimToNull(request.aadhaarNumber()));
        customer.setOccupation(trimToNull(request.occupation()));
        customer.setEmergencyContactName(trimToNull(request.emergencyContactName()));
        customer.setEmergencyContactPhone(trimToNull(request.emergencyContactPhone()));
        customer.setPermanentAddress(trimToNull(request.permanentAddress()));

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDirectoryItemResponse> getCustomerDirectory(Long hostelId) {
        if (hostelId == null) {
            throw new IllegalArgumentException("Hostel ID must be provided");
        }

        // Fetch allocations strictly for this hostel ordered newest first
        List<RoomAllocation> allocations = roomAllocationRepository.findByHostel_IdOrderByCreatedAtDesc(hostelId);

        // Group by customer ID keeping active allocations prioritized
        Map<Long, RoomAllocation> customerAllocationMap = new LinkedHashMap<>();
        for (RoomAllocation alloc : allocations) {
            if (alloc.getCustomer() != null) {
                Long custId = alloc.getCustomer().getId();
                if (!customerAllocationMap.containsKey(custId)) {
                    customerAllocationMap.put(custId, alloc);
                } else if (alloc.getStatus() == AllocationStatus.ACTIVE && customerAllocationMap.get(custId).getStatus() != AllocationStatus.ACTIVE) {
                    customerAllocationMap.put(custId, alloc);
                }
            }
        }

        List<CustomerDirectoryItemResponse> directory = new ArrayList<>();

        for (RoomAllocation allocation : customerAllocationMap.values()) {
            Customer customer = allocation.getCustomer();
            Long customerId = customer.getId();

            AllocationFinancialTerms terms = financialTermsRepository.findByAllocation_Id(allocation.getId())
                    .orElse(null);

            com.atithi.aavas.enums.BillingMode billingMode = terms != null && terms.getBillingMode() != null
                    ? terms.getBillingMode()
                    : com.atithi.aavas.enums.BillingMode.MONTHLY;

            BigDecimal monthlyRent = terms != null && terms.getMonthlyRent() != null
                    ? terms.getMonthlyRent()
                    : BigDecimal.ZERO;

            BigDecimal dailyRate = terms != null ? terms.getDailyRate() : null;

            BigDecimal securityDeposit = terms != null && terms.getSecurityDepositRequired() != null
                    ? terms.getSecurityDepositRequired()
                    : BigDecimal.ZERO;

            BigDecimal securityDepositPaid = paymentAllocationRepository.sumAllocatedAmountByCustomerIdAndChargeType(
                    hostelId,
                    customerId,
                    ChargeType.SECURITY_DEPOSIT
            );

            // Paid rent for current cycle (DAILY or MONTHLY)
            ChargeType rentType = billingMode == com.atithi.aavas.enums.BillingMode.DAILY
                    ? ChargeType.DAILY_RENT
                    : ChargeType.MONTHLY_RENT;

            Optional<Charge> currentRentCharge = chargeRepository.findTopByHostel_IdAndCustomer_IdAndChargeTypeOrderByPeriodStartDesc(
                    hostelId,
                    customerId,
                    rentType
            );

            BigDecimal paidRent = currentRentCharge
                    .map(charge -> paymentAllocationRepository.sumAllocatedAmountByChargeId(charge.getId()))
                    .orElse(BigDecimal.ZERO);

            BigDecimal outstandingBalance = chargeService.calculateOutstandingBalance(hostelId, customerId);

            // Latest payment date for this customer in this hostel
            LocalDate latestPaymentDate = paymentRepository
                    .findTopByHostel_IdAndCustomer_IdOrderByPaymentDateDescCreatedAtDesc(hostelId, customerId)
                    .map(Payment::getPaymentDate)
                    .orElse(null);

            String roomNumber = allocation.getRoom() != null ? allocation.getRoom().getRoomNumber() : "";
            String bedNumber = allocation.getBedNumber();
            LocalDate joinDate = allocation.getCheckInDate();
            LocalDate checkOutDate = allocation.getCheckOutDate();
            Long allocId = allocation.getId();
            AllocationStatus allocStatus = allocation.getStatus();

            directory.add(new CustomerDirectoryItemResponse(
                    customerId,
                    customer.getFullName(),
                    customer.getMobileNumber(),
                    roomNumber,
                    bedNumber,
                    billingMode,
                    monthlyRent,
                    dailyRate,
                    paidRent,
                    securityDeposit,
                    securityDepositPaid,
                    joinDate,
                    checkOutDate,
                    allocId,
                    allocStatus,
                    outstandingBalance,
                    latestPaymentDate
            ));
        }

        return directory;
    }
}
