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
package aspectow.demo.skylark.tts;

import com.sun.speech.freetts.audio.AudioPlayer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Vector;

/**
 * Provides an implementation of <code>AudioPlayer</code> that sends
 * all audio data to the bit bucket. The <code>ByteArrayAudioPlayer</code>
 * is a helper, targeted at obtaining a byte array from the audio stream.
 */
public class ByteArrayAudioPlayer implements AudioPlayer {

    private AudioFormat audioFormat;

    private final Vector<ByteArrayInputStream> outputList;

    private byte[] outputData;

    private int currIndex = 0;

    private int totalBytes = 0;

    /**
     * Constructs a ByteArrayAudioPlayer.
     */
    public ByteArrayAudioPlayer() {
        outputList = new Vector<>();
    }

    /**
     * Sets the audio format for this player.
     * @param format the audio format
     */
    @Override
    public void setAudioFormat(AudioFormat format) {
        this.audioFormat = format;
    }

    /**
     * Retrieves the audio format for this player.
     * @return the current audio format
     */
    @Override
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * Pauses the audio output.
     */
    @Override
    public void pause() {
    }

    /**
     * Resumes audio output.
     */
    @Override
    public synchronized void resume() {
    }

    /**
     * Cancels all queued output. Current 'write' call will return
     * false.
     */
    @Override
    public synchronized void cancel() {
    }

    /**
     * Prepares for another batch of output. Larger groups of output
     * (such as all output associated with a single FreeTTSSpeakable)
     * should be grouped between a reset/drain pair.
     */
    @Override
    public synchronized void reset() {
    }

    /**
     * Waits for all audio playback to stop, and closes this AudioPlayer.
     */
    @Override
    public synchronized void close() {
    }

    /**
     * Returns the current volume.
     * @return the current volume (between 0 and 1)
     */
    @Override
    public float getVolume() {
        return 1.0f;
    }

    /**
     * Sets the current volume.
     * @param volume the current volume (between 0 and 1)
     */
    @Override
    public void setVolume(float volume) {
    }

    /**
     * Waits for all queued audio to be played
     * @return <code>true</code> if the audio played to completion,
     * <code> false </code>if the audio was stopped
     */
    @Override
    public boolean drain() {
        return true;
    }

    /**
     * Gets the amount of played since the last resetTime
     * Currently not supported.
     * @return the amount of audio in milliseconds
     */
    @Override
    public long getTime() {
        return -1L;
    }

    /**
     * Resets the audio clock.
     */
    @Override
    public void resetTime() {
    }

    /**
     * Starts the first sample timer.
     */
    @Override
    public void startFirstSampleTimer() {
    }

    /**
     * Shows metrics for this audio player.
     */
    @Override
    public void showMetrics() {
    }

    /**
     * Starts the output of a set of data.
     * @param size the size of data between now and the end
     */
    @Override
    public void begin(int size) {
        outputData = new byte[size];
        currIndex = 0;
    }

    /**
     * Marks the end of a set of data.
     */
    @Override
    public boolean end() {
        outputList.add(new ByteArrayInputStream(outputData));
        totalBytes += outputData.length;
        return true;
    }

    /**
     * Writes the given bytes to the audio stream.
     * @param audioData array of audio data
     * @return <code>true</code> of the write completed successfully,
     * <code> false </code>if the write was cancelled.
     */
    @Override
    public boolean write(byte[] audioData) {
        return write(audioData, 0, audioData.length);
    }

    /**
     * Writes the given bytes to the audio stream.
     * @param bytes  audio data to write to the device
     * @param offset the offset into the buffer
     * @param size   the size into the buffer
     * @return <code>true</code> of the write completed successfully,
     * <code> false </code>if the write was cancelled.
     */
    @Override
    public boolean write(byte[] bytes, int offset, int size) {
        System.arraycopy(bytes, offset, outputData, currIndex, size);
        currIndex += size;
        return true;
    }

    /**
     * Provide the audio data that has been written to this AudioPlayer since
     * the last call to begin() as a byte array.
     */
    public AudioInputStream getAudioInputStream() {
        InputStream inputStream = new SequenceInputStream(outputList.elements());
        AudioFormat af = getAudioFormat();
        if (af == null) {
            af = new AudioFormat(16000.0f, 16, 1, true, true);
        }
        long lengthInSamples = totalBytes / af.getFrameSize();
        return new AudioInputStream(inputStream, af, lengthInSamples);
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    /**
     * Returns the name of this AudioPlayer.
     * @return the name of the audio player
     */
    @Override
    public String toString() {
        return "ByteArrayAudioPlayer";
    }

}
