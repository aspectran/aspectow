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

/**
 * Aspectow Console Notification & Web Push Client
 */
const NotificationClient = (function () {
    let vapidPublicKey = null;
    let currentSetting = null;
    let userPermissions = null;
    let swRegistration = null;
    let webPushEnabled = true;

    function getContextPath() {
        if (typeof CONTEXT_PATH !== 'undefined' && CONTEXT_PATH !== null) {
            return CONTEXT_PATH;
        }
        if (typeof window.CONTEXT_PATH !== 'undefined' && window.CONTEXT_PATH !== null) {
            return window.CONTEXT_PATH;
        }
        return '';
    }

    function urlB64ToUint8Array(base64String) {
        const padding = '='.repeat((4 - base64String.length % 4) % 4);
        const base64 = (base64String + padding)
            .replace(/\-/g, '+')
            .replace(/_/g, '/');
        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);
        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }

    function updateBadge(count) {
        const badge = document.getElementById('notif-badge');
        if (!badge) return;
        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.classList.remove('d-none');
        } else {
            badge.textContent = '0';
            badge.classList.add('d-none');
        }
    }

    function refreshUnreadCount() {
        const ctx = getContextPath();
        fetch(ctx + '/notification/count')
            .then(res => res.json())
            .then(res => {
                if (res && res.data && typeof res.data.unreadCount !== 'undefined') {
                    updateBadge(res.data.unreadCount);
                }
            })
            .catch(() => {});
    }

    function animateBell() {
        const bellIcon = document.querySelector('#notif-btn i, [data-bs-target="#notificationModal"] i');
        if (bellIcon) {
            bellIcon.classList.add('text-danger');
            setTimeout(() => bellIcon.classList.remove('text-danger'), 1500);
        }
    }

    async function init() {
        const notifBtn = document.getElementById('notif-btn');
        if (!notifBtn) {
            // Notification feature is not enabled or rendered on this page
            return;
        }

        if ('serviceWorker' in navigator) {
            try {
                swRegistration = await navigator.serviceWorker.ready;
            } catch (e) {
                console.warn('[NotificationClient] Service worker not ready:', e);
            }

            navigator.serviceWorker.addEventListener('message', (event) => {
                if (event.data && event.data.type === 'PUSH_NOTIFICATION_RECEIVED') {
                    console.log('[NotificationClient] Realtime push notification received:', event.data);
                    refreshUnreadCount();
                    animateBell();
                    const modal = document.getElementById('notificationModal');
                    if (modal && modal.classList.contains('show')) {
                        loadHistory();
                    }
                }
            });
        }

        // Tab focus / visibility change
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') {
                refreshUnreadCount();
            }
        });

        // Periodic background sync every 30 seconds
        //setInterval(refreshUnreadCount, 30000);

        loadConfig();
        bindEvents();
    }

    function loadConfig() {
        const ctx = getContextPath();
        fetch(ctx + '/notification/config')
            .then(res => res.json())
            .then(res => {
                if (res && res.data) {
                    if (res.data.enabled === false) {
                        const notifBtn = document.getElementById('notif-btn');
                        if (notifBtn) notifBtn.classList.add('d-none');
                        return;
                    }
                    webPushEnabled = (res.data.webPushEnabled !== false);
                    vapidPublicKey = res.data.vapidPublicKey;
                    currentSetting = res.data.setting;
                    if (res.data.permissions && Array.isArray(res.data.permissions)) {
                        userPermissions = new Set(res.data.permissions);
                    }
                    updateBadge(res.data.unreadCount || 0);
                    populateSettingsForm(currentSetting);
                }
            })
            .catch(err => {
                console.warn('[NotificationClient] Failed to load notification config:', err);
            });
    }

    function hasPermission(requiredPermission) {
        return (userPermissions && userPermissions.has(requiredPermission));
    }

    function populateSettingsForm(setting) {
        if (setting) {
            const cpuInput = document.getElementById('notif-cpu-threshold');
            const memInput = document.getElementById('notif-mem-threshold');
            const nodeChk = document.getElementById('notif-node-status');
            const schedChk = document.getElementById('notif-sched-fail');
            const buildChk = document.getElementById('notif-build-result');
            const secChk = document.getElementById('notif-security-alert');
            const cmdChk = document.getElementById('notif-cmd-finish');

            if (cpuInput) cpuInput.value = setting.cpuThreshold || 85;
            if (memInput) memInput.value = setting.memoryThreshold || 85;
            if (nodeChk) nodeChk.checked = setting.notifyNodeStatus === 'Y';
            if (schedChk) schedChk.checked = setting.notifySchedulerFail === 'Y';
            if (buildChk) buildChk.checked = setting.notifyBuildResult === 'Y';
            if (secChk) secChk.checked = setting.notifySecurityAlert === 'Y';
            if (cmdChk) cmdChk.checked = setting.notifyCommandFinish === 'Y';
        }

        applyPermissionFilters();
        checkPushSubscriptionState();
    }

    function applyPermissionFilters() {
        // Monitoring alert thresholds section
        const thresholdsSec = document.getElementById('notif-thresholds-sec');
        const canViewMonitoring = hasPermission('MONITOR_VIEW') || hasPermission('MONITOR_CONTROL');
        if (thresholdsSec) {
            if (canViewMonitoring) {
                thresholdsSec.classList.remove('d-none');
            } else {
                thresholdsSec.classList.add('d-none');
            }
        }

        // Notification event source items
        const eventItems = [
            { id: 'notif-item-node-status', chkId: 'notif-node-status', perm: 'MONITOR_VIEW' },
            { id: 'notif-item-sched-fail', chkId: 'notif-sched-fail', perm: 'NODE_MANAGE' },
            { id: 'notif-item-build-result', chkId: 'notif-build-result', perm: 'BUILD_VIEW' },
            { id: 'notif-item-security-alert', chkId: 'notif-security-alert', perm: 'USER_MANAGE' },
            { id: 'notif-item-cmd-finish', chkId: 'notif-cmd-finish', perm: 'COMMAND_EXECUTE' }
        ];

        let visibleCount = 0;
        eventItems.forEach(item => {
            const container = document.getElementById(item.id);
            const chk = document.getElementById(item.chkId);
            const allowed = hasPermission(item.perm);

            if (container) {
                if (allowed) {
                    container.classList.remove('d-none');
                    visibleCount++;
                } else {
                    container.classList.add('d-none');
                    if (chk) {
                        chk.checked = false;
                    }
                }
            }
        });

        // Notice when no event sources are available for this user's role
        const noEventsNotice = document.getElementById('notif-no-events-notice');
        if (noEventsNotice) {
            if (visibleCount === 0) {
                noEventsNotice.classList.remove('d-none');
            } else {
                noEventsNotice.classList.add('d-none');
            }
        }
    }

    async function checkPushSubscriptionState() {
        const toggle = document.getElementById('notif-push-toggle');
        const testBtn = document.getElementById('notif-test-btn');
        const permNotice = document.getElementById('notif-perm-notice');

        if (!webPushEnabled) {
            if (toggle) {
                toggle.checked = false;
                toggle.disabled = true;
            }
            if (testBtn) testBtn.disabled = true;
            if (permNotice) {
                permNotice.textContent = 'Web Push is disabled by corporate policy or firewall configuration. In-app notifications remain active.';
                permNotice.className = 'small text-warning mt-1';
                permNotice.classList.remove('d-none');
            }
            return;
        }

        if (!('Notification' in window) || !('PushManager' in window)) {
            if (toggle) toggle.disabled = true;
            if (permNotice) {
                permNotice.textContent = 'Web Push notifications are not supported by this browser.';
                permNotice.className = 'small text-danger mt-1';
                permNotice.classList.remove('d-none');
            }
            return;
        }

        if (Notification.permission === 'denied') {
            if (toggle) {
                toggle.checked = false;
                toggle.disabled = true;
            }
            if (testBtn) testBtn.disabled = true;
            if (permNotice) {
                permNotice.textContent = 'Browser notification permission is blocked. Please allow notifications in site settings.';
                permNotice.className = 'small text-danger mt-1';
                permNotice.classList.remove('d-none');
            }
            return;
        }

        if (swRegistration) {
            const sub = await swRegistration.pushManager.getSubscription();
            if (sub) {
                // Verify if existing subscription matches current server VAPID public key
                if (vapidPublicKey && sub.options && sub.options.applicationServerKey) {
                    const currentKeyBytes = new Uint8Array(sub.options.applicationServerKey);
                    const expectedKeyBytes = urlB64ToUint8Array(vapidPublicKey);
                    let mismatch = (currentKeyBytes.length !== expectedKeyBytes.length);
                    if (!mismatch) {
                        for (let i = 0; i < currentKeyBytes.length; i++) {
                            if (currentKeyBytes[i] !== expectedKeyBytes[i]) {
                                mismatch = true;
                                break;
                            }
                        }
                    }
                    if (mismatch) {
                        console.warn('[NotificationClient] VAPID public key mismatch detected. Re-subscribing with updated key...');
                        try {
                            const oldEndpoint = sub.endpoint;
                            await sub.unsubscribe();
                            if (oldEndpoint) {
                                const formData = new URLSearchParams();
                                formData.append('endpoint', oldEndpoint);
                                const ctx = getContextPath();
                                await fetch(ctx + '/notification/unsubscribe', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                    body: formData
                                }).catch(() => {});
                            }
                            await handlePushToggle(true);
                            return;
                        } catch (reSubErr) {
                            console.error('[NotificationClient] Failed to re-subscribe after VAPID key mismatch:', reSubErr);
                        }
                    }
                }

                if (toggle) toggle.checked = true;
                if (testBtn) testBtn.disabled = false;
                if (permNotice) {
                    permNotice.textContent = 'Web Push notifications are active on this device.';
                    permNotice.className = 'small text-success mt-1';
                    permNotice.classList.remove('d-none');
                }
            } else {
                // If browser permission is already granted and push was explicitly enabled in user settings, auto-subscribe silently
                if (Notification.permission === 'granted' && currentSetting && currentSetting.pushEnabled === 'Y' && vapidPublicKey) {
                    console.log('[NotificationClient] User previously enabled push and permission is granted. Re-subscribing push endpoint...');
                    await handlePushToggle(true, true);
                    return;
                }

                if (toggle) toggle.checked = false;
                if (testBtn) testBtn.disabled = true;
                if (permNotice) permNotice.classList.add('d-none');
            }
        }
    }

    async function handlePushToggle(enabled, silent = false) {
        if (!webPushEnabled) {
            if (!silent) alert('Web Push is disabled by corporate policy or firewall configuration.');
            checkPushSubscriptionState();
            return;
        }

        const testBtn = document.getElementById('notif-test-btn');
        const permNotice = document.getElementById('notif-perm-notice');

        if (!swRegistration) {
            swRegistration = await navigator.serviceWorker.ready;
        }

        if (enabled) {
            let permission = Notification.permission;
            if (permission !== 'granted') {
                if (silent) {
                    return;
                }
                permission = await Notification.requestPermission();
                if (permission !== 'granted') {
                    checkPushSubscriptionState();
                    return;
                }
            }

            if (!vapidPublicKey) {
                if (!silent) alert('Failed to retrieve VAPID Public Key.');
                return;
            }

            try {
                const sub = await swRegistration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlB64ToUint8Array(vapidPublicKey)
                });

                const rawKey = sub.getKey ? sub.getKey('p256dh') : null;
                const rawAuth = sub.getKey ? sub.getKey('auth') : null;
                const p256dhKey = rawKey ? btoa(String.fromCharCode.apply(null, new Uint8Array(rawKey))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '') : '';
                const authKey = rawAuth ? btoa(String.fromCharCode.apply(null, new Uint8Array(rawAuth))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '') : '';

                const formData = new URLSearchParams();
                formData.append('endpoint', sub.endpoint);
                formData.append('p256dhKey', p256dhKey);
                formData.append('authKey', authKey);
                formData.append('userAgent', navigator.userAgent);

                const ctx = getContextPath();
                await fetch(ctx + '/notification/subscribe', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: formData
                });

                const toggle = document.getElementById('notif-push-toggle');
                if (toggle) toggle.checked = true;
                if (currentSetting) currentSetting.pushEnabled = 'Y';
                if (testBtn) testBtn.disabled = false;
                if (permNotice) {
                    permNotice.textContent = 'Web Push notifications have been activated on this device.';
                    permNotice.className = 'small text-success mt-1';
                    permNotice.classList.remove('d-none');
                }
            } catch (err) {
                console.error('[NotificationClient] Failed to subscribe to push:', err);
                if (!silent) alert('Error subscribing to push notifications: ' + err.message);
                checkPushSubscriptionState();
            }
        } else {
            try {
                const sub = await swRegistration.pushManager.getSubscription();
                if (sub) {
                    const endpoint = sub.endpoint;
                    await sub.unsubscribe();

                    const formData = new URLSearchParams();
                    formData.append('endpoint', endpoint);
                    const ctx = getContextPath();
                    await fetch(ctx + '/notification/unsubscribe', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: formData
                    });
                }
                const toggle = document.getElementById('notif-push-toggle');
                if (toggle) toggle.checked = false;
                if (currentSetting) currentSetting.pushEnabled = 'N';
                if (testBtn) testBtn.disabled = true;
                if (permNotice) permNotice.classList.add('d-none');
            } catch (err) {
                console.error('[NotificationClient] Failed to unsubscribe from push:', err);
            }
        }
    }

    function saveSettings() {
        const form = document.getElementById('notif-settings-form');
        if (!form) return;

        const formData = new URLSearchParams();
        formData.append('pushEnabled', document.getElementById('notif-push-toggle').checked ? 'Y' : 'N');
        formData.append('cpuThreshold', document.getElementById('notif-cpu-threshold').value);
        formData.append('memoryThreshold', document.getElementById('notif-mem-threshold').value);
        formData.append('notifyNodeStatus', document.getElementById('notif-node-status').checked ? 'Y' : 'N');
        formData.append('notifySchedulerFail', document.getElementById('notif-sched-fail').checked ? 'Y' : 'N');
        formData.append('notifyBuildResult', document.getElementById('notif-build-result').checked ? 'Y' : 'N');
        formData.append('notifySecurityAlert', document.getElementById('notif-security-alert').checked ? 'Y' : 'N');
        formData.append('notifyCommandFinish', document.getElementById('notif-cmd-finish').checked ? 'Y' : 'N');

        const ctx = getContextPath();
        fetch(ctx + '/notification/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        })
        .then(res => res.json())
        .then(res => {
            const saveBtn = document.getElementById('notif-save-btn');
            if (saveBtn) {
                const originHtml = saveBtn.innerHTML;
                saveBtn.innerHTML = '<i class="bi bi-check-lg"></i> Saved';
                saveBtn.classList.replace('btn-primary', 'btn-success');
                setTimeout(() => {
                    saveBtn.innerHTML = originHtml;
                    saveBtn.classList.replace('btn-success', 'btn-primary');
                }, 2000);
            }
        })
        .catch(err => {
            alert('Failed to save settings: ' + err.message);
        });
    }

    function loadHistory() {
        const listContainer = document.getElementById('notif-history-list');
        const emptyNotice = document.getElementById('notif-history-empty');
        if (!listContainer) return;

        listContainer.innerHTML = '<div class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm me-2"></div>Loading notifications...</div>';

        const ctx = getContextPath();
        fetch(ctx + '/notification/history?limit=40')
            .then(res => res.json())
            .then(res => {
                const list = (res && res.data && res.data.history) || [];
                const unreadCount = (res && res.data) ? res.data.unreadCount : 0;
                updateBadge(unreadCount);

                if (list.length === 0) {
                    listContainer.innerHTML = '';
                    if (emptyNotice) emptyNotice.classList.remove('d-none');
                    return;
                }

                if (emptyNotice) emptyNotice.classList.add('d-none');
                listContainer.innerHTML = '';

                list.forEach(item => {
                    const card = createNotificationCard(item);
                    listContainer.appendChild(card);
                });
            })
            .catch(err => {
                listContainer.innerHTML = '<div class="text-center py-4 text-danger">Failed to load notifications.</div>';
            });
    }

    function createNotificationCard(item) {
        const isUnread = (item.readYn === 'N');
        const div = document.createElement('div');
        div.className = 'list-group-item list-group-item-action p-3 notif-item ' + (isUnread ? 'bg-primary-subtle bg-opacity-10' : '');
        div.style.cursor = 'pointer';

        let iconHtml = '<i class="bi bi-info-circle-fill text-primary fs-5"></i>';
        if (item.severity === 'CRITICAL') {
            iconHtml = '<i class="bi bi-exclamation-octagon-fill text-danger fs-5"></i>';
        } else if (item.severity === 'WARN') {
            iconHtml = '<i class="bi bi-exclamation-triangle-fill text-warning fs-5"></i>';
        } else if (item.severity === 'RESOLVED') {
            iconHtml = '<i class="bi bi-check-circle-fill text-success fs-5"></i>';
        }

        const timeStr = typeof formatDateTime === 'function' ? formatDateTime(item.createdAt) : item.createdAt;

        div.innerHTML = `
            <div class="d-flex align-items-start gap-3">
                <div class="flex-shrink-0 mt-1">${iconHtml}</div>
                <div class="flex-grow-1 overflow-hidden">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <div class="d-flex align-items-center gap-1">
                            <span class="badge bg-secondary-subtle text-secondary-emphasis small">${escapeHtml(item.category)}</span>
                            ${isUnread ? '<span class="badge rounded-pill bg-primary" style="font-size: 0.6rem;">NEW</span>' : ''}
                        </div>
                        <div class="d-flex align-items-center gap-1">
                            <small class="text-muted" style="font-size: 0.75rem;">${escapeHtml(timeStr)}</small>
                            ${isUnread ? '<span class="bg-primary rounded-circle ms-1" style="width: 7px; height: 7px; display: inline-block;" title="Unread"></span>' : ''}
                        </div>
                    </div>
                    <div class="text-truncate mb-1 ${isUnread ? 'fw-bold text-body' : 'text-body-secondary'}" style="font-size: 0.9rem;">${escapeHtml(item.title)}</div>
                    <div class="small ${isUnread ? 'text-body-secondary' : 'text-muted'} text-break" style="font-size: 0.8rem; line-height: 1.4;">${escapeHtml(item.message)}</div>
                </div>
            </div>
        `;

        div.addEventListener('click', () => {
            markRead(item.notificationId, item.targetUrl);
        });

        return div;
    }

    function markRead(notificationId, targetUrl) {
        const formData = new URLSearchParams();
        if (notificationId) {
            formData.append('notificationId', notificationId);
        }

        const ctx = getContextPath();
        fetch(ctx + '/notification/read', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        })
        .then(res => res.json())
        .then(res => {
            if (res && res.data) {
                updateBadge(res.data.unreadCount || 0);
            }
            if (targetUrl && targetUrl !== '/') {
                location.href = ctx + targetUrl;
            } else if (notificationId) {
                loadHistory();
            }
        });
    }

    function markAllRead() {
        markRead(null, null);
        setTimeout(loadHistory, 300);
    }

    function clearHistory() {
        if (!confirm('Are you sure you want to clear all notification history?')) return;

        const ctx = getContextPath();
        fetch(ctx + '/notification/clear', { method: 'POST' })
            .then(() => {
                loadHistory();
                updateBadge(0);
            });
    }

    function sendTestNotification() {
        const testBtn = document.getElementById('notif-test-btn');
        if (testBtn) {
            testBtn.disabled = true;
            testBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Sending...';
        }

        const ctx = getContextPath();
        fetch(ctx + '/notification/test', { method: 'POST' })
            .then(res => res.json())
            .then(res => {
                const count = res && res.data ? res.data.sentCount : 0;
                alert(`Test push notification sent (notified devices: ${count}).\nPlease check your browser/system notification tray.`);
                loadHistory();
            })
            .catch(err => {
                alert('Error sending test notification: ' + err.message);
            })
            .finally(() => {
                if (testBtn) {
                    testBtn.disabled = false;
                    testBtn.innerHTML = '<i class="bi bi-send me-1"></i> Send Test Notification';
                }
            });
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function bindEvents() {
        const modal = document.getElementById('notificationModal');
        if (modal) {
            modal.addEventListener('show.bs.modal', () => {
                loadHistory();
                checkPushSubscriptionState();
            });
        }

        const pushToggle = document.getElementById('notif-push-toggle');
        if (pushToggle) {
            pushToggle.addEventListener('change', (e) => {
                handlePushToggle(e.target.checked);
            });
        }

        const saveBtn = document.getElementById('notif-save-btn');
        if (saveBtn) {
            saveBtn.addEventListener('click', saveSettings);
        }

        const markAllBtn = document.getElementById('notif-mark-all-read');
        if (markAllBtn) {
            markAllBtn.addEventListener('click', markAllRead);
        }

        const clearBtn = document.getElementById('notif-clear-history');
        if (clearBtn) {
            clearBtn.addEventListener('click', clearHistory);
        }

        const testBtn = document.getElementById('notif-test-btn');
        if (testBtn) {
            testBtn.addEventListener('click', sendTestNotification);
        }
    }

    return {
        init: init,
        loadHistory: loadHistory,
        markAllRead: markAllRead,
        clearHistory: clearHistory,
        sendTestNotification: sendTestNotification
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    NotificationClient.init();
});
