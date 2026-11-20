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
package com.aspectran.aspectow.console.notification.alert;

import java.io.Serial;
import java.io.Serializable;

/**
 * Holds the minimum CPU and memory alert thresholds across active users.
 * Provides default threshold constants for system resource monitoring.
 *
 * <p>Created: 2026-11-20</p>
 */
public class ResourceThresholds implements Serializable {

    @Serial
    private static final long serialVersionUID = 3249271314808578928L;

    /**
     * Default resource alert threshold percentage (85%).
     */
    public static final int DEFAULT_THRESHOLD = 85;

    /**
     * Default CPU alert threshold percentage.
     */
    public static final int DEFAULT_CPU_THRESHOLD = DEFAULT_THRESHOLD;

    /**
     * Default memory alert threshold percentage.
     */
    public static final int DEFAULT_MEMORY_THRESHOLD = DEFAULT_THRESHOLD;

    private int minCpu = DEFAULT_CPU_THRESHOLD;

    private int minMemory = DEFAULT_MEMORY_THRESHOLD;

    public int getMinCpu() {
        return minCpu;
    }

    public void setMinCpu(int minCpu) {
        this.minCpu = minCpu;
    }

    public int getMinMemory() {
        return minMemory;
    }

    public void setMinMemory(int minMemory) {
        this.minMemory = minMemory;
    }

}
