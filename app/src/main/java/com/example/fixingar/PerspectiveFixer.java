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
    private CameraParameters camParams_b;
    private CameraParameters camParams_f;
    private float[] mCoordinates;
    private float FaceDist;

    public PerspectiveFixer(CameraParameters cpb, CameraParameters cpf, float[] mCoor, float Dist) {
        camParams_b = cpb;
        camParams_f = cpf;
        mCoordinates = mCoor;
        FaceDist = Dist;
    }

    private MatOfPoint3f getArucoPoints(double markerSize) {
        MatOfPoint3f cornerPoints = new MatOfPoint3f();
        double halfSize = markerSize/2.0;
        Vector<Point3> points = new Vector<Point3>();
        points.add(new Point3( halfSize, -halfSize, 0));
        points.add(new Point3(-halfSize, -halfSize, 0));
        points.add(new Point3(-halfSize,  halfSize, 0));
        points.add(new Point3( halfSize,  halfSize, 0));
        cornerPoints.fromList(points);
        return cornerPoints;
    }

    private Mat createCameraMatrix (double fx, double fy, double x0, double y0) {
        Mat EyeCamMatrix = Mat.eye(3,3, CvType.CV_32FC1);
        EyeCamMatrix.put(0,0,fx);
        EyeCamMatrix.put(1,1,fy);
        EyeCamMatrix.put(2,2,1.0);
        EyeCamMatrix.put(0,2,x0);
        EyeCamMatrix.put(1,2,y0);
        return EyeCamMatrix;
    }

    // TODO: turn inputs into arrays or points or something
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

    public Mat fixPerspective(Mat rgba, Marker marker, double markerSize) {
        Size rgbaSize = rgba.size();

        // TODO: stretch image so it fills entire width of screen?

        Mat cam2EyeTransform = getCam2EyeTransform(marker, markerSize);

        Mat screen2DeviceTransform = getScreen2DeviceTransform(rgbaSize); // TODO: this is a constant transform which we only need to calculate once, not at every frame

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


        // Transform frame from camera to eye:
        MatOfPoint2f frameCam2EyeTransformed = new MatOfPoint2f();
        Imgproc.warpPerspective(rgba, frameCam2EyeTransformed, cam2EyeTransform, rgba.size());

        // Stretch frame to entire device size:
        MatOfPoint2f frameScreen2DeviceTransformed = new MatOfPoint2f();
        Imgproc.warpPerspective(frameCam2EyeTransformed, frameScreen2DeviceTransformed, screen2DeviceTransform, frameCam2EyeTransformed.size());


        /*
        // Transform corner-of-device points into phone camera image.
        MatOfPoint2f cornerOfDeviceTr = new MatOfPoint2f();
        Imgproc.warpPerspective(cornerOfDevice, cornerOfDeviceTr, H, cornerOfDeviceTr.size());
        Core.perspectiveTransform(cornersDevice, cornersDevice);
        Log.d("cornerOfDeviceTr",cornerOfDeviceTr.dump());


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
            drawLine(rgba, coloredP[i%4], coloredP[(i+1)%4]);
        }
        */

        return frameScreen2DeviceTransformed;
    }

    private Mat getCam2EyeTransform(Marker marker, double markerSize) {
        // The estimated 4 corner points in 3D marker frame:
        MatOfPoint3f cornerPointsCam = getArucoPoints(markerSize);
        Log.d("Marker corners 3D:",cornerPointsCam.dump());

        // Project points into camera image:
        MatOfPoint2f cornerPointsCamProj = new MatOfPoint2f();
        Calib3d.projectPoints(cornerPointsCam, marker.getRvec(), marker.getTvec(), camParams.getCameraMatrix(), camParams.getDistCoeff(), cornerPointsCamProj);
        Log.d("Marker corners proj:",cornerPointsCamProj.dump());

        Log.d("Camera matrix:", camParams.getCameraMatrix().dump());

        // Create translation vector from camera to the focus point of the pinhole camera constituted by the eyes and camera screen.
        Mat tEye2Device = Mat.zeros(3, 1, CvType.CV_64FC1);
        tEye2Device.put(2, 0, 0.4);  // Z (backwards)
        Mat tDevice2Cam = Mat.zeros(3, 1, CvType.CV_64FC1);
        tDevice2Cam.put(0, 0, 0.05);  // X (shift to move camera to phone center)
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
        MatOfPoint2f cornerPointsEyeProj = new MatOfPoint2f();
        Calib3d.projectPoints(cornerPointsCam, marker.getRvec(), tEye2Marker, EyeCamMatrix, new MatOfDouble(0,0,0,0,0,0,0,0), cornerPointsEyeProj); // camParams.getCameraMatrix()
        Log.d("dstpoints",cornerPointsEyeProj.dump());

        // Use getPerspectiveTransform to get a transform matrix between phone image and EyeCamera image.
        Mat cam2EyeTransform = Imgproc.getPerspectiveTransform(cornerPointsCamProj, cornerPointsEyeProj);
        Log.d("Cam2EyeTransform",cam2EyeTransform.dump());

        return cam2EyeTransform;
    }

    private Mat getScreen2DeviceTransform(Size rgbaSize) {
        // Corner points on image in screen.
        MatOfPoint2f cornersScreen = create4Points(rgbaSize.width, 0,0,  0,0, rgbaSize.height, rgbaSize.width, rgbaSize.height);
        Log.d("cornersScreen",cornersScreen.dump());

        // Generate corner points in screen plane.
        MatOfPoint2f cornersDevice = create4Points(0.0711*2, 0,0, 0,0,  0.03495*2,0.0711*2,  0.03495*2);
        Log.d("cornersDevice",cornersDevice.dump());

        Mat screen2DeviceTransform = Imgproc.getPerspectiveTransform(cornersScreen,cornersDevice);
        Log.d("screen2DeviceTransform:",screen2DeviceTransform.dump());

        return screen2DeviceTransform;
    }

    private void drawLine(Mat img, Point start, Point end) {
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
