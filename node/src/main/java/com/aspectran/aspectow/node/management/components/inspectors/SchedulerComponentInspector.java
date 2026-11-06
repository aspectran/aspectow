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
import com.aspectran.core.component.schedule.ScheduleRuleRegistry;
import com.aspectran.core.component.schedule.ScheduledJobLockProvider;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.ScheduleRule;
import com.aspectran.core.context.rule.ScheduledJobRule;
import com.aspectran.core.context.rule.params.TriggerExpressionParameters;
import com.aspectran.core.context.rule.type.TriggerType;
import com.aspectran.core.scheduler.service.SchedulerService;
import com.aspectran.core.service.CoreService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quartz.Scheduler;
import org.quartz.SchedulerMetaData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of ScheduleRules and Quartz Schedulers.
 *
 * <p>Created: 2026-11-04</p>
 */
public class SchedulerComponentInspector implements ComponentInspector {

    @Override
    @NonNull
    public String getId() {
        return "scheduler";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Scheduler";
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

        ScheduledJobLockProvider lockProvider = schedulerService.getJobLockProvider();

        // Schedule Rule Registry
        ScheduleRuleRegistry scheduleRuleRegistry = context.getScheduleRuleRegistry();
        Collection<ScheduleRule> scheduleRules = (scheduleRuleRegistry != null ? scheduleRuleRegistry.getScheduleRules() : null);

        List<Map<String, Object>> items = new ArrayList<>();
        if (scheduleRules != null) {
            for (ScheduleRule rule : scheduleRules) {
                // Obtain Quartz Scheduler directly from SchedulerService using scheduleRule ID
                Scheduler quartzScheduler = schedulerService.getScheduler(rule.getId());
                if (quartzScheduler == null) {
                    if (rule.getSchedulerBeanClass() != null) {
                        try {
                            quartzScheduler = (Scheduler) context.getBeanRegistry().getBean(rule.getSchedulerBeanClass());
                        } catch (Exception ignored) {
                        }
                    } else if (rule.getSchedulerBeanId() != null) {
                        try {
                            quartzScheduler = context.getBeanRegistry().getBean(rule.getSchedulerBeanId());
                        } catch (Exception ignored) {
                        }
                    }
                }

                Map<String, Object> schedulerMeta = extractSchedulerMeta(quartzScheduler, schedulerService, lockProvider);
                Map<String, Object> ruleInfo = extractRuleInfo(rule, schedulerMeta);
                items.add(ruleInfo);
            }
        }

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("Scheduled Job Rules and Trigger Specifications")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractSchedulerMeta(
            @Nullable Scheduler quartzScheduler,
            @NonNull SchedulerService schedulerService,
            @Nullable ScheduledJobLockProvider lockProvider) {
        Map<String, Object> metaInfo = new LinkedHashMap<>();
        if (quartzScheduler != null) {
            try {
                SchedulerMetaData meta = quartzScheduler.getMetaData();
                if (meta != null) {
                    metaInfo.put("schedulerName", meta.getSchedulerName());
                    metaInfo.put("schedulerInstanceId", meta.getSchedulerInstanceId());
                    metaInfo.put("version", meta.getVersion());

                    Class<?> schedClass = meta.getSchedulerClass();
                    if (schedClass != null) {
                        metaInfo.put("schedulerClassName", schedClass.getName());
                        metaInfo.put("schedulerSimpleClassName", schedClass.getSimpleName());
                    }

                    metaInfo.put("isRemote", meta.isSchedulerRemote());

                    Class<?> jobStoreClass = meta.getJobStoreClass();
                    if (jobStoreClass != null) {
                        metaInfo.put("jobStoreClassName", jobStoreClass.getName());
                        metaInfo.put("jobStoreClass", jobStoreClass.getSimpleName());
                    }

                    metaInfo.put("jobStoreSupportsPersistence", meta.isJobStoreSupportsPersistence());
                    metaInfo.put("jobStoreClustered", meta.isJobStoreClustered());
                    metaInfo.put("isClustered", meta.isJobStoreClustered());

                    Class<?> threadPoolClass = meta.getThreadPoolClass();
                    if (threadPoolClass != null) {
                        metaInfo.put("threadPoolClassName", threadPoolClass.getName());
                        metaInfo.put("threadPoolClass", threadPoolClass.getSimpleName());
                    }
                    metaInfo.put("threadPoolSize", meta.getThreadPoolSize());
                } else {
                    metaInfo.put("schedulerName", quartzScheduler.getSchedulerName());
                    metaInfo.put("schedulerInstanceId", quartzScheduler.getSchedulerInstanceId());
                    metaInfo.put("isClustered", false);
                }
            } catch (Exception ignored) {
            }
        }

        metaInfo.put("serviceClassName", schedulerService.getClass().getName());
        metaInfo.put("serviceSimpleClassName", schedulerService.getClass().getSimpleName());
        metaInfo.put("startDelaySeconds", schedulerService.getStartDelaySeconds());
        metaInfo.put("waitOnShutdown", schedulerService.isWaitOnShutdown());
        metaInfo.put("loggingGroup", schedulerService.getLoggingGroup());

        if (lockProvider != null) {
            metaInfo.put("jobLockProviderClassName", lockProvider.getClass().getName());
            metaInfo.put("jobLockProviderSimpleClassName", lockProvider.getClass().getSimpleName());
        }

        return metaInfo;
    }

    @NonNull
    private Map<String, Object> extractRuleInfo(
            @NonNull ScheduleRule rule,
            @NonNull Map<String, Object> schedulerMeta) {
        Map<String, Object> ruleInfo = new LinkedHashMap<>();
        ruleInfo.put("id", rule.getId());
        TriggerType triggerType = rule.getTriggerType();
        ruleInfo.put("triggerType", triggerType != null ? triggerType.name() : "N/A");

        if (rule.getSchedulerBeanId() != null) {
            ruleInfo.put("schedulerBeanId", rule.getSchedulerBeanId());
        }
        if (rule.getSchedulerBeanClass() != null) {
            ruleInfo.put("schedulerBeanClass", rule.getSchedulerBeanClass().getName());
        }
        if (rule.getIsolated() != null) {
            ruleInfo.put("isolated", rule.getIsolated());
        }
        if (rule.getDisabled() != null) {
            ruleInfo.put("disabled", rule.getDisabled());
        }
        if (rule.getDescriptionRule() != null && rule.getDescriptionRule().getContent() != null) {
            ruleInfo.put("description", rule.getDescriptionRule().getContent().trim());
        }

        TriggerExpressionParameters expr = rule.getTriggerExpressionParameters();
        if (expr != null) {
            Map<String, Object> triggerInfo = new LinkedHashMap<>();
            if (expr.getExpression() != null) {
                triggerInfo.put("expression", expr.getExpression());
                ruleInfo.put("expression", expr.getExpression());
            }
            if (expr.getMisfirePolicy() != null) {
                triggerInfo.put("misfirePolicy", expr.getMisfirePolicy());
            }
            if (expr.getStartDelaySeconds() != null) {
                triggerInfo.put("startDelaySeconds", expr.getStartDelaySeconds());
            }
            if (expr.getIntervalInMilliseconds() != null) {
                triggerInfo.put("intervalInMilliseconds", expr.getIntervalInMilliseconds());
            }
            if (expr.getIntervalInSeconds() != null) {
                triggerInfo.put("intervalInSeconds", expr.getIntervalInSeconds());
            }
            if (expr.getIntervalInMinutes() != null) {
                triggerInfo.put("intervalInMinutes", expr.getIntervalInMinutes());
            }
            if (expr.getIntervalInHours() != null) {
                triggerInfo.put("intervalInHours", expr.getIntervalInHours());
            }
            if (expr.getRepeatCount() != null) {
                triggerInfo.put("repeatCount", expr.getRepeatCount());
            }
            if (expr.getRepeatForever() != null) {
                triggerInfo.put("repeatForever", expr.getRepeatForever());
            }
            ruleInfo.put("trigger", triggerInfo);

            String expressionSummary = null;
            if (triggerType == TriggerType.CRON && expr.getExpression() != null) {
                expressionSummary = expr.getExpression();
            } else if (triggerType == TriggerType.SIMPLE) {
                StringBuilder sb = new StringBuilder();
                if (expr.getIntervalInHours() != null && expr.getIntervalInHours() > 0) {
                    sb.append(expr.getIntervalInHours()).append("h ");
                }
                if (expr.getIntervalInMinutes() != null && expr.getIntervalInMinutes() > 0) {
                    sb.append(expr.getIntervalInMinutes()).append("m ");
                }
                if (expr.getIntervalInSeconds() != null && expr.getIntervalInSeconds() > 0) {
                    sb.append(expr.getIntervalInSeconds()).append("s ");
                }
                if (expr.getIntervalInMilliseconds() != null && expr.getIntervalInMilliseconds() > 0) {
                    sb.append(expr.getIntervalInMilliseconds()).append("ms ");
                }
                if (sb.isEmpty()) {
                    sb.append("simple");
                } else {
                    sb.insert(0, "Every ");
                }
                if (Boolean.TRUE.equals(expr.getRepeatForever())) {
                    sb.append("(Forever)");
                } else if (expr.getRepeatCount() != null) {
                    sb.append("(Repeat ").append(expr.getRepeatCount()).append("x)");
                }
                expressionSummary = sb.toString().trim();
            }
            if (expressionSummary != null) {
                ruleInfo.put("expressionSummary", expressionSummary);
            }
        }

        // Bound Quartz Scheduler engine metadata
        ruleInfo.put("scheduler", schedulerMeta);

        // Scheduled Jobs
        List<ScheduledJobRule> jobRules = rule.getScheduledJobRuleList();
        ruleInfo.put("jobCount", jobRules != null ? jobRules.size() : 0);
        if (jobRules != null && !jobRules.isEmpty()) {
            List<Map<String, Object>> jobs = new ArrayList<>();
            for (ScheduledJobRule jobRule : jobRules) {
                Map<String, Object> jobInfo = new LinkedHashMap<>();
                jobInfo.put("translet", jobRule.getTransletName());
                if (jobRule.getIsolated() != null) {
                    jobInfo.put("isolated", jobRule.getIsolated());
                }
                if (jobRule.getDisabled() != null) {
                    jobInfo.put("disabled", jobRule.getDisabled());
                }
                jobs.add(jobInfo);
            }
            ruleInfo.put("jobs", jobs);
        }

        return ruleInfo;
    }

}
