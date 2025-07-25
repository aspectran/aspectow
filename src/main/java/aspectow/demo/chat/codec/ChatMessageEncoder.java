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
package aspectow.demo.chat.codec;

import aspectow.demo.chat.model.ChatMessage;
import com.aspectran.utils.json.JsonBuilder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

/**
 * Encoder for {@link ChatMessage}.
 *
 * <p>Created: 2019/10/09</p>
 */
public class ChatMessageEncoder implements Encoder.Text<ChatMessage> {

    @Override
    public String encode(ChatMessage message) throws EncodeException {
        try {
            return new JsonBuilder()
                    .prettyPrint(false)
                    .nullWritable(false)
                    .put(message)
                    .toString();
        } catch (Exception e) {
            throw new EncodeException(message, "Badly formatted message", e);
        }
    }

}
