package com.example.demo.service;

import com.example.demo.dto.SeatResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Seat;
import com.example.demo.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SeatService with Redis caching for improved performance
 * in a distributed system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatRepository seatRepository;

    public List<SeatResponse> getSeatsByShow(Long showId) {
        log.debug("Fetching seats for show id: {}", showId);
        List<Seat> seats = seatRepository.findByShowId(showId);
        if (seats.isEmpty()) {
            throw new ResourceNotFoundException("No seats found for show id: " + showId);
        }
        return seats.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "availableSeats", key = "#showId")
    public List<SeatResponse> getAvailableSeatsByShow(Long showId) {
        log.debug("Fetching available seats for show id: {}", showId);
        List<Seat> seats = seatRepository.findByShowIdAndStatus(showId, Seat.SeatStatus.AVAILABLE);
        return seats.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    private SeatResponse convertToResponse(Seat seat) {
        SeatResponse response = new SeatResponse();
        response.setId(seat.getId());
        response.setSeatNumber(seat.getSeatNumber());
        response.setStatus(seat.getStatus().name());
        return response;
    }
}
