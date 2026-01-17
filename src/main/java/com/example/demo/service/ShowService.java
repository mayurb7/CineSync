package com.example.demo.service;

import com.example.demo.dto.ShowResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Movie;
import com.example.demo.model.Seat;
import com.example.demo.model.Show;
import com.example.demo.repository.MovieRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ShowService with Redis caching for improved performance
 * in a distributed system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;

    @CacheEvict(value = "shows", allEntries = true)
    public ShowResponse createShow(Long movieId, Show show) {
        log.info("Creating show for movie id: {}", movieId);
        Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));
        
        show.setMovie(movie);
        Show savedShow = showRepository.save(show);
        
        // Create seats for the show
        createSeatsForShow(savedShow);
        
        return convertToResponse(savedShow);
    }

    private void createSeatsForShow(Show show) {
        int totalSeats = show.getTotalSeats();
        int rows = (int) Math.ceil(Math.sqrt(totalSeats));
        int seatsPerRow = (int) Math.ceil((double) totalSeats / rows);
        
        int seatCounter = 1;
        for (int row = 0; row < rows && seatCounter <= totalSeats; row++) {
            char rowLetter = (char) ('A' + row);
            for (int col = 1; col <= seatsPerRow && seatCounter <= totalSeats; col++) {
                Seat seat = new Seat();
                seat.setShow(show);
                seat.setSeatNumber(rowLetter + String.valueOf(col));
                seat.setStatus(Seat.SeatStatus.AVAILABLE);
                seatRepository.save(seat);
                seatCounter++;
            }
        }
        log.info("Created {} seats for show id: {}", totalSeats, show.getId());
    }

    @Cacheable(value = "shows", key = "'movie:' + #movieId")
    public List<ShowResponse> getShowsByMovie(Long movieId) {
        log.debug("Fetching shows for movie id: {}", movieId);
        return showRepository.findByMovieId(movieId).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "shows", key = "'upcoming:' + #movieId")
    public List<ShowResponse> getUpcomingShowsByMovie(Long movieId) {
        log.debug("Fetching upcoming shows for movie id: {}", movieId);
        return showRepository.findUpcomingShowsByMovie(movieId, LocalDateTime.now()).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "shows", key = "#id")
    public ShowResponse getShowById(Long id) {
        log.debug("Fetching show with id: {}", id);
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + id));
        return convertToResponse(show);
    }

    @Caching(evict = {
        @CacheEvict(value = "shows", key = "#id"),
        @CacheEvict(value = "shows", allEntries = true)
    })
    public void deleteShow(Long id) {
        log.info("Deleting show with id: {}", id);
        Show show = showRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + id));
        showRepository.delete(show);
    }

    private ShowResponse convertToResponse(Show show) {
        ShowResponse response = new ShowResponse();
        response.setId(show.getId());
        response.setMovieId(show.getMovie().getId());
        response.setMovieTitle(show.getMovie().getTitle());
        response.setShowTime(show.getShowTime());
        response.setScreenNumber(show.getScreenNumber());
        response.setTotalSeats(show.getTotalSeats());
        
        // Calculate available seats
        List<Seat> availableSeats = seatRepository.findByShowIdAndStatus(
            show.getId(), 
            Seat.SeatStatus.AVAILABLE
        );
        response.setAvailableSeats(availableSeats.size());
        
        return response;
    }
}
