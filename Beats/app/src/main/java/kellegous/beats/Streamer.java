package kellegous.beats;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.util.List;

public class Streamer {

    private final String TAG = Streamer.class.getSimpleName();

    public interface Listener {
    }

    private final List<String> mUrls;

    private final Handler mBgHandler;
    private final Handler mFgHandler;
    private final HandlerThread mThread;

    private Streamer(List<String> urls, HandlerThread thread) {
        mUrls = urls;

        mThread = thread;

        mBgHandler = new Handler(thread.getLooper());

        mFgHandler = new Handler(Looper.myLooper());
    }

    private static void storeOnBgThread(Handler fgHandler, HeartDataNotifier.Reading reading) {
    }

    public static Streamer start(List<String> urls) {
        return new Streamer(urls, new HandlerThread(Streamer.class.getSimpleName()));
    }

    public void store(final HeartDataNotifier.Reading reading) {
        mBgHandler.post(new Runnable() {
            @Override
            public void run() {
                storeOnBgThread(mFgHandler, reading);
            }
        });
    }

    public void stop() {
        mThread.quitSafely();
    }
}
