package com.example.mrhs.opencvtest;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //********************************** STATIC **********************************
    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    //********************************** DATA **********************************
    LinearLayout linearLayout;
    Mat mainMat, hsvMat, mask, morphOutput;
    SignalHandler signalHandler;

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private Tutorial3View mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

//        imgView = (ImageView) findViewById(R.id.imgView);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.relativeLayout);
        mOpenCvCameraView = new Tutorial3View(this, 0);
        mOpenCvCameraView.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.FILL_PARENT, ConstraintLayout.LayoutParams.FILL_PARENT));
        layout.addView(mOpenCvCameraView);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mOpenCvCameraView.setResolution(mOpenCvCameraView.getResolutionList().get(4));

        mainMat = new Mat(height, width, CvType.CV_8UC4);
        hsvMat = new Mat(height, width, CvType.CV_8UC4);
        mask = new Mat(height, width, CvType.CV_8UC4);
        morphOutput = new Mat(height, width, CvType.CV_8UC4);

        signalHandler = new SignalHandler();
    }

    public void onCameraViewStopped() {
        mainMat.release();
        hsvMat.release();
        mask.release();
        morphOutput.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // remove some noise
        Imgproc.blur(inputFrame.rgba(), mainMat, new Size(2, 2));

        // convert the frame to HSV
        Imgproc.cvtColor(mainMat, hsvMat, Imgproc.COLOR_BGR2HSV);

        // get thresholding values from the UI
        // remember: H ranges 0-180, S and V range 0-255
        Scalar minValues = new Scalar(44, 200, 200);
        Scalar maxValues = new Scalar(72, 255, 255);

        // threshold HSV image to select tennis balls
        Core.inRange(hsvMat, minValues, maxValues, mask);

        // morphological operators
        // dilate with large element, erode with small ones
        Integer amount = 6;
        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(amount * 2, amount * 2));
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(amount, amount));

        Imgproc.erode(mask, morphOutput, erodeElement);
        Imgproc.erode(mask, morphOutput, erodeElement);

        Imgproc.dilate(mask, morphOutput, dilateElement);
        Imgproc.dilate(mask, morphOutput, dilateElement);

        // init object tracking
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        // find contours
        Imgproc.findContours(morphOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        //Find the biggest contour
//        Collections.max(contours, new CustomComparator());

        //Calculate the center of the contours and show them
        ArrayList<Point> centerOfContours = new ArrayList();
        ArrayList<Double> areaOfContours = new ArrayList();
        for (MatOfPoint i : contours) {
            centerOfContours.add(centerOfContour(i));
            areaOfContours.add(Imgproc.contourArea(i));
            Imgproc.circle(mainMat, centerOfContour(i), 2, new Scalar(255, 255, 255), -1);
        }

        signalHandler.setStep(centerOfContours, areaOfContours);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        for (Signal signal : signalHandler.getSignalsList()) {
            Imgproc.putText(mainMat, "F: " + df.format(signal.calculateFrequency()) + " DC: " + df.format(signal.calculateDutyCycle()), signal.getCurrentLocation(), Core.FONT_ITALIC, 0.5,
                    new Scalar(255, 255, 255), 2);
        }

        // if any contour exist...
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            // for each contour, display it in blue
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                Imgproc.drawContours(mainMat, contours, idx, new Scalar(250, 0, 0));
            }
        }


//        Mat blurredImage;
//        Mat hsvImage = new Mat();
////        Mat mask = new Mat();
////        Mat morphOutput = new Mat();
//
//        // TODO Auto-generated method stub
//        mRgba = inputFrame.rgba();
//        // Rotate mRgba 90 degrees
//        Core.transpose(mRgba, mRgbaT);
//        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0, 0, 0);
//        Core.flip(mRgbaF, mRgba, 1);
//        blurredImage = mRgba;
//
//
//        // remove some noise
//        int numberOfTimes = 0;
//
//        for (int i = 0; i < numberOfTimes; i++) {
////            hsvImage = blurredImage.clone();
//            Imgproc.blur(blurredImage, blurredImage, new Size(100, 100));
//        }
//
//        // convert the frame to HSV
//        Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);
//
//        MatOfInt one = new MatOfInt(0);
//
//        MatOfInt histSize = new MatOfInt(10);
//        MatOfFloat range = new MatOfFloat(0f, 180f);
//
//        ArrayList<Mat> list = new ArrayList<Mat>();
//        list.add(hsvImage);
//
//        Imgproc.calcHist(list, one, new Mat(), hist, histSize, range);
//
//        Log.i("Histogram", "width: " + hist.width());
//        Log.i("Histogram", "height: " + hist.height());
//        for (int i = 0; i < 10; i++) {
//            Log.i("Element " + i, "" + hist.get(i, 0)[0]);
//        }
//
//        //Drawing the histogram
//        Core.normalize(hist, hist, 50, 0, Core.NORM_INF);
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                txtViewFps.setText(fps + " fps");
//                linearLayout.removeAllViews();
//                for (int i = 0; i < 10; i++) {
//                    TextView txtView = new TextView(MainActivity.this);
//                    txtView.setText(i + ": " + hist.get(i, 0)[0]);
//                    txtView.setTextColor(Color.WHITE);
//                    linearLayout.addView(txtView);
//                }
//            }
//        });

        return mainMat; // This function must return
    }

    //***************************** Utility functions *****************************

    private Point centerOfContour(MatOfPoint contur) {
        Moments M = Imgproc.moments(contur);
        return new Point(M.get_m10() / M.get_m00(), M.get_m01() / M.get_m00());
    }


}