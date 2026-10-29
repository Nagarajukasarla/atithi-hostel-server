package com.atithi.aavas.models;

import com.atithi.aavas.enums.PaymentMethod;
import com.atithi.aavas.enums.PaymentStatus;
import com.atithi.aavas.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_hostel_id", columnList = "hostel_id"),
        @Index(name = "idx_payments_customer_id", columnList = "customer_id"),
        @Index(name = "idx_payments_allocation_id", columnList = "allocation_id"),
        @Index(name = "idx_payments_payment_date", columnList = "payment_date"),
        @Index(name = "idx_payments_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private RoomAllocation allocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 40)
    private PaymentType paymentType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "receipt_number", length = 64)
    private String receiptNumber;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(length = 500)
    private String notes;

    public Long getHostelId() {
        return hostel != null ? hostel.getId() : null;
    }

    public Long getCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    public Long getAllocationId() {
        return allocation != null ? allocation.getId() : null;
    }
}
