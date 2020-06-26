package com.example.fixingar;


import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.samples.facedetect.DetectionBasedTracker;

import java.lang.Math;


public class FaceDetection {
    private Mat CameraMatrix;

    //Constants
    private static final String TAG = "Main";

    //You must run a calibration prior to detection
    // The activity to run calibration is provided in the repository

    private static final Scalar     COLOR2     = new Scalar(150, 0, 150, 0);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private Mat                    mRgba;
    private Mat                    mGray;
    private CascadeClassifier      mJavaDetector1;
    private DetectionBasedTracker mNativeDetector1;
    private CascadeClassifier      mJavaDetector2;
    private DetectionBasedTracker mNativeDetector2;

    private int                    mDetectorType       = JAVA_DETECTOR;

    private float                  mRelativeEyeSize   = 0.1f; // change this parameter to adjust min Eye size
    private int                    mAbsoluteEyeSize   = 0;

    private float                  mRelativeFaceSize   = 0.2f; // change this parameter to adjust min Face size
    private int                    mAbsoluteFaceSize   = 0;

    private float                  EstimatedFaceWidth;
    private float                  EstimatedEyeDist;
    private double                 est;
    private double                 resolution;
    private double                 focallength;

    private int[]                  Coordinates = new int[4]; //contains x & y coordinate, dist/width, 1 or 2 to define if two eyes or one face
    private float[]                mCoordinates; //x, y, z position in m and if two eyes or one face

    private TextView mDebugText;

    public FaceDetection(Mat Cmat, CascadeClassifier mJavaDetector_eye, CascadeClassifier mJavaDetector_face, DetectionBasedTracker mNativeDetector_eye, DetectionBasedTracker mNativeDetector_face, String who){
        CameraMatrix = Cmat;
        mJavaDetector1 = mJavaDetector_eye;
        mJavaDetector2 = mJavaDetector_face;
        mNativeDetector1 = mNativeDetector_eye;
        mNativeDetector2 = mNativeDetector_face;
        Variables variables = new Variables(who);
        focallength = variables.getFocallength();
        resolution = variables.getResolution();
        est = variables.getEst();
        EstimatedEyeDist = variables.getEyeDist();
        EstimatedFaceWidth = variables.getFaceWidth();
    }

    private int CheckAbsoluteSize(int mAbsoluteSize, int image_height, float mRelativeSize, DetectionBasedTracker mNativeDetector) {
        if (mAbsoluteSize == 0) {
            if (Math.round(image_height * mRelativeSize) > 0) {
                mAbsoluteSize = Math.round(image_height * mRelativeSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteSize);
        }
        return mAbsoluteSize;
    }

    private MatOfRect detect(CascadeClassifier mJavaDetector, DetectionBasedTracker mNativeDetector, MatOfRect eyes, int i){
        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null && i==1)
                mJavaDetector.detectMultiScale(mGray, eyes, 1.1, 2, 2,
                        new Size(mAbsoluteEyeSize, mAbsoluteEyeSize), new Size());
            else if (mJavaDetector != null && i!=1)
                mJavaDetector.detectMultiScale(mGray, eyes, 1.1, 2, 2,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, eyes);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }
        return eyes;
    }
    // Here the coordinates and distance are found in bits in the array "Coordinates".
    // If two eyes are found:
    // Coordinates[0] = (x1+x2)/2 (eye positions)
    // Coordinates[1] = (y1+y2)/2
    // Coordinates[2] = distance between eyes
    // Coordinates[3] = 2 (to indicate two eyes were found)

    private void EyeDetection() {
        mAbsoluteEyeSize = CheckAbsoluteSize(mAbsoluteEyeSize, mGray.rows(), mRelativeEyeSize, mNativeDetector1);
        MatOfRect eyes = new MatOfRect();
        eyes = detect(mJavaDetector1, mNativeDetector1, eyes, 1);
        Rect[] eyesArray;
        eyesArray = eyes.toArray();
        int NumEyes = eyesArray.length;
        int[][] AllEyeCoordinates = new int[NumEyes][2];
        for (int i = 0; i < NumEyes; i++) {
            if (NumEyes > 0) {
                AllEyeCoordinates[i][0] = eyesArray[i].x;
                AllEyeCoordinates[i][1] = eyesArray[i].y;
            }
        }
        if (NumEyes > 1 && Coordinates[3] != 1) {
            if (NumEyes == 2) {
                int x1 = AllEyeCoordinates[0][0];
                int x2 = AllEyeCoordinates[1][0];
                int y1 = AllEyeCoordinates[0][1];
                int y2 = AllEyeCoordinates[1][1];
                int dist = Math.abs(((x1 - x2) ^ 2 + (y1 - y2) ^ 2) ^ (1 / 2));
                int disty = Math.abs(y2-y1);
                if (dist > Math.round(mGray.cols() * 0.1) && disty < Math.round(mGray.rows() * 0.05)) {
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
                            if (dist > Math.round(mGray.cols() * 0.1) && disty < Math.round(mGray.rows() * 0.05)) {
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
    }

    // If only one eye was found (or several eyes, but they weren't matching)
    // Coordinates[0] = x (face position)
    // Coordinates[1] = y
    // Coordinates[2] = width (width of face according to rectangle width)
    // Coordinates[3] = 1 (to indicate one face was found)
    // if nothing is found:
    // Coordinates[3] = 0
    private void FaceDetection(){
        mAbsoluteFaceSize = CheckAbsoluteSize(mAbsoluteFaceSize, mGray.rows(), mRelativeFaceSize, mNativeDetector2);
        MatOfRect faces = new MatOfRect();
        faces = detect(mJavaDetector2, mNativeDetector2, faces, 2);
        Rect[] facesArray;
        facesArray = faces.toArray();
        int NumFaces = facesArray.length;
        int[][] AllFaceCoordinates = new int[NumFaces][3];
        for (int i = 0; i < NumFaces; i++) {
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
            Coordinates[3] = 1;
        }
    }

    public String ObjDetect(float[] Coordinates) {
        String mess1 = "";
        if (Coordinates[3] != 0) {
                mess1 = "?, ";
            // case for 2 eyes detected
            if (Coordinates[3] == 2) {
                mess1 = "eyes, ";
            }
            // case for no eyes detected
            if (Coordinates[3] == 1) {
                mess1 = "face, ";
            }
        }
        return mess1;
    }

    private float RealObjSize(int[] Coordinates) {
            double ObjSize = 0;
            // case for 2 eyes detected
            if (Coordinates[3] == 2) {
                ObjSize = EstimatedEyeDist; //real size in m of the eye dist
            }
            // case for no eyes detected
            if (Coordinates[3] == 1) {
                ObjSize = EstimatedFaceWidth; //real size in m of face
            }
        return (float) ObjSize;
    }

    public float[] getmCoordinates(Mat Rgba, Mat Gray){
        // use this one as the main, which will go through everything
        mRgba = Rgba;
        mGray = Gray;
        mCoordinates = new float[4];
        // start with eye and face detection
        Coordinates[3] = 0;
        FaceDetection();
        EyeDetection();

        if (Coordinates[3] !=0) {
            float ObjSize = RealObjSize(Coordinates);
            double[] fx = CameraMatrix.get(1, 1);// in pix
            double[] fy = CameraMatrix.get(2, 2);// in pix
            double f = Math.round((fx[0] + fy[0]) / 2); // round fct to get an integer
            double m = f / focallength;// from fx = f*mx
            double conv = m * resolution / mGray.cols();// conversion of resolution in px/m
            //width of the image, Julia's phone resolution for video recording with front camera
            // : 1920*1080
            double objImSensor = Coordinates[2] / conv;// object size in pix/conv in px/m => m

            mCoordinates[2] = (float) (ObjSize * focallength / objImSensor * est);// in m and conv from
            //double to float

            double x_coor = Coordinates[0] - mGray.cols() / 2;
            double y_coor = mGray.rows() / 2 - Coordinates[1];
            double mul = mCoordinates[2] / focallength * mGray.cols() / m / resolution / est;
            mCoordinates[0] = (float) (mul * x_coor);
            mCoordinates[1] = (float) (mul * y_coor);
            mCoordinates[3] = Coordinates[3];



        }

            return mCoordinates;
    }
}
