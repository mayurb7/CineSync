package com.example.demo.controller;

import com.example.demo.dto.ShowResponse;
import com.example.demo.model.Show;
import com.example.demo.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping("/movie/{movieId}")
    public ResponseEntity<ShowResponse> createShow(@PathVariable Long movieId, @RequestBody Show show) {
        ShowResponse response = showService.createShow(movieId, show);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(@PathVariable Long movieId) {
        List<ShowResponse> shows = showService.getShowsByMovie(movieId);
        return ResponseEntity.ok(shows);
    }

    @GetMapping("/movie/{movieId}/upcoming")
    public ResponseEntity<List<ShowResponse>> getUpcomingShowsByMovie(@PathVariable Long movieId) {
        List<ShowResponse> shows = showService.getUpcomingShowsByMovie(movieId);
        return ResponseEntity.ok(shows);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable Long id) {
        ShowResponse show = showService.getShowById(id);
        return ResponseEntity.ok(show);
    }
}

