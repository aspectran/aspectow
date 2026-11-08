/*
 * Copyright (c) 2026-present The Aspectran Project
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
package com.aspectran.aspectow.node.management.components.inspectors;

import com.aspectran.aspectow.node.management.components.ComponentDetails;
import com.aspectran.aspectow.node.management.components.ComponentInspector;
import com.aspectran.aspectow.node.management.components.ComponentUtils;
import com.aspectran.core.component.bean.BeanRegistry;
import com.aspectran.core.component.bean.DefaultBeanRegistry;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.BeanRule;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inspector that extracts specification and configuration details of embedded Web and Network Server components
 * (Netty, Undertow, Jetty).
 *
 * <p>Created: 2026-11-08</p>
 */
public class ServerComponentInspector implements ComponentInspector {

    private static final Logger logger = LoggerFactory.getLogger(ServerComponentInspector.class);

    private static final String NETTY_SERVER_CLASS = "com.aspectran.netty.server.NettyServer";
    private static final String TOW_SERVER_CLASS = "com.aspectran.undertow.server.TowServer";
    private static final String JETTY_SERVER_CLASS = "com.aspectran.jetty.server.JettyServer";
    private static final String JETTY_BASE_SERVER_CLASS = "org.eclipse.jetty.server.Server";

    private static final Set<String> KNOWN_SERVER_BEAN_IDS = Set.of(
            "netty.server", "tow.server", "jetty.server", "server", "webServer"
    );

    @Override
    @NonNull
    public String getId() {
        return "server";
    }

    @Override
    @NonNull
    public String getDisplayName() {
        return "Server";
    }

    @Override
    @NonNull
    public String getCategory() {
        return "Infrastructure";
    }

    @Override
    @NonNull
    public String getIcon() {
        return "bi-hdd-network";
    }

    @Override
    public boolean isAvailable(@NonNull ActivityContext context) {
        BeanRegistry registry = context.getBeanRegistry();
        if (registry == null) {
            return false;
        }

        for (String beanId : KNOWN_SERVER_BEAN_IDS) {
            if (registry.containsLocalBean(beanId)) {
                return true;
            }
        }

        for (String className : List.of(NETTY_SERVER_CLASS, TOW_SERVER_CLASS, JETTY_SERVER_CLASS, JETTY_BASE_SERVER_CLASS)) {
            try {
                Class<?> clazz = Class.forName(className);
                if (registry.containsLocalBean(clazz)) {
                    return true;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            if (!defaultBeanRegistry.getBeanRuleRegistry().findBeanRules(this::isServerBeanRule).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Nullable
    public ComponentDetails inspect(@NonNull ActivityContext context) {
        BeanRegistry registry = context.getBeanRegistry();
        if (registry == null) {
            return null;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        List<Object> inspectedBeans = new ArrayList<>();

        if (registry instanceof DefaultBeanRegistry defaultBeanRegistry) {
            List<BeanRule> serverRules = defaultBeanRegistry.getBeanRuleRegistry().findBeanRules(this::isServerBeanRule);
            for (BeanRule rule : serverRules) {
                try {
                    Object bean = defaultBeanRegistry.getBean(rule);
                    if (bean == null && rule.getId() != null) {
                        bean = defaultBeanRegistry.getBean(rule.getId());
                    }
                    if (bean != null && !containsInstance(inspectedBeans, bean)) {
                        String beanName = ComponentUtils.resolveBeanName(rule, "server");
                        items.add(extractServerInfo(beanName, bean));
                        inspectedBeans.add(bean);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to inspect server bean: {}", rule.getId(), e);
                }
            }
        }

        if (items.isEmpty()) {
            for (String beanId : KNOWN_SERVER_BEAN_IDS) {
                if (registry.containsLocalBean(beanId)) {
                    try {
                        Object bean = registry.getBean(beanId);
                        if (bean != null && !containsInstance(inspectedBeans, bean)) {
                            items.add(extractServerInfo(beanId, bean));
                            inspectedBeans.add(bean);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to inspect server bean by ID: {}", beanId, e);
                    }
                }
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        return new ComponentDetails()
                .setId(getId())
                .setDisplayName(getDisplayName())
                .setCategory(getCategory())
                .setIcon(getIcon())
                .setDescription("Embedded Web and Network Server Specifications and Topologies")
                .setItems(items);
    }

    private boolean isServerBeanRule(BeanRule rule) {
        if (rule == null) {
            return false;
        }
        if (rule.getId() != null && KNOWN_SERVER_BEAN_IDS.contains(rule.getId())) {
            return true;
        }
        Class<?> beanClass = rule.getBeanClass();
        if (beanClass != null) {
            return matchesServerClass(beanClass);
        }
        String className = rule.getClassName();
        if (className != null) {
            return className.contains("NettyServer") ||
                    className.contains("TowServer") ||
                    className.contains("JettyServer") ||
                    className.equals(JETTY_BASE_SERVER_CLASS);
        }
        return false;
    }

    private boolean matchesServerClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        String name = clazz.getName();
        if (name.equals(NETTY_SERVER_CLASS) || name.equals(TOW_SERVER_CLASS) ||
                name.equals(JETTY_SERVER_CLASS) || name.equals(JETTY_BASE_SERVER_CLASS)) {
            return true;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (matchesServerClass(iface)) {
                return true;
            }
        }
        return matchesServerClass(clazz.getSuperclass());
    }

    private boolean containsInstance(@NonNull List<Object> list, Object target) {
        for (Object item : list) {
            if (item == target) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private Map<String, Object> extractServerInfo(@NonNull String beanName, @NonNull Object serverBean) {
        Map<String, Object> info = new LinkedHashMap<>();
        Class<?> clazz = serverBean.getClass();

        info.put("beanName", beanName);
        info.put("className", clazz.getName());
        info.put("simpleClassName", clazz.getSimpleName());

        String serverType = detectServerType(clazz);
        info.put("serverType", serverType);

        // Version extraction
        String version = extractVersion(serverType, clazz);
        if (version != null) {
            info.put("version", version);
        }

        // Common lifecycle options
        Boolean autoStart = ComponentUtils.invokeBoolean(serverBean, "isAutoStart", "getAutoStart");
        if (autoStart != null) {
            info.put("autoStart", autoStart);
        }
        Boolean shutdownGracefully = ComponentUtils.invokeBoolean(serverBean, "isShutdownGracefully", "getShutdownGracefully");
        if (shutdownGracefully != null) {
            info.put("shutdownGracefully", shutdownGracefully);
        }
        Number shutdownTimeoutSecs = ComponentUtils.invokeNumber(serverBean, "getShutdownTimeoutSecs", "getStopTimeout");
        if (shutdownTimeoutSecs != null) {
            info.put("shutdownTimeoutSecs", shutdownTimeoutSecs.intValue());
        }
        Boolean proxyAddressForwarding = ComponentUtils.invokeBoolean(serverBean, "isProxyAddressForwarding", "getProxyAddressForwarding");
        if (proxyAddressForwarding != null) {
            info.put("proxyAddressForwarding", proxyAddressForwarding);
        }

        // Engine-specific inspections
        switch (serverType) {
            case "Netty" -> inspectNettyServer(info, serverBean);
            case "Undertow" -> inspectUndertowServer(info, serverBean);
            case "Jetty" -> inspectJettyServer(info, serverBean);
            default -> inspectGenericServer(info, serverBean);
        }

        return info;
    }

    private @NonNull String detectServerType(@NonNull Class<?> clazz) {
        String name = clazz.getName();
        if (name.contains("Netty") || hasInterfaceOrSuperclass(clazz, NETTY_SERVER_CLASS)) {
            return "Netty";
        }
        if (name.contains("Undertow") || name.contains("Tow") || hasInterfaceOrSuperclass(clazz, TOW_SERVER_CLASS)) {
            return "Undertow";
        }
        if (name.contains("Jetty") || hasInterfaceOrSuperclass(clazz, JETTY_BASE_SERVER_CLASS)) {
            return "Jetty";
        }
        return "Server";
    }

    private boolean hasInterfaceOrSuperclass(Class<?> clazz, String targetClassName) {
        if (clazz == null || Object.class.equals(clazz)) {
            return false;
        }
        if (clazz.getName().equals(targetClassName)) {
            return true;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (hasInterfaceOrSuperclass(iface, targetClassName)) {
                return true;
            }
        }
        return hasInterfaceOrSuperclass(clazz.getSuperclass(), targetClassName);
    }

    @Nullable
    private String extractVersion(String serverType, Class<?> clazz) {
        try {
            switch (serverType) {
                case "Netty" -> {
                    Class<?> nettyServerClass = Class.forName(NETTY_SERVER_CLASS);
                    Method m = nettyServerClass.getMethod("getVersion");
                    return (String) m.invoke(null);
                }
                case "Undertow" -> {
                    Class<?> towServerClass = Class.forName(TOW_SERVER_CLASS);
                    Method m = towServerClass.getMethod("getVersion");
                    return (String) m.invoke(null);
                }
                case "Jetty" -> {
                    Class<?> jettyBase = Class.forName(JETTY_BASE_SERVER_CLASS);
                    Method m = jettyBase.getMethod("getVersion");
                    return (String) m.invoke(null);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    //---------------------------------------------------------//
    // Netty Server Inspection
    //---------------------------------------------------------//

    private void inspectNettyServer(Map<String, Object> info, Object nettyServer) {
        Boolean virtualThreads = ComponentUtils.invokeBoolean(nettyServer, "isVirtualThreads");
        if (virtualThreads != null) {
            info.put("virtualThreads", virtualThreads);
        }
        Boolean nativeTransport = ComponentUtils.invokeBoolean(nettyServer, "isNativeTransport");
        if (nativeTransport != null) {
            info.put("nativeTransport", nativeTransport);
        }
        String threadNamePrefix = ComponentUtils.invokeString(nettyServer, "getThreadNamePrefix");
        if (threadNamePrefix != null) {
            info.put("threadNamePrefix", threadNamePrefix);
        }
        String workerName = ComponentUtils.invokeString(nettyServer, "getWorkerName");
        if (workerName != null) {
            info.put("workerName", workerName);
        }
        Number bossThreads = ComponentUtils.invokeNumber(nettyServer, "getBossThreads");
        if (bossThreads != null) {
            info.put("bossThreads", bossThreads.intValue());
        }
        Number workerThreads = ComponentUtils.invokeNumber(nettyServer, "getWorkerThreads");
        if (workerThreads != null) {
            info.put("workerThreads", workerThreads.intValue());
        }
        Number idleTimeout = ComponentUtils.invokeNumber(nettyServer, "getIdleTimeout");
        if (idleTimeout != null) {
            info.put("idleTimeout", idleTimeout.intValue());
        }
        Number maxContentLength = ComponentUtils.invokeNumber(nettyServer, "getMaxContentLength");
        if (maxContentLength != null) {
            info.put("maxContentLength", maxContentLength.longValue());
            info.put("maxContentLengthFormatted", ComponentUtils.formatBytes(maxContentLength));
        }

        // Listeners
        Object listenersObj = ComponentUtils.invokeObject(nettyServer, "getListeners");
        if (listenersObj != null) {
            List<Map<String, Object>> listenerList = new ArrayList<>();
            for (Object cfg : toList(listenersObj)) {
                if (cfg != null) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", ComponentUtils.invokeString(cfg, "getName"));
                    item.put("host", ComponentUtils.invokeString(cfg, "getHost"));
                    item.put("port", ComponentUtils.invokeNumber(cfg, "getPort"));
                    item.put("ssl", ComponentUtils.invokeBoolean(cfg, "isSsl"));
                    item.put("http2", ComponentUtils.invokeBoolean(cfg, "isHttp2"));
                    item.put("backlog", ComponentUtils.invokeNumber(cfg, "getBacklog"));
                    item.put("tcpNoDelay", ComponentUtils.invokeBoolean(cfg, "isTcpNoDelay"));
                    item.put("keepAlive", ComponentUtils.invokeBoolean(cfg, "isKeepAlive"));
                    listenerList.add(item);
                }
            }
            if (!listenerList.isEmpty()) {
                info.put("listeners", listenerList);
            }
        }

        // Encoding Handler
        Object encodingHandler = ComponentUtils.invokeObject(nettyServer, "getEncodingHandler");
        if (encodingHandler != null) {
            Map<String, Object> encodingInfo = new LinkedHashMap<>();
            Object providers = ComponentUtils.invokeObject(encodingHandler, "getEncodingProviders");
            if (providers != null) {
                encodingInfo.put("providers", toList(providers));
            }
            Object predicates = ComponentUtils.invokeObject(encodingHandler, "getEncodingPredicates");
            if (predicates != null) {
                List<Map<String, Object>> predList = new ArrayList<>();
                for (Object p : toList(predicates)) {
                    if (p != null) {
                        Map<String, Object> predMap = new LinkedHashMap<>();
                        Object mediaTypes = ComponentUtils.invokeObject(p, "getMediaTypes");
                        if (mediaTypes != null) {
                            predMap.put("mediaTypes", toList(mediaTypes));
                        }
                        Number minSize = ComponentUtils.invokeNumber(p, "getContentSizeLargerThan");
                        if (minSize != null && minSize.longValue() > 0) {
                            predMap.put("contentSizeLargerThan", minSize.longValue());
                            predMap.put("contentSizeLargerThanFormatted", ComponentUtils.formatBytes(minSize));
                        }
                        predList.add(predMap);
                    }
                }
                encodingInfo.put("predicates", predList);
            }
            info.put("encoding", encodingInfo);
        }

        // Access Log Handler
        Object accessLogHandler = ComponentUtils.invokeObject(nettyServer, "getAccessLogHandler");
        if (accessLogHandler != null) {
            Map<String, Object> accessLogInfo = new LinkedHashMap<>();
            accessLogInfo.put("formatString", ComponentUtils.invokeString(accessLogHandler, "getFormatString"));
            accessLogInfo.put("category", ComponentUtils.invokeString(accessLogHandler, "getCategory"));
            info.put("accessLog", accessLogInfo);
        }

        // Deployed Contexts
        Object contextsObj = ComponentUtils.invokeObject(nettyServer, "getContexts");
        if (contextsObj != null) {
            List<Map<String, Object>> contextList = new ArrayList<>();
            for (Object ctx : toList(contextsObj)) {
                if (ctx != null) {
                    Map<String, Object> ctxMap = new LinkedHashMap<>();
                    ctxMap.put("contextPath", ComponentUtils.invokeString(ctx, "getContextPath"));
                    ctxMap.put("name", ComponentUtils.invokeString(ctx, "getName"));
                    contextList.add(ctxMap);
                }
            }
            if (!contextList.isEmpty()) {
                info.put("contexts", contextList);
            }
        }
    }

    //---------------------------------------------------------//
    // Undertow Server Inspection
    //---------------------------------------------------------//

    private void inspectUndertowServer(Map<String, Object> info, Object towServer) {
        // Builder or Undertow options extraction
        Object undertowInstance = ComponentUtils.invokeObject(towServer, "getUndertow");

        // Worker options
        Object worker = (undertowInstance != null ? ComponentUtils.invokeObject(undertowInstance, "getWorker") : null);
        if (worker != null) {
            Number ioThreads = ComponentUtils.invokeNumber(worker, "getIoThreadCount");
            if (ioThreads != null) {
                info.put("workerIoThreads", ioThreads.intValue());
            }
            String workerName = ComponentUtils.invokeString(worker, "getName");
            if (workerName != null) {
                info.put("workerName", workerName);
            }
            Number maxThreads = ComponentUtils.invokeNumber(worker, "getMaxPoolSize");
            if (maxThreads != null) {
                info.put("workerTaskMaxThreads", maxThreads.intValue());
            }
        }

        // TowOptions
        Object workerOptions = ComponentUtils.getFieldValue(towServer, "workerOptions");
        if (workerOptions != null) {
            extractTowOptionMap(info, workerOptions);
        }
        Object serverOptions = ComponentUtils.getFieldValue(towServer, "serverOptions");
        if (serverOptions != null) {
            extractTowOptionMap(info, serverOptions);
        }

        // Listeners from Undertow instance
        if (undertowInstance != null) {
            Object listenerInfos = ComponentUtils.invokeObject(undertowInstance, "getListenerInfo");
            if (listenerInfos instanceof Iterable<?> iter) {
                List<Map<String, Object>> listenerList = new ArrayList<>();
                for (Object lInfo : iter) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("protocol", ComponentUtils.invokeString(lInfo, "getProtcol", "getProtocol"));
                    Object addr = ComponentUtils.invokeObject(lInfo, "getAddress");
                    if (addr instanceof InetSocketAddress sockAddr) {
                        item.put("host", sockAddr.getHostString());
                        item.put("port", sockAddr.getPort());
                    } else if (addr != null) {
                        item.put("address", addr.toString());
                    }
                    Boolean ssl = ComponentUtils.invokeBoolean(lInfo, "isSsl");
                    if (ssl != null) {
                        item.put("ssl", ssl);
                    }
                    listenerList.add(item);
                }
                if (!listenerList.isEmpty()) {
                    info.put("listeners", listenerList);
                }
            }
        }

        // Request Handler Factory / Handlers
        Object reqHandlerFactory = ComponentUtils.invokeObject(towServer, "getRequestHandlerFactory");
        if (reqHandlerFactory != null) {
            inspectUndertowHandlerFactory(info, reqHandlerFactory);
        }
    }

    private void extractTowOptionMap(Map<String, Object> info, Object towOptions) {
        Object optionMap = ComponentUtils.invokeObject(towOptions, "getOptionMap");
        if (optionMap instanceof Iterable<?> iter) {
            for (Object option : iter) {
                if (option != null) {
                    String optName = ComponentUtils.invokeString(option, "getName");
                    Object val = ComponentUtils.invokeAny(optionMap, "get");
                    if (optName != null && val != null) {
                        if ("WORKER_IO_THREADS".equalsIgnoreCase(optName) && !info.containsKey("workerIoThreads")) {
                            info.put("workerIoThreads", val);
                        } else if ("WORKER_TASK_MAX_THREADS".equalsIgnoreCase(optName) && !info.containsKey("workerTaskMaxThreads")) {
                            info.put("workerTaskMaxThreads", val);
                        } else if ("WORKER_NAME".equalsIgnoreCase(optName) && !info.containsKey("workerName")) {
                            info.put("workerName", val);
                        } else if ("DECODE_URL".equalsIgnoreCase(optName)) {
                            info.put("decodeUrl", val);
                        } else if ("URL_CHARSET".equalsIgnoreCase(optName)) {
                            info.put("urlCharset", val);
                        }
                    }
                }
            }
        }
    }

    private void inspectUndertowHandlerFactory(Map<String, Object> info, Object reqHandlerFactory) {
        Object wrappers = ComponentUtils.invokeObject(reqHandlerFactory, "getHandlerChainWrappers");
        if (wrappers != null) {
            List<Map<String, Object>> wrapperList = new ArrayList<>();
            for (Object w : toList(wrappers)) {
                if (w != null) {
                    Map<String, Object> wMap = new LinkedHashMap<>();
                    Class<?> wClass = w.getClass();
                    wMap.put("className", wClass.getName());
                    wMap.put("simpleClassName", wClass.getSimpleName());

                    // Encoding wrapper
                    if (wClass.getSimpleName().contains("Encoding")) {
                        Object providers = ComponentUtils.invokeObject(w, "getEncodingProviders");
                        if (providers != null) {
                            wMap.put("providers", toList(providers));
                        }
                        info.put("encodingWrapper", wMap);
                    }
                    // Access log wrapper
                    if (wClass.getSimpleName().contains("AccessLog")) {
                        wMap.put("formatString", ComponentUtils.invokeString(w, "getFormatString"));
                        wMap.put("category", ComponentUtils.invokeString(w, "getCategory"));
                        info.put("accessLogWrapper", wMap);
                    }
                    // Logging group wrapper
                    if (wClass.getSimpleName().contains("LoggingGroup")) {
                        Object patterns = ComponentUtils.invokeObject(w, "getPathPatternsByGroupName");
                        if (patterns instanceof Map<?, ?> pMap) {
                            wMap.put("pathPatterns", pMap);
                        }
                        info.put("loggingGroupWrapper", wMap);
                    }
                    wrapperList.add(wMap);
                }
            }
            if (!wrapperList.isEmpty()) {
                info.put("handlerWrappers", wrapperList);
            }
        }

        // Deployed Contexts from ServletContainer
        Object container = ComponentUtils.invokeObject(reqHandlerFactory, "getServletContainer");
        if (container != null) {
            Object deployments = ComponentUtils.invokeObject(container, "listDeployments");
            if (deployments instanceof Collection<?> coll) {
                List<Map<String, Object>> ctxList = new ArrayList<>();
                for (Object depName : coll) {
                    if (depName != null) {
                        Map<String, Object> cMap = new LinkedHashMap<>();
                        cMap.put("name", depName.toString());
                        Object depManager = ComponentUtils.invokeAny(container, "getDeployment", "getDeploymentByPath");
                        if (depManager != null) {
                            Object dep = ComponentUtils.invokeObject(depManager, "getDeployment");
                            if (dep != null) {
                                Object depInfo = ComponentUtils.invokeObject(dep, "getDeploymentInfo");
                                if (depInfo != null) {
                                    cMap.put("contextPath", ComponentUtils.invokeString(depInfo, "getContextPath"));
                                    cMap.put("displayName", ComponentUtils.invokeString(depInfo, "getDisplayName"));
                                }
                            }
                        }
                        ctxList.add(cMap);
                    }
                }
                if (!ctxList.isEmpty()) {
                    info.put("contexts", ctxList);
                }
            }
        }
    }

    //---------------------------------------------------------//
    // Jetty Server Inspection
    //---------------------------------------------------------//

    private void inspectJettyServer(Map<String, Object> info, Object jettyServer) {
        // Thread Pool
        Object threadPool = ComponentUtils.invokeObject(jettyServer, "getThreadPool");
        if (threadPool != null) {
            Map<String, Object> tpInfo = new LinkedHashMap<>();
            Class<?> tpClass = threadPool.getClass();
            tpInfo.put("className", tpClass.getName());
            tpInfo.put("simpleClassName", tpClass.getSimpleName());
            tpInfo.put("minThreads", ComponentUtils.invokeNumber(threadPool, "getMinThreads"));
            tpInfo.put("maxThreads", ComponentUtils.invokeNumber(threadPool, "getMaxThreads"));
            Boolean virtualThreads = ComponentUtils.invokeBoolean(threadPool, "isVirtualThreadsAvailable");
            if (virtualThreads != null) {
                tpInfo.put("virtualThreads", virtualThreads);
                info.put("virtualThreads", virtualThreads);
            }
            info.put("threadPool", tpInfo);
        }

        // Max connections
        Number maxConnections = ComponentUtils.invokeNumber(jettyServer, "getMaxConnections");
        if (maxConnections != null) {
            info.put("maxConnections", maxConnections.intValue());
        }

        // Connectors
        Object connectors = ComponentUtils.invokeObject(jettyServer, "getConnectors");
        if (connectors != null) {
            List<Map<String, Object>> connectorList = new ArrayList<>();
            for (Object conn : toList(connectors)) {
                if (conn != null) {
                    Map<String, Object> cMap = new LinkedHashMap<>();
                    cMap.put("name", ComponentUtils.invokeString(conn, "getName"));
                    cMap.put("host", ComponentUtils.invokeString(conn, "getHost"));
                    cMap.put("port", ComponentUtils.invokeNumber(conn, "getPort"));
                    Object protocols = ComponentUtils.invokeObject(conn, "getProtocols");
                    if (protocols != null) {
                        cMap.put("protocols", toList(protocols));
                    }
                    connectorList.add(cMap);
                }
            }
            if (!connectorList.isEmpty()) {
                info.put("listeners", connectorList);
            }
        }

        // Handler Hierarchy (Gzip, Logging, Contexts)
        Object rootHandler = ComponentUtils.invokeObject(jettyServer, "getHandler");
        if (rootHandler != null) {
            inspectJettyHandler(info, rootHandler);
        }

        // Request Log
        Object requestLog = ComponentUtils.invokeObject(jettyServer, "getRequestLog");
        if (requestLog != null) {
            Map<String, Object> rLog = new LinkedHashMap<>();
            rLog.put("formatString", ComponentUtils.invokeString(requestLog, "getFormatString"));
            rLog.put("className", requestLog.getClass().getName());
            info.put("accessLog", rLog);
        }
    }

    private void inspectJettyHandler(Map<String, Object> info, Object handler) {
        if (handler == null) {
            return;
        }
        String simpleName = handler.getClass().getSimpleName();

        // Gzip handler
        if (simpleName.contains("Gzip")) {
            Map<String, Object> gzip = new LinkedHashMap<>();
            Number minSize = ComponentUtils.invokeNumber(handler, "getMinGzipSize");
            if (minSize != null) {
                gzip.put("minGzipSize", minSize.longValue());
                gzip.put("minGzipSizeFormatted", ComponentUtils.formatBytes(minSize));
            }
            Object incMime = ComponentUtils.invokeObject(handler, "getIncludedMimeTypes");
            if (incMime != null) {
                gzip.put("includedMimeTypes", toList(incMime));
            }
            info.put("gzipHandler", gzip);
        }

        // PathBasedLoggingGroupHandler
        if (simpleName.contains("LoggingGroup")) {
            Object map = ComponentUtils.invokeObject(handler, "getPathPatternsByGroupName");
            if (map instanceof Map<?, ?> pMap) {
                info.put("loggingGroupPatterns", pMap);
            }
        }

        // Context Handler Collection
        if (simpleName.contains("ContextHandlerCollection")) {
            Object handlers = ComponentUtils.invokeObject(handler, "getHandlers");
            if (handlers != null) {
                List<Map<String, Object>> ctxList = new ArrayList<>();
                for (Object h : toList(handlers)) {
                    if (h != null) {
                        Map<String, Object> cMap = new LinkedHashMap<>();
                        cMap.put("contextPath", ComponentUtils.invokeString(h, "getContextPath"));
                        cMap.put("displayName", ComponentUtils.invokeString(h, "getDisplayName"));
                        ctxList.add(cMap);
                    }
                }
                if (!ctxList.isEmpty()) {
                    info.put("contexts", ctxList);
                }
            }
        }

        // Traverse wrapped handler if present
        Object nestedHandler = ComponentUtils.invokeObject(handler, "getHandler");
        if (nestedHandler != null && nestedHandler != handler) {
            inspectJettyHandler(info, nestedHandler);
        }
    }

    //---------------------------------------------------------//
    // Generic Server Inspection
    //---------------------------------------------------------//

    private void inspectGenericServer(Map<String, Object> info, Object serverBean) {
        Number port = ComponentUtils.invokeNumber(serverBean, "getPort");
        if (port != null) {
            info.put("port", port.intValue());
        }
        String host = ComponentUtils.invokeString(serverBean, "getHost");
        if (host != null) {
            info.put("host", host);
        }
    }

    private @NonNull List<?> toList(Object obj) {
        if (obj == null) {
            return List.of();
        }
        if (obj instanceof Collection<?> coll) {
            return new ArrayList<>(coll);
        }
        if (obj instanceof Iterable<?> iter) {
            List<Object> list = new ArrayList<>();
            for (Object item : iter) {
                list.add(item);
            }
            return list;
        }
        if (obj.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                list.add(Array.get(obj, i));
            }
            return list;
        }
        return List.of(obj);
    }

}
