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
package com.aspectran.aspectow.console.framework;

import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.config.AspectranConfig;
import com.aspectran.core.service.CoreService;
import com.aspectran.core.service.CoreServiceHolder;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.activity.response.DefaultRestResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A controller that provides framework configuration data for the viewer.
 */
@Component("/framework/config")
@Bean("configurationActivity")
public class ConfigurationActivity {

    /**
     * Dispatches to the configuration viewer page.
     */
    @Request("/${contextName}")
    @Dispatch("framework/config/viewer")
    @Action("page")
    public Map<String, Object> viewer(String contextName) {
        Map<String, ActivityContext> contexts = AnatomyActivity.prepareContextMap();
        List<String> allContextNames = new ArrayList<>(contexts.keySet());
        if (contextName == null || !allContextNames.contains(contextName)) {
            if (!allContextNames.isEmpty()) {
                contextName = allContextNames.getFirst();
            }
        }
        if (contextName == null) {
            contextName = "0";
        }
        return Map.of(
                "title", "Framework Configuration",
                "style", "config-page",
                "group", "framework-menu",
                "allContextNames", allContextNames,
                "contextName", contextName
        );
    }

    /**
     * Provides framework configuration data as JSON.
     * @return a map containing the configuration data, identified by "configData"
     */
    @Request("/${contextName}/data")
    public RestResponse data(String contextName) {
        CoreService targetService = null;
        List<CoreService> services = new ArrayList<>(CoreServiceHolder.getAllServices());
        Collections.reverse(services);
        int index = 0;
        for (CoreService service : services) {
            String name = service.getContextName();
            if (StringUtils.isEmpty(name)) {
                name = Integer.toString(index);
            }
            if (name.equals(contextName)) {
                targetService = service;
                break;
            }
            index++;
        }

        if (targetService == null) {
            return new DefaultRestResponse().notFound();
        }

        AspectranConfig aspectranConfig = targetService.getAspectranConfig();
        if (aspectranConfig == null) {
            return new DefaultRestResponse("configData", Collections.emptyMap()).ok();
        }

        Map<String, Object> configData = new LinkedHashMap<>();

        // Full Configuration
        configData.put("Full Configuration", Map.of(
                "name", "Full Configuration",
                "apon", aspectranConfig.toString()
        ));

        // Sections
        if (aspectranConfig.hasSystemConfig()) {
            configData.put("System Config", Map.of(
                    "name", "system",
                    "apon", aspectranConfig.getSystemConfig().toString()
            ));
        }
        if (aspectranConfig.hasContextConfig()) {
            configData.put("Context Config", Map.of(
                    "name", "context",
                    "apon", aspectranConfig.getContextConfig().toString()
            ));
        }
        if (aspectranConfig.hasSchedulerConfig()) {
            configData.put("Scheduler Config", Map.of(
                    "name", "scheduler",
                    "apon", aspectranConfig.getSchedulerConfig().toString()
            ));
        }
        if (aspectranConfig.hasWebConfig()) {
            configData.put("Web Config", Map.of(
                    "name", "web",
                    "apon", aspectranConfig.getWebConfig().toString()
            ));
        }
        if (aspectranConfig.getDaemonConfig() != null) {
            configData.put("Daemon Config", Map.of(
                    "name", "daemon",
                    "apon", aspectranConfig.getDaemonConfig().toString()
            ));
        }
        if (aspectranConfig.getShellConfig() != null) {
            configData.put("Shell Config", Map.of(
                    "name", "shell",
                    "apon", aspectranConfig.getShellConfig().toString()
            ));
        }
        if (aspectranConfig.getEmbedConfig() != null) {
            configData.put("Embed Config", Map.of(
                    "name", "embed",
                    "apon", aspectranConfig.getEmbedConfig().toString()
            ));
        }

        return new DefaultRestResponse("configData", configData).nullWritable(false).ok();
    }

}
