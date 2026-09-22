/*
 * Copyright (c) 2020-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.appmon.engine.exporter.log;

import com.aspectran.utils.scheduling.Scheduler;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A listener for {@link Tailer} events.
 * It batches new log lines and forwards them to the {@link LogExporter} to be broadcast in-order.
 *
 * <p>Created: 2020. 12. 24.</p>
 */
public class LogTailerListener implements TailerListener {

    private static final int DEFAULT_MAX_BATCH_SIZE = 100;

    private static final long DEFAULT_FLUSH_DELAY_MILLIS = 30L;

    private final LogExporter logExporter;

    private final Scheduler scheduler;

    private final int maxBatchSize;

    private final long flushDelayMillis;

    private final List<String> buffer = new ArrayList<>();

    private Scheduler.Task flushTask;

    /**
     * Instantiates a new LogTailerListener.
     * @param logExporter the exporter to which new log lines will be sent
     */
    public LogTailerListener(LogExporter logExporter) {
        this(logExporter, null, DEFAULT_MAX_BATCH_SIZE, DEFAULT_FLUSH_DELAY_MILLIS);
    }

    /**
     * Instantiates a new LogTailerListener.
     * @param logExporter the exporter to which new log lines will be sent
     * @param scheduler the scheduler for flushing buffered log lines
     */
    public LogTailerListener(LogExporter logExporter, Scheduler scheduler) {
        this(logExporter, scheduler, DEFAULT_MAX_BATCH_SIZE, DEFAULT_FLUSH_DELAY_MILLIS);
    }

    /**
     * Instantiates a new LogTailerListener.
     * @param logExporter the exporter to which new log lines will be sent
     * @param scheduler the scheduler for flushing buffered log lines
     * @param maxBatchSize the maximum number of lines to buffer before an immediate flush
     * @param flushDelayMillis the delay in milliseconds before flushing buffered lines
     */
    public LogTailerListener(LogExporter logExporter, Scheduler scheduler, int maxBatchSize, long flushDelayMillis) {
        this.logExporter = logExporter;
        this.scheduler = scheduler;
        this.maxBatchSize = (maxBatchSize > 0 ? maxBatchSize : DEFAULT_MAX_BATCH_SIZE);
        this.flushDelayMillis = (flushDelayMillis > 0 ? flushDelayMillis : DEFAULT_FLUSH_DELAY_MILLIS);
    }

    /**
     * This method is called if the tailed file is not found.
     */
    @Override
    public void init(Tailer tailer) {
        // Not used
    }

    /**
     * This method is called if the tailed file is not found.
     */
    @Override
    public void fileNotFound() {
        // Not used
    }

    /**
     * Called if a file rotation is detected.
     */
    @Override
    public void fileRotated() {
        // Not used
    }

    /**
     * Handles a new line from the tailed file.
     * @param line the new line
     */
    @Override
    public synchronized void handle(String line) {
        if (line != null) {
            buffer.add(line);
            if (buffer.size() >= maxBatchSize) {
                flush();
            } else if (flushTask == null) {
                if (scheduler != null && scheduler.isRunning()) {
                    flushTask = scheduler.schedule(this::scheduledFlush, flushDelayMillis, TimeUnit.MILLISECONDS);
                } else {
                    flush();
                }
            }
        }
    }

    /**
     * Handles an exception thrown by the Tailer.
     * @param e the exception
     */
    @Override
    public void handle(Exception e) {
        // Not used
    }

    private void scheduledFlush() {
        synchronized (this) {
            flushTask = null;
            flush();
        }
    }

    /**
     * Flushes all currently buffered log lines to the exporter.
     */
    public synchronized void flush() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        if (!buffer.isEmpty()) {
            List<String> batch = new ArrayList<>(buffer);
            buffer.clear();
            logExporter.broadcast(batch);
        }
    }

    /**
     * Closes the listener, flushing any remaining buffered log lines.
     */
    public synchronized void close() {
        flush();
    }

}
