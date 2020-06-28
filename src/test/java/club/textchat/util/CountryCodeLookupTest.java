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
