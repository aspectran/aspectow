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
package com.aspectran.aspectow.appmon.anatomy;

import com.aspectran.aspectow.node.management.anatomy.AnatomyDataService;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.web.activity.response.DefaultRestResponse;
import com.aspectran.web.activity.response.RestResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A controller that provides framework anatomy data for the viewer.
 */
@Component
@Profile("appmon.standalone")
@Bean("anatomyActivity")
public class AnatomyActivity {

    private final AnatomyDataService anatomyDataService;

    public AnatomyActivity() {
        this.anatomyDataService = new AnatomyDataService();
    }

    /**
     * Dispatches to the anatomy viewer page within the default template.
     */
    @Request("/anatomy/${contextName}")
    @Dispatch("anatomy/viewer")
    @Action("page")
    public Map<String, Object> viewer(String contextName) {
        Map<String, ActivityContext> contexts = AnatomyDataService.prepareContextMap();
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
                "headinclude", "anatomy/_contexts",
                "style", "fluid compact",
                "allContextNames", allContextNames,
                "contextName", contextName
        );
    }

    /**
     * Provides framework anatomy data as JSON.
     * @return a map containing the anatomy data, identified by "anatomyData"
     */
    @Request("/anatomy/${contextName}/data")
    @Transform(format = FormatType.JSON)
    public RestResponse data(String contextName) {
        Map<String, ActivityContext> contexts = AnatomyDataService.prepareContextMap();
        ActivityContext context = contexts.get(contextName);
        if (context == null) {
            return new DefaultRestResponse().notFound();
        }
        Map<String, Object> data = anatomyDataService.getAnatomyData(context);
        return new DefaultRestResponse("anatomyData", data).nullWritable(false).ok();
    }

}
