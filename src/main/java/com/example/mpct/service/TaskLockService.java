package com.example.mpct.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskLockService {

    // Simula Redis usando memoria (para evitar que explote si no hay Redis instalado).
    // Mapa: ID de Tarea -> Info del candado
    private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();

    public record LockInfo(String lockedBy, LocalDateTime lockedAt) {}

    /**
     * Intenta bloquear una tarea.
     * @return true si se bloqueó exitosamente o si ya estaba bloqueada por el mismo usuario.
     */
    public boolean lockTask(String taskId, String username) {
        cleanExpiredLocks();
        
        LockInfo currentLock = locks.get(taskId);
        if (currentLock == null) {
            locks.put(taskId, new LockInfo(username, LocalDateTime.now()));
            return true;
        } else if (currentLock.lockedBy().equals(username)) {
            // Refrescar el candado
            locks.put(taskId, new LockInfo(username, LocalDateTime.now()));
            return true;
        }
        return false;
    }

    public void unlockTask(String taskId, String username) {
        LockInfo currentLock = locks.get(taskId);
        if (currentLock != null && currentLock.lockedBy().equals(username)) {
            locks.remove(taskId);
        }
    }
    
    public void forceUnlock(String taskId) {
        locks.remove(taskId);
    }

    public Optional<String> getLockOwner(String taskId) {
        cleanExpiredLocks();
        LockInfo info = locks.get(taskId);
        return info != null ? Optional.of(info.lockedBy()) : Optional.empty();
    }

    private void cleanExpiredLocks() {
        // Expirar candados después de 15 minutos
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        locks.entrySet().removeIf(entry -> entry.getValue().lockedAt().isBefore(threshold));
    }
}
