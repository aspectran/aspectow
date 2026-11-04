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
package com.aspectran.aspectow.node.management.components;

import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import com.aspectran.utils.DataSizeUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Utility class for inspecting component properties using reflection.
 *
 * <p>Created: 2026-11-04</p>
 */
public abstract class ComponentUtils {

    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "secret", "passphrase", "privatekey", "secretkey", "token", "credential", "apikey"
    );

    @Nullable
    public static Method findMethod(Class<?> clazz, String name) {
        if (clazz == null || name == null) {
            return null;
        }
        try {
            return clazz.getMethod(name);
        } catch (NoSuchMethodException e) {
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                try {
                    return current.getDeclaredMethod(name);
                } catch (NoSuchMethodException ignored) {
                }
                current = current.getSuperclass();
            }
            return null;
        }
    }

    @Nullable
    public static Object getFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Exception ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @Nullable
    public static String invokeString(Object target, String... methodNames) {
        Object val = invokeAny(target, methodNames);
        return (val != null ? val.toString() : null);
    }

    @Nullable
    public static Number invokeNumber(Object target, String... methodNames) {
        Object val = invokeAny(target, methodNames);
        if (val instanceof Number num) {
            return num;
        }
        return null;
    }

    @Nullable
    public static Boolean invokeBoolean(Object target, String... methodNames) {
        Object val = invokeAny(target, methodNames);
        if (val instanceof Boolean b) {
            return b;
        }
        return null;
    }

    @Nullable
    public static Object invokeObject(Object target, String... methodNames) {
        return invokeAny(target, methodNames);
    }

    @Nullable
    public static Object invokeAny(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = findMethod(target.getClass(), methodName);
                if (method != null) {
                    method.setAccessible(true);
                    return method.invoke(target);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static String maskIfSensitive(String key, String value) {
        if (key == null || value == null) {
            return value;
        }
        String lowerKey = key.toLowerCase();
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lowerKey.contains(keyword)) {
                return "********";
            }
        }
        return value;
    }

    public static String formatBytes(Number bytes) {
        if (bytes == null) {
            return null;
        }
        long b = bytes.longValue();
        if (b < 0) {
            return "Unlimited";
        }
        return DataSizeUtils.toHumanFriendlyByteSize(b);
    }

    public static boolean hasLocalBean(@Nullable ActivityContext context, @NonNull Class<?> type) {
        if (context == null) {
            return false;
        }
        BeanRegistry registry = context.getBeanRegistry();
        return (registry != null && registry.containsLocalBean(type));
    }

    public static boolean hasLocalBean(@Nullable ActivityContext context, @NonNull String id) {
        if (context == null) {
            return false;
        }
        BeanRegistry registry = context.getBeanRegistry();
        return (registry != null && registry.containsLocalBean(id));
    }

    @Nullable
    public static BeanRule[] getLocalBeanRules(@Nullable ActivityContext context, @NonNull Class<?> type) {
        if (context == null) {
            return null;
        }
        BeanRegistry registry = context.getBeanRegistry();
        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            return defaultBeanRegistry.getBeanRuleRegistry().getBeanRules(type);
        }
        return null;
    }

    @NonNull
    public static String resolveBeanName(@NonNull BeanRule rule, @NonNull String defaultName) {
        if (rule.getId() != null) {
            return rule.getId();
        }
        Class<?> beanClass = rule.getBeanClass();
        if (beanClass != null) {
            String name = beanClass.getSimpleName();
            if (!name.isEmpty()) {
                return Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
        }
        return defaultName;
    }

}
