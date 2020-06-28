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
package club.textchat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Created: 2020/06/29</p>
 */
class CountryCodeLookupTest {

    @Test
    void getCountryCodeByIP1() {
        String countryCode = CountryCodeLookup.getInstance().getCountryCodeByIP("185.80.140.175");
        assertEquals("YE", countryCode);
    }

    @Test
    void getCountryCodeByIP2() {
        String countryCode = CountryCodeLookup.getInstance().getCountryCodeByIP("41.13.196.81");
        assertEquals("ZA", countryCode);
    }

    @Test
    void getCountryCodeByIP3() {
        String countryCode = CountryCodeLookup.getInstance().getCountryCodeByIP("94.153.65.249");
        assertEquals("UA", countryCode);
    }

}
