package kellegous.beats;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

public class Streamer {

    private final String TAG = Streamer.class.getSimpleName();

    public interface Listener {
        void steamerDidConnect();
        void streamerDidDisconnect();
    }

    private final InetAddress mAddress;
    private final int mPort;

    private final Listener mListener;

    private final Handler mBgHandler;
    private final Handler mFgHandler;
    private final HandlerThread mThread;

    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private Streamer(InetAddress address, int port, Listener listener, HandlerThread thread) {
        mAddress = address;
        mPort = port;

        mListener = listener;

        mThread = thread;
        mBgHandler = new Handler(thread.getLooper());
        mFgHandler = new Handler(Looper.myLooper());
    }

    private static void storeOnBgThread(Handler fgHandler, HeartDataNotifier.Reading reading) {
    }

    private void connect() throws IOException {
        if (mSocket != null) {
            return;
        }

        Socket socket = new Socket(mAddress, mPort);
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();

        mSocket = socket;
        mOutputStream = output;
        mInputStream = input;

        mFgHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.steamerDidConnect();
            }
        });
    }

    private void disconnect() throws IOException {
        if (mSocket == null) {
            return;
        }

        Socket socket = mSocket;
        mSocket = null;
        mOutputStream = null;
        mInputStream = null;

        mFgHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.streamerDidDisconnect();
            }
        });

        socket.close();
    }

    private void connectOnBgThread() {
        mBgHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    connect();
                    mFgHandler.post(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                } catch (IOException e) {
                    Log.d(TAG, "error on connect", e);
                }
            }
        });
    }

    private void disconnectOnBgThread() {
        mBgHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    disconnect();
                } catch (IOException e) {
                    Log.d(TAG, "error on disconnect", e);
                }
            }
        });
    }

    public static Streamer start(InetAddress address, int port, Listener listener) {
        HandlerThread thread = new HandlerThread(Streamer.class.getSimpleName());
        thread.start();

        Streamer streamer = new Streamer(address, port, listener, thread);

        streamer.connectOnBgThread();

        return streamer;
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
