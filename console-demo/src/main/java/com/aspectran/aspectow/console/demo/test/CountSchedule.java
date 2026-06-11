package com.aspectran.aspectow.console.demo.test;

import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Job;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Schedule;
import com.aspectran.core.component.bean.annotation.SimpleTrigger;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.context.rule.type.MisfirePolicy;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                startDelaySeconds = 3,      // Start 3 seconds after scheduler starts
                intervalInSeconds = 3,      // Repeat every 3 seconds
                repeatForever = true,       // Repeat indefinitely
                misfirePolicy = MisfirePolicy.SMART_POLICY
        ),
        jobs = {
                @Job(translet = "test/schedule/count.job")
        },
        disabled = true
)
public class CountSchedule {

    private final Logger logger = LoggerFactory.getLogger(CountSchedule.class);

    private final AtomicInteger counter = new AtomicInteger(0);

    private final String nodeId;

    @Autowired
    public CountSchedule(@NonNull NodeManager nodeManager) {
        this.nodeId = nodeManager.getNodeId();
    }

    @Request("test/schedule/count.job")
    @Transform(FormatType.TEXT)
    public String count() {
        int count = counter.incrementAndGet();
        String result = "NodeId: " + nodeId + ", Count: " + count;
        logger.info(result);
        return result;
    }

}
