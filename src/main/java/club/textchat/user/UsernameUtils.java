/*
 * Copyright (c) 2020 The Aspectran Project
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
package club.textchat.user;

import java.util.regex.Pattern;

/**
 * <p>Created: 2020/05/11</p>
 */
public class UsernameUtils {

    private static final int MAX_USERNAME_LEN = 30;

    private static final Pattern USERNAME_NORMALIZATION_PATTERN = Pattern.compile("[,'`\"#&/<>\\\\]");

    private static final Pattern USERNAME_CONDENSATION_PATTERN = Pattern.compile("[\\s`~!@#$%^&*()_|+\\-=?;:'\",.<>\\{\\}\\[\\]\\\\\\/]");

    private static final Pattern SPACE_CONDENSATION_PATTERN = Pattern.compile("\\s{2,}");

    public static String normalize(String username) {
        username = USERNAME_NORMALIZATION_PATTERN.matcher(username).replaceAll("");
        username = SPACE_CONDENSATION_PATTERN.matcher(username).replaceAll(" ").trim();
        if (username.length() > MAX_USERNAME_LEN) {
            username = username.substring(0, MAX_USERNAME_LEN);
        }
        return username;
    }

    public static String condense(String username) {
        return USERNAME_CONDENSATION_PATTERN.matcher(username).replaceAll("");
    }

}
