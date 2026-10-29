package com.atithi.aavas.models;

import com.atithi.aavas.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "alt_mobile_number", length = 15)
    private String altMobileNumber;

    @Column(length = 255)
    private String email;

    @Column(name = "aadhaar_number", unique = true, length = 12)
    private String aadhaarNumber;

    @Column(length = 100)
    private String occupation;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 15)
    private String emergencyContactPhone;

    @Column(name = "permanent_address", columnDefinition = "TEXT")
    private String permanentAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "hostel_token", length = 64)
    private String hostelToken;

    @PrePersist
    @PreUpdate
    public void updateFullName() {
        if (firstName != null && lastName != null) {
            this.fullName = (firstName.trim() + " " + lastName.trim()).trim();
        } else if (firstName != null) {
            this.fullName = firstName.trim();
        }
    }
}
