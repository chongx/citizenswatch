package com.citizenswatch.citizenswatch;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecorder extends Thread {
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static int bufferSize = 0;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean recording = new AtomicBoolean(false);
    private File mFile = null;
    private FileOutputStream streamWriter = null;
    private WebSocketClient mWebSocketClient = null;

    public AudioRecorder(File file) {
        bufferSize = AudioRecord.getMinBufferSize(
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING);
        mFile = file;
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        initWebSocket();
    }

    private void initWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://cw-ranker.herokuapp.com");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");

            }

            @Override
            public void onMessage(String s) {
                //
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
        Log.i("Websocket", "initiating connection");
    }

    @Override
    public void run() {
        running.set(true);
        Log.i("Audio", "Running Audio Thread");
        AudioRecord recorder = null;
        byte[][] buffers = new byte[256][bufferSize];
        int ix = 0;

        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize * 10);
            recorder.startRecording();
            while (running.get()) {
                byte[] buffer = buffers[ix++ % buffers.length];
                int bytesRead = recorder.read(buffer, 0, buffer.length);
                if (recording.get()) {
                    streamWriter.write(buffer, 0, bytesRead);
                    if (bytesRead < bufferSize) {
                        buffer[bytesRead] = '\0'; // null terminate the buffer
                    }
                    mWebSocketClient.send(buffer);
                }
            }
        } catch (Throwable x) {
            Log.w("Audio", "Error reading voice audio", x);
        } finally {
            try {
                streamWriter.close();
            } catch (Throwable x) {
                //
            }
            recorder.stop();
            recorder.release();
        }
    }

    public void close() {
        running.set(false);
    }

    public void startRecording() {
        Log.i("AudioRecorder", "startRecording called");
        try {
            Log.i("AudioRecorder", "FileOutputStream init, path: " + mFile.getAbsolutePath());
            streamWriter = new FileOutputStream(mFile);
            Log.i("AudioRecorder", "FileOutputStream init success");
            recording.set(true);
        } catch (Throwable x) {
            //
        }
    }

    public void stopRecording() {
        Log.i("AudioRecorder", "stopRecording called");
        try {
            streamWriter.close();
        } catch (Throwable x) {
            //
        }
        recording.set(false);
        streamWriter = null;
    }
}