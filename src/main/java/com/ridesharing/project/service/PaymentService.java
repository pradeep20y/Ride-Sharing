package com.ridesharing.project.service;

import com.ridesharing.project.entity.Payment;
import com.ridesharing.project.entity.Ride;
import com.ridesharing.project.exception.BusinessException;
import com.ridesharing.project.exception.ResourceNotFoundException;
import com.ridesharing.project.repository.PaymentRepository;
import com.ridesharing.project.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// This service manages payment records for rides, handling creation and transitions between Pending, Success, and Failed states.
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RideRepository rideRepository;

    // Constructor injection ensures both repositories are always available and promotes testability.
    public PaymentService(PaymentRepository paymentRepository, RideRepository rideRepository) {
        this.paymentRepository = paymentRepository;
        this.rideRepository = rideRepository;
    }

    // Creates a new payment record for a completed ride, using the ride's fare as the payment amount.
    @Transactional
    public Payment createPayment(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));

        // Prevent duplicate payment records for the same ride
        if (paymentRepository.existsByRide(ride)) {
            throw new BusinessException("A payment record already exists for ride ID: " + rideId);
        }

        if (!"Completed".equals(ride.getStatus())) {
            throw new BusinessException("Payment can only be created for completed rides. Current ride status: " + ride.getStatus());
        }

        Payment payment = new Payment();
        payment.setRide(ride);
        // Use the ride's final fare as the authoritative payment amount
        payment.setAmount(ride.getFare());
        payment.setStatus("Pending");

        return paymentRepository.save(payment);
    }

    // Marks a pending payment as successfully processed after the payment gateway confirms the charge.
    @Transactional
    public Payment markAsSuccess(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        if (!"Pending".equals(payment.getStatus())) {
            throw new BusinessException("Only payments in 'Pending' status can be marked as success. Current status: " + payment.getStatus());
        }

        payment.setStatus("Success");
        return paymentRepository.save(payment);
    }

    // Marks a pending payment as failed when the payment gateway reports an error or the charge is declined.
    @Transactional
    public Payment markAsFailed(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        if (!"Pending".equals(payment.getStatus())) {
            throw new BusinessException("Only payments in 'Pending' status can be marked as failed. Current status: " + payment.getStatus());
        }

        payment.setStatus("Failed");
        return paymentRepository.save(payment);
    }

    // Retrieves the payment record associated with a specific ride by the ride's ID.
    public Payment getPaymentByRideId(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));

        return paymentRepository.findByRide(ride)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "rideId", rideId));
    }
}
