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
package com.aspectran.aspectow.appmon.framework;

import com.aspectran.aspectow.node.management.anatomy.AnatomyDataService;
import com.aspectran.aspectow.node.management.components.ComponentDetails;
import com.aspectran.aspectow.node.management.components.ComponentInspectorService;
import com.aspectran.core.activity.Translet;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller that provides framework component specification data for the viewer
 * in standalone AppMon.
 *
 * <p>Created: 2026-11-05</p>
 */
@Component("/framework/components")
@Profile("appmon.standalone")
@Bean("componentsActivity")
public class ComponentsActivity {

    private final ComponentInspectorService componentInspectorService;

    public ComponentsActivity() {
        this.componentInspectorService = new ComponentInspectorService();
    }

    /**
     * Dispatches to the components viewer page.
     * @param translet the active translet
     * @return the model map containing attributes for rendering the view
     */
    @Request
    @Dispatch("framework/components/viewer")
    @Action("page")
    public Map<String, Object> index(@NonNull Translet translet) {
        return viewer(translet, null);
    }

    /**
     * Dispatches to the components viewer page with a specified context.
     * @param translet the active translet
     * @param contextName the name of the context
     * @return the model map containing attributes for rendering the view
     */
    @Request("/${contextName}")
    @Dispatch("framework/components/viewer")
    @Action("page")
    public Map<String, Object> viewer(@NonNull Translet translet, String contextName) {
        List<String> allContextNames = AnatomyDataService.getContextNames();
        if (contextName == null || !allContextNames.contains(contextName)) {
            if (!allContextNames.isEmpty()) {
                contextName = allContextNames.getFirst();
            }
        }
        if (contextName == null) {
            contextName = "0";
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("title", "Framework Components");
        model.put("style", "compact");
        model.put("headinclude", "framework/_contexts");
        model.put("allContextNames", allContextNames);
        model.put("contextName", contextName);
        return model;
    }

    /**
     * Retrieves the list of context names for the local node.
     * @return a REST response containing the list of context names
     */
    @Request("/contexts")
    public RestResponse contexts() {
        List<String> names = AnatomyDataService.getContextNames();
        return new DefaultRestResponse("allContextNames", names).ok();
    }

    /**
     * Provides framework component specification data as JSON for the specified context.
     * @param translet the active translet
     * @param contextName the name of the context
     * @return a REST response containing the components data
     */
    @Request("/${contextName}/data")
    public RestResponse data(@NonNull Translet translet, String contextName) {
        if (contextName == null) {
            contextName = translet.getParameter("contextName");
        }
        Map<String, ActivityContext> contexts = AnatomyDataService.prepareContextMap();
        ActivityContext context = (contextName != null ? contexts.get(contextName) : null);
        if (context == null && !contexts.isEmpty()) {
            context = contexts.values().iterator().next();
        }
        if (context == null) {
            return new DefaultRestResponse().notFound();
        }
        Map<String, ComponentDetails> data = componentInspectorService.inspectAll(context);
        return new DefaultRestResponse("componentsData", data).nullWritable(false).ok();
    }

    /**
     * Provides framework component specification data as JSON for the default context.
     * @param translet the active translet
     * @return a REST response containing the components data
     */
    @Request("/data")
    public RestResponse dataDefault(@NonNull Translet translet) {
        return data(translet, null);
    }

}
