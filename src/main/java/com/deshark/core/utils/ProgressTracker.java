package com.deshark.core.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProgressTracker {
    private final int totalFiles;
    private final AtomicInteger completedFiles = new AtomicInteger(0);
    private final ConcurrentMap<String, Long> activeUploads = new ConcurrentHashMap<>();
    private final UploadProgressListener listener;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> progressFuture;

    public ProgressTracker(int totalFiles, UploadProgressListener listener) {
        this.totalFiles = totalFiles;
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startUpload(String fileName) {
        activeUploads.put(fileName, System.currentTimeMillis());
    }

    public void completeUpload(String fileName) {
        activeUploads.remove(fileName);
        int completed = completedFiles.incrementAndGet();
        if (listener != null && completed == totalFiles) {
            listener.onComplete();
        }
    }

    public void startReporting() {
        if (listener == null) return;

        progressFuture = scheduler.scheduleAtFixedRate(() -> {
            int completed = completedFiles.get();
            String currentFiles = activeUploads.keySet().stream()
                    .limit(1)
                    .collect(Collectors.joining(", "));

            if (activeUploads.size() > 3) {
                currentFiles += ", ...";
            }

            listener.onProgress(completed, totalFiles, currentFiles);
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopReporting() {
        if (progressFuture != null) {
            progressFuture.cancel(false);
        }
        scheduler.shutdown();
    }
}
