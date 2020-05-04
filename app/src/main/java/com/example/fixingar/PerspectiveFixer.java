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
    private double[] mCoor;
    private float Dist;

    private double Eye_Focal = 0.022; // ToDo: check and adapt values
    private double Pixel_Density_Eye = 338; // pixel per inch

    public PerspectiveFixer(double[] mCoordinates, float DistFace) {
        mCoor = mCoordinates;
        Dist = DistFace;
    }

    private MatOfPoint2f PointsFromCameraInBits(Marker marker) { // get Aruco corner points in bits
        Vector<Point> corners = marker.getPoints(); // ToDo: test
        MatOfPoint2f corners_camera = new MatOfPoint2f();
        corners_camera.fromList(corners);
        return corners_camera; //ToDo: check original order: anti-clockwise, starting top left?
    }

    private double[][] CreateEyeMatrix() {
        double ratio = Pixel_Density_Eye/0.0254; // pixel per m
        double f = Eye_Focal*ratio;
        double x = mCoor[0]*ratio;
        double y = mCoor[1]*ratio;
        double[][] IntrEyeMatrix = new double[][]{{f, 0, x}, {0, f, y}, {0, 0, 1}};
        double[][] ExtrEyeMatrix = new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}};
        double[][] EyeMatrix = new double[3][4];
        for (int i = 0; i<3; i++) {
            for (int j = 0; j < 4; j++) {
                EyeMatrix[i][j] = IntrEyeMatrix[i][0] * ExtrEyeMatrix[0][j] + IntrEyeMatrix[i][1] * ExtrEyeMatrix[1][j] + IntrEyeMatrix[i][2] * ExtrEyeMatrix[2][j];
            }
        }
        return EyeMatrix;
    }

    private Point Transform3Dto2D(double x, double y, double z, double[][] matrix) {
        double x_2D = matrix[0][0]*x+matrix[0][1]*y+matrix[0][2]*z+matrix[0][3];
        double y_2D = matrix[1][0]*x+matrix[1][1]*y+matrix[1][2]*z+matrix[1][3];
        double s = matrix[2][0]*x+matrix[2][1]*y+matrix[2][2]*z+matrix[2][3];
        Point point = new Point(x_2D/s, y_2D/s);
        return point;
    }

    private MatOfPoint2f PointsFromEyeInBits(Marker marker, double markerSize) {
        MatOfPoint2f corners_eye = new MatOfPoint2f();
        Mat CamToWall = marker.getTvec();
        double[] T1 = CamToWall.get(0, 0); // ToDo: check if values are correct
        double[] T2 = CamToWall.get(1, 0);
        double[] T3 = CamToWall.get(2, 0); // value is not correct, is it not the distance from phone to aruco marker?
        double X = T1[0] + mCoor[0];
        double Y = T2[0] - mCoor[1];
        double Z = T3[0] + Dist;
        // assuming Aruco markers are on wall in correct orientation
        double x_3DPoint_1 = X - markerSize/2;
        double y_3DPoint_1 = Y + markerSize/2;
        double x_3DPoint_2 = X - markerSize/2;
        double y_3DPoint_2 = Y - markerSize/2;
        double x_3DPoint_3 = X + markerSize/2;
        double y_3DPoint_3 = Y - markerSize/2;
        double x_3DPoint_4 = X + markerSize/2;
        double y_3DPoint_4 = Y + markerSize/2;

        double[][] EyeMatrix = CreateEyeMatrix();

        Vector<Point> Points = new Vector<Point>();
        Points.add(Transform3Dto2D(x_3DPoint_1, y_3DPoint_1, Z, EyeMatrix));
        Points.add(Transform3Dto2D(x_3DPoint_2, y_3DPoint_2, Z, EyeMatrix));
        Points.add(Transform3Dto2D(x_3DPoint_3, y_3DPoint_3, Z, EyeMatrix));
        Points.add(Transform3Dto2D(x_3DPoint_4, y_3DPoint_4, Z, EyeMatrix));

        corners_eye.fromList(Points);
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
