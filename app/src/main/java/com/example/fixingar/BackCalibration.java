package com.example.fixingar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

public class BackCalibration extends CameraActivity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "CameraCalibrator";

    private CameraBridgeViewBase mOpenCvCameraView;
    private int mCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
    private CameraCalibrator mCalibrator;
    private OnCameraFrameRender mOnCameraFrameRender;
    private Menu mMenu;
    private int mWidth;
    private int mHeight;
    private Button mCalibrate;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setCameraIndex(mCameraIndex);
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(BackCalibration.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public BackCalibration() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_calibration2);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view2);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mCalibrate = findViewById(R.id.button_calibrate);
        mCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calibrate();
            }
        });
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

    public void Calibrate() {
        final Resources res = getResources();
        if (mCalibrator.getCornersBufferSize() < 2) {
            (Toast.makeText(this, res.getString(R.string.more_samples), Toast.LENGTH_SHORT)).show();
            return;
        }

        mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog calibrationProgress;

            @Override
            protected void onPreExecute() {
                calibrationProgress = new ProgressDialog(BackCalibration.this);
                calibrationProgress.setTitle(res.getString(R.string.calibrating));
                calibrationProgress.setMessage(res.getString(R.string.please_wait));
                calibrationProgress.setCancelable(false);
                calibrationProgress.setIndeterminate(true);
                calibrationProgress.show();
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                mCalibrator.calibrate();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                calibrationProgress.dismiss();
                mCalibrator.clearCorners();
                mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
                String resultMessage = (mCalibrator.isCalibrated()) ?
                        res.getString(R.string.calibration_successful)  + " " + mCalibrator.getAvgReprojectionError() :
                        res.getString(R.string.calibration_unsuccessful);
                (Toast.makeText(BackCalibration.this, resultMessage, Toast.LENGTH_SHORT)).show();

                if (mCalibrator.isCalibrated()) {
                    CalibrationResult.save(BackCalibration.this,
                            mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients(), "back");
                }
            }
        }.execute();
        if (mCalibrator.isCalibrated()) {
            Intent intent = new Intent(this, Calibration.class);
            startActivity(intent);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            mCalibrator = new CameraCalibrator(mWidth, mHeight);
            if (CalibrationResult.tryLoad(BackCalibration.this, mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients(), "back")) {
                mCalibrator.setCalibrated();
            } else {
                if (mMenu != null && !mCalibrator.isCalibrated()) {
                    mMenu.findItem(R.id.preview_mode).setEnabled(false);
                }
            }

            mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
        }
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return mOnCameraFrameRender.render(inputFrame);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "onTouch invoked");

        mCalibrator.addCorners();
        return false;
    }

}
