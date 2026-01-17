package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private LocalDateTime showTime;
    private String screenNumber;
    private Integer totalSeats;
    private Integer availableSeats;
}

