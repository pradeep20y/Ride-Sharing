package com.ridesharing.project.controller;

import com.ridesharing.project.dto.response.ApiResponse;
import com.ridesharing.project.dto.response.ErrorResponse;
import com.ridesharing.project.dto.response.PaymentResponse;
import com.ridesharing.project.entity.Payment;
import com.ridesharing.project.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// This controller exposes REST endpoints for creating payment records and updating their processing status.
@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "*")
@Tag(name = "Payment", description = "APIs for managing ride payments — creation and status transitions")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Creates a new payment record for the completed ride identified by the given ride ID.
    @Operation(
        summary = "Create a payment for a ride",
        description = "Creates a new payment record for the completed ride identified by the given ride ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Payment created successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Ride is not completed or a payment already exists for this ride",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Ride not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Parameter(description = "ID of the ride for which the payment is being created", required = true)
            @PathVariable String rideId) {
        Payment payment = paymentService.createPayment(rideId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(convertToResponse(payment), "Payment created successfully"));
    }

    // Marks the payment with the given ID as successfully processed.
    @Operation(
        summary = "Mark a payment as successful",
        description = "Marks the payment with the given ID as successfully processed. The payment must be in Pending status."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Payment marked as successful",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Payment is not in Pending status",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}/success")
    public ResponseEntity<ApiResponse<PaymentResponse>> markAsSuccess(
            @Parameter(description = "ID of the payment to mark as successful", required = true)
            @PathVariable String id) {
        Payment payment = paymentService.markAsSuccess(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(payment), "Payment marked as successful"));
    }

    // Marks the payment with the given ID as failed.
    @Operation(
        summary = "Mark a payment as failed",
        description = "Marks the payment with the given ID as failed. The payment must be in Pending status."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Payment marked as failed",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Payment is not in Pending status",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PutMapping("/{id}/failed")
    public ResponseEntity<ApiResponse<PaymentResponse>> markAsFailed(
            @Parameter(description = "ID of the payment to mark as failed", required = true)
            @PathVariable String id) {
        Payment payment = paymentService.markAsFailed(id);
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(payment), "Payment marked as failed"));
    }

    // Returns the payment record associated with the ride identified by the given ride ID.
    @Operation(
        summary = "Get payment by ride ID",
        description = "Returns the payment record associated with the ride identified by the given ride ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Payment retrieved successfully",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Payment or ride not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByRideId(
            @Parameter(description = "ID of the ride whose payment record is being retrieved", required = true)
            @PathVariable String rideId) {
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
