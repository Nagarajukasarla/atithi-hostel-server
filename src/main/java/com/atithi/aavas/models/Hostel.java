package com.atithi.aavas.models;

import com.atithi.aavas.enums.HostelStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hostels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hostel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "address_lane_1")
    private String addressLane1;

    @Column(name = "address_lane_2")
    private String addressLane2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String zipcode;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private HostelStatus status = HostelStatus.ACTIVE;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String token;

    @PrePersist
    public void prePersist() {
        if (this.token == null || this.token.isBlank()) {
            this.token = com.atithi.aavas.utils.TokenGenerator.generateToken();
        }
    }
}
