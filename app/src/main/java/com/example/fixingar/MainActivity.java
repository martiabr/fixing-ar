package com.example.fixingar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "MainActivity";

    private CameraBridgeViewBase mOpenCvCameraViewBack;
    //private CameraBridgeViewBase mOpenCvCameraViewFront;
    private int cameraIndex = 1;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraViewBack.enableView();
                    mOpenCvCameraViewBack.setOnTouchListener(MainActivity.this);
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        if(getPermission()){
            mOpenCvCameraViewBack = (CameraBridgeViewBase) findViewById(R.id.camera_view_back);
            //mOpenCvCameraViewFront = (CameraBridgeViewBase) findViewById(R.id.camera_view_front);

            mOpenCvCameraViewBack.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraViewBack.setCameraIndex(cameraIndex);
            mOpenCvCameraViewBack.setCvCameraViewListener(this);

            //mOpenCvCameraViewFront.setVisibility(SurfaceView.VISIBLE);
            //mOpenCvCameraViewFront.setCvCameraViewListener(this);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraViewBack != null)
            mOpenCvCameraViewBack.disableView();
        //if (mOpenCvCameraViewFront != null)
            //mOpenCvCameraViewFront.disableView();
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
        //if (cameraIndex == 0) {
            return Collections.singletonList(mOpenCvCameraViewBack);
        //}
        //return Collections.singletonList(mOpenCvCameraViewFront);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraViewBack != null)
            mOpenCvCameraViewBack.disableView();
        //if (mOpenCvCameraViewFront != null)
        //    mOpenCvCameraViewFront.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG,"onTouch event");
        Toast.makeText(this, "How ya goin cunt", Toast.LENGTH_SHORT).show();

        Log.i(TAG, String.valueOf(cameraIndex));

        if (cameraIndex == 0) {
            cameraIndex = 1;
        } else {
            cameraIndex = 0;
        }

        mOpenCvCameraViewBack.disableView();
        mOpenCvCameraViewBack.setCameraIndex(cameraIndex);
        mOpenCvCameraViewBack.enableView();


        /*if (cameraIndex == 0) {
            Log.i(TAG, String.valueOf(cameraIndex));

            //mOpenCvCameraViewBack.setVisibility(SurfaceView.GONE);
            mOpenCvCameraViewBack.disableView();
            mOpenCvCameraViewFront = (CameraBridgeViewBase) findViewById(R.id.camera_view_front);
            //mOpenCvCameraViewFront.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraViewFront.enableView();

            cameraIndex = 1;
        } else if (cameraIndex == 1){
            Log.i(TAG, String.valueOf(cameraIndex));

            //mOpenCvCameraViewFront.setVisibility(SurfaceView.GONE);
            mOpenCvCameraViewFront.disableView();
            mOpenCvCameraViewBack = (CameraBridgeViewBase) findViewById(R.id.camera_view_back);
            //mOpenCvCameraViewBack.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraViewBack.enableView();

            cameraIndex = 0;
        }*/

        return false;
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
