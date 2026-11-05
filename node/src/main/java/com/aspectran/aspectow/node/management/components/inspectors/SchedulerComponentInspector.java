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
package com.aspectran.aspectow.node.management.components.inspectors;

import com.aspectran.aspectow.node.management.components.ComponentDetails;
import com.aspectran.aspectow.node.management.components.ComponentInspector;
import com.aspectran.aspectow.node.management.components.ComponentUtils;
import com.aspectran.core.component.schedule.ScheduleRuleRegistry;
import com.aspectran.core.component.schedule.ScheduledJobLockProvider;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.ScheduleRule;
import com.aspectran.core.context.rule.ScheduledJobRule;
import com.aspectran.core.context.rule.type.TriggerType;
import com.aspectran.core.scheduler.service.SchedulerService;
import com.aspectran.core.service.CoreService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of SchedulerService components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class SchedulerComponentInspector implements ComponentInspector {

    @Override
    @NonNull
    public String getId() {
        return "schedulerService";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Scheduler Service";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Scheduling";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-alarm";
    }

    @Nullable
    private SchedulerService getSchedulerService(@NonNull ActivityContext context) {
        CoreService masterService = context.getMasterService();
        if (masterService != null) {
            return masterService.getSchedulerService();
        }
        return null;
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        return (getSchedulerService(context) != null);
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        SchedulerService schedulerService = getSchedulerService(context);
        if (schedulerService == null) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = schedulerService.getClass();

        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        // Status
        String status = (schedulerService.isActive() ? "Active" : "Inactive");
        if (schedulerService.isBusy()) {
            status = "Busy";
        }
        info.put("status", status);
        info.put("startDelaySeconds", schedulerService.getStartDelaySeconds());
        info.put("waitOnShutdown", schedulerService.isWaitOnShutdown());
        info.put("loggingGroup", schedulerService.getLoggingGroup());

        // Job lock provider
        ScheduledJobLockProvider lockProvider = schedulerService.getJobLockProvider();
        if (lockProvider != null) {
            info.put("jobLockProviderClassName", lockProvider.getClass().getName());
            info.put("jobLockProviderSimpleClassName", lockProvider.getClass().getSimpleName());
        }

        // Schedule Rule Registry
        ScheduleRuleRegistry scheduleRuleRegistry = context.getScheduleRuleRegistry();
        if (scheduleRuleRegistry != null) {
            Collection<ScheduleRule> scheduleRules = scheduleRuleRegistry.getScheduleRules();
            info.put("scheduleRulesCount", scheduleRules != null ? scheduleRules.size() : 0);

            if (scheduleRules != null) {
                List<Map<String, Object>> rulesSummary = new ArrayList<>();
                for (ScheduleRule rule : scheduleRules) {
                    Map<String, Object> ruleInfo = new LinkedHashMap<>();
                    ruleInfo.put("id", rule.getId());
                    TriggerType triggerType = rule.getTriggerType();
                    ruleInfo.put("triggerType", triggerType != null ? triggerType.name() : "N/A");

                    List<ScheduledJobRule> jobRules = rule.getScheduledJobRuleList();
                    ruleInfo.put("jobCount", jobRules != null ? jobRules.size() : 0);
                    rulesSummary.add(ruleInfo);
                }
                info.put("rulesSummary", rulesSummary);
            }
        } else {
            info.put("scheduleRulesCount", 0);
        }

        // Quartz Scheduler Reflection
        try {
            Collection<?> schedulers = (Collection<?>) ComponentUtils.invokeObject(schedulerService, "getSchedulers");
            if (schedulers != null && !schedulers.isEmpty()) {
                Object quartzScheduler = schedulers.iterator().next();
                if (quartzScheduler != null) {
                    info.put("schedulerName", ComponentUtils.invokeString(quartzScheduler, "getSchedulerName"));
                    info.put("schedulerInstanceId", ComponentUtils.invokeString(quartzScheduler, "getSchedulerInstanceId"));
                    info.put("isClustered", ComponentUtils.invokeBoolean(quartzScheduler, "isClustered"));

                    Object meta = ComponentUtils.invokeObject(quartzScheduler, "getMetaData");
                    if (meta != null) {
                        info.put("threadPoolSize", ComponentUtils.invokeNumber(meta, "getThreadPoolSize"));
                        info.put("numberOfJobsExecuted", ComponentUtils.invokeNumber(meta, "getNumberOfJobsExecuted"));

                        Object jobStoreClass = ComponentUtils.invokeObject(meta, "getJobStoreClass");
                        if (jobStoreClass instanceof Class<?> jsc) {
                            info.put("jobStoreClass", jsc.getSimpleName());
                        }

                        Object threadPoolClass = ComponentUtils.invokeObject(meta, "getThreadPoolClass");
                        if (threadPoolClass instanceof Class<?> tpc) {
                            info.put("threadPoolClass", tpc.getSimpleName());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        items.add(info);

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("Quartz-based Task Scheduling Service and Job Triggers")
                .setItems(items);
    }

}
