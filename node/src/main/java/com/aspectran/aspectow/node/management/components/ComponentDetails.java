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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the extracted configuration and specification details
 * of a component.
 *
 * <p>Created: 2026-11-04</p>
 */
public class ComponentDetails {

    private String id;

    private String displayName;

    private String category;

    private String icon;

    private String description;

    private int itemCount;

    private boolean refreshable;

    private List<Map<String, Object>> items = new ArrayList<>();

    private Map<String, Object> properties = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public ComponentDetails setId(String id) {
        this.id = id;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ComponentDetails setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public ComponentDetails setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getIcon() {
        return icon;
    }

    public ComponentDetails setIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ComponentDetails setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getItemCount() {
        return itemCount;
    }

    public ComponentDetails setItemCount(int itemCount) {
        this.itemCount = itemCount;
        return this;
    }

    public boolean isRefreshable() {
        return refreshable;
    }

    public ComponentDetails setRefreshable(boolean refreshable) {
        this.refreshable = refreshable;
        return this;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public ComponentDetails setItems(List<Map<String, Object>> items) {
        this.items = items;
        if (items != null) {
            this.itemCount = items.size();
        }
        return this;
    }

    public ComponentDetails addItem(Map<String, Object> item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        itemCount = items.size();
        return this;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public ComponentDetails setProperties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    public ComponentDetails putProperty(String key, Object value) {
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        properties.put(key, value);
        return this;
    }

}
