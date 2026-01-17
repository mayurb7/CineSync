package com.example.demo.service;

import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.BookingResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.SeatAlreadyBookedException;
import com.example.demo.model.Booking;
import com.example.demo.model.Seat;
import com.example.demo.model.Show;
import com.example.demo.model.User;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.repository.ShowRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BookingService with distributed locking support using Redis
 * Suitable for multi-instance deployments in a distributed system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final DistributedLockService distributedLockService;

    /**
     * Creates a booking with distributed locking to handle concurrent requests
     * across multiple application instances
     */
    @Transactional
    @CacheEvict(value = "availableSeats", key = "#request.showId")
    public BookingResponse createBooking(BookingRequest request) {
        log.info("Creating booking for user: {} with seats: {}", request.getUserId(), request.getSeatIds());
        
        // Use distributed lock to prevent race conditions across multiple instances
        return distributedLockService.executeWithSeatLock(request.getSeatIds(), () -> {
            return attemptBooking(request);
        });
    }

    /**
     * Attempts to create a booking after acquiring distributed lock
     * This method is called within the lock context
     */
    private BookingResponse attemptBooking(BookingRequest request) {
        // Fetch user
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // Fetch show
        Show show = showRepository.findById(request.getShowId())
            .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + request.getShowId()));

        // Validate show time
        if (show.getShowTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book seats for a past show");
        }

        // Fetch seats (no need for database-level lock as we have distributed lock)
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("One or more seats not found");
        }

        // Validate all seats belong to the same show
        for (Seat seat : seats) {
            if (!seat.getShow().getId().equals(request.getShowId())) {
                throw new IllegalArgumentException("All seats must belong to the same show");
            }
        }

        // Check if seats are available
        for (Seat seat : seats) {
            if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
                throw new SeatAlreadyBookedException(
                    "Seat " + seat.getSeatNumber() + " is already booked or reserved"
                );
            }
        }

        // Calculate total amount
        Double totalAmount = seats.size() * show.getMovie().getTicketPrice();

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeats(seats);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setBookingTime(LocalDateTime.now());

        // Update seat statuses
        for (Seat seat : seats) {
            seat.setStatus(Seat.SeatStatus.BOOKED);
        }

        // Save booking and seats
        booking = bookingRepository.save(booking);
        seatRepository.saveAll(seats);

        log.info("Booking created successfully with id: {}", booking.getId());
        
        // Build response
        return buildBookingResponse(booking);
    }

    /**
     * Gets a booking by ID with caching
     */
    @Cacheable(value = "bookings", key = "#bookingId")
    public BookingResponse getBooking(Long bookingId) {
        log.debug("Fetching booking with id: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        return buildBookingResponse(booking);
    }

    /**
     * Gets all bookings for a user
     */
    public List<BookingResponse> getUserBookings(Long userId) {
        log.debug("Fetching bookings for user: {}", userId);
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream()
            .map(this::buildBookingResponse)
            .collect(Collectors.toList());
    }

    /**
     * Cancels a booking with distributed locking
     * Uses show-level lock to prevent conflicts during cancellation
     */
    @Transactional
    @CacheEvict(value = {"bookings", "availableSeats"}, allEntries = true)
    public void cancelBooking(Long bookingId) {
        log.info("Cancelling booking with id: {}", bookingId);
        
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        // Get seat IDs for distributed locking
        List<Long> seatIds = booking.getSeats().stream()
            .map(Seat::getId)
            .collect(Collectors.toList());

        // Use distributed lock to release seats safely
        distributedLockService.executeWithSeatLock(seatIds, () -> {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            
            // Release seats
            for (Seat seat : booking.getSeats()) {
                seat.setStatus(Seat.SeatStatus.AVAILABLE);
            }
            
            seatRepository.saveAll(booking.getSeats());
            bookingRepository.save(booking);
            
            log.info("Booking cancelled successfully: {}", bookingId);
            return null;
        });
    }

    /**
     * Reserves seats temporarily (useful for payment flow)
     * Seats are reserved for a limited time before confirmation
     */
    @Transactional
    @CacheEvict(value = "availableSeats", key = "#showId")
    public BookingResponse reserveSeats(Long userId, Long showId, List<Long> seatIds) {
        log.info("Reserving seats for user: {} show: {} seats: {}", userId, showId, seatIds);
        
        return distributedLockService.executeWithSeatLock(seatIds, () -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + showId));

            List<Seat> seats = seatRepository.findAllById(seatIds);

            if (seats.size() != seatIds.size()) {
                throw new ResourceNotFoundException("One or more seats not found");
            }

            // Validate and reserve seats
            for (Seat seat : seats) {
                if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
                    throw new SeatAlreadyBookedException(
                        "Seat " + seat.getSeatNumber() + " is already booked or reserved"
                    );
                }
                seat.setStatus(Seat.SeatStatus.RESERVED);
            }

            // Calculate total amount
            Double totalAmount = seats.size() * show.getMovie().getTicketPrice();

            // Create pending booking
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setShow(show);
            booking.setSeats(seats);
            booking.setTotalAmount(totalAmount);
            booking.setStatus(Booking.BookingStatus.CONFIRMED); // Could add PENDING status
            booking.setBookingTime(LocalDateTime.now());

            seatRepository.saveAll(seats);
            booking = bookingRepository.save(booking);

            log.info("Seats reserved successfully with booking id: {}", booking.getId());
            return buildBookingResponse(booking);
        });
    }

    private BookingResponse buildBookingResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setBookingId(booking.getId());
        response.setUserName(booking.getUser().getName());
        response.setMovieTitle(booking.getShow().getMovie().getTitle());
        response.setShowTime(booking.getShow().getShowTime());
        response.setSeatNumbers(booking.getSeats().stream()
            .map(Seat::getSeatNumber)
            .collect(Collectors.toList()));
        response.setTotalAmount(booking.getTotalAmount());
        response.setStatus(booking.getStatus().name());
        response.setBookingTime(booking.getBookingTime());
        return response;
    }
}
