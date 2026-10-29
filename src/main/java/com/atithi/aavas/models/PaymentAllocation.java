package com.atithi.aavas.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Connects received money (Payment) to specific owed money (Charge).
 * Rule: SUM(payment_allocations.allocated_amount) <= payment.amount
 */
@Entity
@Table(
        name = "payment_allocations",
        indexes = {
                @Index(name = "idx_payment_allocation_payment", columnList = "payment_id"),
                @Index(name = "idx_payment_allocation_charge", columnList = "charge_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge;

    @Column(
            name = "allocated_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal allocatedAmount;

    public Long getPaymentId() {
        return payment != null ? payment.getId() : null;
    }

    public Long getChargeId() {
        return charge != null ? charge.getId() : null;
    }
}
