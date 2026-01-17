package com.example.demo.service;

import com.example.demo.dto.MovieResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Movie;
import com.example.demo.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MovieService with Redis caching for improved performance
 * in a distributed system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;

    @CacheEvict(value = "movies", allEntries = true)
    public MovieResponse createMovie(Movie movie) {
        log.info("Creating movie: {}", movie.getTitle());
        Movie savedMovie = movieRepository.save(movie);
        return convertToResponse(savedMovie);
    }

    @Cacheable(value = "movies", key = "'all'")
    public List<MovieResponse> getAllMovies() {
        log.info("Fetching all movies from database");
        return movieRepository.findAll().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "movies", key = "#id")
    public MovieResponse getMovieById(Long id) {
        log.debug("Fetching movie with id: {} from database", id);
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        return convertToResponse(movie);
    }

    @Cacheable(value = "movies", key = "'title:' + #title")
    public List<MovieResponse> searchMoviesByTitle(String title) {
        log.debug("Searching movies by title: {}", title);
        return movieRepository.findByTitleContainingIgnoreCase(title).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "movies", key = "'genre:' + #genre")
    public List<MovieResponse> getMoviesByGenre(String genre) {
        log.debug("Fetching movies by genre: {}", genre);
        return movieRepository.findByGenreIgnoreCase(genre).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Caching(evict = {
        @CacheEvict(value = "movies", key = "#id"),
        @CacheEvict(value = "movies", key = "'all'")
    })
    public MovieResponse updateMovie(Long id, Movie movieDetails) {
        log.info("Updating movie with id: {}", id);
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        
        movie.setTitle(movieDetails.getTitle());
        movie.setDescription(movieDetails.getDescription());
        movie.setDuration(movieDetails.getDuration());
        movie.setGenre(movieDetails.getGenre());
        movie.setLanguage(movieDetails.getLanguage());
        movie.setReleaseDate(movieDetails.getReleaseDate());
        movie.setTicketPrice(movieDetails.getTicketPrice());
        
        Movie updatedMovie = movieRepository.save(movie);
        return convertToResponse(updatedMovie);
    }

    @Caching(evict = {
        @CacheEvict(value = "movies", key = "#id"),
        @CacheEvict(value = "movies", key = "'all'")
    })
    public void deleteMovie(Long id) {
        log.info("Deleting movie with id: {}", id);
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        movieRepository.delete(movie);
    }

    private MovieResponse convertToResponse(Movie movie) {
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setDescription(movie.getDescription());
        response.setDuration(movie.getDuration());
        response.setGenre(movie.getGenre());
        response.setLanguage(movie.getLanguage());
        response.setReleaseDate(movie.getReleaseDate());
        response.setTicketPrice(movie.getTicketPrice());
        return response;
    }
}
