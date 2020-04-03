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
import org.w3c.dom.Text;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

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

public class MainActivity extends CameraActivity implements CvCameraViewListener2, OnTouchListener {
    //Constants
    private static final String TAG = "Main";
    private static final float MARKER_SIZE = (float) 0.017;

    //Preferences
    private static final boolean SHOW_MARKERID = true;

    //You must run a calibration prior to detection
    // The activity to run calibration is provided in the repository

    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView mDebugText;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
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

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
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
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        //Convert input to rgba
        Mat rgba = inputFrame.rgba();

        //Setup required parameters for detect method
        MarkerDetector mDetector = new MarkerDetector();
        Vector<Marker> detectedMarkers = new Vector<>();
        CameraParameters camParams = new CameraParameters();

        //camParams.readFromFile(Environment.getExternalStorageDirectory().toString() + DATA_FILEPATH);
        camParams.read(this);

        //Populate detectedMarkers
        mDetector.detect(rgba, detectedMarkers, camParams, MARKER_SIZE);

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

                detectedMarkers.get(i).draw3dAxis(rgba, camParams);
                detectedMarkers.get(i).draw3dCube(rgba, camParams, new Scalar(255,255,0));

                /*if (SHOW_MARKERID) {
                    //Setup
                    int idValue = detectedMarkers.get(i).getMarkerId();
                    Vector<Point3> points = new Vector<>();
                    points.add(new Point3(0, 0, 0));
                    MatOfPoint3f pointMat = new MatOfPoint3f();
                    pointMat.fromList(points);
                    MatOfPoint2f outputPoints = new MatOfPoint2f();

                    //Project point to marker origin
                    Calib3d.projectPoints(pointMat, marker.getRvec(), marker.getTvec(), camParams.getCameraMatrix(), camParams.getDistCoeff(), outputPoints);
                    List<Point> pts = new Vector<>();
                    pts = outputPoints.toList();

                    //Draw id number
                    Imgproc.putText(rgba, Integer.toString(idValue), pts.get(0), Imgproc.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255,0,0));
                }*/
            }
        }
        return rgba;
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
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "onTouch invoked");

        return false;
    }
}
