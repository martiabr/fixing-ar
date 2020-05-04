package com.example.fixingar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
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

// Average re-projection error: 0.226845
//2020-03-28 14:30:00.740 30657-31407/com.example.fixingar I/CameraCalibrator: Camera matrix: [1107.420238521566, 0, 639.5;
//     0, 1107.420238521566, 359.5;
//     0, 0, 1]
//2020-03-28 14:30:00.740 30657-31407/com.example.fixingar I/CameraCalibrator: Distortion coefficients: [0.1777787296818345;
//     -0.4618245249197365;
//     0;
//     0;
//     -0.1959808318795349]


public class MainActivity extends CameraActivity implements CvCameraViewListener2, View.OnClickListener {

    //Constants
    private static final String TAG = "Main";
    private static final float MARKER_SIZE = (float) 0.13;

    //Preferences
    private static final boolean SHOW_MARKERID = true;

    //You must run a calibration prior to detection
    // The activity to run calibration is provided in the repository

    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    private MenuItem               mItemType;

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

    private float                  mRelativeEyeSize   = 0.1f; // change this parameter to adjust min Eye size
    private int                    mAbsoluteEyeSize   = 0;
    private float[]                mCoordinates = new float[4]; //x and y position in m

    private float                  mRelativeFaceSize   = 0.2f; // change this parameter to adjust min Face size
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    private int mCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
    private TextView mDebugText;
    private Button mCameraButton;

    private Handler mHandler = new Handler();
    private boolean timerRunning = true;
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
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mHandler.removeCallbacks(mCameraSwitchRunnable);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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
        mGray = inputFrame.gray();

        // Do marker detection if we use the back camera:
        if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK) {
            //Setup required parameters for detect method
            MarkerDetector mDetector = new MarkerDetector();
            Vector<Marker> detectedMarkers = new Vector<>();
            CameraParameters camParams = new CameraParameters("back");

            camParams.read(this);

            //Populate detectedMarkers
            mDetector.detect(mRgba, detectedMarkers, camParams, MARKER_SIZE);

            //Draw Axis for each marker detected
            if (detectedMarkers.size() != 0) {
                for (int i = 0; i < detectedMarkers.size(); i++) {
                    Marker marker = detectedMarkers.get(i);

                    debugMsg(marker.getRvec().dump() + "\n" + marker.getTvec().dump());
                    // Rvec and Tvec are the rotation and translation from the marker frame to the camera frame!
                    // Use Rodriguez() method from calib3d to turn rotation vector into rotation matrix if we need this.
                    // The x,y,z position of the camera is: cameraPosition = -rotM.T * tvec
                    // ProjectPoints projects 3D points to image plane
                    // EstimateAffine3D computes an optimal affine transformation between two 3D point sets
                    // SolvePnP finds an object pose from 3D-2D point correspondences
                    // warpPerspective applies a perspective transformation to an image

                    detectedMarkers.get(i).draw3dAxis(mRgba, camParams);
                    detectedMarkers.get(i).draw3dCube(mRgba, camParams, new Scalar(255,255,0));
                }
            }
          
        } else if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT) {
        CameraParameters camParams_f = new CameraParameters("front");
        camParams_f.read(this);
        Mat Cmat = camParams_f.getCameraMatrix();

        FaceDetection facedetection = new FaceDetection(Cmat, mJavaDetector1, mJavaDetector2, mNativeDetector1, mNativeDetector2);
        mCoordinates = facedetection.getmCoordinates(mRgba, mGray);
            // mCoordinates[0] = x
            // mCoordinates[1] = y
            // mCoordinates[2] = z
            // mCoordinates[3] = 1 or 2 if face or eyes were found, it's 0 if nothing was found

            String mess1 = facedetection.ObjDetect(mCoordinates);
            String mess = mess1 + "Dist: " + Float.toString(mCoordinates[3]) + "m, x: " + Float.toString(mCoordinates[0]) + "m, y: " + Float.toString(mCoordinates[1]) + "m";
            debugMsg(mess);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType   = menu.add(mDetectorName[mDetectorType]);
        return true;
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

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
            setDetectorType(tmpDetectorType);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }
    private void setMinEyeSize(float eyeSize) {
        mRelativeEyeSize = eyeSize;
        mAbsoluteEyeSize = 0;
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

    private boolean getPermission(){
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.CAMERA},
                    50);
        } else {
            return true;
        }
        return false;
    }
}
