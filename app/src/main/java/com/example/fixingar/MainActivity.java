package com.example.fixingar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

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

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";

    private static final Scalar EYE_RECT_COLOR     = new Scalar(0, 150, 0, 150);
    private static final Scalar FACE_RECT_COLOR     = new Scalar(150, 0, 150, 0);
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
    private int                    NumEyes;
    private int[][]                AllEyeCoordinates;
    private int[]                  EyeCoordinates; //contains x & y coordinate, dist, 1 or 2 to define if one eye or two were found

    private float                  mRelativeFaceSize   = 0.2f; // change this parameter to adjust min Face size
    private int                    mAbsoluteFaceSize   = 0;
    private int                    NumFaces;
    private int[][]                AllFaceCoordinates;
    private int[]                  FaceCoordinates; //contains x & y coordinate, width

    private CameraBridgeViewBase mOpenCvCameraView;

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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        if(getPermission()){
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
        }
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

        if (mAbsoluteEyeSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeEyeSize) > 0) {
                mAbsoluteEyeSize = Math.round(height * mRelativeEyeSize);
            }
            mNativeDetector1.setMinFaceSize(mAbsoluteEyeSize);
        }
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector2.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect eyes = new MatOfRect();
        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector1 != null)
                mJavaDetector1.detectMultiScale(mGray, eyes, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteEyeSize, mAbsoluteEyeSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector1 != null)
                mNativeDetector1.detect(mGray, eyes);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }
        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector2 != null)
                mJavaDetector2.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector2 != null)
                mNativeDetector2.detect(mGray, faces);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }
        // Here the eye coordinates and distance are found in bits in the array "EyeCoordinates".
        // The app always finds the biggest eye. I implemented two scenarios: either the app finds
        // a second eye belonging to the first one (found by smallest distance) or not. The last
        // entry in "EyeCoordinates" describes which case we are in:
        // If EyeCoordinates[3] = 1: Only one eye was found. In this case:
        // EyeCoordinates[0] contains the x coordinate in bits.
        // EyeCoordinates[1] contains the y coordinate in bits.
        // EyeCoordinates[2] contains the width of the square in bits.
        // If EyeCoordinates[3] = 2: Two eyes were found. In this case:
        // EyeCoordinates[0] contains the x location between both eyes in bits.
        // EyeCoordinates[1] contains the y location between both eyes in bits.
        // EyeCoordinates[2] contains the distance between the eyes in bits.
        Rect[] eyesArray;
        eyesArray = eyes.toArray();
        NumEyes = eyesArray.length;
        AllEyeCoordinates = new int[NumEyes][3];
        EyeCoordinates = new int[4];
        for (int i = 0; i < NumEyes; i++) {
            Imgproc.rectangle(mRgba, eyesArray[i].tl(), eyesArray[i].br(), EYE_RECT_COLOR, 3);
            if (NumEyes > 0) {
                AllEyeCoordinates[i][0] = eyesArray[i].x;
                AllEyeCoordinates[i][1] = eyesArray[i].y;
                AllEyeCoordinates[i][2] = eyesArray[i].width;
                }
        }
        if (NumEyes == 1) {
                EyeCoordinates[0] = AllEyeCoordinates[0][0];
                EyeCoordinates[1] = AllEyeCoordinates[0][1];
                EyeCoordinates[2] = AllEyeCoordinates[0][2];
                EyeCoordinates[3] = 1;
            }
        else if (NumEyes > 1) {
            int width = AllEyeCoordinates[0][2];
            int index1 = 0;
            for (int i = 0; i < NumEyes; i++) {
                if (AllEyeCoordinates[i][2] > width) {
                    index1 = i;
                    width = AllEyeCoordinates[index1][2];
                }
            }
            int index2 = 0;
            int dist = 10000000;
            for (int i = 0; i < NumEyes && i != index1; i++){
                int find = ((AllEyeCoordinates[index1][0]-AllEyeCoordinates[i][0])^2+(AllEyeCoordinates[index1][1]-AllEyeCoordinates[i][1])^2)^(1/2);
                if (find<dist) {
                    index2 = i;
                    dist = find;
                }
            }
            EyeCoordinates[3] = 2;
            for (int i = 0; i < NumEyes && i != index1 && i!= index2; i++){
                int find = ((AllEyeCoordinates[index2][0]-AllEyeCoordinates[i][0])^2+(AllEyeCoordinates[index2][1]-AllEyeCoordinates[i][1])^2)^(1/2);
                if (find<dist) {
                    EyeCoordinates[3] = 1;
                }
            }
            if (EyeCoordinates[3] == 1) {
                EyeCoordinates[0] = AllEyeCoordinates[index1][0];
                EyeCoordinates[1] = AllEyeCoordinates[index1][1];
                EyeCoordinates[2] = AllEyeCoordinates[index1][2];
            }
            else {
                EyeCoordinates[0] = (AllEyeCoordinates[index1][0]+AllEyeCoordinates[index2][0])/2;
                EyeCoordinates[1] = (AllEyeCoordinates[index1][1]+AllEyeCoordinates[index2][1])/2;
                EyeCoordinates[2] = dist;
            }

        }
        Rect[] facesArray;
        facesArray = faces.toArray();
        NumFaces = facesArray.length;
        AllFaceCoordinates = new int[NumFaces][3];
        FaceCoordinates = new int[3];
        for (int i = 0; i < NumFaces; i++) {
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
            }


        return mRgba;

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

    @Override
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
