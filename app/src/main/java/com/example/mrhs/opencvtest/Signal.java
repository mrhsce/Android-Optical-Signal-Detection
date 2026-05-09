package com.example.mrhs.opencvtest;

import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;

/**
 * Created by mrhs on 11/9/2017.
 */

public class Signal {

    //********************************** DATA **********************************
    private Point currentLocation;
    private double currentArea;
    private Long firstSeenTime;
    private Long lastSeenTime;

    private ArrayList<Long> seenTimeList;
    private ArrayList<TimeSlot> signalList;

    //********************************** FUNCTION **********************************

    //********************** Constructor **********************
    public Signal(Point currentLocation, Double currentArea, Long firstSeenTime) {
        this.currentLocation = currentLocation;
        this.currentArea = currentArea;
        this.firstSeenTime = firstSeenTime;
        this.lastSeenTime = firstSeenTime;
        this.seenTimeList = new ArrayList<>();
        this.signalList = new ArrayList<>();
    }


    //********************** Setter **********************
    public void setCurrentCondition(Point center, Double area) {
        setCurrentLocation(center);
        setCurrentArea(area);
    }

    public void setCurrentLocation(Point currentLocation) {
        this.currentLocation = currentLocation;
    }

    public void setCurrentArea(double currentArea) {
        this.currentArea = currentArea;
    }

    public void setFirstSeenTime(Long firstSeenTime) {
        this.firstSeenTime = firstSeenTime;
    }

    public void setLastSeenTime(Long lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }

    public void addToSeenTimeList(Long timeSeen) {
        this.seenTimeList.add(timeSeen);
        lastSeenTime = timeSeen;
    }

    //********************** Getter **********************
    public Point getCurrentLocation() {
        return currentLocation;
    }

    public double getCurrentArea() {
        return currentArea;
    }

    public Long getFirstSeenTime() {
        return firstSeenTime;
    }

    public Long getLastSeenTime() {
        return lastSeenTime;
    }

    public ArrayList<Long> getSeenTimeList() {
        return seenTimeList;
    }

    //******************************* Utility functions *******************************
    public float calculateDutyCycle() {
        if (signalList.size() > 2) {
            double sum = 0, total = signalList.get(signalList.size() - 1).first - signalList.get(0).first;
            for (int i = 0; i < signalList.size() - 1; i++) {
                sum += signalList.get(i).diff;
            }
            Log.d("Duty cycle", "Duty cycle " + (float) (sum / total));
            return (float) (sum / total);
        } else {
            return -1;
        }
    }

    public float calculateFrequency() {
        if (signalList.size() > 2) {
            double total = signalList.get(signalList.size() - 1).first - signalList.get(0).first;
            Log.d("Frequency", "Frequency" + (float) ((signalList.size() - 1) / total) * 1000);
            return (float) ((signalList.size() - 1) / total) * 1000;
        } else {
            return -1;
        }
    }

    // This function is for checking if the time slots are equal or not
    public boolean areSlotsEqual() {
        return false;
    }

    public void reduceTimeList(ArrayList<Long> seenTimeList) {
        if (this.seenTimeList.size() > 2) {
            long startTime = this.seenTimeList.get(0);
            long endTime;
            int localIndex = 0;
            int mainIndex = seenTimeList.indexOf(this.seenTimeList.get(localIndex));
            while (localIndex < this.seenTimeList.size()) {
                //When signal is active
                if (this.seenTimeList.get(localIndex) == seenTimeList.get(mainIndex)) {
                    mainIndex++;
                    localIndex++;
                    continue;
                }
                //When signal is inactive
                else {
                    endTime = this.seenTimeList.get(localIndex - 1);
                    signalList.add(new TimeSlot(startTime, endTime));
                    while (this.seenTimeList.get(localIndex) != seenTimeList.get(mainIndex)) {
                        mainIndex++;
                    }
                    startTime = this.seenTimeList.get(localIndex);
                }
            }
            if (signalList.size() > 1) {
                clearTimeList();
            }
        }
    }

    private void clearTimeList() {
        long last = signalList.get(signalList.size() - 1).last;
        ArrayList<Long> tmpSeenTimeList = new ArrayList<>();
        for (Long seenTime : this.seenTimeList) {
            if (seenTime > last) {
                tmpSeenTimeList.add(seenTime);
            }
        }
        this.seenTimeList = tmpSeenTimeList;
    }

    //********************************** TIME FRAME **********************************
    public class TimeSlot {
        Long first, last, diff;

        public TimeSlot(Long first, Long last) {
            this.first = first;
            this.last = last;
            this.diff = last - first;
        }
    }

}
