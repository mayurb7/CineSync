package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String userName;
    private String movieTitle;
    private LocalDateTime showTime;
    private List<String> seatNumbers;
    private Double totalAmount;
    private String status;
    private LocalDateTime bookingTime;
}

