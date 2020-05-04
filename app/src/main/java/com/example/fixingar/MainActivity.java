package com.example.fixingar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
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

public class MainActivity extends CameraActivity implements CvCameraViewListener2, View.OnClickListener {
    //Constants
    private static final String TAG = "Main";
    private static final float MARKER_SIZE = (float) 0.03;

    //Preferences
    private static final boolean SHOW_MARKERID = true;

    private CameraBridgeViewBase mOpenCvCameraView;
    private int mCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
    private TextView mDebugText;
    private Button mCameraButton;
    private CameraParameters camParams;
    private PerspectiveFixer perspectiveFixer;

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
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        //Convert input to rgba
        Mat rgba = inputFrame.rgba();
        Size rgbaSize = rgba.size();

        // Do marker detection if we use the back camera:
        if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK) {
            //Setup required parameters for detect method
            MarkerDetector mDetector = new MarkerDetector();
            Vector<Marker> detectedMarkers = new Vector<>();

            // TODO: For some stupid reason i am not able to move these init lines to onCreate()... Now we make new objects each frame which is stupid since the camera params are constant
            camParams = new CameraParameters();
            camParams.read(this);
            perspectiveFixer = new PerspectiveFixer(camParams);

            //Populate detectedMarkers
            mDetector.detect(rgba, detectedMarkers, camParams, MARKER_SIZE);
            Log.d("NumberMarker",detectedMarkers.toString());

            if (detectedMarkers.size() == 1) {
                Marker marker = detectedMarkers.get(0);
                Log.d("markerPoints", String.valueOf(marker.getPoints().get(0)));
                debugMsg(marker.getPoints().get(0) + "\n" + marker.getPoints().get(1) + "\n" + marker.getPoints().get(2) + "\n" + marker.getPoints().get(3) + "\n");
                debugMsg(marker.getRvec().dump() + "\n" + marker.getTvec().dump());

                // Mat dst = perspectiveFixer.fixPerspective(rgba, marker, MARKER_SIZE);
                //return rgba;
                return rgba;
            }

            //Draw Axis for each marker detected
            if (detectedMarkers.size() == 5) {
                Log.d("howmany","Detected 5 markers.");
                Log.d("markerPoints", String.valueOf(detectedMarkers.toArray().toString()));
                Mat dst = perspectiveFixer.fixPerspectiveMultipleMarker(rgba,detectedMarkers,MARKER_SIZE);
                return dst;
            }
        } else if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT) {
            // Do facial recognition here
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
}
