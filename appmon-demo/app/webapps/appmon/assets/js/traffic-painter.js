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
 * @last-modified 2026-10-19
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
        this.ejectRatio = 0;
        this.prismProgress = 0.0;
        this.prismAlpha = 1.0;
        this.currentLeftPrismX = 0;
        this.currentRightPrismX = 0;
        this.blackHole = {
            radius: 14,
            gravityRadius: 140,
            angle: 0,
            pulse: 0,
            trappedCount: 0
        };

        this.resize();
        this.resizeObserver = new ResizeObserver(() => this.resize());
        this.resizeObserver.observe(this.canvas.parentElement);
    }

    resize() {
        const rect = this.canvas.parentElement.getBoundingClientRect();
        this.canvas.width = rect.width;
        this.canvas.height = rect.height;
        this.updatePrismPositions();
        this.draw();
    }

    updatePrismPositions() {
        const finishLine = this.canvas.width - this.finishLineOffset;
        const bhX = finishLine * 0.5;
        const spreadDistance = 60;
        const prismOffset = this.blackHole.radius + 2 + spreadDistance;
        const leftActiveX = bhX - prismOffset;
        const rightActiveX = bhX + prismOffset;
        const leftParkedX = 10;
        const rightParkedX = Math.max(leftParkedX + 40, finishLine - 10);

        const p = this.prismProgress || 0.0;
        const ease = p * p * (3 - 2 * p);
        this.currentLeftPrismX = leftParkedX + (leftActiveX - leftParkedX) * ease;
        this.currentRightPrismX = rightParkedX + (rightActiveX - rightParkedX) * ease;
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
        
        const baseSize = 3.0 + (timeIntensity * 4);
        const fullSize = baseSize + (activityIntensity * 4);
        const initialSize = this.enableBlackHole ? baseSize : fullSize;
        const baseSpeed = (this.canvas.width - this.finishLineOffset) / (900 / 16.6);
        const speed = baseSpeed * (1 - (timeIntensity * 0.6));
        const initY = Math.random() * (this.canvas.height - 20) + 10;

        const bullet = {
            x: -(Math.random() * 150),
            y: initY,
            initY: initY,
            targetY: initY,
            speed: speed,
            baseSize: baseSize,
            fullSize: fullSize,
            size: initialSize,
            renderSize: initialSize,
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
            orbitAngle: 0,
            orbitRadius: 0,
            orbitSpeed: 0,
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

            const isPrismAnimating = (this.bullets.length === 0 && (this.prismProgress > 0 || this.prismAlpha < 1.0));
            if (this.bullets.length > 0 || isPrismAnimating) {
                this.animationId = requestAnimationFrame(loop);
            } else {
                this.isRunning = false;
                this.draw();
            }
        };
        this.animationId = requestAnimationFrame(loop);
    }

    update(deltaTime) {
        const finishLine = this.canvas.width - this.finishLineOffset;
        const now = Date.now();
        const bhX = finishLine * 0.5;
        const bhY = this.canvas.height * 0.5;

        // 두 개의 유리막 사이 넓이 (전개 시 좌우 유리막 스팬)
        const spreadDistance = 60;
        const prismOffset = this.blackHole.radius + 2 + spreadDistance;
        const prismSpan = prismOffset * 2 + 30;
        const isNarrowTrack = finishLine < prismSpan;

        // 트랙의 넓이가 두 개의 유리막 넓이보다 좁은 경우:
        // 처음(대기 시)에는 양 끝에 보이고, 총알이 나타나면 유리막을 감춤 (페이드아웃)
        // 충분히 넓은 경우:
        // 총알이 나타나면 양 끝에서 중앙으로 슬라이딩 전개
        const hasBullets = this.bullets.length > 0;
        const targetProgress = (!isNarrowTrack && hasBullets) ? 1.0 : 0.0;
        const targetAlpha = (isNarrowTrack && hasBullets) ? 0.0 : 1.0;

        // 슬라이딩 진행도 업데이트
        const deploySpeed = 0.14;  // 전개 속도: 총알 발생 시 신속히 블랙홀 옆으로 이동
        const retractSpeed = 0.05; // 복귀 속도: 총알 소멸 시 트랙 양 끝으로 우아하게 복귀
        const moveSpeed = targetProgress > this.prismProgress ? deploySpeed : retractSpeed;
        if (targetProgress > this.prismProgress) {
            this.prismProgress = Math.min(1.0, this.prismProgress + moveSpeed * deltaTime);
        } else if (targetProgress < this.prismProgress) {
            this.prismProgress = Math.max(0.0, this.prismProgress - moveSpeed * deltaTime);
        }

        // 투명도(Alpha) 업데이트 (좁은 트랙 감춤/표시 처리)
        const fadeOutSpeed = 0.15; // 총알 등장 시 신속히 페이드아웃
        const fadeInSpeed = 0.06;  // 총알 소멸 시 부드럽게 페이드인
        const alphaSpeed = targetAlpha < this.prismAlpha ? fadeOutSpeed : fadeInSpeed;
        if (targetAlpha > this.prismAlpha) {
            this.prismAlpha = Math.min(1.0, this.prismAlpha + alphaSpeed * deltaTime);
        } else if (targetAlpha < this.prismAlpha) {
            this.prismAlpha = Math.max(0.0, this.prismAlpha - alphaSpeed * deltaTime);
        }

        this.updatePrismPositions();

        let trappedCount = 0;

        for (let i = this.bullets.length - 1; i >= 0; i--) {
            const b = this.bullets[i];

            if (!b.arrived) {
                if (this.enableBlackHole && finishLine > 120) {
                    if (b.bhStage === 0) {
                        const distToBh = bhX - b.x;
                        const leftGateX = this.currentLeftPrismX;

                        if (distToBh <= Math.max(b.speed * deltaTime, 6)) {
                            b.x = bhX;
                            b.y = bhY;
                            b.bhStage = 1;
                            const minFrames = 4;
                            const maxFrames = 50;
                            b.transitFrames = Math.round(minFrames + b.timeIntensity * (maxFrames - minFrames));
                            b.orbitAngle = Math.random() * Math.PI * 2;
                            b.orbitSpeed = (0.12 + Math.random() * 0.08) * (Math.random() < 0.5 ? 1 : -1);
                            b.orbitRadius = (this.blackHole.radius * 0.75) * (0.25 + Math.random() * 0.75);
                            b.renderSize = Math.max(1.2, Math.min(2.5, b.baseSize * 0.4));
                            this.blackHole.pulse = Math.max(this.blackHole.pulse, 0.6);
                        } else if (b.x >= leftGateX) {
                            // 좌측 유리막을 통과하면서 블랙홀 인력 영역으로 유선형 진입
                            const pullDist = Math.max(10, bhX - leftGateX);
                            const pullProgress = Math.min(1.0, Math.max(0.0, (b.x - leftGateX) / pullDist));
                            const easePull = Math.pow(pullProgress, 1.8);
                            b.y = (b.initY || bhY) + (bhY - (b.initY || bhY)) * easePull;
                            if (distToBh < 35) {
                                b.renderSize = Math.max(0.6, b.baseSize * (distToBh / 35));
                            }
                            b.x += b.speed * deltaTime;
                        } else {
                            // 좌측 유리막에 도달하기 전까지는 완전한 수평 평행 비행
                            b.y = b.initY || bhY;
                            b.x += b.speed * deltaTime;
                        }
                    } else if (b.bhStage === 1) {
                        trappedCount++;
                        b.transitFrames -= deltaTime;
                        b.orbitAngle += b.orbitSpeed * deltaTime;
                        b.x = bhX + Math.cos(b.orbitAngle) * b.orbitRadius;
                        b.y = bhY + Math.sin(b.orbitAngle) * b.orbitRadius;

                        if (b.transitFrames <= 0) {
                            b.bhStage = 2;
                            b.size = b.fullSize;

                            // 프리즘 레인 확산 (Prism Lane Dispersion):
                            // 황금비(Golden Ratio: 0.61803398875) 기반 저변위 수열(Low-Discrepancy Sequence)
                            this.ejectRatio = ((this.ejectRatio || 0) + 0.61803398875) % 1.0;
                            const minY = 14;
                            const maxY = this.canvas.height - 14;
                            b.targetY = minY + this.ejectRatio * (maxY - minY);

                            // 1. 사출 시작 위치: 블랙홀 출구 테두리
                            const halfH = (this.canvas.height - 24) * 0.5;
                            const ejectAngle = (b.targetY - bhY) / (halfH || 1);
                            b.ejectStartX = bhX + this.blackHole.radius + 2;
                            b.ejectStartY = bhY + Math.sin(ejectAngle * 0.45) * (this.blackHole.radius * 0.6);

                            b.x = b.ejectStartX;
                            b.y = b.ejectStartY;
                            b.renderSize = 0.8;
                            b.speed *= 1.25;
                            this.blackHole.pulse = Math.max(this.blackHole.pulse, 0.5);
                        }
                    } else if (b.bhStage === 2) {
                        // 2. 분산 구간: 블랙홀 출구에서 우측 유리막(currentRightPrismX)까지 부채꼴로 퍼져나감
                        const rightGateX = isNarrowTrack 
                            ? Math.min(this.currentRightPrismX, b.ejectStartX + Math.max(20, (finishLine - b.ejectStartX) * 0.6))
                            : this.currentRightPrismX;
                        const totalSpread = Math.max(15, rightGateX - b.ejectStartX);
                        const progress = Math.min(1.0, Math.max(0.0, (b.x - b.ejectStartX) / totalSpread));
                        // Smoothstep S-Curve: 우측 유리막 지점에 도달할 때 완벽한 수평 레인으로 정렬
                        const ease = progress * progress * (3 - 2 * progress);
                        b.y = b.ejectStartY + (b.targetY - b.ejectStartY) * ease;
                        b.renderSize = Math.min(b.size, 0.8 + (b.size - 0.8) * Math.min(1.0, progress * 2.0));
                        b.x += b.speed * deltaTime;

                        if (progress >= 1.0 || b.x >= rightGateX) {
                            // 우측 유리막을 통과하는 순간 분산 완료 및 수평 레인 정렬 완료!
                            b.y = b.targetY;
                            b.renderSize = b.size;
                            b.bhStage = 3; // 이후 도착선까지 완전한 평행 수평 비행!
                        }
                    } else {
                        // 3. 우측 유리막 통과 후: 독립 레인에서 완전한 수평 평행 비행 (개수 즉시 가늠!)
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

        if (this.enableBlackHole && finishLine > 120) {
            this.blackHole.trappedCount = trappedCount;
            const speedBoost = Math.min(trappedCount * 0.006, 0.10);
            this.blackHole.angle += (0.04 + speedBoost) * deltaTime;
            if (this.blackHole.pulse > 0) {
                this.blackHole.pulse -= 0.08 * deltaTime;
                if (this.blackHole.pulse < 0) this.blackHole.pulse = 0;
            }
        }
    }

    draw() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        const finishLine = this.canvas.width - this.finishLineOffset;
        const hasBlackHole = this.enableBlackHole && finishLine > 120;
        if (hasBlackHole) {
            const bhX = finishLine * 0.5;
            const bhY = this.canvas.height * 0.5;
            this.drawBlackHole(bhX, bhY);
            this.drawPrism(this.currentLeftPrismX);
            this.drawPrism(this.currentRightPrismX);
        }

        for (let i = 0; i < this.bullets.length; i++) {
            const b = this.bullets[i];

            // 0. Bullets trapped inside the black hole (swirling mini photons)
            if (b.bhStage === 1) {
                this.ctx.globalAlpha = b.alpha * 0.85;
                this.ctx.fillStyle = b.color;
                this.ctx.beginPath();
                this.ctx.arc(b.x, b.y, b.renderSize, 0, Math.PI * 2);
                this.ctx.fill();

                this.ctx.fillStyle = '#fff';
                this.ctx.beginPath();
                this.ctx.arc(b.x, b.y, Math.max(0.6, b.renderSize * 0.45), 0, Math.PI * 2);
                this.ctx.fill();
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

            // 3. Activity Glow (Hot Core) - apply only after exiting the black hole
            const canShowActivity = !hasBlackHole || b.bhStage >= 2;
            if (canShowActivity && b.activityIntensity > 0.3) {
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
        const trappedCount = this.blackHole.trappedCount || 0;
        const trappedBoost = Math.min(trappedCount * 0.04, 0.45);
        const r = this.blackHole.radius * (1 + pulse * 0.25 + trappedBoost * 0.15);

        this.ctx.save();

        // 1. External Accretion Aura (Soft violet / energetic magenta glow when holding bullets)
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, r * (2.4 + trappedBoost * 0.6), 0, Math.PI * 2);
        const auraAlpha = Math.min(0.12 + trappedCount * 0.015, 0.30);
        this.ctx.fillStyle = trappedCount > 5 
            ? `rgba(220, 80, 255, ${auraAlpha})` 
            : `rgba(120, 70, 255, ${auraAlpha})`;
        this.ctx.fill();

        // 2. Pulse Shockwave Ring when bullet is absorbed/ejected or highly populated
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

        // Cyan / Gold Arc (Heats up as trapped bullets increase)
        this.ctx.beginPath();
        this.ctx.arc(0, 0, r * 1.5, 0, Math.PI * 1.15);
        this.ctx.strokeStyle = trappedCount > 8 ? 'rgba(255, 190, 40, 0.9)' : 'rgba(0, 229, 255, 0.75)';
        this.ctx.lineWidth = 2 + Math.min(trappedCount * 0.08, 1.2);
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
        this.ctx.strokeStyle = trappedCount > 5 ? 'rgba(255, 210, 120, 0.95)' : 'rgba(100, 210, 255, 0.9)';
        this.ctx.lineWidth = 1.5;
        this.ctx.stroke();
    }

    drawPrism(px) {
        if (this.prismAlpha <= 0.001) return;
        const h = this.canvas.height;
        this.ctx.save();
        if (this.prismAlpha < 1.0) {
            this.ctx.globalAlpha = this.prismAlpha;
        }

        // 1. 반투명 프리즘 유리막 본체 (Ambient Glass Sheet)
        const glassGrad = this.ctx.createLinearGradient(px - 3, 0, px + 3, 0);
        glassGrad.addColorStop(0, 'rgba(0, 229, 255, 0.04)');
        glassGrad.addColorStop(0.5, 'rgba(217, 70, 239, 0.14)');
        glassGrad.addColorStop(1, 'rgba(0, 229, 255, 0.04)');

        this.ctx.fillStyle = glassGrad;
        this.ctx.fillRect(px - 3, 6, 6, h - 12);

        // 2. 프리즘 크리스탈 엣지 (세로 광선 굴절선)
        const lineGrad = this.ctx.createLinearGradient(0, 6, 0, h - 6);
        lineGrad.addColorStop(0, 'rgba(0, 240, 255, 0.0)');
        lineGrad.addColorStop(0.15, 'rgba(0, 240, 255, 0.45)');
        lineGrad.addColorStop(0.5, 'rgba(235, 130, 255, 0.85)');
        lineGrad.addColorStop(0.85, 'rgba(0, 240, 255, 0.45)');
        lineGrad.addColorStop(1, 'rgba(0, 240, 255, 0.0)');

        this.ctx.strokeStyle = lineGrad;
        this.ctx.lineWidth = 1.5;
        this.ctx.beginPath();
        this.ctx.moveTo(px, 6);
        this.ctx.lineTo(px, h - 6);
        this.ctx.stroke();

        // 3. 내부 광학 굴절 하이라이트 (미세 투과 반사선)
        this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
        this.ctx.lineWidth = 0.8;
        this.ctx.beginPath();
        this.ctx.moveTo(px + 1, 14);
        this.ctx.lineTo(px + 1, h - 14);
        this.ctx.stroke();

        // 4. 상·하단 사이버네틱 마운트 핀
        this.ctx.fillStyle = 'rgba(0, 240, 255, 0.7)';
        this.ctx.fillRect(px - 4, 6, 8, 2);
        this.ctx.fillRect(px - 4, h - 8, 8, 2);

        this.ctx.restore();
    }

    clear() {
        this.bullets = [];
        if (!this.isRunning && (this.prismProgress > 0 || this.prismAlpha < 1.0)) {
            this.start();
        } else {
            this.draw();
        }
    }

    destroy() {
        if (this.animationId) cancelAnimationFrame(this.animationId);
        this.resizeObserver.disconnect();
    }
}
