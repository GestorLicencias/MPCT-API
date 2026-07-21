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
    }

    public void unlockTask(String taskId, String username) {
        String key = LOCK_PREFIX + taskId;
        String owner = redisTemplate.opsForValue().get(key);
        if (username.equals(owner)) {
            redisTemplate.delete(key);
        }
    }
    
    public void forceUnlock(String taskId) {
        String key = LOCK_PREFIX + taskId;
        redisTemplate.delete(key);
    }

    public Optional<String> getLockOwner(String taskId) {
        String key = LOCK_PREFIX + taskId;
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }
}
