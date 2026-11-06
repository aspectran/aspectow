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
import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.component.bean.event.EventListenerRegistry;
import com.aspectran.core.component.bean.event.EventPublisher;
import com.aspectran.core.component.bean.event.ListenerMethod;
import com.aspectran.core.context.ActivityContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspector that extracts specification and configuration details of EventPublisher components.
 *
 * <p>Created: 2026-11-04</p>
 */
public class EventPublisherComponentInspector implements ComponentInspector {

    @Override
    @NonNull
    public String getId() {
        return "eventPublisher";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Event Publisher";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Core";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-broadcast";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        BeanRegistry beanRegistry = context.getBeanRegistry();
        if (beanRegistry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            return (defaultBeanRegistry.getEventListenerRegistry() != null);
        }
        return (beanRegistry != null && beanRegistry.containsBean(EventPublisher.class));
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        BeanRegistry registry = context.getBeanRegistry();
        if (!(registry instanceof DefaultBeanRegistry defaultBeanRegistry)) {
            return null;
        }

        EventListenerRegistry listenerRegistry = defaultBeanRegistry.getEventListenerRegistry();
        if (listenerRegistry == null) {
            return null;
        }

        Map<Class<?>, List<ListenerMethod>> listenerMap = listenerRegistry.getListenerMap();
        List<Map<String, Object>> listenerList = new ArrayList<>();

        if (listenerMap != null) {
            for (Map.Entry<Class<?>, List<ListenerMethod>> entry : listenerMap.entrySet()) {
                Class<?> eventClass = entry.getKey();
                List<ListenerMethod> methods = entry.getValue();
                for (ListenerMethod lm : methods) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("eventType", eventClass.getName());
                    item.put("eventSimpleType", eventClass.getSimpleName());

                    Object bean = lm.getBean();
                    Method method = lm.getMethod();

                    item.put("beanClass", (bean != null ? bean.getClass().getName() : null));
                    item.put("methodName", (method != null ? method.getName() : lm.toString()));
                    item.put("methodSignature", (method != null ? method.toGenericString() : lm.toString()));
                    listenerList.add(item);
                }
            }
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("totalListeners", listenerList.size());
        props.put("uniqueEventTypes", (listenerMap != null ? listenerMap.size() : 0));

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("Application Event Listeners and Subscription Specifications")
                .setItems(listenerList)
                .setProperties(props);
    }

}
