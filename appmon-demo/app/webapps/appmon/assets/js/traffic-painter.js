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

/**
 * Advanced Canvas-based particle engine for AppMon traffic visualization.
 * Handles tab visibility to prevent "bullet bursts" when returning to the tab.
 *
 * @version 4.3
 * @last-modified 2026-10-14
 */
class TrafficPainter {
    constructor(canvas, options = {}) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.bullets = [];
        this.animationId = null;
        this.isRunning = false;
        this.finishLineOffset = 180; // track-stack width

        this.enableBlackHole = options.enableBlackHole !== false;
        this.blackHole = {
            radius: 14,
            gravityRadius: 140,
            angle: 0,
            pulse: 0
        };

        this.resize();
        this.resizeObserver = new ResizeObserver(() => this.resize());
        this.resizeObserver.observe(this.canvas.parentElement);
    }

    resize() {
        const rect = this.canvas.parentElement.getBoundingClientRect();
        this.canvas.width = rect.width;
        this.canvas.height = rect.height;
    }

    /**
     * Adds a new bullet to the painter.
     * @param {Object} data - Bullet data (error, elapsedTime, activityCount).
     * @param {Function} onArriving - Callback when bullet reaches the finish line.
     */
    addBullet(data, onArriving) {
        const elapsedTime = data.elapsedTime || 0;
        const activityCount = data.activityCount || 0;
        const hasError = !!(data.error);
        const timeIntensity = Math.min(elapsedTime / 5000, 1);
        const targetMax = 1000;
        const activityIntensity = activityCount > 0 
            ? Math.min(Math.log10(activityCount + 1) / Math.log10(targetMax + 1), 1)
            : 0;
        
        const size = 3.0 + (timeIntensity * 4) + (activityIntensity * 4);
        const baseSpeed = (this.canvas.width - this.finishLineOffset) / (900 / 16.6);
        const speed = baseSpeed * (1 - (timeIntensity * 0.6));
        const initY = Math.random() * (this.canvas.height - 20) + 10;

        const bullet = {
            x: -(Math.random() * 150),
            y: initY,
            targetY: initY,
            speed: speed,
            size: size,
            renderSize: size,
            timeIntensity: timeIntensity,
            activityIntensity: activityIntensity,
            color: hasError ? '#ff0000' : (timeIntensity > 0.5 ? '#f1c40f' : '#11d539'),
            elapsedTime: Math.max(elapsedTime, 500),
            arrived: false,
            arrivedTime: 0,
            impactPulse: 0,
            alpha: 1.0,
            bhStage: 0,
            transitFrames: 0,
            onArriving: onArriving
        };
        this.bullets.push(bullet);

        if (!this.isRunning) {
            this.start();
        }
    }

    start() {
        this.isRunning = true;
        this.lastTime = performance.now();
        const loop = (currentTime) => {
            if (document.hidden) {
                this.isRunning = false;
                return;
            }
            
            const deltaTime = Math.min((currentTime - this.lastTime) / 16.66, 3.0);
            this.lastTime = currentTime;

            this.update(deltaTime);
            this.draw();

            if (this.bullets.length > 0) {
                this.animationId = requestAnimationFrame(loop);
            } else {
                this.isRunning = false;
                this.clear();
            }
        };
        this.animationId = requestAnimationFrame(loop);
    }

    update(deltaTime) {
        const finishLine = this.canvas.width - this.finishLineOffset;
        const now = Date.now();
        const bhX = finishLine * 0.48;
        const bhY = this.canvas.height * 0.5;

        if (this.enableBlackHole && finishLine > 120) {
            this.blackHole.angle += 0.04 * deltaTime;
            if (this.blackHole.pulse > 0) {
                this.blackHole.pulse -= 0.08 * deltaTime;
                if (this.blackHole.pulse < 0) this.blackHole.pulse = 0;
            }
        }

        for (let i = this.bullets.length - 1; i >= 0; i--) {
            const b = this.bullets[i];

            if (!b.arrived) {
                if (this.enableBlackHole && finishLine > 120) {
                    if (b.bhStage === 0) {
                        const distToBh = bhX - b.x;
                        if (distToBh <= Math.max(b.speed * deltaTime, 6)) {
                            b.x = bhX;
                            b.y = bhY;
                            b.renderSize = 0.5;
                            b.bhStage = 1;
                            b.transitFrames = 4;
                            this.blackHole.pulse = 1.0;
                        } else if (distToBh < this.blackHole.gravityRadius) {
                            const pullFactor = 1 - (distToBh / this.blackHole.gravityRadius);
                            b.y += (bhY - b.y) * (0.08 + pullFactor * 0.22) * deltaTime;

                            if (distToBh < 35) {
                                b.renderSize = Math.max(0.6, b.size * (distToBh / 35));
                            }
                            b.x += b.speed * deltaTime;
                        } else {
                            b.x += b.speed * deltaTime;
                        }
                    } else if (b.bhStage === 1) {
                        b.transitFrames -= deltaTime;
                        if (b.transitFrames <= 0) {
                            b.bhStage = 2;
                            b.ejectStartX = bhX + this.blackHole.radius + 2;
                            b.x = b.ejectStartX;
                            b.y = bhY;
                            b.renderSize = 0.6;
                            b.speed *= 1.25;
                            this.blackHole.pulse = Math.max(this.blackHole.pulse, 0.5);
                        }
                    } else if (b.bhStage === 2) {
                        const spreadDistance = 90;
                        const progress = Math.min(1.0, (b.x - b.ejectStartX) / spreadDistance);
                        const ease = 1 - Math.pow(1 - progress, 2.5);
                        b.y = bhY + (b.targetY - bhY) * ease;
                        b.renderSize = Math.min(b.size, 0.6 + (b.size - 0.6) * Math.min(1.0, progress * 2));
                        b.x += b.speed * deltaTime;

                        if (progress >= 1.0) {
                            b.y = b.targetY;
                            b.renderSize = b.size;
                            b.bhStage = 3;
                        }
                    } else {
                        b.y = b.targetY;
                        b.x += b.speed * deltaTime;
                    }
                } else {
                    b.x += b.speed * deltaTime;
                }

                if (b.x >= finishLine) {
                    b.x = finishLine;
                    b.renderSize = b.size;
                    b.arrived = true;
                    b.arrivedTime = now;
                    b.impactPulse = 1.0;
                }
            } else {
                if (b.impactPulse > 0) {
                    b.impactPulse -= 0.05 * deltaTime;
                }

                const stayElapsed = now - b.arrivedTime;
                if (stayElapsed > b.elapsedTime + 200) {
                    b.alpha -= 0.04 * deltaTime;
                    if (b.alpha <= 0) {
                        if (b.onArriving) b.onArriving();
                        this.bullets.splice(i, 1);
                    }
                }
            }
        }
    }

    draw() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        const finishLine = this.canvas.width - this.finishLineOffset;
        if (this.enableBlackHole && finishLine > 120) {
            this.drawBlackHole(finishLine * 0.48, this.canvas.height * 0.5);
        }

        for (let i = 0; i < this.bullets.length; i++) {
            const b = this.bullets[i];
            if (b.bhStage === 1) {
                continue;
            }

            this.ctx.globalAlpha = b.alpha;
            
            const drawSize = b.arrived ? b.size * (1 + b.impactPulse * 0.3) : b.renderSize;

            // 1. Aura (Glow) - high performance alternative to shadowBlur
            this.ctx.fillStyle = b.color;
            this.ctx.beginPath();
            let auraSize = drawSize * (1.2 + b.timeIntensity + (b.arrived ? b.impactPulse : 0));
            this.ctx.globalAlpha = b.alpha * 0.3;
            this.ctx.arc(b.x, b.y, auraSize, 0, Math.PI * 2);
            this.ctx.fill();

            // 2. Main Bullet Body
            this.ctx.globalAlpha = b.alpha;
            this.ctx.beginPath();
            this.ctx.arc(b.x, b.y, drawSize, 0, Math.PI * 2);
            this.ctx.fill();

            // 3. Activity Glow (Hot Core) - high performance alternative to radial gradient
            if (b.activityIntensity > 0.3) {
                const coreSize = drawSize * (0.3 + b.activityIntensity * 0.4);
                this.ctx.fillStyle = '#fff';
                this.ctx.beginPath();
                this.ctx.arc(b.x, b.y, coreSize, 0, Math.PI * 2);
                this.ctx.fill();
            }
        }
        this.ctx.globalAlpha = 1.0;
    }

    drawBlackHole(cx, cy) {
        const pulse = this.blackHole.pulse;
        const r = this.blackHole.radius * (1 + pulse * 0.25);

        this.ctx.save();

        // 1. External Accretion Aura (Soft violet glow)
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, r * 2.4, 0, Math.PI * 2);
        this.ctx.fillStyle = 'rgba(120, 70, 255, 0.12)';
        this.ctx.fill();

        // 2. Pulse Shockwave Ring when bullet is absorbed/ejected
        if (pulse > 0.05) {
            this.ctx.beginPath();
            this.ctx.arc(cx, cy, r * (1.8 + (1 - pulse) * 1.2), 0, Math.PI * 2);
            this.ctx.strokeStyle = `rgba(0, 240, 255, ${pulse * 0.6})`;
            this.ctx.lineWidth = 1.5;
            this.ctx.stroke();
        }

        // 3. Swirling Accretion Arcs (Rotating space distortion)
        this.ctx.translate(cx, cy);
        this.ctx.rotate(this.blackHole.angle);

        // Cyan Arc
        this.ctx.beginPath();
        this.ctx.arc(0, 0, r * 1.5, 0, Math.PI * 1.15);
        this.ctx.strokeStyle = 'rgba(0, 229, 255, 0.75)';
        this.ctx.lineWidth = 2;
        this.ctx.stroke();

        // Magenta / Violet Arc
        this.ctx.beginPath();
        this.ctx.arc(0, 0, r * 1.75, Math.PI, Math.PI * 2.25);
        this.ctx.strokeStyle = 'rgba(217, 70, 239, 0.65)';
        this.ctx.lineWidth = 1.5;
        this.ctx.stroke();

        this.ctx.restore();

        // 4. Event Horizon (Black Void Core)
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, r, 0, Math.PI * 2);
        this.ctx.fillStyle = '#060b17';
        this.ctx.fill();

        // 5. Photon Ring (Crisp boundary highlight)
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, r, 0, Math.PI * 2);
        this.ctx.strokeStyle = 'rgba(100, 210, 255, 0.9)';
        this.ctx.lineWidth = 1.5;
        this.ctx.stroke();
    }

    clear() {
        this.bullets = [];
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }

    destroy() {
        if (this.animationId) cancelAnimationFrame(this.animationId);
        this.resizeObserver.disconnect();
    }
}
