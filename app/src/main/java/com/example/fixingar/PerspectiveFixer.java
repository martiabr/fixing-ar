package com.example.fixingar;

import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Vector;

import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;

public class PerspectiveFixer {
    private CameraParameters cPb;
    private double[] mCoor;
    private float Dist;

    private double Eye_Focal = (float) 0.022;
    private double Eye_SensorWidth = (float) 0.032;
    private double Eye_PixelWidth;

    public PerspectiveFixer(CameraParameters CamParams, double[] mCoordinates, float DistFace) {
        cPb = CamParams;
        mCoor = mCoordinates;
        Dist = DistFace;
    }

    private MatOfPoint2f PointsFromCameraInBits(Marker marker) { // get Aruco corner points in bits
        Vector<Point> corners = marker.getPoints(); // ToDo: test
        MatOfPoint2f corners_camera = ... ;
        return corners_camera;
    }

    private double[][] CreateEyeMatrix() {
        double ratio = Eye_PixelWidth/Eye_SensorWidth;
        double f = Eye_Focal*ratio;
        double x = mCoor[0]*ratio;
        double y = mCoor[1]*ratio;
        double[][] IntrEyeMatrix = new double[][]{{f, 0, x}, {0, f, y}, {0, 0, 1}};
        double[][] ExtrEyeMatrix = new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}};
        double[][] EyeMatrix = new double[][];
        for (int i = 0; i<3; i++) {
            for (int j = 0; j < 4; j++) {
                EyeMatrix[i][j] = IntrEyeMatrix[i][0] * ExtrEyeMatrix[0][j] + IntrEyeMatrix[i][1] * ExtrEyeMatrix[1][j] + IntrEyeMatrix[i][2] * ExtrEyeMatrix[2][j];
            }
        }
        return EyeMatrix;
    }

    private MatOfPoint2f PointsFromEyeInBits(Marker marker, double markerSize) {
        MatOfPoint2f corners_eye;
        Mat CamToWall = marker.getTvec();
        double X = CamToWall.get(0,0) + mCoor[0];
        double Y = CamToWall.get(1,0) - mCoor[1];
        double Z = CamToWall.get(2,0) + Dist;
        corners_eye

        return corners_eye;
    }


    public Mat fixPerspective(Mat mRgba, Marker marker, double markerSize) {
        Mat dst = new Mat();
        MatOfPoint2f corners_camera = PointsFromCameraInBits(marker);
        MatOfPoint2f corners_eye = PointsFromEyeInBits(marker, markerSize);
        Mat homography = Calib3d.findHomography(corners_camera, corners_eye);
        Imgproc.warpPerspective(mRgba, dst, homography, mRgba.size());
        return dst;
    }
}
