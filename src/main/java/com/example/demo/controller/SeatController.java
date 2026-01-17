package com.example.demo.controller;

import com.example.demo.dto.SeatResponse;
import com.example.demo.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/show/{showId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByShow(@PathVariable Long showId) {
        List<SeatResponse> seats = seatService.getSeatsByShow(showId);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/show/{showId}/available")
    public ResponseEntity<List<SeatResponse>> getAvailableSeatsByShow(@PathVariable Long showId) {
        List<SeatResponse> seats = seatService.getAvailableSeatsByShow(showId);
        return ResponseEntity.ok(seats);
    }
}

