package com.atithi.aavas.models;

import com.atithi.aavas.enums.ChargeStatus;
import com.atithi.aavas.enums.ChargeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "charges",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_charge_allocation_period",
                        columnNames = {"allocation_id", "charge_type", "period_start"}
                )
        },
        indexes = {
                @Index(name = "idx_charge_customer", columnList = "customer_id"),
                @Index(name = "idx_charge_allocation", columnList = "allocation_id"),
                @Index(name = "idx_charge_due_date", columnList = "due_date"),
                @Index(name = "idx_charge_hostel", columnList = "hostel_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private RoomAllocation allocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 40)
    private ChargeType chargeType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "notes", length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ChargeStatus status = ChargeStatus.OPEN;
}
