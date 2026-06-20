/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.node.management.scheduler.log;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

/**
 * A listener for {@link Tailer} events.
 * It forwards new log lines to the {@link SchedulerLogExporter} to be broadcast.
 */
public class SchedulerLogTailerListener implements TailerListener {

    private final SchedulerLogExporter logExporter;

    /**
     * Constructs a new SchedulerLogTailerListener.
     * @param logExporter the log exporter to broadcast log lines to
     */
    public SchedulerLogTailerListener(SchedulerLogExporter logExporter) {
        this.logExporter = logExporter;
    }

    /**
     * {@inheritDoc}
     * @param tailer the tailer instance
     */
    @Override
    public void init(Tailer tailer) {
    }

    @Override
    public void fileNotFound() {
    }

    @Override
    public void fileRotated() {
    }

    /**
     * {@inheritDoc}
     * @param line the line of log text
     */
    @Override
    public void handle(String line) {
        logExporter.broadcast(line);
    }

    /**
     * {@inheritDoc}
     * @param e the exception that occurred
     */
    @Override
    public void handle(Exception e) {
    }

}
