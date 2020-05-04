package com.example.fixingar;

import android.bluetooth.BluetoothClass;
import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.Vector;

import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;

public class PerspectiveFixer {
    private CameraParameters camParams;

    public PerspectiveFixer(CameraParameters cp) {
        camParams = cp;
    }

    private MatOfPoint3f getArucoPoints(double markerSize, Marker marker) {
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

        Mat cam2EyeTransform = getCam2EyeTransform(marker, markerSize);

        // Corner points on image in screen.
        MatOfPoint2f cornersScreen = create4Points(rgbaSize.width, 0,0,  0,0, rgbaSize.height, rgbaSize.width, rgbaSize.height);
        Log.d("cornersScreen",cornersScreen.dump());

        // Generate corner points in screen plane.
        MatOfPoint2f cornersDevice = create4Points(0.0711*2, 0,0, 0,0,  0.03495*2,0.0711*2,  0.03495*2); // 0.0711,-0.03495,-0.0711,-0.03495, -0.0711, 0.03495, 0.0711,0.03495
                        //
        Log.d("cornersDevice",cornersDevice.dump());

        MatOfPoint2f cornersDeviceTr = new MatOfPoint2f();
        Core.perspectiveTransform(cornersDevice,cornersDeviceTr, cam2EyeTransform); //,cornersDevice.size()
        Log.d("cornersDeviceTr",cornersDeviceTr.dump());

        Mat point2CornersTransform = Imgproc.getPerspectiveTransform(cornersDeviceTr,cornersScreen);
        Log.d("screen2DeviceTransform:",point2CornersTransform.dump());

        // Transform to corners for the final image to show on screen!
        Mat dst = new Mat(rgba.size(), CvType.CV_64FC1);
        Imgproc.warpPerspective(rgba, dst, point2CornersTransform, rgba.size()); //

        // Following used for debugging, instead of
        Point[] coloredP = cornersDeviceTr.toArray();
        Log.d("ColoredP",coloredP[0].toString()+coloredP[1].toString()+coloredP[2].toString()+coloredP[3].toString());
        for (int i = 0; i < 4; i++) {
            drawLine(rgba, coloredP[i%4], coloredP[(i+1)%4]);
        }
        return dst;
    }

    private Mat getCam2EyeTransform(Marker marker, double markerSize) {
        Mat rMatrix = new Mat();
        Calib3d.Rodrigues(marker.getRvec(),rMatrix);

        // The estimated 4 corner points in 3D marker frame:
        MatOfPoint3f cornerPointsCam = getArucoPoints(markerSize,marker);
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
        Mat tDevice2Cam2 = new Mat(tDevice2Cam.size(),tDevice2Cam.type());
        Core.gemm(rMatrix,tDevice2Cam,1,tDevice2Cam,0,tDevice2Cam2,0);
        Mat tEye2Cam = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.add(tEye2Device, tDevice2Cam2, tEye2Cam);
        // TODO: add calibration procedure for x and y offset and set input as the estimates by the eye tracking software (x,y and z). Just some sliders for x and y could work fine i guess?

        // Get translation vector from marker to EyeCamera. Therefore we have the definite extrinsic matrix since the rotation vector.
        Mat tEye2Marker = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.add(tEye2Device, marker.getTvec(), tEye2Marker);

        // Create estimation of intrinsic camera matrix for the EyeCamera.
        Mat EyeCamMatrix = createCameraMatrix(0.4,0.4, 0.0711,0.03495); //
        Log.d("EyeCameraMatrix:", EyeCamMatrix.dump());
        // TODO: Insert parameters from eye detection here as well in some way.

        // Project Aruco points onto the screen through the Eye Camera matrix.
        MatOfPoint2f cornerPointsEyeProj = new MatOfPoint2f();
        Calib3d.projectPoints(cornerPointsCam, marker.getRvec(), tEye2Marker, EyeCamMatrix, new MatOfDouble(0,0,0,0,0,0,0,0), cornerPointsEyeProj); // camParams.getCameraMatrix()
        Log.d("dstpoints",cornerPointsEyeProj.dump());

        // Use getPerspectiveTransform to get a transform matrix between phone image and EyeCamera image.
        Mat cam2EyeTransform = Imgproc.getPerspectiveTransform(cornerPointsEyeProj,cornerPointsCamProj);
        Log.d("Cam2EyeTransform",cam2EyeTransform.dump());

        return cam2EyeTransform;
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

    public Mat fixPerspectiveMultipleMarker(Mat rgba, Vector<Marker> detectedMarkers, float markerSize) {
        // 1. Get Pose from rvec and tvec of first marker.
        Marker marker = detectedMarkers.get(0);
        Mat rMatrix = new Mat();
        Calib3d.Rodrigues(marker.getRvec(),rMatrix);
        Mat tVec = marker.getTvec();
        // 2. Save Tvec from 4 other markers into a MatOfPoint3f.
        MatOfPoint3f markerPoints = new MatOfPoint3f();
        Vector<Point3> points = new Vector<Point3>();
        points.add(new Point3( detectedMarkers.get(1).getTvec().get(0,0)[0],detectedMarkers.get(1).getTvec().get(1,0)[0],detectedMarkers.get(1).getTvec().get(2,0)[0]));
        points.add(new Point3(detectedMarkers.get(2).getTvec().get(0,0)[0],detectedMarkers.get(2).getTvec().get(1,0)[0],detectedMarkers.get(2).getTvec().get(2,0)[0]));
        points.add(new Point3(detectedMarkers.get(3).getTvec().get(0,0)[0],detectedMarkers.get(3).getTvec().get(1,0)[0],detectedMarkers.get(3).getTvec().get(2,0)[0]));
        points.add(new Point3(detectedMarkers.get(4).getTvec().get(0,0)[0],detectedMarkers.get(4).getTvec().get(1,0)[0],detectedMarkers.get(4).getTvec().get(2,0)[0]));
        markerPoints.fromList(points);
        Log.d("Vectorpoints",markerPoints.dump());
        // 3. Project points genom ögonkameran för att få matchande points i båda bilderna.
        Mat tEye2Device = Mat.zeros(3, 1, CvType.CV_64FC1);
        tEye2Device.put(2, 0, -0.4);  // Z (backwards)
        tEye2Device.put(0, 0, 0.05);  // X (shift to move camera to phone center)
        // TODO: add calibration procedure for x and y offset and set input as the estimates by the eye tracking software (x,y and z). Just some sliders for x and y could work fine i guess?
        Mat tVecEye = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.add(marker.getTvec(),tEye2Device, tVecEye);
        Mat EyeCamMatrix = createCameraMatrix(0.4,0.4, 0.0711,0.03495); //
        // TODO: Insert parameters from eye detection here as well in some way.
        MatOfPoint2f markerPointsProjEye = new MatOfPoint2f();
        Calib3d.projectPoints(markerPoints, Mat.zeros(3,1,marker.getRvec().type()), tEye2Device, EyeCamMatrix, new MatOfDouble(0,0,0,0,0,0,0,0), markerPointsProjEye); // marker.getRvec()& tVecEye
        // 4. Get perspective transform like before. Call it H.
        MatOfPoint2f markerPointsIm = new MatOfPoint2f();
        Vector<Point> DevicePoints = new Vector<Point>();
        DevicePoints.add(new Point(detectedMarkers.get(1).getPoints().get(0).x,detectedMarkers.get(1).getPoints().get(0).y));
        DevicePoints.add(new Point(detectedMarkers.get(2).getPoints().get(0).x,detectedMarkers.get(2).getPoints().get(0).y));
        DevicePoints.add(new Point(detectedMarkers.get(3).getPoints().get(0).x,detectedMarkers.get(3).getPoints().get(0).y));
        DevicePoints.add(new Point(detectedMarkers.get(4).getPoints().get(0).x,detectedMarkers.get(4).getPoints().get(0).y));
        markerPointsIm.fromList(DevicePoints);
        Log.d("markerpointsIm",markerPointsIm.dump());
        Mat H = Calib3d.findHomography(markerPointsProjEye,markerPointsIm);
        Log.d("H",H.dump());
        // 5. Enter corners of device into the transform H.
        MatOfPoint2f cornersDevice = create4Points(0.0711*2, 0,0, 0,0,  0.03495*2,0.0711*2,  0.03495*2); // 0.0711,-0.03495,-0.0711,-0.03495, -0.0711, 0.03495, 0.0711,0.03495
        MatOfPoint2f cornersDeviceTr = new MatOfPoint2f();
        Core.perspectiveTransform(cornersDevice,cornersDeviceTr, H); //,cornersDevice.size()
        Log.d("cornersOfDeviceMulti",cornersDeviceTr.dump());
        // 6. Strech these points to the corners of the image.
        MatOfPoint2f cornersScreen = create4Points(rgba.size().width, 0,0,  0,0, rgba.size().height, rgba.size().width, rgba.size().height);
        Mat point2CornersTransform = Imgproc.getPerspectiveTransform(cornersDeviceTr,cornersScreen);
        Mat dst = new Mat(rgba.size(), CvType.CV_64FC1);
        Imgproc.warpPerspective(rgba, dst, point2CornersTransform, rgba.size());

        // Following used for debugging, instead of
        Point[] coloredP = cornersDeviceTr.toArray();
        Log.d("ColoredP",coloredP[0].toString()+coloredP[1].toString()+coloredP[2].toString()+coloredP[3].toString());
        for (int i = 0; i < 4; i++) {
            drawLine(rgba, coloredP[i%4], coloredP[(i+1)%4]);
        }

        return rgba;
    }
}