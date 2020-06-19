package com.example.fixingar;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;

public class FrontCalibration extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "CameraCalibrator";

    private CameraBridgeViewBase mOpenCvCameraView;
    private int mCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
    private CameraCalibrator mCalibrator;
    private OnCameraFrameRender mOnCameraFrameRender;
    private Menu mMenu;
    private int mWidth;
    private int mHeight;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(FrontCalibration.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setCameraIndex(mCameraIndex);
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(FrontCalibration.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

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

        setContentView(R.layout.activity_calibration2);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(FrontCalibration.this);
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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, FrontCalibration.this, mLoaderCallback);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.calibration, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.preview_mode).setEnabled(true);
        if (mCalibrator != null && !mCalibrator.isCalibrated()) {
            menu.findItem(R.id.preview_mode).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.calibration:
                mOnCameraFrameRender =
                        new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
                item.setChecked(true);
                return true;
            case R.id.undistortion:
                mOnCameraFrameRender =
                        new OnCameraFrameRender(new UndistortionFrameRender(mCalibrator));
                item.setChecked(true);
                return true;
            case R.id.comparison:
                mOnCameraFrameRender =
                        new OnCameraFrameRender(new ComparisonFrameRender(mCalibrator, mWidth, mHeight, getResources()));
                item.setChecked(true);
                return true;
            case R.id.calibrate:
                final Resources res = getResources();
                if (mCalibrator.getCornersBufferSize() < 2) {
                    (Toast.makeText(FrontCalibration.this, res.getString(R.string.more_samples), Toast.LENGTH_SHORT)).show();
                    return true;
                }

                mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
                new AsyncTask<Void, Void, Void>() {
                    private ProgressDialog calibrationProgress;

                    @Override
                    protected void onPreExecute() {
                        calibrationProgress = new ProgressDialog(FrontCalibration.this);
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
                        (Toast.makeText(FrontCalibration.this, resultMessage, Toast.LENGTH_SHORT)).show();

                        if (mCalibrator.isCalibrated()) {
                            CalibrationResult.save(FrontCalibration.this,
                                    mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients(), "front");
                        }
                    }
                }.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            mCalibrator = new CameraCalibrator(mWidth, mHeight);
            if (CalibrationResult.tryLoad(FrontCalibration.this, mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients(), "front")) {
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
