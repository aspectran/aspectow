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
package club.textchat.server.message.payload;

import com.aspectran.core.util.apon.AbstractParameters;
import com.aspectran.core.util.apon.ParameterKey;
import com.aspectran.core.util.apon.ValueType;

/**
 * Represents the payload of a WebSocket frame to welcome a user.
 *
 * <p>Created: 2019/10/09</p>
 */
public class AbortPayload extends AbstractParameters {

    private static final ParameterKey cause;

    private static final ParameterKey[] parameterKeys;

    static {
        cause = new ParameterKey("cause", ValueType.STRING);

        parameterKeys = new ParameterKey[] {
                cause
        };
    }

    public AbortPayload() {
        super(parameterKeys);
    }

    public void setCause(String cause) {
        putValue(AbortPayload.cause, cause);
    }

}
