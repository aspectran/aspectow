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
package aspectow.demo.examples.customer;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.utils.annotation.jsr305.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The DAO to retrieve or manipulate customer data.
 */
@Component
@Bean
public class CustomerRepository {

    private final Logger logger = LoggerFactory.getLogger(CustomerRepository.class);

    private final Map<Integer, Customer> customerMap;

    private static final AtomicInteger counter = new AtomicInteger();

    public CustomerRepository() {
        // Pre-create 10 customers whose names begin with "Guest"
        Map<Integer, Customer> customerMap = new ConcurrentSkipListMap<>();
        for (int i = 1; i <= 10; i++) {
            Customer customer = new Customer();
            customer.putValue(Customer.id, i);
            customer.putValue(Customer.name, "Guest " + i);
            customer.putValue(Customer.age, i + 20);
            customer.putValue(Customer.approved, true);
            customerMap.put(i, customer);
        }
        this.customerMap = customerMap;
        counter.set(customerMap.size());
    }

    public Customer getCustomer(int id) {
        logger.debug("Gets the details of customer: {}", id);
        return customerMap.get(id);

    }

    public boolean isCustomer(int id) {
        if (customerMap.containsKey(id)) {
            logger.debug("Customer {} exists", id);
            return true;
        } else {
            logger.debug("Customer {} does not exists", id);
            return false;
        }
    }

    public List<Customer> getCustomerList() {
        logger.debug("Get a list of all customers");

        List<Customer> customerList = new ArrayList<>(customerMap.values());

        logger.debug("Retrieved {} customers", customerList.size());

        return customerList;
    }

    public int insertCustomer(Customer customer) {
        if (customerMap.size() > 9999) {
            return -1;
        }

        int id = counter.incrementAndGet();
        customer.putValue(Customer.id, id);

        customerMap.put(id, customer);

        logger.debug("Customer {} is registered", id);

        return id;
    }

    public synchronized boolean updateCustomer(@NonNull Customer customer) {
        int id = customer.getInt(Customer.id);
        if (customerMap.containsKey(id)) {
            logger.debug("Update customer: {}", id);
            customerMap.put(id, customer);
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean deleteCustomer(int id) {
        if (customerMap.containsKey(id)) {
            logger.debug("Delete customer: {}", id);
            customerMap.remove(id);
            return true;
        } else {
            return false;
        }
    }

    public boolean approve(int id, boolean approved) {
        Customer customer = customerMap.get(id);
        if (customer != null) {
            logger.debug("{}Approval for customer {} (approved: {})", id, id, approved);
            customer.putValue(Customer.approved, approved);
            return true;
        } else {
            return false;
        }
    }

    public boolean isApproved(int id) {
        Customer customer = customerMap.get(id);
        if (customer != null) {
            logger.debug("Returns whether customer {} is approved", id);
            return customer.getBoolean(Customer.approved);
        } else {
            return false;
        }
    }

}
