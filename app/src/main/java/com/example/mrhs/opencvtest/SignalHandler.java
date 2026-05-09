package com.example.mrhs.opencvtest;

import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;

public class SignalHandler {

    //********************************** STATIC **********************************
    final static int TOLERANCE = 5;
    final static int SIGNAL_TIMEOUT_MILI = 5000;
    final static int SIGNAL_HANDLER_TIMEOUT_MILI = 1000;

    //********************************** DATA **********************************
    Long lastUpdatetime;
    ArrayList<Long> seenTimeList;
    ArrayList<Signal> signalsList;

    //********************************** FUNCTION **********************************
    public SignalHandler() {
        this.seenTimeList = new ArrayList<>();
        this.signalsList = new ArrayList<>();
        this.lastUpdatetime = (long) 0;
    }

    public void setStep(ArrayList<Point> centers, ArrayList<Double> areas) {
        Log.d("SignalHandler", "size is " + signalsList.size());
        Long time = System.currentTimeMillis();
        checkForUpdate(time);
        ArrayList<Signal> tmpsignalsList = new ArrayList<>();
        seenTimeList.add(time);
        for (int i = 0; i < centers.size(); i++) {
            int index = findClosestSignal(signalsList, centers.get(i), areas.get(i), TOLERANCE);
            if (index == -1) {
                tmpsignalsList.add(new Signal(centers.get(i), areas.get(i), time));
            } else {
                signalsList.get(index).setCurrentCondition(centers.get(i), areas.get(i));
                signalsList.get(index).addToSeenTimeList(time);
            }
        }
        signalsList.addAll(tmpsignalsList);
    }

    private void checkForUpdate(Long time) {
        if (time - lastUpdatetime > SIGNAL_HANDLER_TIMEOUT_MILI) {
            lastUpdatetime = time;
            updateSignal(time);
        }
    }

    public void updateSignal(Long time) {
        ArrayList<Signal> tmpSignalsList = new ArrayList<>();
        for (Signal signal : signalsList) {
            if (time - signal.getLastSeenTime() < SIGNAL_TIMEOUT_MILI) {
                signal.reduceTimeList(seenTimeList);
                tmpSignalsList.add(signal);
            }
        }
        signalsList = tmpSignalsList;
    }

    //********************** Setter **********************
    public void setLastUpdatetime(Long lastUpdatetime) {
        this.lastUpdatetime = lastUpdatetime;
    }

    public void setSeenTimeList(ArrayList<Long> seenTimeList) {
        this.seenTimeList = seenTimeList;
    }

    public void setSignalsList(ArrayList<Signal> signalsList) {
        this.signalsList = signalsList;
    }

    //********************** Getter **********************
    public Long getLastUpdatetime() {
        return lastUpdatetime;
    }

    public ArrayList<Long> getSeenTimeList() {
        return seenTimeList;
    }

    public ArrayList<Signal> getSignalsList() {
        return signalsList;
    }

    //***************************** Utility functions *****************************
    private int findClosestSignal(ArrayList<Signal> points, Point center, Double area, Integer tolerance) {
        //TODO here integrate area in finding the closes signal
        float minDistance = 1000;
        int index = -1;
        float distance;
        for (int i = 0; i < points.size(); i++) {
            distance = calculateDistance(points.get(i).getCurrentLocation(), center);
            if (distance <= tolerance && distance < minDistance) {
                minDistance = distance;
                index = i;
            }
        }
        return index;
    }

    private float calculateDistance(Point a, Point b) {
        double xDiff = a.x - b.x;
        double yDiff = a.y - b.y;
        return (float) Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
    }
}
