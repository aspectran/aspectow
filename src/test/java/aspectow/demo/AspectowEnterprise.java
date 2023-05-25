/*
 * Copyright (c) 2008-2023 The Aspectran Project
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
package aspectow.demo;

import com.aspectran.core.util.ResourceUtils;
import com.aspectran.shell.jline.JLineAspectranShell;

import java.io.File;
import java.io.IOException;

import static com.aspectran.core.context.config.AspectranConfig.BASE_PATH_PROPERTY_NAME;

/**
 * Main entry point for the application.
 */
public class AspectowEnterprise {

    public static void main(String[] args) {
        try {
            File root = new File(ResourceUtils.getResourceAsFile(""), "../../app");
            System.setProperty(BASE_PATH_PROPERTY_NAME, root.getCanonicalPath()); // for logback
            JLineAspectranShell.main(new String[] { root.getCanonicalPath(), "config/aspectran-config.apon" });
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

}
