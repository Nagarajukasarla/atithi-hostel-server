package com.atithi.aavas.repositories;

import com.atithi.aavas.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByAadhaarNumber(String aadhaarNumber);

    boolean existsByAadhaarNumber(String aadhaarNumber);

    Optional<Customer> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumberAndHostelToken(String mobileNumber, String hostelToken);

    Optional<Customer> findByMobileNumberAndHostelToken(String mobileNumber, String hostelToken);

    List<Customer> findByHostelToken(String hostelToken);
}
