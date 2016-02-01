package com.michalkowalkowski.reactnativeble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;

/**
 * Created by reion on 1/21/2016.
 */
public class ReactNativeBLEScanCallback implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "ReactNativeBLE";
    private ReactNativeBLEModule mReactNativeBLEModule;

    public ReactNativeBLEScanCallback(ReactNativeBLEModule reactNativeBLEModule) {
        mReactNativeBLEModule = reactNativeBLEModule;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.v(TAG, "onLeScan: "+device.getName());
        WritableMap params = Arguments.createMap();
        params.putString("name", device.getName());
        params.putString("address", device.getAddress());
        mReactNativeBLEModule.sendEvent(mReactNativeBLEModule.getContext(), "onLeScan", params);
    }
}
