package com.aspectran.aspectow.console.demo.test;

import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Job;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Schedule;
import com.aspectran.core.component.bean.annotation.SimpleTrigger;
import com.aspectran.core.context.rule.type.MisfirePolicy;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Created: 2026. 6. 6.</p>
 */
@Component
@Bean
@Schedule(
        id = "countSchedule",
        scheduler = "testScheduler",
        simpleTrigger = @SimpleTrigger(
                startDelaySeconds = 5,      // Start 5 seconds after scheduler starts
                intervalInSeconds = 5,      // Repeat every 1 minute
                repeatForever = true,       // Repeat indefinitely
                misfirePolicy = MisfirePolicy.RESCHEDULE_NEXT_WITH_REMAINING_COUNT
        ),
        jobs = {
                @Job(translet = "test/schedule/count.job")
        }
)
public class CountSchedule {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Request("test/schedule/count.job")
    public int count() {
        return counter.incrementAndGet();
    }

}
