package com.ridesharing.project.controller;

import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.PaymentResponse;
import com.ridesharing.project.entity.Payment;
import com.ridesharing.project.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// This controller exposes REST endpoints for creating payment records and updating their processing status.
@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Creates a new payment record for the completed ride identified by the given ride ID.
    @PostMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@PathVariable String rideId) {
        Payment payment = paymentService.createPayment(rideId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToResponse(payment), "Payment created successfully"));
    }

    // Marks the payment with the given ID as successfully processed.
    @PutMapping("/{id}/success")
    public ResponseEntity<ApiResponse<PaymentResponse>> markAsSuccess(@PathVariable String id) {
        Payment payment = paymentService.markAsSuccess(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(payment), "Payment marked as successful"));
    }

    // Marks the payment with the given ID as failed.
    @PutMapping("/{id}/failed")
    public ResponseEntity<ApiResponse<PaymentResponse>> markAsFailed(@PathVariable String id) {
        Payment payment = paymentService.markAsFailed(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(payment), "Payment marked as failed"));
    }

    // Returns the payment record associated with the ride identified by the given ride ID.
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByRideId(@PathVariable String rideId) {
        Payment payment = paymentService.getPaymentByRideId(rideId);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(payment), "Payment retrieved successfully"));
    }

    // Converts a Payment entity to a PaymentResponse DTO for inclusion in the API response.
    private PaymentResponse convertToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .rideId(payment.getRide().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdDate(payment.getCreatedDate())
                .build();
    }
}
