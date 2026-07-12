package com.aspectran.aspectow.appmon.engine.persist.db.mapper;

import com.aspectran.aspectow.appmon.engine.persist.counter.EventCountVO;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Profile;
import com.aspectran.core.component.bean.annotation.Qualifier;
import com.aspectran.mybatis.SqlMapperAccess;
import com.aspectran.mybatis.SqlMapperProvider;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access Object (DAO) for {@link EventCountMapper}.
 * Provides a convenient way to access the mapper methods using Aspectran's bean container.
 */
@Component
@Bean("appmon.eventCountDao")
@Profile("!appmon.ext-persistence")
public class EventCountDao extends SqlMapperAccess<EventCountMapper> implements EventCountMapper {

    /**
     * Constructs a new Dao.
     * @param sqlMapperProvider the SQL mapper provider
     */
    @Autowired
    public EventCountDao(@Qualifier("appMonSqlMapperProvider") SqlMapperProvider sqlMapperProvider) {
        super(sqlMapperProvider, EventCountMapper.class);
    }

    @Override
    public EventCountVO getLastEventCount(String nodeId, String appId, String eventId) {
        return mapper().getLastEventCount(nodeId, appId, eventId);
    }

    @Override
    public void updateLastEventCount(EventCountVO eventCountVO) {
        mapper().updateLastEventCount(eventCountVO);
    }

    @Override
    public void insertEventCount(EventCountVO eventCountVO) {
        mapper().insertEventCount(eventCountVO);
    }

    @Override
    public void insertEventCountHourly(EventCountVO eventCountVO) {
        mapper().insertEventCountHourly(eventCountVO);
    }

    @Override
    public List<EventCountVO> getChartData(String nodeId, String appId, String eventId, LocalDateTime dateOffset) {
        return mapper().getChartData(nodeId, appId, eventId, dateOffset);
    }

    @Override
    public List<EventCountVO> getGroupChartData(String groupId, String appId, String eventId, LocalDateTime dateOffset) {
        return mapper().getGroupChartData(groupId, appId, eventId, dateOffset);
    }

    @Override
    public List<EventCountVO> getChartDataByHour(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return mapper().getChartDataByHour(nodeId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getGroupChartDataByHour(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return mapper().getGroupChartDataByHour(groupId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getChartDataByDay(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return mapper().getChartDataByDay(nodeId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getGroupChartDataByDay(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return mapper().getGroupChartDataByDay(groupId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getChartDataByMonth(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return mapper().getChartDataByMonth(nodeId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getGroupChartDataByMonth(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return mapper().getGroupChartDataByMonth(groupId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getChartDataByYear(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return mapper().getChartDataByYear(nodeId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getGroupChartDataByYear(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return mapper().getGroupChartDataByYear(groupId, appId, eventId, zoneOffset, dateOffset);
    }

}
