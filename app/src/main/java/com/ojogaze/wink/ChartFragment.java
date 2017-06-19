package com.ojogaze.wink;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ChartFragment extends Fragment {
    private static final String TAG = "ChartFragment";

    private int channel1Color;
    private int feature1Color;
    private int feature2Color;

    private ChartView chartView;

    private ChartView.Chart channel1;
    private ChartView.Chart feature1;
    private ChartView.Chart feature2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        channel1Color = getResources().getColor(android.R.color.holo_blue_dark);
        feature1Color = getResources().getColor(android.R.color.holo_orange_dark);
        feature2Color = getResources().getColor(android.R.color.holo_green_light);
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartView = ((ChartView) getView().findViewById(R.id.eog));
        channel1 = chartView.makeLineChart(channel1Color, 2);
        channel1.setXRange(0, GestureRecognizer.GRAPH_LENGTH);

        feature1 = chartView.makePointsChart(feature1Color, 3);
        feature1.setXRange(0, GestureRecognizer.GRAPH_LENGTH);

        feature2 = chartView.makePointsChart(feature2Color, 3);
        feature2.setXRange(0, GestureRecognizer.GRAPH_LENGTH);
    }

    public void clear() {
        chartView.clear();
    }

    public void updateChannel1(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            points.add(Pair.create(i, data[i]));
        }
        channel1.setYRange(range.first, range.second);
        channel1.setData(points);
    }

    public void updateFeature1(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                points.add(Pair.create(i, data[i]));
            }
        }
        feature1.setYRange(range.first, range.second);
        feature1.setData(points);
    }

    public void updateFeature2(int data[], Pair<Integer,Integer> range) {
        List<Pair<Integer, Integer>> points = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                points.add(Pair.create(i, data[i]));
            }
        }
        feature2.setYRange(range.first, range.second);
        feature2.setData(points);
    }

    public void updateUI() {
        chartView.invalidate();
    }
}
