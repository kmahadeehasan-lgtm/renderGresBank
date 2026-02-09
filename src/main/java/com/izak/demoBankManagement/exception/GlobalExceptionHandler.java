package com.izak.demoBankManagement.exception;

import com.izak.demoBankManagement.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================
    // ACCOUNT EXCEPTIONS
    // ============================================

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountInactive(AccountInactiveException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================
    // TRANSACTION EXCEPTIONS
    // ============================================

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFound(TransactionNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransaction(InvalidTransactionException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================
    // CUSTOMER EXCEPTIONS
    // ============================================

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomerNotFound(CustomerNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================
    // CARD EXCEPTIONS
    // ============================================

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCardNotFound(CardNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCardOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCardOperation(InvalidCardOperationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CardExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleCardExpired(CardExpiredException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================
    // LOAN EXCEPTIONS - NEW
    // ============================================

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoanNotFound(LoanNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(LoanApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoanApplication(LoanApplicationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(LoanEligibilityException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleLoanEligibility(LoanEligibilityException ex) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("reasons", ex.getReasons());

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                false,
                "Loan eligibility check failed",
                errorDetails,
                java.time.LocalDateTime.now().toString()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(InvalidLoanStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidLoanState(InvalidLoanStateException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientCollateralException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientCollateral(InsufficientCollateralException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DisbursementFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisbursementFailed(DisbursementFailedException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedLoanAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedLoanAccess(UnauthorizedLoanAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(LoanAlreadyDisbursedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoanAlreadyDisbursed(LoanAlreadyDisbursedException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================
    // AUTHENTICATION/AUTHORIZATION EXCEPTIONS
    // ============================================

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================
    // VALIDATION EXCEPTIONS
    // ============================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                false,
                "Validation failed",
                errors,
                java.time.LocalDateTime.now().toString()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // ============================================
    // GENERAL EXCEPTION
    // ============================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}



//package com.izak.demoBankManagement.exception;
//
//import com.izak.demoBankManagement.dto.ApiResponse;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(AccountNotFoundException.class)
//    public ResponseEntity<ApiResponse<Void>> handleAccountNotFound(AccountNotFoundException ex) {
//        return ResponseEntity
//                .status(HttpStatus.NOT_FOUND)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(AccountInactiveException.class)
//    public ResponseEntity<ApiResponse<Void>> handleAccountInactive(AccountInactiveException ex) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(InsufficientBalanceException.class)
//    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(TransactionNotFoundException.class)
//    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFound(TransactionNotFoundException ex) {
//        return ResponseEntity
//                .status(HttpStatus.NOT_FOUND)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(InvalidTransactionException.class)
//    public ResponseEntity<ApiResponse<Void>> handleInvalidTransaction(InvalidTransactionException ex) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(CustomerNotFoundException.class)
//    public ResponseEntity<ApiResponse<Void>> handleCustomerNotFound(CustomerNotFoundException ex) {
//        return ResponseEntity
//                .status(HttpStatus.NOT_FOUND)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(DuplicateResourceException.class)
//    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
//        return ResponseEntity
//                .status(HttpStatus.CONFLICT)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//
//
//    @ExceptionHandler(CardNotFoundException.class)
//    public ResponseEntity<ApiResponse<Void>> handleCardNotFound(CardNotFoundException ex) {
//        return ResponseEntity
//                .status(HttpStatus.NOT_FOUND)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(InvalidCardOperationException.class)
//    public ResponseEntity<ApiResponse<Void>> handleInvalidCardOperation(InvalidCardOperationException ex) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(CardExpiredException.class)
//    public ResponseEntity<ApiResponse<Void>> handleCardExpired(CardExpiredException ex) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach((error) -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });
//
//        ApiResponse<Map<String, String>> response = new ApiResponse<>(
//                false,
//                "Validation failed",
//                errors,
//                java.time.LocalDateTime.now().toString()
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(response);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
//    }
//}
//
//
//
////package com.izak.demoBankManagement.exception;
////
////import com.izak.demoBankManagement.dto.ApiResponse;
////import org.springframework.http.HttpStatus;
////import org.springframework.http.ResponseEntity;
////import org.springframework.validation.FieldError;
////import org.springframework.web.bind.MethodArgumentNotValidException;
////import org.springframework.web.bind.annotation.ExceptionHandler;
////import org.springframework.web.bind.annotation.RestControllerAdvice;
////
////import java.util.HashMap;
////import java.util.Map;
////
////@RestControllerAdvice
////public class GlobalExceptionHandler {
////    @ExceptionHandler(AccountNotFoundException.class)
////    public ResponseEntity<ApiResponse<Void>> handleAccountNotFound(AccountNotFoundException ex) {
////        return ResponseEntity
////                .status(HttpStatus.NOT_FOUND)
////                .body(ApiResponse.error(ex.getMessage()));
////    }
////
////    @ExceptionHandler(AccountInactiveException.class)
////    public ResponseEntity<ApiResponse<Void>> handleAccountInactive(AccountInactiveException ex) {
////        return ResponseEntity
////                .status(HttpStatus.BAD_REQUEST)
////                .body(ApiResponse.error(ex.getMessage()));
////    }
////
////    @ExceptionHandler(InsufficientBalanceException.class)
////    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex) {
////        return ResponseEntity
////                .status(HttpStatus.BAD_REQUEST)
////                .body(ApiResponse.error(ex.getMessage()));
////    }
////
////    @ExceptionHandler(TransactionNotFoundException.class)
////    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFound(TransactionNotFoundException ex) {
////        return ResponseEntity
////                .status(HttpStatus.NOT_FOUND)
////                .body(ApiResponse.error(ex.getMessage()));
////    }
////
////    @ExceptionHandler(InvalidTransactionException.class)
////    public ResponseEntity<ApiResponse<Void>> handleInvalidTransaction(InvalidTransactionException ex) {
////        return ResponseEntity
////                .status(HttpStatus.BAD_REQUEST)
////                .body(ApiResponse.error(ex.getMessage()));
////    }
////
////    @ExceptionHandler(CustomerNotFoundException.class)
////    public ResponseEntity<ApiResponse<Void>> handleCustomerNotFound(CustomerNotFoundException ex) {
////        return ResponseEntity
////                .status(HttpStatus.NOT_FOUND)
////                .body(ApiResponse.error(ex.getMessage()));
////    }
////
////    @ExceptionHandler(DuplicateResourceException.class)
////    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
////        return ResponseEntity
////                .status(HttpStatus.CONFLICT)
////                .body(ApiResponse.error(ex.getMessage()));
////    }
////
////    @ExceptionHandler(MethodArgumentNotValidException.class)
////    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
////        Map<String, String> errors = new HashMap<>();
////        ex.getBindingResult().getAllErrors().forEach((error) -> {
////            String fieldName = ((FieldError) error).getField();
////            String errorMessage = error.getDefaultMessage();
////            errors.put(fieldName, errorMessage);
////        });
////
////        ApiResponse<Map<String, String>> response = new ApiResponse<>(
////                false,
////                "Validation failed",
////                errors,
////                java.time.LocalDateTime.now().toString()
////        );
////
////        return ResponseEntity
////                .status(HttpStatus.BAD_REQUEST)
////                .body(response);
////    }
////
////    @ExceptionHandler(Exception.class)
////    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
////        return ResponseEntity
////                .status(HttpStatus.INTERNAL_SERVER_ERROR)
////                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
////    }
////}
