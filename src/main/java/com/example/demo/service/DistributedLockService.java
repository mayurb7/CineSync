package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed Lock Service using Redis (Redisson)
 * Provides distributed locking capabilities for a multi-instance deployment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;

    @Value("${distributed.lock.wait-time:10}")
    private long lockWaitTime;

    @Value("${distributed.lock.lease-time:30}")
    private long lockLeaseTime;

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String BOOKING_LOCK_PREFIX = "booking:lock:";
    private static final String SHOW_LOCK_PREFIX = "show:lock:";

    /**
     * Acquires a distributed lock for a single seat
     */
    public RLock acquireSeatLock(Long seatId) {
        String lockKey = SEAT_LOCK_PREFIX + seatId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire lock for seat: {}", seatId);
                throw new LockAcquisitionException("Unable to acquire lock for seat: " + seatId);
            }
            log.debug("Acquired lock for seat: {}", seatId);
            return lock;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Interrupted while acquiring lock for seat: " + seatId, e);
        }
    }

    /**
     * Acquires distributed locks for multiple seats using a MultiLock
     * This ensures atomic locking of all seats or none
     */
    public RLock acquireMultiSeatLock(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Seat IDs cannot be null or empty");
        }

        // Sort seat IDs to prevent deadlocks (always acquire locks in the same order)
        List<Long> sortedSeatIds = seatIds.stream().sorted().toList();
        
        RLock[] locks = sortedSeatIds.stream()
            .map(id -> redissonClient.getLock(SEAT_LOCK_PREFIX + id))
            .toArray(RLock[]::new);

        RLock multiLock = redissonClient.getMultiLock(locks);
        
        try {
            boolean acquired = multiLock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire multi-lock for seats: {}", sortedSeatIds);
                throw new LockAcquisitionException("Unable to acquire locks for seats: " + sortedSeatIds);
            }
            log.debug("Acquired multi-lock for seats: {}", sortedSeatIds);
            return multiLock;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Interrupted while acquiring locks for seats: " + sortedSeatIds, e);
        }
    }

    /**
     * Acquires a distributed lock for a show (useful for show-level operations)
     */
    public RLock acquireShowLock(Long showId) {
        String lockKey = SHOW_LOCK_PREFIX + showId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire lock for show: {}", showId);
                throw new LockAcquisitionException("Unable to acquire lock for show: " + showId);
            }
            log.debug("Acquired lock for show: {}", showId);
            return lock;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Interrupted while acquiring lock for show: " + showId, e);
        }
    }

    /**
     * Releases a distributed lock safely
     */
    public void releaseLock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                log.debug("Released lock: {}", lock.getName());
            } catch (IllegalMonitorStateException e) {
                log.warn("Lock already released or expired: {}", lock.getName());
            }
        }
    }

    /**
     * Executes a task with a distributed lock on multiple seats
     * Automatically handles lock acquisition and release
     */
    public <T> T executeWithSeatLock(List<Long> seatIds, Supplier<T> task) {
        RLock lock = acquireMultiSeatLock(seatIds);
        try {
            return task.get();
        } finally {
            releaseLock(lock);
        }
    }

    /**
     * Executes a task with a distributed lock on a show
     * Automatically handles lock acquisition and release
     */
    public <T> T executeWithShowLock(Long showId, Supplier<T> task) {
        RLock lock = acquireShowLock(showId);
        try {
            return task.get();
        } finally {
            releaseLock(lock);
        }
    }

    /**
     * Custom exception for lock acquisition failures
     */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

