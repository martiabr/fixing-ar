package com.example.fixingar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import android.view.MotionEvent;

import android.view.Menu;
import android.view.MenuItem;

import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.samples.facedetect.DetectionBasedTracker;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.lang.Math;

import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;
import es.ava.aruco.MarkerDetector;

public class MainActivity extends CameraActivity implements CvCameraViewListener2, View.OnClickListener {

    //Constants
    private static final String TAG = "Main";
    private String WHO = "Julia";
    private float MARKER_SIZE;

    //You must run a calibration prior to detection
    // The activity to run calibration is provided in the repository

    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile1;
    private File                   mCascadeFile2;
    private CascadeClassifier      mJavaDetector1;
    private DetectionBasedTracker mNativeDetector1;
    private CascadeClassifier      mJavaDetector2;
    private DetectionBasedTracker mNativeDetector2;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float[]                mCoordinates = new float[4]; //x and y position in m
    private FaceDetection          faceDetection;

    private CameraBridgeViewBase mOpenCvCameraView;
    private int mCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
    private TextView mDebugText;
    private Button mCameraButton;
    private CameraParameters camParamsBack;
    private CameraParameters camParamsFront;
    private PerspectiveFixer perspectiveFixer;
    private Variables variables;

    private Handler mHandler = new Handler();
    private boolean timerRunning = false;
    private static final int DELAY = 5000;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile1 = new File(cascadeDir, "haarcascade_eye.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile1);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector1 = new CascadeClassifier(mCascadeFile1.getAbsolutePath());
                        if (mJavaDetector1.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector1 = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile1.getAbsolutePath());

                        mNativeDetector1 = new DetectionBasedTracker(mCascadeFile1.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile2 = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile2);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector2 = new CascadeClassifier(mCascadeFile2.getAbsolutePath());
                        if (mJavaDetector2.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector2 = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile2.getAbsolutePath());

                        mNativeDetector2 = new DetectionBasedTracker(mCascadeFile2.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                    mCameraButton.setOnClickListener(MainActivity.this);

                    if (timerRunning) {
                        mHandler.postDelayed(mCameraSwitchRunnable, DELAY);
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        setContentView(R.layout.activity_main);

        mDebugText = (TextView) findViewById(R.id.debug_text);

        mCameraButton = (Button) findViewById(R.id.camera_button);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(1280,720);
        mOpenCvCameraView.setCameraIndex(mCameraIndex);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mHandler.removeCallbacks(mCameraSwitchRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

            variables = new Variables(WHO);
            MARKER_SIZE = variables.getMarkerSize();

            camParamsBack = new CameraParameters("Back");
            camParamsBack.read(this);
            perspectiveFixer = new PerspectiveFixer(camParamsBack, WHO);

            camParamsFront = new CameraParameters("front");
            camParamsFront.read(this);
            faceDetection = new FaceDetection(camParamsFront.getCameraMatrix(), mJavaDetector1, mJavaDetector2, mNativeDetector1, mNativeDetector2,WHO);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mHandler.removeCallbacks(mCameraSwitchRunnable);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        Log.d("heightwidth",String.valueOf(mRgba.height()) + " " + String.valueOf(mRgba.width()));
        mGray = inputFrame.gray();
     

        // Do marker detection if we use the back camera:
        if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK) {
            //Setup required parameters for detect method
            MarkerDetector mDetector = new MarkerDetector();
            Vector<Marker> detectedMarkers = new Vector<>();

            //Populate detectedMarkers
            Log.d("camparamback",camParamsBack.getCameraMatrix().dump());
            Log.d("camparamfront",camParamsFront.getCameraMatrix().dump());
            mDetector.detect(mRgba, detectedMarkers, camParamsBack, MARKER_SIZE);

            if (detectedMarkers.size() >= 1 && detectedMarkers.size()<4) {
                Mat dst = perspectiveFixer.fixPerspectiveSingleMarker(mRgba, detectedMarkers.get(0), MARKER_SIZE, mCoordinates);
                return dst;
            }

            if (detectedMarkers.size() >= 4) {
                Log.d("markerPoints", String.valueOf(detectedMarkers.toArray().toString()));
                if (mCoordinates != null) {
                    if (mCoordinates[3] != 0) {
                        Mat dst = perspectiveFixer.fixPerspectiveMultipleMarker(mRgba, detectedMarkers, MARKER_SIZE, mCoordinates);
                return dst; }
                    else return mRgba;
                }
                else return mRgba;
            }
          
        } else if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT) {
            mCoordinates = faceDetection.getmCoordinates(mRgba, mGray);
            // mCoordinates[0] = x
            // mCoordinates[1] = y
            // mCoordinates[2] = z
            // mCoordinates[3] = 1 or 2 if face or eyes were found, it's 0 if nothing was found
            if (mCoordinates != null) {
            String mess1 = faceDetection.ObjDetect(mCoordinates);
            String mess = mess1 + "Dist: " + Float.toString(mCoordinates[2]) + "m, x: " + Float.toString(mCoordinates[0]) + "m, y: " + Float.toString(mCoordinates[1]) + "m";
            debugMsg(mess);
            }
        }

        return mRgba;
    }
  
      public void debugMsg(String msg) {
        final String str = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDebugText.setText(str);
            }
        });
    }

    private Runnable mCameraSwitchRunnable = new Runnable() {
        @Override
        public void run() {
            switchCameras();
            mHandler.postDelayed(this, DELAY);
        }
    };

    private boolean switchCameras() {
        if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK) {
            mCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;

        } else if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT){
            mCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
            perspectiveFixer = new PerspectiveFixer(camParamsBack, WHO);
        }

        Toast.makeText(MainActivity.this, "Switching camera to " + mCameraIndex, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Switching camera to " + mCameraIndex);
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraIndex);
        mOpenCvCameraView.enableView();

        return true;  // TODO: check success somehow?
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick invoked");

        if (timerRunning) {
            Toast.makeText(this, "Turning off", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Turning off");
            mHandler.removeCallbacks(mCameraSwitchRunnable);
        } else {
            Toast.makeText(this, "Turning on", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Turning on");
            mCameraSwitchRunnable.run();
        }

        timerRunning = !timerRunning;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector1.start();
                mNativeDetector2.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector1.stop();
                mNativeDetector2.stop();
            }
        }

    }
}
