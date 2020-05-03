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

    public String                  FrontOrBack;

    //Preferences
    private static final boolean SHOW_MARKERID = true;

    //You must run a calibration prior to detection
    // The activity to run calibration is provided in the repository

    private static final Scalar     COLOR1     = new Scalar(0, 150, 0, 150);
    private static final Scalar     COLOR2     = new Scalar(150, 0, 150, 0);
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
    private int[]                  Coordinates; //contains x & y coordinate, dist, 1 or 2 to define if one eye or two were found
    private float[]                mCoordinates; //x and y position in m

    private float                  mRelativeFaceSize   = 0.2f; // change this parameter to adjust min Face size
    private int                    mAbsoluteFaceSize   = 0;
    private int                    NumFaces;
    private int[][]                AllFaceCoordinates;

    private float                  EstimatedFaceWidth   = 0.14f; // in m
    private float                  EstimatedEyeDist     = 0.06f; // in m
    private float                  DistFace;//in m

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
            FrontOrBack = "back";
            //Setup required parameters for detect method
            MarkerDetector mDetector = new MarkerDetector();
            Vector<Marker> detectedMarkers = new Vector<>();
            CameraParameters camParams = new CameraParameters(FrontOrBack);

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
            FrontOrBack = "front";
            // Do facial recognition here
        CameraParameters camParams_f = new CameraParameters(FrontOrBack);

        //camParams.readFromFile(Environment.getExternalStorageDirectory().toString() + DATA_FILEPATH);
        camParams_f.read(this);
        Mat Cmat = camParams_f.getCameraMatrix();
          
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
        // Here the coordinates and distance are found in bits in the array "Coordinates".
        // If two eyes are found:
        // Coordinates[0] = (x1+x2)/2 (eye positions)
        // Coordinates[1] = (y1+y2)/2
        // Coordinates[2] = distance between eyes
        // Coordinates[3] = 2 (to indicate two eyes were found)
        // If only one eye was found (or several eyes, but they weren't matching)
        // Coordinates[0] = x (face position)
        // Coordinates[1] = y
        // Coordinates[2] = width (width of face according to rectangle width)
        // Coordinates[3] = 1 (to indicate one face was found)
        // if nothing is found:
        // Coordinates[3] = 0
        Rect[] eyesArray;
        eyesArray = eyes.toArray();
        NumEyes = eyesArray.length;
        AllEyeCoordinates = new int[NumEyes][2];
        Coordinates = new int[4];
        for (int i = 0; i < NumEyes; i++) {
            Imgproc.rectangle(mRgba, eyesArray[i].tl(), eyesArray[i].br(), COLOR1, 3);
            if (NumEyes > 0) {
                AllEyeCoordinates[i][0] = eyesArray[i].x;
                AllEyeCoordinates[i][1] = eyesArray[i].y;
            }
        }
        int width = mGray.cols();
        int height = mGray.rows();
        Coordinates[3] = 0;
        if (NumEyes > 1) {
            if (NumEyes == 2) {
                int x1 = AllEyeCoordinates[0][0];
                int x2 = AllEyeCoordinates[1][0];
                int y1 = AllEyeCoordinates[0][1];
                int y2 = AllEyeCoordinates[1][1];
                int dist = Math.abs(((x1 - x2) ^ 2 + (y1 - y2) ^ 2) ^ (1 / 2));
                int disty = Math.abs(y2-y1);
                if (dist > Math.round(width * 0.1) && disty < Math.round(height * 0.05)) {
                    Coordinates[0] = (x1 + x2) / 2;
                    Coordinates[1] = (y1 + y2) / 2;
                    Coordinates[2] = dist;
                    Coordinates[3] = 2;
                    Imgproc.rectangle(mRgba, eyesArray[0].tl(), eyesArray[0].br(), COLOR2, 5);
                    Imgproc.rectangle(mRgba, eyesArray[1].tl(), eyesArray[1].br(), COLOR2, 5);
                }
            }
            else {
                for (int i = 0; i < NumEyes; i++) {
                    for (int j = 0; j < NumEyes; j++) {
                        if (Coordinates[3] != 2 && j!=i) {
                            int x1 = AllEyeCoordinates[i][0];
                            int x2 = AllEyeCoordinates[j][0];
                            int y1 = AllEyeCoordinates[i][1];
                            int y2 = AllEyeCoordinates[j][1];
                            int dist = Math.abs(((x1 - x2) ^ 2 + (y1 - y2) ^ 2) ^ (1 / 2));
                            int disty = Math.abs(y2-y1);
                            if (dist > Math.round(width * 0.1) && disty < Math.round(height * 0.05)) {
                                Coordinates[0] = (x1 + x2) / 2;
                                Coordinates[1] = (y1 + y2) / 2;
                                Coordinates[2] = dist;
                                Coordinates[3] = 2;
                                Imgproc.rectangle(mRgba, eyesArray[i].tl(), eyesArray[i].br(), COLOR2, 5);
                                Imgproc.rectangle(mRgba, eyesArray[j].tl(), eyesArray[j].br(), COLOR2, 5);
                            }
                        }
                    }

                }
            }

        }

        Rect[] facesArray;
        facesArray = faces.toArray();
        NumFaces = facesArray.length;
        AllFaceCoordinates = new int[NumFaces][3];
        for (int i = 0; i < NumFaces; i++) {
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), COLOR1, 3);
            if (NumFaces > 0) {
                AllFaceCoordinates[i][0] = facesArray[i].x;
                AllFaceCoordinates[i][1] = facesArray[i].y;
                AllFaceCoordinates[i][2] = facesArray[i].width;
            }
        }
        if (NumFaces > 0 && Coordinates[3] != 2) {
            int face_width = AllFaceCoordinates[0][2];
            int index1 = 0;
            for (int i = 0; i < NumFaces; i++) {
                if (AllFaceCoordinates[i][2] > face_width) {
                    index1 = i;
                    face_width = AllFaceCoordinates[index1][2];
                }
            }
            Imgproc.rectangle(mRgba, facesArray[index1].tl(), facesArray[index1].br(), COLOR2, 5);
            Coordinates[0] = AllFaceCoordinates[index1][0];
            Coordinates[1] = AllFaceCoordinates[index1][1];
            Coordinates[2] = AllFaceCoordinates[index1][2];
        }
        if (Coordinates[3] !=0){
            double ObjSize = 0;
            String mess1 = "?, ";
            // case for 2 eyes detected
            if (Coordinates[3] ==2){
                ObjSize = EstimatedEyeDist; //real size in m of the eye dist
                mess1 = "eyes, ";
            }
            // case for no eyes detected
            if (Coordinates[3] ==1){
                ObjSize = EstimatedFaceWidth; //real size in m of face
                mess1 = "face, ";
            }
            double focalLength = 3.75*0.001;//real size in m, usually val between 4 and 6 mm TBD
            double[] fx = Cmat.get(1,1);// in pix
            double[] fy = Cmat.get(2,2);// in pix
            double f = Math.round((fx[0]+fy[0])/2); // round fct to get an integer
            double m= f/focalLength;// from fx = f*mx
            double conv = m*1920/width;// conversion of resolution in px/m
            //width of the image, Julia's phone resolution for video recording with front camera
            // : 1920*1080
            double objImSensor = Coordinates[2]/conv ;// object size in pix/conv in px/m => m
            double est = 1.3; // to correct the distance

            DistFace = (float) (ObjSize * focalLength / objImSensor * est);// in m and conv from
            //double to float

            double x_coor = Coordinates[0] - width/2;
            double y_coor = height/2 - Coordinates[1];
            mCoordinates = new float[2];
            double mul = DistFace/focalLength*width/m/1920;
            mCoordinates[0] = (float) (mul*x_coor);
            mCoordinates[1] = (float) (mul*y_coor);

            String mess = mess1 + "Dist: " + Float.toString(DistFace) + "m, x: " + Float.toString(mCoordinates[0]) + "m, y: " + Float.toString(mCoordinates[1]) + "m";
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
