package com.ojogaze.wink;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements
        BluetoothSmartClient.BluetoothDeviceListener {
    private final static String TAG = "MainActivity";

    public static final int GRAPH_LENGTH = 1000;			// 5 seconds at 200Hz

    private final int chartValues[] = new int[GRAPH_LENGTH];
    private BluetoothSmartClient patchClient;

    private ChartFragment chart;
    private TextView count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hideBars();

        BluetoothSmartClient.maybeEnableBluetooth(this);

        chart = (ChartFragment) getSupportFragmentManager().findFragmentById(R.id.chart);
        count = (TextView) findViewById(R.id.count);
    }

    @Override
    protected void onStart() {
        super.onStart();
        patchClient = new BluetoothSmartClient(this, this);
        patchClient.startScan();
    }

    @Override
    protected void onStop() {
        if (patchClient != null) {
            patchClient.stopScan();
            patchClient.close();
            patchClient = null;
        }
        super.onStop();
    }

    public void onScanStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress).setVisibility(View.VISIBLE);
            }
        });
    }

    public void onScanResult(String deviceAddress) {
        patchClient.stopScan();
        patchClient.connect(deviceAddress);
    }

    public void onScanEnd() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress).setVisibility(View.GONE);
            }
        });
    }

    public void onConnect(String address) {
        Log.i(TAG, String.format("Connected to %s", address));
    }

    public void onDisconnect(String address) {
        Log.i(TAG, String.format("Disconnected from %s", address));
        if (patchClient != null) {
            patchClient.startScan();
        }
    }

    public void onNewValues(int values[]) {
        System.arraycopy(chartValues, values.length, chartValues, 0, values.length);
        System.arraycopy(chartValues, chartValues.length - values.length,
                values, values.length, values.length);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isDestroyed() || isRestricted() || isFinishing()) {
                    return;
                }
                chart.updateChannel1(chartValues, Pair.create(0, 255));
            }
        });
    }

    private void hideBars() {
        findViewById(R.id.chart).setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
