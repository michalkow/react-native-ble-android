package com.michalkowalkowski.reactnativeble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Created by reion on 1/21/2016.
 */
public class ReactNativeBLEModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    private static final String TAG = "ReactNativeBLE";
    public static final String REACT_CLASS = "ReactNativeBLEModule";
    private static final Integer REQUEST_ENABLE_BT = 927281;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private ReactContext mContext;
    private BluetoothManager mBluetoothManager;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public ReactNativeBLEModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mBluetoothManager = (BluetoothManager) reactContext.getSystemService(ReactApplicationContext.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mContext = reactContext;
        mContext.addLifecycleEventListener(this);
    }

    private void scanLeDevice(final boolean enable, @Nullable Integer duration) {
        final ReactNativeBLEScanCallback mLeScanCallback = new ReactNativeBLEScanCallback(this);
        if (enable) {
            Log.v(TAG, "Start Scan");
            // Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    Log.v(TAG, "Finished Scan");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, duration);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                WritableMap params = Arguments.createMap();
                params.putString("msg", "Connected to GATT server.");
                params.putInt("connected", STATE_CONNECTED);
                sendEvent(mContext, "onConnectionStateChange", params);
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                WritableMap params = Arguments.createMap();
                params.putString("msg", "Disconnected from GATT server.");
                params.putInt("connected", STATE_DISCONNECTED);
                sendEvent(mContext, "onConnectionStateChange", params);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                WritableMap params = Arguments.createMap();
                params.putInt("status", status);
                sendEvent(mContext, "onServicesDiscovered", params);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                WritableMap params = Arguments.createMap();
                params.putInt("status", status);
                sendEvent(mContext, "onServicesDiscovered", params);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                WritableMap params = Arguments.createMap();
                params.putInt("status", status);
                params.putString("action", ACTION_DATA_AVAILABLE);
                params.putString("characteristic", characteristic.getUuid().toString());
                sendEvent(mContext, "onCharacteristicRead", params);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data) stringBuilder.append(String.format("%02X ", byteChar));
                WritableMap params = Arguments.createMap();
                params.putString("action", ACTION_DATA_AVAILABLE);
                params.putString("hexValue", stringBuilder.toString());
                params.putString("characteristic", characteristic.getUuid().toString());
                sendEvent(mContext, "onCharacteristicChanged", params);
            }
        }
    };

    public ReactContext getContext() {
       return mContext;
    }

    public void sendEvent(ReactContext reactContext, String eventName, WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void startScan(int duration, Callback callback) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            callback.invoke("Error: Bluetooth not enabled");
        } else {
            Log.v(TAG, "Bluetooth Enabled");
            scanLeDevice(true, duration);
            callback.invoke("Scanning");
        }
    }

    @ReactMethod
    public void stopScan(@Nullable Callback callback) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            if (callback != null) {
                callback.invoke("Error: Bluetooth not enabled");
            }
        } else {
            scanLeDevice(false, null);
            if (callback != null) {
                callback.invoke("Stopped");
            }
        }
    }

    @ReactMethod
    public void connect(String address, Callback callback) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            callback.invoke(false);
            return;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                callback.invoke(true);
                return;
            } else {
                callback.invoke(false);
                return;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            callback.invoke(false);
            return;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        callback.invoke(true);
        return;
    }

    @ReactMethod
    public void disconnect(@Nullable Callback callback) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            callback.invoke(false, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        callback.invoke(true, "Disconnected");
    }

    @ReactMethod
    public void close(@Nullable Callback callback) {
        if (mBluetoothGatt == null) {
            callback.invoke(false, "mBluetoothGatt not initialized");
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        callback.invoke(true, "Closed");
    }

    @ReactMethod
    public void readCharacteristic(String serviceString, String characteristicString) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(UUID.fromString(serviceString)).getCharacteristic(UUID.fromString(characteristicString));
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    @ReactMethod
    public void setCharacteristicNotification(String serviceString, String characteristicString, boolean enabled, @Nullable String descriptorString) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(serviceString));
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicString));
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (descriptorString != null) {
            UUID descriptorUuid = UUID.fromString(descriptorString);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorString));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    @Override
    public void onHostResume() {
        Log.v(TAG, "onHostResume");
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
            if (mBluetoothDeviceAddress != null && mBluetoothGatt != null) {
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                if (mBluetoothGatt.connect()) {
                    mConnectionState = STATE_CONNECTING;
                }
            }
        }
    }

    @Override
    public void onHostPause() {
        Log.v(TAG, "onHostPause");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        if (mBluetoothAdapter != null) {
            scanLeDevice(false, 0);
            mBluetoothAdapter.disable();
        }
    }

    @Override
    public void onHostDestroy() {
        Log.v(TAG, "onHostDestroy");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
}
