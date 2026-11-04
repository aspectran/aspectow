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
import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider that extracts specification and configuration details of MultipartFormDataParser components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class MultipartComponentProvider implements ComponentInfoProvider {

    private static final String MULTIPART_PARSER_INTERFACE = "com.aspectran.web.activity.request.MultipartFormDataParser";

    @Override
    @NonNull
    public String getId() {
        return "multipartFormDataParser";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Multipart Form Data Parser";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Web";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-cloud-upload";
    }

    @Nullable
    private Class<?> getMultipartClass() {
        try {
            return Class.forName(MULTIPART_PARSER_INTERFACE);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        Class<?> clazz = getMultipartClass();
        if (clazz == null) {
            return false;
        }
        return ComponentUtils.hasLocalBean(context, clazz);
    }

    @Override
    @Nullable
    public ComponentDetails extractDetails(@NonNull ActivityContext context) {
        Class<?> clazz = getMultipartClass();
        if (clazz == null) {
            return null;
        }

        BeanRegistry registry = context.getBeanRegistry();
        if (registry == null) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();

        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            BeanRule[] rules = defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(clazz);
            if (rules != null) {
                for (BeanRule rule : rules) {
                    try {
                        Object bean = defaultBeanRegistry.getBean(rule);
                        if (bean == null && rule.getId() != null) {
                            bean = defaultBeanRegistry.getBean(rule.getId());
                        }
                        if (bean == null && rule.getBeanClass() != null) {
                            bean = defaultBeanRegistry.getBean(rule.getBeanClass());
                        }
                        if (clazz.isInstance(bean)) {
                            String beanName = ComponentUtils.resolveBeanName(rule, "multipartFormDataParser");
                            items.add(extractParserInfo(beanName, bean));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            Object[] parsers = registry.getBeansOfType((Class<?>) clazz);
            if (parsers != null) {
                int index = 1;
                for (Object parser : parsers) {
                    items.add(extractParserInfo("multipartFormDataParser" + (index > 1 ? index : ""), parser));
                    index++;
                }
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("File Upload and Multipart Request Parsing Specifications")
                .setItems(items);
    }

    @NonNull
    private Map<String, Object> extractParserInfo(String beanName, @NonNull Object parser) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = parser.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        Number maxReq = ComponentUtils.invokeNumber(parser, "getMaxRequestSize");
        Number maxFile = ComponentUtils.invokeNumber(parser, "getMaxFileSize");
        Number maxInMemory = ComponentUtils.invokeNumber(parser, "getMaxInMemorySize");

        info.put("maxRequestSize", maxReq);
        info.put("maxRequestSizeFormatted", ComponentUtils.formatBytes(maxReq));
        info.put("maxFileSize", maxFile);
        info.put("maxFileSizeFormatted", ComponentUtils.formatBytes(maxFile));
        info.put("maxInMemorySize", maxInMemory);
        info.put("maxInMemorySizeFormatted", ComponentUtils.formatBytes(maxInMemory));

        String tempDir = ComponentUtils.invokeString(parser, "getTempDir");
        info.put("tempDirectory", tempDir);
        info.put("defaultTempDirectory", System.getProperty("java.io.tmpdir"));

        info.put("allowedFileExtensions", parseExtensions(parser, "getAllowedFileExtensions"));
        info.put("deniedFileExtensions", parseExtensions(parser, "getDeniedFileExtensions"));

        return info;
    }

    private @NonNull List<String> parseExtensions(Object parser, String... methodNames) {
        Object val = ComponentUtils.invokeObject(parser, methodNames);
        if (val instanceof Collection<?> col) {
            List<String> list = new ArrayList<>();
            for (Object item : col) {
                if (item != null) {
                    list.add(item.toString());
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

}
