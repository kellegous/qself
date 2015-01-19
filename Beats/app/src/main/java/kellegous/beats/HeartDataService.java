package kellegous.beats;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class HeartDataService extends Service implements HeartDataNotifier.Observer {
    private static final String TAG = HeartDataService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 42;

    private static final List<String> STEAMER_URLS = listOf(
            "http://10.0.1.4:6661"
    );

    private HeartDataNotifier mNotifier;

    private Streamer mStreamer;

    private NotificationManager mNotificationManager;

    private final List<Listener> mListeners = new ArrayList<>();

    private final Binder mBinder = new Binder();

    public interface Listener {
        void readingReceived(HeartDataNotifier.Reading reading);
    }

    public class Binder extends android.os.Binder {
        public HeartDataService getService() {
            return HeartDataService.this;
        }
    }

    public HeartDataService() {
    }

    private static List<String> listOf(String... vals) {
        List<String> l = new ArrayList<>(vals.length);
        for (int i = 0, n = vals.length; i < n; i++) {
            l.add(vals[i]);
        }
        return l;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mNotifier == null) {
            Log.d(TAG, "creating notifier");
            mNotifier = HeartDataNotifier.connect(this, this);
            mStreamer = Streamer.start(STEAMER_URLS);
        }

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mNotifier != null) {
            Log.d(TAG, "shutting down notifier");
            mNotifier.shutdown();
        }

        mNotificationManager.cancel(NOTIFICATION_ID);

        mStreamer.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void messageWasLogged(String message) {

    }

    @Override
    public void deviceWasFound(BluetoothDevice device) {

    }

    @Override
    public void deviceDidConnect(BluetoothDevice device) {

    }

    @Override
    public void deviceDidDisconnect(BluetoothDevice device) {
        mNotificationManager.cancel(NOTIFICATION_ID);

    }

    private void updateNotification(HeartDataNotifier.Reading reading) {
        CharSequence title = getText(R.string.heart_notification_title);

        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(String.format("%d bpm, %d ms", reading.heartRate(), reading.interval()))
                .setSmallIcon(R.drawable.heart)
                .setContentIntent(intent)
                .build();

        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void readingWasReceived(HeartDataNotifier.Reading reading) {
        updateNotification(reading);

        mStreamer.store(reading);

        for (int i = 0, n = mListeners.size(); i < n; i++) {
            mListeners.get(i).readingReceived(reading);
        }
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }
}
