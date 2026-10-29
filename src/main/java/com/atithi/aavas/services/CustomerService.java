package com.atithi.aavas.services;

import com.atithi.aavas.requests.CustomerRequest;
import com.atithi.aavas.responses.CustomerDirectoryItemResponse;
import com.atithi.aavas.responses.CustomerResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse getCustomerByAadhaar(String aadhaarNumber);

    CustomerResponse saveOrUpdateCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    List<CustomerResponse> getAllCustomers();

    CustomerResponse updateCustomer(Long id, CustomerRequest request);

    List<CustomerDirectoryItemResponse> getCustomerDirectory(Long hostelId);
}
