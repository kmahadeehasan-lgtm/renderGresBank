package com.izak.demoBankManagement.service;




import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.entity.Customer;
import com.izak.demoBankManagement.entity.User;
import com.izak.demoBankManagement.exception.*;
import com.izak.demoBankManagement.repository.CustomerRepository;
import com.izak.demoBankManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // ADD THIS

    @Transactional
    public CustomerResponseDTO createCustomer(CustomerCreateRequestDTO request) {
        log.info("Creating new customer: {} {}", request.getFirstName(), request.getLastName());

        // Validate email and username uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer with this email already exists");
        }

        // Create User account with ENCODED password
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // ENCODE PASSWORD
        user.setEmail(request.getEmail());
        user.setRole(User.Role.CUSTOMER);
        user.setIsActive(true);
        user = userRepository.save(user);

        // Create Customer
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setCustomerId(generateCustomerId());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setZipCode(request.getZipCode());
        customer.setStatus(Customer.Status.ACTIVE);
        customer.setKycStatus(Customer.KycStatus.PENDING);
        customer.setImage(request.getImage());

        customer = customerRepository.save(customer);

        log.info("Customer created successfully with ID: {}", customer.getCustomerId());

        return mapToResponseDTO(customer);
    }

    public CustomerResponseDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
        return mapToResponseDTO(customer);
    }

    public CustomerResponseDTO getCustomerByCustomerId(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with Customer ID: " + customerId));
        return mapToResponseDTO(customer);
    }

    public List<CustomerListItemDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    public List<CustomerListItemDTO> getCustomersByStatus(String status) {
        Customer.Status customerStatus = Customer.Status.valueOf(status.toUpperCase());
        return customerRepository.findByStatus(customerStatus).stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    public List<CustomerListItemDTO> getCustomersByKycStatus(String kycStatus) {
        Customer.KycStatus status = Customer.KycStatus.valueOf(kycStatus.toUpperCase());
        return customerRepository.findByKycStatus(status).stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerResponseDTO updateCustomer(Long id, CustomerUpdateRequestDTO request) {
        log.info("Updating customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));

        // Update fields if provided
        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            if (!customer.getEmail().equals(request.getEmail()) &&
                    customerRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
            }
            customer.setEmail(request.getEmail());
            customer.getUser().setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            customer.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            customer.setCity(request.getCity());
        }
        if (request.getState() != null) {
            customer.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            customer.setZipCode(request.getZipCode());
        }
        if (request.getImage() != null) {
            customer.setImage(request.getImage());
        }
        if (request.getStatus() != null) {
            customer.setStatus(Customer.Status.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getKycStatus() != null) {
            customer.setKycStatus(Customer.KycStatus.valueOf(request.getKycStatus().toUpperCase()));
        }

        customer = customerRepository.save(customer);

        log.info("Customer updated successfully: {}", customer.getCustomerId());

        return mapToResponseDTO(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));

        customer.setStatus(Customer.Status.INACTIVE);
        customer.getUser().setIsActive(false);

        customerRepository.save(customer);

        log.info("Customer soft deleted: {}", customer.getCustomerId());
    }

    @Transactional
    public void hardDeleteCustomer(Long id) {
        log.info("Permanently deleting customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));

        customerRepository.delete(customer);

        log.info("Customer permanently deleted");
    }

    public List<CustomerListItemDTO> searchCustomers(String searchTerm) {
        return customerRepository.findAll().stream()
                .filter(customer ->
                        customer.getCustomerId().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                customer.getFirstName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                customer.getLastName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                customer.getEmail().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                customer.getPhone().contains(searchTerm)
                )
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerResponseDTO updateKycStatus(String customerId, String kycStatus) {
        log.info("Updating KYC status for customer: {} to {}", customerId, kycStatus);

        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        customer.setKycStatus(Customer.KycStatus.valueOf(kycStatus.toUpperCase()));
        customer = customerRepository.save(customer);

        log.info("KYC status updated successfully");

        return mapToResponseDTO(customer);
    }

    private String generateCustomerId() {
        String customerId;
        do {
            int randomNum = new Random().nextInt(9000) + 1000;
            customerId = "CUST" + randomNum;
        } while (customerRepository.existsByCustomerId(customerId));

        return customerId;
    }

    private CustomerResponseDTO mapToResponseDTO(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setId(customer.getId());
        dto.setCustomerId(customer.getCustomerId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setAddress(customer.getAddress());
        dto.setCity(customer.getCity());
        dto.setState(customer.getState());
        dto.setZipCode(customer.getZipCode());
        dto.setStatus(customer.getStatus().name().toLowerCase());
        dto.setKycStatus(customer.getKycStatus().name().toLowerCase());
        dto.setImage(customer.getImage());
        dto.setCreatedDate(customer.getCreatedDate());
        dto.setLastUpdated(customer.getLastUpdated());
        dto.setUsername(customer.getUser().getUsername());
        dto.setIsActive(customer.getUser().getIsActive());
        dto.setTotalAccounts(customer.getAccounts() != null ? customer.getAccounts().size() : 0);
        dto.setTotalLoans(customer.getLoans() != null ? customer.getLoans().size() : 0);
        dto.setTotalCards(customer.getCards() != null ? customer.getCards().size() : 0);

        return dto;
    }

    private CustomerListItemDTO mapToListItemDTO(Customer customer) {
        CustomerListItemDTO dto = new CustomerListItemDTO();
        dto.setId(customer.getId());
        dto.setCustomerId(customer.getCustomerId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setCity(customer.getCity());
        dto.setState(customer.getState());
        dto.setStatus(customer.getStatus().name().toLowerCase());
        dto.setKycStatus(customer.getKycStatus().name().toLowerCase());
        dto.setImage(customer.getImage());
        dto.setCreatedDate(customer.getCreatedDate());

        return dto;
    }
}


//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CustomerService {
//
//    private final CustomerRepository customerRepository;
//    private final UserRepository userRepository;
//
//    // ============================================
//    // CREATE CUSTOMER
//    // ============================================
//    @Transactional
//    public CustomerResponseDTO createCustomer(CustomerCreateRequestDTO request) {
//        log.info("Creating new customer: {} {}", request.getFirstName(), request.getLastName());
//
//        // 1. Validate email and username uniqueness
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
//        }
//
//        if (userRepository.existsByUsername(request.getUsername())) {
//            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
//        }
//
//        if (customerRepository.existsByEmail(request.getEmail())) {
//            throw new DuplicateResourceException("Customer with this email already exists");
//        }
//
//        // 2. Create User account
//        User user = new User();
//        user.setUsername(request.getUsername());
//        user.setPassword(request.getPassword()); // TODO: Encrypt with BCrypt when adding security
//        user.setEmail(request.getEmail());
//        user.setRole(User.Role.CUSTOMER);
//        user.setIsActive(true);
//        user = userRepository.save(user);
//
//        // 3. Create Customer
//        Customer customer = new Customer();
//        customer.setUser(user);
//        customer.setCustomerId(generateCustomerId());
//        customer.setFirstName(request.getFirstName());
//        customer.setLastName(request.getLastName());
//        customer.setEmail(request.getEmail());
//        customer.setPhone(request.getPhone());
//        customer.setDateOfBirth(request.getDateOfBirth());
//        customer.setAddress(request.getAddress());
//        customer.setCity(request.getCity());
//        customer.setState(request.getState());
//        customer.setZipCode(request.getZipCode());
//        customer.setStatus(Customer.Status.ACTIVE);
//        customer.setKycStatus(Customer.KycStatus.PENDING);
//        customer.setImage(request.getImage());
//
//        customer = customerRepository.save(customer);
//
//        log.info("Customer created successfully with ID: {}", customer.getCustomerId());
//
//        return mapToResponseDTO(customer);
//    }
//
//    // ============================================
//    // GET CUSTOMER BY ID
//    // ============================================
//    public CustomerResponseDTO getCustomerById(Long id) {
//        Customer customer = customerRepository.findById(id)
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
//
//        return mapToResponseDTO(customer);
//    }
//
//    // ============================================
//    // GET CUSTOMER BY CUSTOMER ID
//    // ============================================
//    public CustomerResponseDTO getCustomerByCustomerId(String customerId) {
//        Customer customer = customerRepository.findByCustomerId(customerId)
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with Customer ID: " + customerId));
//
//        return mapToResponseDTO(customer);
//    }
//
//    // ============================================
//    // GET ALL CUSTOMERS
//    // ============================================
//    public List<CustomerListItemDTO> getAllCustomers() {
//        return customerRepository.findAll().stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    // ============================================
//    // GET CUSTOMERS BY STATUS
//    // ============================================
//    public List<CustomerListItemDTO> getCustomersByStatus(String status) {
//        Customer.Status customerStatus = Customer.Status.valueOf(status.toUpperCase());
//        return customerRepository.findByStatus(customerStatus).stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    // ============================================
//    // GET CUSTOMERS BY KYC STATUS
//    // ============================================
//    public List<CustomerListItemDTO> getCustomersByKycStatus(String kycStatus) {
//        Customer.KycStatus status = Customer.KycStatus.valueOf(kycStatus.toUpperCase());
//        return customerRepository.findByKycStatus(status).stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    // ============================================
//    // UPDATE CUSTOMER
//    // ============================================
//    @Transactional
//    public CustomerResponseDTO updateCustomer(Long id, CustomerUpdateRequestDTO request) {
//        log.info("Updating customer with ID: {}", id);
//
//        Customer customer = customerRepository.findById(id)
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
//
//        // Update fields if provided
//        if (request.getFirstName() != null) {
//            customer.setFirstName(request.getFirstName());
//        }
//        if (request.getLastName() != null) {
//            customer.setLastName(request.getLastName());
//        }
//        if (request.getEmail() != null) {
//            // Check if new email already exists
//            if (!customer.getEmail().equals(request.getEmail()) &&
//                    customerRepository.existsByEmail(request.getEmail())) {
//                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
//            }
//            customer.setEmail(request.getEmail());
//            customer.getUser().setEmail(request.getEmail());
//        }
//        if (request.getPhone() != null) {
//            customer.setPhone(request.getPhone());
//        }
//        if (request.getDateOfBirth() != null) {
//            customer.setDateOfBirth(request.getDateOfBirth());
//        }
//        if (request.getAddress() != null) {
//            customer.setAddress(request.getAddress());
//        }
//        if (request.getCity() != null) {
//            customer.setCity(request.getCity());
//        }
//        if (request.getState() != null) {
//            customer.setState(request.getState());
//        }
//        if (request.getZipCode() != null) {
//            customer.setZipCode(request.getZipCode());
//        }
//        if (request.getImage() != null) {
//            customer.setImage(request.getImage());
//        }
//        if (request.getStatus() != null) {
//            customer.setStatus(Customer.Status.valueOf(request.getStatus().toUpperCase()));
//        }
//        if (request.getKycStatus() != null) {
//            customer.setKycStatus(Customer.KycStatus.valueOf(request.getKycStatus().toUpperCase()));
//        }
//
//        customer = customerRepository.save(customer);
//
//        log.info("Customer updated successfully: {}", customer.getCustomerId());
//
//        return mapToResponseDTO(customer);
//    }
//
//    // ============================================
//    // DELETE CUSTOMER (Soft Delete - Set Status to INACTIVE)
//    // ============================================
//    @Transactional
//    public void deleteCustomer(Long id) {
//        log.info("Deleting customer with ID: {}", id);
//
//        Customer customer = customerRepository.findById(id)
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
//
//        // Soft delete - set status to inactive
//        customer.setStatus(Customer.Status.INACTIVE);
//        customer.getUser().setIsActive(false);
//
//        customerRepository.save(customer);
//
//        log.info("Customer soft deleted: {}", customer.getCustomerId());
//    }
//
//    // ============================================
//    // HARD DELETE CUSTOMER (Permanently remove)
//    // ============================================
//    @Transactional
//    public void hardDeleteCustomer(Long id) {
//        log.info("Permanently deleting customer with ID: {}", id);
//
//        Customer customer = customerRepository.findById(id)
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
//
//        customerRepository.delete(customer);
//
//        log.info("Customer permanently deleted");
//    }
//
//    // ============================================
//    // SEARCH CUSTOMERS
//    // ============================================
//    public List<CustomerListItemDTO> searchCustomers(String searchTerm) {
//        // Simple search implementation - can be enhanced with JPA Specifications
//        return customerRepository.findAll().stream()
//                .filter(customer ->
//                        customer.getCustomerId().toLowerCase().contains(searchTerm.toLowerCase()) ||
//                                customer.getFirstName().toLowerCase().contains(searchTerm.toLowerCase()) ||
//                                customer.getLastName().toLowerCase().contains(searchTerm.toLowerCase()) ||
//                                customer.getEmail().toLowerCase().contains(searchTerm.toLowerCase()) ||
//                                customer.getPhone().contains(searchTerm)
//                )
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    // ============================================
//    // UPDATE KYC STATUS
//    // ============================================
//    @Transactional
//    public CustomerResponseDTO updateKycStatus(String customerId, String kycStatus) {
//        log.info("Updating KYC status for customer: {} to {}", customerId, kycStatus);
//
//        Customer customer = customerRepository.findByCustomerId(customerId)
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
//
//        customer.setKycStatus(Customer.KycStatus.valueOf(kycStatus.toUpperCase()));
//        customer = customerRepository.save(customer);
//
//        log.info("KYC status updated successfully");
//
//        return mapToResponseDTO(customer);
//    }
//
//    // ============================================
//    // HELPER METHODS
//    // ============================================
//
//    private String generateCustomerId() {
//        String customerId;
//        do {
//            int randomNum = new Random().nextInt(9000) + 1000;
//            customerId = "CUST" + randomNum;
//        } while (customerRepository.existsByCustomerId(customerId));
//
//        return customerId;
//    }
//
//    private CustomerResponseDTO mapToResponseDTO(Customer customer) {
//        CustomerResponseDTO dto = new CustomerResponseDTO();
//        dto.setId(customer.getId());
//        dto.setCustomerId(customer.getCustomerId());
//        dto.setFirstName(customer.getFirstName());
//        dto.setLastName(customer.getLastName());
//        dto.setEmail(customer.getEmail());
//        dto.setPhone(customer.getPhone());
//        dto.setDateOfBirth(customer.getDateOfBirth());
//        dto.setAddress(customer.getAddress());
//        dto.setCity(customer.getCity());
//        dto.setState(customer.getState());
//        dto.setZipCode(customer.getZipCode());
//        dto.setStatus(customer.getStatus().name().toLowerCase());
//        dto.setKycStatus(customer.getKycStatus().name().toLowerCase());
//        dto.setImage(customer.getImage());
//        dto.setCreatedDate(customer.getCreatedDate());
//        dto.setLastUpdated(customer.getLastUpdated());
//        dto.setUsername(customer.getUser().getUsername());
//        dto.setIsActive(customer.getUser().getIsActive());
//        dto.setTotalAccounts(customer.getAccounts() != null ? customer.getAccounts().size() : 0);
//        dto.setTotalLoans(customer.getLoans() != null ? customer.getLoans().size() : 0);
//        dto.setTotalCards(customer.getCards() != null ? customer.getCards().size() : 0);
//
//        return dto;
//    }
//
//    private CustomerListItemDTO mapToListItemDTO(Customer customer) {
//        CustomerListItemDTO dto = new CustomerListItemDTO();
//        dto.setId(customer.getId());
//        dto.setCustomerId(customer.getCustomerId());
//        dto.setFirstName(customer.getFirstName());
//        dto.setLastName(customer.getLastName());
//        dto.setEmail(customer.getEmail());
//        dto.setPhone(customer.getPhone());
//        dto.setCity(customer.getCity());
//        dto.setState(customer.getState());
//        dto.setStatus(customer.getStatus().name().toLowerCase());
//        dto.setKycStatus(customer.getKycStatus().name().toLowerCase());
//        dto.setImage(customer.getImage());
//        dto.setCreatedDate(customer.getCreatedDate());
//
//        return dto;
//    }
//}
//
