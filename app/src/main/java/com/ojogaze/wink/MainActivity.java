package com.ojogaze.wink;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends FragmentActivity implements EogDevice.Observer {
    private final static String TAG = "MainActivity";

    private static final int SACCADE_THRESHOLD = 10;  // 800

    private SaccadeRecognizer saccadeRecognizer;
    private EogDevice device;

    private ChartFragment chart;
    private View chartContainer;
    private TextView count;
    private GestureView gestureView;
    private Switch toggle;

    private Timer resetTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideBars(findViewById(R.id.chart));

        EogDevice.maybeEnableBluetooth(this);

        chart = (ChartFragment) getSupportFragmentManager().findFragmentById(R.id.chart);
        chartContainer = findViewById(R.id.chart_container);
        count = (TextView) findViewById(R.id.count);
        gestureView = (GestureView) findViewById(R.id.gestures);
        toggle = (Switch) findViewById(R.id.toggle);

        findViewById(R.id.eog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (saccadeRecognizer == null) {
                    return;
                }
                Pair<Integer, Integer> minmax = getMinMax(saccadeRecognizer.window);
                Log.d(TAG, String.format("Min Max %d %d", minmax.first, minmax.second));
            }
        });

        ((CheckBox) findViewById(R.id.show_chart)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                        chartContainer.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
                        hideBars(btn);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
//        device = new ShimmerClient(this);
        device = new BluetoothSmartClient(this);
        device.add(this);
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        } else {
            device.startScan();
        }
        new Timer().schedule(chartUpdater, 100, 100);
    }

    @Override
    protected void onPause() {
        if (device != null) {
            device.stopScan();
            device.remove(this);
            device.close();
            device = null;
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            device.startScan();
        }
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
        device.stopScan();
        device.connect(deviceAddress);
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
        saccadeRecognizer = new SaccadeRecognizer(device.getSamplingFrequency(), SACCADE_THRESHOLD);
    }

    public void onDisconnect(String address) {
        Log.i(TAG, String.format("Disconnected from %s", address));
        if (device != null) {
            device.startScan();
        }
    }

    public void onNewValues(int values[]) {
        for (int i = 0; i < values.length; i++) {
            saccadeRecognizer.update(values[i]);
            if (saccadeRecognizer.hasSaccade()) {
                showGesture(saccadeRecognizer.saccadeAmplitude < 0
                        ? GestureView.Direction.LEFT : GestureView.Direction.RIGHT);
            }
        }
    }

    private void showGesture(final GestureView.Direction direction) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isDestroyed() || isRestricted() || isFinishing()) {
                    return;
                }
                gestureView.showArrow(direction, true /* clear */);
                reset(1000);
                toggle.setChecked(!toggle.isChecked());
            }
        });
    }

    private void reset(int delayMillis) {
        if (resetTimer != null) {
            resetTimer.cancel();
        }
        resetTimer = new Timer();
        resetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isDestroyed() || isRestricted() || isFinishing()) {
                            return;
                        }
                        gestureView.clear();
                    }
                });
            }
        }, delayMillis);
    }

    private TimerTask chartUpdater = new TimerTask() {
        @Override
        public void run() {
            if (saccadeRecognizer == null) {
                return;
            }
//            Pair<Integer, Integer> minMax = Pair.create(-5000, 5000);
//            Pair<Integer, Integer> minMax = getMinMax(saccadeRecognizer.window);
            Pair<Integer, Integer> minMax = Pair.create(100, 150);
//            Pair<Integer, Integer> minMax = Pair.create(-20, 20);
            chart.clear();
            chart.updateChannel1(saccadeRecognizer.window, minMax);
            chart.updateFeature1(saccadeRecognizer.feature1, minMax);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isDestroyed() || isRestricted() || isFinishing()) {
                        return;
                    }
                    chart.updateUI();
                }
            });
        }
    };

    private void hideBars(View view) {
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private static Pair<Integer, Integer> getMinMax(int series[]) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int value : series) {
            min = value < min ? value : min;
            max = value > max ? value : max;
        }
        return Pair.create(min, max);
    }
}
