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
package com.aspectran.aspectow.appmon.engine.persist.db.mapper;

import com.aspectran.aspectow.appmon.engine.persist.counter.EventCountVO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The MyBatis mapper interface for event count data.
 * Defines methods for CRUD operations on event count records in the database.
 */
@Mapper
public interface EventCountMapper {

    /**
     * Retrieves the last recorded event count for the specified node, instance, and event.
     * @param nodeId the node identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @return the last event count VO, or null if not found
     */
    EventCountVO getLastEventCount(String nodeId, String appId, String eventId);

    /**
     * Updates the last recorded event count.
     * @param eventCountVO the event count data to update
     */
    void updateLastEventCount(EventCountVO eventCountVO);

    /**
     * Inserts a new raw event count record.
     * @param eventCountVO the event count data to insert
     */
    void insertEventCount(EventCountVO eventCountVO);

    /**
     * Inserts an hourly aggregated event count record.
     * @param eventCountVO the event count data to insert
     */
    void insertEventCountHourly(EventCountVO eventCountVO);

    /**
     * Retrieves raw chart data for the specified criteria.
     * @param nodeId the node identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records
     */
    List<EventCountVO> getChartData(String nodeId, String appId, String eventId, LocalDateTime dateOffset);

    /**
     * Retrieves raw chart data for the specified group and other criteria.
     * @param groupId the node group identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by group
     */
    List<EventCountVO> getGroupChartData(String groupId, String appId, String eventId, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by hour for the specified criteria.
     * @param nodeId the node identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by hour
     */
    List<EventCountVO> getChartDataByHour(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by hour for the specified group and other criteria.
     * @param groupId the node group identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by hour and group
     */
    List<EventCountVO> getGroupChartDataByHour(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by day for the specified criteria.
     * @param nodeId the node identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by day
     */
    List<EventCountVO> getChartDataByDay(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by day for the specified group and other criteria.
     * @param groupId the node group identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by day and group
     */
    List<EventCountVO> getGroupChartDataByDay(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by month for the specified criteria.
     * @param nodeId the node identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by month
     */
    List<EventCountVO> getChartDataByMonth(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by month for the specified group and other criteria.
     * @param groupId the node group identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by month and group
     */
    List<EventCountVO> getGroupChartDataByMonth(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by year for the specified criteria.
     * @param nodeId the node identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by year
     */
    List<EventCountVO> getChartDataByYear(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset);

    /**
     * Retrieves chart data aggregated by year for the specified group and other criteria.
     * @param groupId the node group identifier
     * @param appId the app identifier
     * @param eventId the event identifier
     * @param zoneOffset the time zone offset in seconds
     * @param dateOffset the start date/time for fetching data
     * @return a list of event count records aggregated by year and group
     */
    List<EventCountVO> getGroupChartDataByYear(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset);

}
