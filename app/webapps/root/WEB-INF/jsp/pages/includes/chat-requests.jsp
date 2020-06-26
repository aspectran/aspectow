<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://aspectran.com/tags" prefix="aspectran" %>
<div class="chat-requests t15">
    <div class="confirm-request template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell">
                <p class="title"><aspectran:message code='chat.stranger.confirm_request'/></p>
            </div>
            <div class="cell align-self-bottom text-right">
                <button type="button" class="button small alert cancel"><aspectran:message code="common.button.cancel"/></button>
                <button type="button" class="button small success ok"><aspectran:message code="common.button.ok"/></button>
            </div>
        </div>
    </div>
    <div class="request active template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell">
                <p class="title"><aspectran:message code='chat.stranger.request'/></p>
                <p><i class="icon-heart"></i> <aspectran:message code='chat.stranger.time_left'/>: <span class="remains">35</span> <aspectran:message code='chat.stranger.time_left_secs'/></p>
                <p class="done"><aspectran:message code='chat.stranger.request_done'/></p>
            </div>
            <div class="cell align-self-bottom text-right">
                <button type="button" class="button small alert cancel"><aspectran:message code="common.button.cancel"/></button>
            </div>
        </div>
    </div>
    <div class="request-received active template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell">
                <p class="title"><aspectran:message code='chat.stranger.request_received'/></p>
                <p><aspectran:message code='chat.stranger.request_received_1'/></p>
                <p><i class="icon-heart"></i> <aspectran:message code='chat.stranger.time_left'/>: <span class="remains">30</span> <aspectran:message code='chat.stranger.time_left_secs'/></p>
                <p class="done"><aspectran:message code='chat.stranger.request_done'/></p>
            </div>
            <div class="cell align-self-bottom text-right">
                <button type="button" class="button small alert decline"><aspectran:message code='common.button.decline'/></button>
                <button type="button" class="button small success accept"><aspectran:message code='common.button.accept'/></button>
            </div>
        </div>
    </div>
    <div class="canceled-request template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell auto">
                <p class="title"><aspectran:message code='chat.stranger.canceled_request'/></p>
            </div>
        </div>
    </div>
    <div class="request-canceled template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell auto">
                <p class="title"><aspectran:message code='chat.stranger.request_canceled'/></p>
            </div>
        </div>
    </div>
    <div class="declined-request template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell auto">
                <p class="title"><aspectran:message code='chat.stranger.declined_request'/></p>
            </div>
        </div>
    </div>
    <div class="request-declined template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell auto">
                <p class="title"><aspectran:message code='chat.stranger.request_declined'/></p>
            </div>
        </div>
    </div>
    <div class="request-timeout template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell auto">
                <p class="title"><aspectran:message code='chat.stranger.request_timeout'/></p>
            </div>
        </div>
    </div>
    <div class="exceeded-requests template grid-x grid-padding-y">
        <div class="cell grid-x grid-padding-x medium-up-1 large-up-2">
            <div class="cell auto">
                <p class="title"><aspectran:message code='chat.stranger.exceeded_requests'/></p>
            </div>
        </div>
    </div>
</div>