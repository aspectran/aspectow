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

import com.aspectran.aspectow.node.management.scheduler.SchedulerResponseParameters;
import com.aspectran.aspectow.node.management.scheduler.bridge.SchedulerBroker;
import com.aspectran.logging.LoggingDefaults;
import com.aspectran.utils.lifecycle.AbstractLifeCycle;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SchedulerLogExporter tails a specific scheduler log file and broadcasts
 * new lines via the SchedulerBroker.
 */
public class SchedulerLogExporter extends AbstractLifeCycle {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerLogExporter.class);

    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    private final String nodeId;

    private final String loggingGroup;

    private final File logFile;

    private final SchedulerBroker broker;

    private final Charset charset;

    private final long sampleInterval;

    private final int lastLines;

    private Tailer tailer;

    /**
     * Constructs a new SchedulerLogExporter with default charset, sample interval, and last lines.
     * @param nodeId the ID of the node
     * @param loggingGroup the logging group
     * @param logFile the log file to export
     * @param broker the broker to broadcast logs to
     */
    public SchedulerLogExporter(String nodeId, String loggingGroup, File logFile, SchedulerBroker broker) {
        this(nodeId, loggingGroup, logFile, broker, DEFAULT_CHARSET.name());
    }

    /**
     * Constructs a new SchedulerLogExporter with specified charset.
     * @param nodeId the ID of the node
     * @param loggingGroup the logging group
     * @param logFile the log file to export
     * @param broker the broker to broadcast logs to
     * @param charsetName the name of the charset to use
     */
    public SchedulerLogExporter(String nodeId, String loggingGroup, File logFile, SchedulerBroker broker, String charsetName) {
        this(nodeId, loggingGroup, logFile, broker, charsetName, 1000L, 100);
    }

    /**
     * Constructs a new SchedulerLogExporter with specified charset, sample interval, and last lines.
     * @param nodeId the ID of the node
     * @param loggingGroup the logging group
     * @param logFile the log file to export
     * @param broker the broker to broadcast logs to
     * @param charsetName the name of the charset to use
     * @param sampleInterval the sample interval in milliseconds
     * @param lastLines the number of last lines to read initially
     */
    public SchedulerLogExporter(
            String nodeId, String loggingGroup, File logFile, SchedulerBroker broker,
            String charsetName, long sampleInterval, int lastLines) {
        this.nodeId = nodeId;
        this.loggingGroup = loggingGroup;
        this.logFile = logFile;
        this.broker = broker;
        this.charset = (charsetName != null ? Charset.forName(charsetName) : DEFAULT_CHARSET);
        this.sampleInterval = sampleInterval;
        this.lastLines = lastLines;
    }

    /**
     * Gets the node ID.
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Gets the logging group.
     * @return the logging group
     */
    public String getLoggingGroup() {
        return loggingGroup;
    }

    /**
     * Reads the last N lines from the log file and adds them to the message list.
     * @param messages the list to add log lines to
     */
    public void read(@NonNull List<String> messages) {
        readLastLines(messages);
    }

    /**
     * Reads the last lines and appends them to the messages list.
     * @param messages the list to add log lines to
     */
    public void readLastLines(@NonNull List<String> messages) {
        if (lastLines > 0) {
            try {
                List<String> lines = new ArrayList<>();
                if (logFile.exists()) {
                    lines.addAll(readLastLines(logFile, lastLines));
                }
                if (lines.size() < lastLines) {
                    File archivedDir = getArchivedDir();
                    if (archivedDir.exists() && archivedDir.isDirectory()) {
                        File[] archivedFiles = getArchivedFiles(archivedDir);
                        if (archivedFiles != null) {
                            for (File archivedFile : archivedFiles) {
                                int remaining = lastLines - lines.size();
                                List<String> archivedLines = readLastLines(archivedFile, remaining);
                                lines.addAll(0, archivedLines);
                                if (lines.size() >= lastLines) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!lines.isEmpty()) {
                    messages.addAll(lines);
                }
            } catch (IOException e) {
                logger.error("Failed to read log file {}", logFile, e);
            }
        }
    }

    /**
     * Reads a number of previous lines before the currently loaded ones.
     * @param loadedLines the number of lines already loaded
     * @return a list of previous log lines
     */
    public List<String> readPreviousLines(int loadedLines) {
        try {
            return readPreviousLines(loadedLines, lastLines);
        } catch (IOException e) {
            logger.error("Failed to read previous log lines from {}", logFile, e);
            return Collections.emptyList();
        }
    }

    @NonNull
    private List<String> readPreviousLines(int loadedLines, int countToRead) throws IOException {
        int totalSkipped = 0;
        List<String> lines = new ArrayList<>();

        // Main log file
        if (logFile.exists()) {
            try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                    .setFile(logFile)
                    .setCharset(charset)
                    .get()) {
                while (totalSkipped < loadedLines) {
                    if (reader.readLine() == null) {
                        break;
                    }
                    totalSkipped++;
                }
                if (totalSkipped == loadedLines) {
                    String line;
                    while (lines.size() < countToRead && (line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                    if (!lines.isEmpty()) {
                        Collections.reverse(lines);
                    }
                }
            }
        }

        if (lines.size() >= countToRead) {
            return lines;
        }

        // Archived files
        File archivedDir = getArchivedDir();
        if (archivedDir.exists() && archivedDir.isDirectory()) {
            File[] archivedFiles = getArchivedFiles(archivedDir);
            if (archivedFiles != null) {
                for (File archivedFile : archivedFiles) {
                    try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                            .setFile(archivedFile)
                            .setCharset(charset)
                            .get()) {
                        while (totalSkipped < loadedLines) {
                            if (reader.readLine() == null) {
                                break;
                            }
                            totalSkipped++;
                        }
                        if (totalSkipped == loadedLines) {
                            List<String> moreLines = new ArrayList<>();
                            int remaining = countToRead - lines.size();
                            String line;
                            while (moreLines.size() < remaining && (line = reader.readLine()) != null) {
                                moreLines.add(line);
                            }
                            if (!moreLines.isEmpty()) {
                                Collections.reverse(moreLines);
                                lines.addAll(0, moreLines);
                            }
                        }
                    }
                    if (lines.size() >= countToRead) {
                        break;
                    }
                }
            }
        }
        return lines;
    }

    @NonNull
    private File getArchivedDir() {
        String archivedDirPath = System.getProperty(LoggingDefaults.ARCHIVED_LOGS_DIR_PROPERTY);
        File archivedDir;
        if (archivedDirPath != null) {
            archivedDir = new File(archivedDirPath);
            if (!archivedDir.isAbsolute()) {
                archivedDir = new File(logFile.getParentFile(), archivedDirPath);
            }
        } else {
            archivedDir = new File(logFile.getParentFile(), LoggingDefaults.DEFAULT_ARCHIVED_LOGS_DIR);
        }
        return archivedDir;
    }

    private File[] getArchivedFiles(File archivedDir) {
        String baseName = logFile.getName();
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx != -1) {
            baseName = baseName.substring(0, dotIdx);
        }
        final String fileNamePrefix = baseName + ".";
        File[] archivedFiles = archivedDir.listFiles((dir, name) -> name.startsWith(fileNamePrefix));
        if (archivedFiles != null && archivedFiles.length > 0) {
            Arrays.sort(archivedFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        }
        return archivedFiles;
    }

    @NonNull
    private List<String> readLastLines(File file, int lastLines) throws IOException {
        List<String> list = new ArrayList<>();
        try (ReversedLinesFileReader reversedLinesFileReader = ReversedLinesFileReader.builder()
                .setFile(file)
                .setCharset(charset)
                .get()) {
            int count = 0;
            while (count++ < lastLines) {
                String line = reversedLinesFileReader.readLine();
                if (line == null) {
                    break;
                }
                list.add(line);
            }
            Collections.reverse(list);
        }
        return list;
    }

    /**
     * Broadcasts a log line message to all connected sessions.
     * @param message the log line to broadcast
     */
    protected void broadcast(String message) {
        SchedulerResponseParameters response = new SchedulerResponseParameters()
                .setHeader("log")
                .setNodeId(nodeId)
                .setOwner(loggingGroup)
                .setData(message);
        broker.bridgeLog(nodeId, response.toString(), true);
    }

    /**
     * Starts tailing the scheduler log file.
     * @throws Exception if tailing fails to start
     */
    @Override
    protected void doStart() throws Exception {
        if (logFile.exists()) {
            tailer = Tailer.builder()
                    .setFile(logFile)
                    .setTailerListener(new SchedulerLogTailerListener(this))
                    .setDelayDuration(Duration.ofMillis(sampleInterval))
                    .setTailFromEnd(true)
                    .get();
            logger.info("Started tailing scheduler log file: {}", logFile.getAbsolutePath());
        } else {
            logger.warn("Scheduler log file does not exist: {}", logFile.getAbsolutePath());
        }
    }

    /**
     * Stops tailing the scheduler log file.
     * @throws Exception if tailing fails to stop
     */
    @Override
    protected void doStop() throws Exception {
        if (tailer != null) {
            tailer.close();
            tailer = null;
            logger.info("Stopped tailing scheduler log file: {}", logFile.getAbsolutePath());
        }
    }

}
