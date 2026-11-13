/**
 * Aspectow Console Service Worker
 *
 * Implements caching strategies optimized for management console applications:
 * - Static assets (CSS, JS, Fonts, Images): Stale-While-Revalidate
 * - HTML Pages: Network-First (always prefer real-time server rendering)
 * - Dynamic APIs, Commands, and WebSockets: Network-Only (never cached)
 */

const CACHE_NAME = 'aspectow-console-v2';

// Pre-cached critical assets
const PRECACHE_URLS = [
    './manifest.webmanifest',
    './assets/icons/icon-192x192.png',
    './assets/icons/icon-512x512.png',
    'https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,400;0,700;1,400&display=swap',
    'https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets@main/assets/bootstrap@5.3.8/css/aspectran.css',
    'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.13.2/font/bootstrap-icons.min.css',
    'https://code.jquery.com/jquery-3.7.1.min.js',
    'https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js',
    'https://cdn.jsdelivr.net/npm/chart.js@4.6.1/dist/chart.umd.min.js',
    'https://cdn.jsdelivr.net/npm/dayjs@1.11.23/dayjs.min.js',
    'https://cdn.jsdelivr.net/npm/dayjs@1.11.23/plugin/utc.js',
    'https://cdn.jsdelivr.net/npm/dayjs@1.11.23/plugin/localizedFormat.js',
    'https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets@main/assets/js/theme-toggler.js',
    'https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets@main/assets/countries/countries.js',
    'https://cdn.jsdelivr.net/gh/aspectran/aspectran-assets@main/assets/img/aspectran-logo.svg'
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => cache.addAll(PRECACHE_URLS))
            .then(() => self.skipWaiting())
            .catch((err) => {
                console.warn('[SW] Pre-caching warning:', err);
                return self.skipWaiting();
            })
    );
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((name) => {
                    if (name !== CACHE_NAME) {
                        return caches.delete(name);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    const url = new URL(request.url);

    // Bypass non-GET requests or WebSocket connections
    if (request.method !== 'GET' || url.protocol.startsWith('ws')) {
        return;
    }

    // Bypass dynamic data endpoints (APIs, JSON responses, command executions)
    const acceptHeader = request.headers.get('accept') || '';
    if (
        url.pathname.includes('/api/') ||
        url.pathname.includes('/command/') ||
        acceptHeader.includes('application/json') ||
        acceptHeader.includes('text/event-stream')
    ) {
        return;
    }

    // Strategy 1: Static assets (CDN or /assets/ or static extensions) -> Stale-While-Revalidate
    const isStaticAsset = (
        url.hostname.includes('jsdelivr.net') ||
        url.hostname.includes('jquery.com') ||
        url.hostname.includes('googleapis.com') ||
        url.hostname.includes('gstatic.com') ||
        url.pathname.includes('/assets/') ||
        /\.(?:css|js|woff2?|ttf|eot|svg|png|jpg|jpeg|gif|ico|webp)$/i.test(url.pathname)
    );

    if (isStaticAsset) {
        event.respondWith(
            caches.open(CACHE_NAME).then((cache) => {
                return cache.match(request).then((cachedResponse) => {
                    const fetchPromise = fetch(request).then((networkResponse) => {
                        if (networkResponse && networkResponse.status === 200) {
                            cache.put(request, networkResponse.clone());
                        }
                        return networkResponse;
                    }).catch(() => cachedResponse);

                    return cachedResponse || fetchPromise;
                });
            })
        );
        return;
    }

    // Strategy 2: HTML Page Navigation -> Network-First (realtime content first, fallback to cached shell)
    if (request.mode === 'navigate' || acceptHeader.includes('text/html')) {
        event.respondWith(
            fetch(request).then((networkResponse) => {
                // If ok, update page cache
                if (networkResponse && networkResponse.status === 200) {
                    const responseClone = networkResponse.clone();
                    caches.open(CACHE_NAME).then((cache) => {
                        cache.put(request, responseClone);
                    });
                }
                return networkResponse;
            }).catch(() => {
                return caches.match(request).then((cachedResponse) => {
                    return cachedResponse || caches.match('./');
                });
            })
        );
    }
});
