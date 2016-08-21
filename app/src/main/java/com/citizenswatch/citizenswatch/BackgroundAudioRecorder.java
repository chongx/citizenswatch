package com.citizenswatch.citizenswatch;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;

public class BackgroundAudioRecorder extends Service {
    public static String EXTRA_FILENAME = "filename";
    // Binder given to clients
    private final IBinder mBinder = new BackgroundAudioRecorderBinder();
    private AudioRecorder mAudioRecorder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Background audio recorder starting", Toast.LENGTH_SHORT).show();
        File p = this.getExternalFilesDir(null);
        File f = new File(p, intent.getStringExtra(EXTRA_FILENAME));
        mAudioRecorder = new AudioRecorder(f);
        mAudioRecorder.start();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startRecording() {
        mAudioRecorder.startRecording();
    }

    public void stopRecording() {
        mAudioRecorder.stopRecording();
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Background audio recorder destroyed", Toast.LENGTH_SHORT).show();
    }

    public class BackgroundAudioRecorderBinder extends Binder {
        BackgroundAudioRecorder getService() {
            return BackgroundAudioRecorder.this;
        }
    }
}
