package kellegous.beats;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by knorton on 1/16/15.
 */
public class HeartDataNotifier extends BluetoothGattCallback {
    private static final String TAG = HeartDataNotifier.class.getSimpleName();

    private static final ParcelUuid heartServiceUuid = ParcelUuid.fromString(
            "0000180d-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid heartCharacteristicUuid = ParcelUuid.fromString(
            "00002a37-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid heartDescriptorUuid = ParcelUuid.fromString(
            "00002902-0000-1000-8000-00805f9b34fb");

    private static final ParcelUuid batteryServiceUuid = ParcelUuid.fromString(
            "0000180f-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid batteryCharacteristicUuid = ParcelUuid.fromString(
            "00002a19-0000-1000-8000-00805f9b34fb");

    public interface Observer {
        void messageWasLogged(String message);
        void deviceWasFound(BluetoothDevice device);
        void deviceDidConnect(BluetoothDevice device);
        void deviceDidDisconnect(BluetoothDevice device);
        void readingWasReceived(Reading reading);
        void batteryLevelWasReceived(float pct);
    }

    public static class Reading {
        public static int NOT_INCLUDED = -1;

        public static int CONTACT_PRESENT = 1;
        public static int CONTACT_NOT_PRESENT = 2;

        int mRate;
        int mInterval;
        int mContact;
        int mEnergyExpended;

        public int heartRate() {
            return mRate;
        }

        public boolean hasEnergyExpended() {
            return mEnergyExpended != NOT_INCLUDED;
        }

        public int energyExpended() {
            return mEnergyExpended;
        }

        public boolean hasContact() {
            return mContact != NOT_INCLUDED;
        }

        public boolean inContact() {
            return mContact == CONTACT_PRESENT;
        }

        public boolean hasInterval() {
            return mInterval != NOT_INCLUDED;
        }

        public int interval() {
            return mInterval;
        }

        private static Reading parse(BluetoothGattCharacteristic characteristic) {
            Reading reading = new Reading();
            byte[] value = characteristic.getValue();
            byte head = value[0];

            int offset;
            if ((head & 0x1) != 0) {
                reading.mRate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
                offset = 3;
            } else {
                reading.mRate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                offset = 2;
            }

            if ((head & 0x2) != 0) {
                reading.mContact = ((head & 0x4) != 0) ? CONTACT_PRESENT : CONTACT_NOT_PRESENT;
            } else {
                reading.mContact = NOT_INCLUDED;
            }

            if ((head & 0x8) != 0) {
                reading.mEnergyExpended = characteristic.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT16,
                        offset);
                offset += 2;
            } else {
                reading.mEnergyExpended = NOT_INCLUDED;
            }

            if ((head & 0x10) != 0) {
                reading.mInterval = characteristic.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT16,
                        offset);
            } else {
                reading.mInterval = NOT_INCLUDED;
            }

            return reading;
        }
    }

    private final BluetoothLeScanner mScanner;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Observer mObserver;
    private final Context mContext;

    private BluetoothGatt mGatt;

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mScanner.stopScan(this);

            final BluetoothDevice device = result.getDevice();

            device.connectGatt(mContext, true, HeartDataNotifier.this);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mObserver.deviceWasFound(device);
                }
            });
        }
    };

    private static List<ScanFilter> filtersForHeartRateService() {
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(heartServiceUuid)
                .build());
        return filters;
    }

    private HeartDataNotifier(Context context, BluetoothLeScanner scanner, Observer observer) {
        mContext = context;
        mScanner = scanner;
        mObserver = observer;
        scanner.startScan(filtersForHeartRateService(),
                new ScanSettings.Builder().build(),
                mScanCallback);
    }

    public void shutdown() {
    }

    public static HeartDataNotifier connect(Context context, Observer observer) {
        BluetoothManager manager = (BluetoothManager)context.getSystemService(
                Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            return null;
        }

        return new HeartDataNotifier(
                context,
                adapter.getBluetoothLeScanner(),
                observer);
    }

    private void logOnMainThread(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mObserver.messageWasLogged(message);
            }
        });
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
        Log.d(TAG, "onConnectionStateChange");

        if (status != BluetoothGatt.GATT_SUCCESS) {
            fireDeviceDisconnect(gatt.getDevice());
            return;
        }

        switch (newState) {
        case BluetoothGatt.STATE_CONNECTED:
            if (!gatt.discoverServices()) {
                logOnMainThread("ERROR: discover services");
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mObserver.deviceDidConnect(gatt.getDevice());
                }
            });

            break;

        case BluetoothGatt.STATE_DISCONNECTED:
            fireDeviceDisconnect(gatt.getDevice());
            break;
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "onCharacteristicRead");
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        if (characteristic.getUuid().equals(batteryCharacteristicUuid.getUuid())) {
            final byte[] value = characteristic.getValue();
            if (value == null || value.length == 0) {
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mObserver.batteryLevelWasReceived((float)value[0]/100f);
                }
            });

        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        mGatt = gatt;
        startHeartRateNotifications(gatt);
    }

    private void startHeartRateNotifications(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(heartServiceUuid.getUuid());
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(
                heartCharacteristicUuid.getUuid());

        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            logOnMainThread("ERROR: unable to set characteristic notification");
            return;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                heartDescriptorUuid.getUuid());

        if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
            logOnMainThread("ERROR: unable to set value on descriptor");
            return;
        }

        if (!gatt.writeDescriptor(descriptor)) {
            logOnMainThread("ERROR: unable to write descriptor");
            return;
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        final Reading reading = Reading.parse(characteristic);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mObserver.readingWasReceived(reading);
            }
        });
    }

    private void fireDeviceDisconnect(final BluetoothDevice device) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mObserver.deviceDidDisconnect(device);
            }
        });
    }

    public boolean requestBatteryLevel() {
        BluetoothGattService service = mGatt.getService(batteryServiceUuid.getUuid());
        if (service == null) {
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(
                batteryCharacteristicUuid.getUuid());
        if (characteristic == null) {
            return false;
        }

        return mGatt.readCharacteristic(characteristic);
    }
}
