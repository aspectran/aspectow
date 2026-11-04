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
package com.aspectran.aspectow.node.management.components.providers;

import com.aspectran.aspectow.node.management.components.ComponentDetails;
import com.aspectran.aspectow.node.management.components.ComponentInfoProvider;
import com.aspectran.aspectow.node.management.components.ComponentUtils;
import com.aspectran.core.component.template.TemplateRenderer;
import com.aspectran.core.context.ActivityContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider that extracts specification and configuration details of TemplateRenderer components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class TemplateRendererComponentProvider implements ComponentInfoProvider {

    @Override
    @NonNull
    public String getId() {
        return "templateRenderer";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Template Renderer";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "View";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-palette";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        return (context.getTemplateRenderer() != null);
    }

    @Override
    @Nullable
    public ComponentDetails extractDetails(@NonNull ActivityContext context) {
        TemplateRenderer renderer = context.getTemplateRenderer();
        if (renderer == null) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = renderer.getClass();

        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        // Template rule count
        Object rules = ComponentUtils.invokeObject(renderer, "getTemplateRules");
        if (rules instanceof Collection<?> col) {
            info.put("templateRulesCount", col.size());
        } else {
            info.put("templateRulesCount", 0);
        }

        // Internal engine inspection
        Object engine = ComponentUtils.invokeObject(renderer, "getTemplateEngine");
        if (engine == null) {
            engine = ComponentUtils.getFieldValue(renderer, "templateEngine");
        }

        if (engine != null) {
            Class<?> engineClass = engine.getClass();
            info.put("engineClassName", engineClass.getName());
            info.put("engineSimpleClassName", engineClass.getSimpleName());

            info.put("defaultEncoding", ComponentUtils.invokeString(engine, "getDefaultEncoding", "getEncoding"));
            info.put("prefix", ComponentUtils.invokeString(engine, "getPrefix"));
            info.put("suffix", ComponentUtils.invokeString(engine, "getSuffix"));
            info.put("cacheEnabled", ComponentUtils.invokeBoolean(engine, "isCacheEnabled", "isCacheable"));
        } else {
            info.put("defaultEncoding", ComponentUtils.invokeString(renderer, "getDefaultEncoding"));
        }

        items.add(info);

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("Template Rendering Engines and View Specifications")
                .setItems(items);
    }

}
