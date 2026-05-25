# RideSharing Backend

A production-ready ride-sharing backend built with **Spring Boot 3**, featuring real-time driver matching, WebSocket-based offer delivery, Redis-powered location tracking, and a full ride lifecycle from request to payment.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [WebSocket Guide](#websocket-guide)
- [Driver Matching Algorithm](#driver-matching-algorithm)
- [Offer Expiry Flow](#offer-expiry-flow)
- [Project Structure](#project-structure)

---

## Overview

This backend powers a ride-sharing platform where passengers request rides and drivers receive sequential, scored offers in real time. The system uses **Redis GEO** for proximity-based driver discovery, **STOMP over WebSocket** for instant offer delivery, and **Redis keyspace notifications** to automatically advance expired offers to the next eligible driver.

---

## Features

### Core
- **User Management** — Create and manage users with roles (DRIVER / PASSENGER)
- **Driver Profiles** — Registration, vehicle info, availability status, ratings, earnings tracking
- **Passenger Profiles** — Profile creation, wallet balance management

### Ride Matching
- **Sequential Offer Delivery** — Offers sent to one driver at a time, not broadcast to all
- **Score-Based Selection** — Drivers ranked by `60% proximity + 40% rating`
- **10-Second Offer Window** — Each driver has 10 seconds to accept or the offer moves on
- **Auto-Expiry via Redis TTL** — Redis keyspace notifications advance expired offers instantly
- **Max Attempt Guard** — After 5 failed attempts the request is cancelled and passenger is notified
- **Optimistic Locking** — `@Version` on `RideRequest` prevents race conditions when two drivers accept simultaneously

### Real-Time Notifications (WebSocket / STOMP)
- Driver receives ride offer with fare, pickup/dropoff, and distance
- Driver receives `OFFER_EXPIRED` notification when their window closes
- Passenger receives driver assignment with name, plate, vehicle type, and rating
- Passenger receives live GPS tracking during the trip
- Passenger receives cancellation notification if no driver is found

### Ride Lifecycle
- **Assigned → InProgress → Completed / Cancelled**
- Driver earnings and total rides auto-updated on completion
- Driver status automatically returns to `ONLINE` after trip ends

### Payment
- Payment record created for completed rides
- Transitions: `Pending → Success` or `Pending → Failed`
- Duplicate payment prevention

### Location Tracking
- Redis GEO index for real-time driver positions
- Nearby driver search within configurable radius (default 5 km)
- Live GPS coordinate push to passenger during active ride

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5 |
| Language | Java 17 |
| Database | MySQL 8 |
| Cache / Geo | Redis 7 |
| Real-Time | WebSocket + STOMP (SockJS) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Spring Boot Validation (Jakarta) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Utilities | Lombok |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Apps                          │
│              (Passenger App / Driver App)                   │
└────────────────────┬──────────────────┬────────────────────┘
                     │  REST            │  WebSocket (STOMP)
                     ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot API                          │
│                                                             │
│   Controllers  →  Services  →  Repositories                │
│                                                             │
│   RideMatchingService  (scoring + offer state machine)      │
│   RideNotificationService  (all WebSocket pushes)          │
│   LocationService  (Redis GEO operations)                   │
└──────────────┬───────────────────────────┬─────────────────┘
               │                           │
               ▼                           ▼
        ┌──────────┐               ┌──────────────┐
        │  MySQL   │               │    Redis     │
        │          │               │              │
        │ Users    │               │ Driver GEO   │
        │ Drivers  │               │ Offer TTLs   │
        │ Passengers│              │              │
        │ Rides    │               └──────────────┘
        │ Payments │
        └──────────┘
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/your-username/ridesharing-backend.git
cd ridesharing-backend/project
```

### 2. Set up MySQL

```sql
-- MySQL will auto-create the database on first run
-- Just make sure the MySQL server is running on port 3306
```

### 3. Enable Redis Keyspace Notifications

This is required for the 10-second offer expiry to work:

```bash
redis-cli CONFIG SET notify-keyspace-events KEA
```

### 4. Configure application properties

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ridesharing?createDatabaseIfNotExist=true
spring.datasource.username=your_username
spring.datasource.password=your_password

spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 5. Run the application

```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8082/api`

### 6. Open Swagger UI

```
http://localhost:8082/api/swagger-ui.html
```

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8082` | Server port |
| `server.servlet.context-path` | `/api` | Base path for all endpoints |
| `spring.datasource.url` | `localhost:3306/ridesharing` | MySQL connection |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema auto-update |

---

## API Reference

All endpoints are documented interactively at `http://localhost:8082/api/swagger-ui.html`.

### Users — `/api/users`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/users` | Create a new user |
| GET | `/users` | Get all users |
| GET | `/users/{id}` | Get user by ID |
| GET | `/users/email/{email}` | Get user by email |
| GET | `/users/type/passenger` | Get all passengers |
| GET | `/users/type/driver` | Get all drivers |
| PUT | `/users/{id}` | Update user |
| DELETE | `/users/{id}` | Delete user |

### Drivers — `/api/drivers`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/drivers` | Register a new driver |
| GET | `/drivers` | Get all drivers |
| GET | `/drivers/{id}` | Get driver by ID |
| GET | `/drivers/user/{userId}` | Get driver by user ID |
| GET | `/drivers/status/online` | Get online drivers (sorted by rating) |
| GET | `/drivers/status/{status}` | Get drivers by status |
| GET | `/drivers/vehicle/{type}` | Get drivers by vehicle type |
| PUT | `/drivers/{id}/status` | Update driver status |
| PUT | `/drivers/{id}/location` | Update driver location |
| PUT | `/drivers/{id}/vehicle` | Update vehicle info |
| PUT | `/drivers/{id}/rating` | Update driver rating |
| DELETE | `/drivers/{id}` | Delete driver |

### Passengers — `/api/passengers`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/passengers` | Create passenger profile |
| GET | `/passengers/{id}` | Get passenger by ID |
| GET | `/passengers/user/{userId}` | Get passenger by user ID |
| PUT | `/passengers/{id}` | Update passenger |
| GET | `/passengers/{id}/wallet` | Get wallet balance |

### Ride Requests — `/api/rides/request`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/rides/request` | Create ride request (triggers matching) |
| GET | `/rides/request/{id}` | Get request by ID |
| GET | `/rides/request/open` | Get all open requests |
| GET | `/rides/request/passenger/{id}` | Get requests by passenger |
| PUT | `/rides/request/{id}/cancel` | Cancel a ride request |

### Rides — `/api/rides`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/rides/{requestId}/assign/{driverId}` | Manually assign driver |
| PUT | `/rides/{id}/start` | Start ride |
| PUT | `/rides/{id}/complete` | Complete ride |
| PUT | `/rides/{id}/cancel` | Cancel ride |
| GET | `/rides/{id}` | Get ride by ID |
| GET | `/rides/driver/{driverId}` | Get rides by driver |
| GET | `/rides/passenger/{passengerId}` | Get rides by passenger |

### Payments — `/api/payments`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/payments/ride/{rideId}` | Create payment for completed ride |
| PUT | `/payments/{id}/success` | Mark payment as successful |
| PUT | `/payments/{id}/failed` | Mark payment as failed |
| GET | `/payments/ride/{rideId}` | Get payment by ride ID |

### Location — `/api/location`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/location/driver/{driverId}` | Push driver location to Redis GEO |
| DELETE | `/location/driver/{driverId}` | Remove driver from location tracking |
| GET | `/location/nearby?latitude=&longitude=` | Find nearby drivers (5km radius) |
| GET | `/location/nearby/radius?latitude=&longitude=&radius=` | Find nearby drivers (custom radius) |
| GET | `/location/driver/{driverId}` | Get last known driver location |
| GET | `/location/count` | Get online driver count |

---

## WebSocket Guide

### Connection

Connect via SockJS at:
```
ws://localhost:8082/api/ws/ridesharing/{serverId}/{sessionId}/websocket
```

Example:
```
ws://localhost:8082/api/ws/ridesharing/100/driver1session/websocket
```

### STOMP Handshake

After connecting, send:
```
["CONNECT\naccept-version:1.1,1.2\nheart-beat:0,0\n\n "]
```

### Subscribe Topics

| Topic | Who subscribes | What they receive |
|---|---|---|
| `/topic/driver/{driverId}` | Driver | Ride offers + OFFER_EXPIRED notifications |
| `/topic/passenger/{passengerId}` | Passenger | Driver assigned + ride cancelled notifications |
| `/topic/ride/{rideId}/tracking` | Passenger | Live GPS coordinates during trip |

### Send Destinations

| Destination | Payload | Description |
|---|---|---|
| `/app/ride/accept` | `{"requestId":"...","driverId":"..."}` | Accept a ride offer |
| `/app/ride/reject` | `{"requestId":"...","driverId":"..."}` | Reject a ride offer |
| `/app/ride/{rideId}/location` | `{"latitude":...,"longitude":...}` | Push live location during trip |

---

## Driver Matching Algorithm

When a passenger creates a ride request, the system finds and scores all drivers within **5 km** of the pickup point using Redis GEO:

```
score = (0.6 × distanceScore) + (0.4 × ratingScore)

where:
  distanceScore = 1 / (1 + distanceKm)   ← closer = higher score
  ratingScore   = rating / 5.0            ← higher rating = higher score
```

The highest-scored driver receives the offer first. Proximity is weighted more (60%) because passenger wait time is the primary concern.

---

## Offer Expiry Flow

```
Passenger creates ride request
         │
         ▼
System scores nearby drivers → sends offer to Driver 1
         │
         ▼ (10 seconds pass, no response)
Driver 1 receives OFFER_EXPIRED
         │
         ▼
System sends offer to Driver 2
         │
         ▼ (10 seconds pass)
Driver 2 receives OFFER_EXPIRED
         │
         ▼
System sends offer to Driver 3
         │
         ├── Driver 3 ACCEPTS
         │       │
         │       ▼
         │   Ride created → Passenger notified with driver details
         │
         └── Driver 3 does not respond (10 seconds)
                 │
                 ▼
             After 5 total attempts → Request CANCELLED
             Passenger notified
```

The expiry mechanism uses **Redis TTL keyspace notifications**. When the `offer:{requestId}` key expires, `OfferExpiryListener` fires and advances the state machine to the next driver. A MySQL fallback (`OfferExpirationJob`) exists for recovery after Redis restarts.

---

## Project Structure

```
src/main/java/com/ridesharing/project/
│
├── config/
│   ├── RedisConfig.java              # Redis connection and template config
│   ├── RedisListenerConfig.java      # Keyspace notification listener setup
│   └── WebSocketConfig.java          # STOMP broker and endpoint config
│
├── controller/
│   ├── UserController.java
│   ├── DriverController.java
│   ├── PassengerController.java
│   ├── RideRequestController.java
│   ├── RideController.java
│   ├── PaymentController.java
│   ├── LocationController.java
│   └── WebSocketController.java      # STOMP message handlers (accept/reject/location)
│
├── dto/
│   ├── request/                      # Incoming request payloads
│   └── response/                     # Outgoing response payloads
│
├── entity/
│   ├── User.java
│   ├── Driver.java
│   ├── Passenger.java
│   ├── Ride.java
│   ├── RideRequest.java              # @Version for optimistic locking
│   ├── RideRequestStatus.java        # OPEN, OFFER_PENDING, MATCHED, COMPLETED, CANCELLED
│   └── Payment.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── BusinessException.java
│
├── jobs/
│   └── OfferExpirationJob.java       # MySQL fallback sweep (disabled by default)
│
├── listener/
│   └── OfferExpiryListener.java      # Redis TTL expiry → advance to next driver
│
├── repository/
│   ├── UserRepository.java
│   ├── DriverRepository.java
│   ├── PassengerRepository.java
│   ├── RideRepository.java
│   ├── RideRequestRepository.java
│   └── PaymentRepository.java
│
├── service/
│   ├── UserService.java
│   ├── DriverService.java
│   ├── PassengerService.java
│   ├── RideRequestService.java       # Creates request, triggers matching
│   ├── RideService.java              # Ride lifecycle management
│   ├── RideMatchingService.java      # Core matching algorithm + offer state machine
│   ├── RideNotificationService.java  # All WebSocket push notifications
│   ├── PaymentService.java
│   └── LocationService.java          # Redis GEO operations
│
└── util/
    └── FareCalculator.java           # Haversine distance + fare + duration estimation
```

---

## Ride Request Status Flow

```
OPEN → OFFER_PENDING → MATCHED → COMPLETED
                    ↘
                     CANCELLED
```

| Status | Meaning |
|---|---|
| `OPEN` | Request created, matching not yet started |
| `OFFER_PENDING` | Offer sent to a driver, waiting for response |
| `MATCHED` | Driver accepted, ride created |
| `COMPLETED` | Trip finished |
| `CANCELLED` | All attempts exhausted or passenger cancelled |
