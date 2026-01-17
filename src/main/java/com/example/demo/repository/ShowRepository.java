package com.example.demo.repository;

import com.example.demo.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieId(Long movieId);
    
    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId AND s.showTime >= :startTime ORDER BY s.showTime")
    List<Show> findUpcomingShowsByMovie(@Param("movieId") Long movieId, @Param("startTime") LocalDateTime startTime);
}

