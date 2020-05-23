/*
 * Copyright (c) 2020 The Aspectran Project
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
package club.textchat.server;

import com.aspectran.core.util.logging.Logger;
import com.aspectran.core.util.logging.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>Created: 2020/05/15</p>
 */
public class RandomChatCoupler {

    private static final Logger logger = LoggerFactory.getLogger(RandomChatCoupler.class);

    private static final int THREAD_HOLD = 5000;

    private static final int MIN_SLEEP_TIME = 100;

    private final ConcurrentLinkedQueue<ChaterInfo> queue = new ConcurrentLinkedQueue<>();

    private final RandomChatHandler randomChatHandler;

    private volatile boolean active;

    public RandomChatCoupler(RandomChatHandler randomChatHandler) {
        this.randomChatHandler = randomChatHandler;
    }

    public void request(ChaterInfo chaterInfo) {
        offer(chaterInfo);
        if (!queue.isEmpty() && !active) {
            run();
        }
    }

    public void withdraw(ChaterInfo chaterInfo) {
        queue.remove(chaterInfo);
    }

    private void offer(ChaterInfo chaterInfo) {
        if (!queue.contains(chaterInfo)) {
            queue.offer(chaterInfo);
        }
    }

    private ChaterInfo poll() {
        return queue.poll();
    }

    private void clear() {
        queue.clear();
    }

    private int interval() {
        return Math.max(THREAD_HOLD / queue.size(), MIN_SLEEP_TIME);
    }

    private void run() {
        Runnable runnable = () -> {
            active = true;
            ChaterInfo chaterInfo1;
            while ((chaterInfo1 = poll()) != null) {
                try {
                    ChaterInfo chaterInfo2 = randomChatHandler.randomChater();
                    if (chaterInfo2 != null) {
                        if (chaterInfo1.getUserNo() == chaterInfo2.getUserNo() ||
                                randomChatHandler.hasPartner(chaterInfo2.getUserNo()) ||
                                randomChatHandler.isPastPartner(chaterInfo1.getUserNo(), chaterInfo2.getUserNo())) {
                            chaterInfo2 = null;
                        }
                    }
                    if (chaterInfo2 != null) {
                        withdraw(chaterInfo2);
                        randomChatHandler.setPartner(chaterInfo1, chaterInfo2);
                        try {
                            Thread.sleep(MIN_SLEEP_TIME);
                        } catch (InterruptedException ie) {
                            clear();
                            break;
                        }
                    } else {
                        offer(chaterInfo1);
                        try {
                            Thread.sleep(interval());
                        } catch (InterruptedException ie) {
                            clear();
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn(e);
                }
            }
            active = false;
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

}
