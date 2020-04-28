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
    private static final float MARKER_SIZE = (float) 0.13;

    //Preferences
    private static final boolean SHOW_MARKERID = true;

    private CameraBridgeViewBase mOpenCvCameraView;
    private int mCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
    private TextView mDebugText;
    private Button mCameraButton;

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
            CameraParameters camParams = new CameraParameters();

            camParams.read(this);

            //Populate detectedMarkers
            mDetector.detect(rgba, detectedMarkers, camParams, MARKER_SIZE);

            if (detectedMarkers.size() == 1) {
                Marker marker = detectedMarkers.get(0);
                //Log.d(TAG, String.valueOf(marker.getPoints().get(0)));
                debugMsg(marker.getPoints().get(0) + "\n" + marker.getPoints().get(1) + "\n" + marker.getPoints().get(2) + "\n" + marker.getPoints().get(3) + "\n");
                debugMsg(marker.getRvec().dump() + "\n" + marker.getTvec().dump());

                Mat dst = fixPerspective(rgba,marker,camParams);
                return rgba;
                //return dst;
            }

            //Draw Axis for each marker detected
            if (detectedMarkers.size() != 0) {
                for (int i = 0; i < detectedMarkers.size(); i++) {
                    /*
                    Rvec and Tvec are the rotation and translation from the marker frame to the camera frame!
                    Use Rodriguez() method from calib3d to turn rotation vector into rotation matrix if we need this.
                    The x,y,z position of the camera is: cameraPosition = -rotM.T * tvec
                    ProjectPoints projects 3D points to image plane
                    EstimateAffine3D computes an optimal affine transformation between two 3D point sets
                    SolvePnP finds an object pose from 3D-2D point correspondences
                    warpPerspective applies a perspective transformation to an image
                     */


                    //Marker marker = detectedMarkers.get(i);
                    //marker.draw3dAxis(rgba, camParams);
                    //marker.draw3dCube(rgba, camParams, new Scalar(255,255,0));
                }
            }
        } else if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT) {
            // Do facial recognition here
        }
            return rgba;
    }

    private MatOfPoint3f getArucoPoints() {
        MatOfPoint3f cornerPoints = new MatOfPoint3f();
        double halfSize = MARKER_SIZE/2.0;
        Vector<Point3> points = new Vector<Point3>();
        points.add(new Point3( halfSize, -halfSize, 0));
        points.add(new Point3(-halfSize, -halfSize, 0));
        points.add(new Point3(-halfSize,  halfSize, 0));
        points.add(new Point3( halfSize,  halfSize, 0));
        cornerPoints.fromList(points);
        return cornerPoints;
    }

    private Mat createCameraMatrix (double fx, double fy, double x0, double y0) {
        Mat EyeCamMatrix = Mat.eye(3,3,CvType.CV_32FC1);
        EyeCamMatrix.put(0,0,fx);
        EyeCamMatrix.put(1,1,fy);
        EyeCamMatrix.put(2,2,1.0);
        EyeCamMatrix.put(0,2,x0);
        EyeCamMatrix.put(1,2,y0);
        return EyeCamMatrix;
    }

    private MatOfPoint2f create4Points (double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        MatOfPoint2f cornerOfDevice = new MatOfPoint2f();
        Vector<Point> DevicePoints = new Vector<Point>();
        DevicePoints.add(new Point(x1, y1));
        DevicePoints.add(new Point(x2, y2));
        DevicePoints.add(new Point(x3, y3));
        DevicePoints.add(new Point(x4, y4));
        cornerOfDevice.fromList(DevicePoints);
        return cornerOfDevice;
    }

    private Mat fixPerspective(Mat rgba, Marker marker, CameraParameters camParams) {
        Size rgbaSize = rgba.size();

        // The estimated 4 corner points in 3D marker frame:
        MatOfPoint3f cornerPoints = getArucoPoints();
        Log.d("Arucopoints",cornerPoints.dump());

        // Project points into camera image.
        MatOfPoint2f srcPointsProj = new MatOfPoint2f();
        Calib3d.projectPoints(cornerPoints, marker.getRvec(), marker.getTvec(), camParams.getCameraMatrix(), camParams.getDistCoeff(), srcPointsProj);
        Log.d("srcpoints",srcPointsProj.dump());
        Log.d("CameraMatrix:", camParams.getCameraMatrix().dump());

        // Create translation vector from camera to the focus point of the pinhole camera constituted by the eyes and camera screen.
        Mat tEye2Device = Mat.zeros(3, 1, CvType.CV_64FC1);
        tEye2Device.put(2, 0, 0.4);  // Z (backwards)
        Mat tDevice2Cam = Mat.zeros(3, 1, CvType.CV_64FC1);
        tDevice2Cam.put(0, 0, -0.05);  // X (shift to move camera to phone center)
        Mat tEye2Cam = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.add(tEye2Device, tDevice2Cam, tEye2Cam);
        // TODO: add calibration procedure for x and y offset and set input as the estimates by the eye tracking software (x,y and z). Just some sliders for x and y could work fine i guess?

        // Get translation vector from marker to EyeCamera. Therefore we have the definite extrinsic matrix since the rotation vector.
        Mat tEye2Marker = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.add(tEye2Device, marker.getTvec(), tEye2Marker);

        // Create estimation of intrinsic camera matrix for the EyeCamera.
        Mat EyeCamMatrix = createCameraMatrix(0.4,0.4,0.0711,0.03495);
        Log.d("EyeCameraMatrix:", EyeCamMatrix.dump());
        // TODO: Insert parameters from eye detection here as well.

        // Project Aruco points onto the screen through the Eye Camera matrix.
        MatOfPoint2f dstPointsProj = new MatOfPoint2f();
        Calib3d.projectPoints(cornerPoints, marker.getRvec(), tEye2Marker, EyeCamMatrix, new MatOfDouble(0,0,0,0,0,0,0,0), dstPointsProj); // camParams.getCameraMatrix()
        Log.d("dstpoints",dstPointsProj.dump());

        /* Make this entire section about drawing squares into its own method.
        List<Point> srcPointsProjList = new Vector<Point>();
        srcPointsProjList = srcPointsProj.toList();

        List<Point> dstPointsProjList = new Vector<Point>();
        dstPointsProjList = dstPointsProj.toList();

                // Draw squares:
                Scalar color = new Scalar(255,255,0);
                for (int i = 0; i < 4; i++){
                    Imgproc.line(rgba, srcPointsProjList.get(i), srcPointsProjList.get((i+1)%4), color, 2);
                    Imgproc.line(rgba, dstPointsProjList.get(i), dstPointsProjList.get((i+1)%4), color, 2);
                    Imgproc.line(rgba, dstPointsProjList.get(i), srcPointsProjList.get(i), color, 2);
                }
*/

        // Use findHomography to get a transform matrix between phone image and EyeCamera image.
        Mat H = Imgproc.getPerspectiveTransform(dstPointsProj, srcPointsProj);
        Log.d("Cam2Dev",H.dump());

        // Generate corner points in screen plane.
        MatOfPoint2f cornerOfDevice =create4Points(0.0711*2, 0,0, 0,0,  0.03495*2,0.0711*2,  0.03495*2);

        // Transform corner-of-device points into phone camera image.
        MatOfPoint2f cornerOfDeviceTr = new MatOfPoint2f();
        Imgproc.warpPerspective(cornerOfDevice, cornerOfDeviceTr, H,cornerOfDeviceTr.size());
        Log.d("cornerOfDeviceTr",cornerOfDeviceTr.dump());

        // Corner points on image in screen.
        MatOfPoint2f imageCorners = create4Points(rgbaSize.width, 0,0,  0,0, rgbaSize.height, rgbaSize.width, rgbaSize.height);
        Log.d("screenCorners",imageCorners.dump());

        // find the final transformation to stretch points to corners of image.
        Mat finalTr = Imgproc.getPerspectiveTransform(cornerOfDeviceTr,imageCorners);
        Log.d("Final:",finalTr.dump());

        // Transform to corners for the final image to show on screen!
        Mat dst = new Mat(rgba.size(), CvType.CV_64FC1);
        Imgproc.warpPerspective(rgba, dst, finalTr, rgba.size());

        // Following used for debugging, instead of
        Point[] coloredP = cornerOfDeviceTr.toArray();
        Log.d("ColoredP",coloredP[0].toString()+coloredP[1].toString()+coloredP[2].toString()+coloredP[3].toString());
        for (int i = 0; i < 4; i++) {
            MyLine(rgba,coloredP[i%4],coloredP[(i+1)%4]);
        }
        return dst;
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
    private void MyLine( Mat img, Point start, Point end ) {
        int thickness = 2;
        int lineType = 8;
        int shift = 0;
        Imgproc.line( img,
                start,
                end,
                new Scalar( 255, 0, 0 ),
                thickness,
                lineType,
                shift );
    }
}


