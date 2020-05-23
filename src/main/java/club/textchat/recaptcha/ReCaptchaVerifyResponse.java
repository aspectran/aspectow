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
package club.textchat.recaptcha;

import com.aspectran.core.util.apon.AbstractParameters;
import com.aspectran.core.util.apon.ParameterKey;
import com.aspectran.core.util.apon.ValueType;

import java.util.List;

/**
 * Google Re-Captcha API result.
 */
public class ReCaptchaVerifyResponse extends AbstractParameters {

    private static final ParameterKey success;
    private static final ParameterKey score;
    private static final ParameterKey action;
    private static final ParameterKey challengeTimestamp;
    private static final ParameterKey hostname;
    private static final ParameterKey errorCodes;

    private static final ParameterKey[] parameterKeys;

    static {
        success = new ParameterKey("success", ValueType.BOOLEAN);
        score = new ParameterKey("score", ValueType.FLOAT);
        action = new ParameterKey("action", ValueType.STRING);
        challengeTimestamp = new ParameterKey("challenge_ts", ValueType.STRING);
        hostname = new ParameterKey("hostname", ValueType.STRING);
        errorCodes = new ParameterKey("error-codes", ValueType.STRING, true);

        parameterKeys = new ParameterKey[] {
                success,
                score,
                action,
                challengeTimestamp,
                hostname,
                errorCodes
        };
    }

    public ReCaptchaVerifyResponse() {
        super(parameterKeys);
    }

    public boolean isSuccess() {
        return getBoolean(success, false);
    }

    public float getScore() {
        return getFloat(score, 0.0f);
    }

    public String getAction() {
        return getString(action);
    }

    public String getChallengeTimestamp() {
        return getString(challengeTimestamp);
    }

    public String getHostname() {
        return getString(hostname);
    }

    public List<String> getErrorCodes() {
        return getStringList(errorCodes);
    }

}
