package com.example.mpct.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TaskLockService {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "task:lock:";
    private static final Duration LOCK_EXPIRATION = Duration.ofMinutes(30);

    public TaskLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Intenta bloquear una tarea en Redis.
     * @return true si se bloqueó exitosamente o si ya estaba bloqueada por el mismo usuario.
     */
    public boolean lockTask(String taskId, String username) {
        String key = LOCK_PREFIX + taskId;
        
        try {
            // Use setIfAbsent to prevent race conditions
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, username, LOCK_EXPIRATION);
            
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            } else {
                // Check if already locked by the same user
                String owner = redisTemplate.opsForValue().get(key);
                if (username.equals(owner)) {
                    // Refresh the expiration time
                    redisTemplate.expire(key, LOCK_EXPIRATION);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("REDIS ERROR [lockTask]: " + e.getMessage());
            return false;
        }
    }

    public void unlockTask(String taskId, String username) {
        String key = LOCK_PREFIX + taskId;
        try {
            String owner = redisTemplate.opsForValue().get(key);
            if (username.equals(owner)) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            System.err.println("REDIS ERROR [unlockTask]: " + e.getMessage());
        }
    }
    
    public void forceUnlock(String taskId) {
        String key = LOCK_PREFIX + taskId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("REDIS ERROR [forceUnlock]: " + e.getMessage());
        }
    }

    public Optional<String> getLockOwner(String taskId) {
        String key = LOCK_PREFIX + taskId;
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (Exception e) {
            System.err.println("REDIS ERROR [getLockOwner]: " + e.getMessage());
            return Optional.empty();
        }
    }
}
