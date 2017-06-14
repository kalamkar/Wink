package com.ojogaze.wink;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by abhi on 5/23/17.
 */

public abstract class EogDevice {

    protected final Context context;
    private final Set<Observer> observers = new HashSet<>();

    public interface Observer {
        void onScanStart();

        void onScanResult(String deviceAddress);

        void onScanEnd();

        void onConnect(String name);

        void onDisconnect(String name);

        void onNewValues(int[] values);
    }

    protected EogDevice(Context context) {
        this.context = context;
    }

    public final void add(Observer observer) {
        observers.add(observer);
    }

    public final void remove(Observer observer) {
        observers.remove(observer);
    }

    public abstract void startScan();

    public abstract void stopScan();

    public abstract boolean isConnected();

    public abstract void close();

    public abstract void connect(String address);

    protected final void onScanStart() {
        for (Observer observer : observers) {
            observer.onScanStart();
        }
    }

    protected final void onScanResult(String name) {
        for (Observer observer : observers) {
            observer.onScanResult(name);
        }
    }

    protected final void onScanEnd() {
        for (Observer observer : observers) {
            observer.onScanEnd();
        }
    }

    protected final void onConnect(String name) {
        for (Observer observer : observers) {
            observer.onConnect(name);
        }
    }

    protected final void onDisconnect(String name) {
        for (Observer observer : observers) {
            observer.onDisconnect(name);
        }
    }

    protected final void onNewValues(int values[]) {
        for (Observer observer : observers) {
            observer.onNewValues(values);
        }
    }

    public static void maybeEnableBluetooth(Activity activity) {
        BluetoothManager bluetoothManager =
                (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetooth = bluetoothManager.getAdapter();

        if (bluetooth == null || !bluetooth.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, 0);
        }
    }
}
