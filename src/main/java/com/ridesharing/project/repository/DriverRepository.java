package com.ridesharing.project.repository;

import com.ridesharing.project.entity.Driver;
import com.ridesharing.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, String> {

    // Find driver by user
    Optional<Driver> findByUser(User user);

    // Find driver by license plate
    Optional<Driver> findByLicensePlate(String licensePlate);

    // Find all drivers with specific status
    List<Driver> findByStatus(String status);

    // Find all drivers with specific vehicle type
    List<Driver> findByVehicleType(String vehicleType);

    // Find all online drivers
    List<Driver> findByStatusOrderByRatingDesc(String status);

    // Check if license plate exists
    boolean existsByLicensePlate(String licensePlate);

    @Query("SELECT d FROM Driver d WHERE d.user.phone = :phone")
    Optional<Driver> findByUserPhone(@Param("phone") String phone);

    Optional<Driver> findByUser_Id(String userId);
}