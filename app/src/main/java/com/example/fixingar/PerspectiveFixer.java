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
import org.opencv.video.KalmanFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;

public class PerspectiveFixer {
    public double halfwidth; // Half width of image shown on screen.
    public double halfHeight; // Half height of image shown on screen.
    public double[] ShiftBackFront;
    public double EyeResolution; //
    public int ShiftResolution;
    public double[] BackCameraShift;
    public double camTo00CornerX; // x vector from front camera to (0,0)/top-left corner of image shown on screen. Changed in variables.
    public double camTo00CornerY; // y vector from front camera to (0,0)/top-left corner of image shown on screen. Changed in variables.
    private CameraParameters camParams; // Parameters of back camera.

    private Point[] corners_b;
    private Point[] corners_bb;
    private int test_b = 0;
    private int test_bb = 0;
    private double deltaT = 1/24;

    private KalmanFilter kalman;
    private boolean kalmanInitialized = false;

    private boolean draw_cubes = true;
    private List<Scalar> colorsBase;
    private List<Scalar> colorsCube;

    public PerspectiveFixer(CameraParameters cp, String WHO) {
        camParams = cp;
        Variables variables = new Variables(WHO);
        halfHeight = variables.getHalfHeight();
        halfwidth = variables.getHalfWidth();
        camTo00CornerX = variables.getCamTo00CornerX();
        camTo00CornerY = variables.getCamTo00CornerY();
        ShiftBackFront = variables.getShiftFrontBackCamera();
        EyeResolution = variables.getEyeResolution();
        ShiftResolution = variables.getShiftResolution();
        BackCameraShift = variables.getBackCameraShift();
        kalmanInitialized = false;

        colorsBase = new ArrayList<>();
        colorsCube = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < 20; ++i) {
            float r = rand.nextFloat()*120 + (255-120);
            float g = rand.nextFloat()*120 + (255-120);
            float b = rand.nextFloat()*120 + (255-120);
            colorsBase.add(new Scalar(r, g, b));
            colorsCube.add(new Scalar(0.7*r, 0.7*g, 0.7*b));
        }
    }

    /**
     * Return aruco marker corners in standard basis.
     * @param markerSize
     * @param marker
     * @return returns 4 corners.
     */
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

    /**
     * Returns a camera matrix with given input values.
     * @param fx
     * @param fy
     * @param x0
     * @param y0
     * @return Mat camera matrix.
     */
    private Mat createCameraMatrix (double fx, double fy, double x0, double y0) {
        Mat CamMatrix = Mat.eye(3,3, CvType.CV_32FC1);
        CamMatrix.put(0,0,fx);
        CamMatrix.put(1,1,fy);
        CamMatrix.put(2,2,1.0);
        CamMatrix.put(0,2,x0);
        CamMatrix.put(1,2,y0);
        return CamMatrix;
    }

    /**
     * Creates a MatOfPoint2f of 4 points with given individual coordinates. 2D.
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     * @param x4
     * @param y4
     * @return 4 points in MatOfPoint2f
     */
    private MatOfPoint2f create4Points (double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        MatOfPoint2f points = new MatOfPoint2f();
        Vector<Point> points2add = new Vector<Point>();
        points2add.add(new Point(x1, y1));
        points2add.add(new Point(x2, y2));
        points2add.add(new Point(x3, y3));
        points2add.add(new Point(x4, y4));
        points.fromList(points2add);
        return points;
    }

    /**
     * Draws line on image img with start and end with color rgb(ij,k).
     * @param img
     * @param start
     * @param end
     * @param i
     * @param j
     * @param k
     */
    private void drawLine(Mat img, Point start, Point end, int i, int j, int k) {
        int thickness = 2;
        int lineType = 8;
        int shift = 0;
        Imgproc.line( img, start, end, new Scalar( i,j,k), thickness, lineType, shift );
    }

    /**
     * Returns mean point out of 3D points within a marker.
     * @param detectedMarkers
     * @param id_marker
     * @return mean point Point.
     */
    private Point getMeanPoint (Vector<Marker> detectedMarkers, int id_marker) {
        double x = 0;
        double y = 0;
        for (int i = 0; i < detectedMarkers.get(id_marker).getPoints().size(); i++) {
            x = x + detectedMarkers.get(id_marker).getPoints().get(i).x;
            y = y + detectedMarkers.get(id_marker).getPoints().get(i).y;
        }
        x = x/detectedMarkers.get(id_marker).getPoints().size();
        y = y/detectedMarkers.get(id_marker).getPoints().size();
        return new Point(x,y);
    }

    /**
     * Initiate Kalman filter for our points.
     * @param r0
     * @return Returns initiated kalman.
     */
    public static KalmanFilter initKalman (Mat r0) {
        KalmanFilter kalman = new KalmanFilter(16, 16, 0, CvType.CV_64FC1);
        double deltaT = ((double) 1)/24;

        // transition matrix
        Mat transitionMatrix=Mat.eye(16,16,CvType.CV_64FC1);
        transitionMatrix.put(0,8,deltaT);
        transitionMatrix.put(1,9,deltaT);
        transitionMatrix.put(2,10,deltaT);
        transitionMatrix.put(3,11,deltaT);
        transitionMatrix.put(4,12,deltaT);
        transitionMatrix.put(5,13,deltaT);
        transitionMatrix.put(6,14,deltaT);
        transitionMatrix.put(7,15,deltaT);
        kalman.set_transitionMatrix(transitionMatrix);

        // measurement matrix
        Mat measurementMatrix=Mat.eye(8,16,CvType.CV_64FC1);
        measurementMatrix=measurementMatrix.mul(measurementMatrix,1);
        kalman.set_measurementMatrix(measurementMatrix);

        //Process noise Covariance matrix
        Mat processNoiseCov=Mat.eye(16,16,CvType.CV_64FC1);
        processNoiseCov=processNoiseCov.mul(processNoiseCov,1);
        kalman.set_processNoiseCov(processNoiseCov);

        //Measurement noise Covariance matrix: reliability on our first measurement
        Mat measurementNoiseCov=Mat.eye(8,8,CvType.CV_64FC1);
        measurementNoiseCov=measurementNoiseCov.mul(measurementNoiseCov,10); // I hope this is in m and not in bits
        kalman.set_measurementNoiseCov(measurementNoiseCov);

        // initial variance
        Mat id2=Mat.eye(16,16,CvType.CV_64FC1);
        id2=id2.mul(id2,0.1); // again I hope this is in m
        kalman.set_errorCovPost(id2);

        // Set initial state:
        Mat x0 = Mat.zeros(16, 1, CvType.CV_64FC1);
        for (int i = 0; i < 8; ++i) {
            x0.put(i, 0, r0.get(i, 0));
        }
        kalman.set_statePost(x0);

        return kalman;
    }

    /**
     * Fix perspective of image rgba for multiple markers (=>4).
     * @param rgba
     * @param detectedMarkers
     * @param markerSize
     * @param mCoordinates
     * @return fixed image rgba.
     */
    public Mat fixPerspectiveMultipleMarker(Mat rgba, Vector<Marker> detectedMarkers, float markerSize,float[] mCoordinates) {
        Log.d("sizeofimage",rgba.size().toString());

        // 1. Save Tvec from markers into a MatOfPoint3f.
        MatOfPoint3f markerPoints = new MatOfPoint3f();
        Vector<Point3> points = new Vector<Point3>();
        for (int i = 0; i < detectedMarkers.size(); i++) {
            Mat tVec_i = detectedMarkers.get(i).getTvec();
            points.add(new Point3(tVec_i.get(0,0)[0], tVec_i.get(1,0)[0], tVec_i.get(2,0)[0]));
        }
        markerPoints.fromList(points);
        Log.d("markerPoints3D",markerPoints.dump());

        // 2. Create a translation vector from eye poisition to back camera position. This is to translate coordinates for correct camera projection.
        Mat tEye2Device = Mat.zeros(3, 1, CvType.CV_64FC1);
        tEye2Device.put(0, 0, (mCoordinates[0]+ShiftBackFront[0]));  // X
        tEye2Device.put(1, 0, (mCoordinates[1]+ShiftBackFront[1])); // Y
        tEye2Device.put(2, 0, (mCoordinates[2]+ShiftBackFront[2]));  // Z


        // 3. Project points through eye camera matrix to find marker points in eye projection.
        Mat EyeCamMatrix = createCameraMatrix(mCoordinates[2],mCoordinates[2], 0,0);
        MatOfPoint2f markerPointsProjEye = new MatOfPoint2f();
        Calib3d.projectPoints(markerPoints, Mat.zeros(3,1,detectedMarkers.get(0).getRvec().type()), tEye2Device, EyeCamMatrix, new MatOfDouble(0,0,0,0,0,0,0,0), markerPointsProjEye);
        Log.d("markerPointsEye",markerPointsProjEye.dump());

        // 4. Get marker points in device camera image.
        MatOfPoint2f markerPointsIm = new MatOfPoint2f();
        Vector<Point> DevicePoints = new Vector<Point>();
        for (int i = 0; i < detectedMarkers.size(); i++) {
            DevicePoints.add(getMeanPoint(detectedMarkers, i));
        }
        markerPointsIm.fromList(DevicePoints);
        Log.d("markerpointsIm",markerPointsIm.dump());

        // 5. Get perspective transform between eye-image and device image. Call it H1.
        Mat H1 = Calib3d.findHomography(markerPointsProjEye,markerPointsIm,8,10);
        Log.d("H1",H1.dump());

        // 6. Call method to use H1 to correct perspective of image. Then return fixed perspective.
        return correctCorners(rgba,H1,mCoordinates);
    }

    /**
     * Fix perspective of image rgba for single marker case (< 4)
     * @param rgba
     * @param marker
     * @param markerSize
     * @param mCoordinates
     * @return
     */
    public Mat fixPerspectiveSingleMarker2(Mat rgba, Marker marker, double markerSize, float[] mCoordinates) {
        // 0. If you want, draw cube.
        if (draw_cubes) {
            marker.drawCubeBottom(rgba, camParams, colorsBase.get(0));
            marker.draw3dCube(rgba, camParams, colorsCube.get(0));
        }

        // 1. Find the 4 corner points in 3D marker frame:
        MatOfPoint3f markerPoints = getArucoPoints(markerSize, marker);
        Log.d("markerPoints3D",markerPoints.dump());

        // 2. Project points into device image.
        MatOfPoint2f markerPointsIm = new MatOfPoint2f();
        Calib3d.projectPoints(markerPoints, marker.getRvec(), marker.getTvec(), camParams.getCameraMatrix(), camParams.getDistCoeff(), markerPointsIm);
        Log.d("markerPointsIm",markerPointsIm.dump());

        // 3. Create a translation vector from eye poisition to marker. This is to translate coordinates for correct camera projection.
        Mat tEye2Device = Mat.zeros(3, 1, CvType.CV_64FC1);
        tEye2Device.put(0, 0, (mCoordinates[0]+ShiftBackFront[0]));  // X
        tEye2Device.put(1, 0, (mCoordinates[1]+ShiftBackFront[1])); // Y
        tEye2Device.put(2, 0, (mCoordinates[2]+ShiftBackFront[2]));  // Z
        Mat eye2Marker = new Mat();
        Core.add(tEye2Device, marker.getTvec(), eye2Marker);

        // 3. Project points through eye camera matrix to find marker points in eye projection.
        Mat EyeCamMatrix = createCameraMatrix(mCoordinates[2],mCoordinates[2], 0,0);
        MatOfPoint2f markerPointsProjEye = new MatOfPoint2f();
        Calib3d.projectPoints(markerPoints, marker.getRvec(), eye2Marker, EyeCamMatrix, new MatOfDouble(0,0,0,0,0,0,0,0), markerPointsProjEye);
        Log.d("markerPointsEye",markerPointsProjEye.dump());

        // 4. Get perspective transform between eye-image and device image. Call it H1.
        Mat H1 = Calib3d.findHomography(markerPointsProjEye,markerPointsIm,8,10);
        Log.d("H1",H1.dump());

        // 5. Call method to use H1 to correct perspective of image. Then return fixed perspective.
        return correctCorners(rgba,H1,mCoordinates);
    }

    /**
     * Method to correct perspective in image rgba given position of markers and user.
     * @param rgba
     * @param H1
     * @param mCoordinates
     * @return correct perspective.
     */
    private Mat correctCorners(Mat rgba, Mat H1, float[] mCoordinates) {

        // 1. Enter corners of device into the transform H1. Observe the corners are defined as: (vector: eye->front cam) + (vector: front cam -> top-left corner of screen) + (units to given corner). Units are meters.
        MatOfPoint2f cornersDevice = create4Points((mCoordinates[0]+halfwidth*2+camTo00CornerX), (mCoordinates[1]+0+camTo00CornerY),(mCoordinates[0]+0+camTo00CornerX), (mCoordinates[1]+0+camTo00CornerY),(mCoordinates[0]+0+camTo00CornerX), (mCoordinates[1]+halfHeight*2+camTo00CornerY),(mCoordinates[0]+halfwidth*2+camTo00CornerX), (mCoordinates[1]+halfHeight*2+camTo00CornerY));
        MatOfPoint2f cornersDeviceTr = new MatOfPoint2f();
        Core.perspectiveTransform(cornersDevice,cornersDeviceTr, H1);
        Log.d("cornersOfDeviceMulti",cornersDevice.dump());
        Log.d("cornersOfDeviceMultiTr",cornersDeviceTr.dump());

        // 6. check that perspective transform is reasonable
        Mat kalman_corners = new Mat(4,2,CvType.CV_64FC1);
        kalman_corners.put(0,0, cornersDeviceTr.get(0,0)[0]);
        kalman_corners.put(0,1, cornersDeviceTr.get(0,0)[1]);
        kalman_corners.put(1,0, cornersDeviceTr.get(1,0)[0]);
        kalman_corners.put(1,1, cornersDeviceTr.get(1,0)[1]);
        kalman_corners.put(2,0, cornersDeviceTr.get(2,0)[0]);
        kalman_corners.put(2,1, cornersDeviceTr.get(2,0)[1]);
        kalman_corners.put(3,0, cornersDeviceTr.get(3,0)[0]);
        kalman_corners.put(3,1, cornersDeviceTr.get(3,0)[1]);

        Mat kalman_corners_vector = kalman_corners.reshape(0,8);
        Log.d("kalman measurement", kalman_corners_vector.dump());
        if (kalmanInitialized) {
            kalman_corners_vector = kalman.correct(kalman_corners_vector);
        } else {
            kalman = initKalman(kalman_corners_vector);
            kalmanInitialized = true;
        }
        Log.d("kalman correct", kalman_corners_vector.dump());

        kalman.predict();

        MatOfPoint2f corr_corners = new MatOfPoint2f();
        Vector<Point> Points = new Vector<Point>();
        Points.add(new Point(kalman_corners_vector.get(0,0)[0], kalman_corners_vector.get(1,0)[0]));
        Points.add(new Point(kalman_corners_vector.get(2,0)[0], kalman_corners_vector.get(3,0)[0]));
        Points.add(new Point(kalman_corners_vector.get(4,0)[0], kalman_corners_vector.get(5,0)[0]));
        Points.add(new Point(kalman_corners_vector.get(6,0)[0], kalman_corners_vector.get(7,0)[0]));
        corr_corners.fromList(Points);
  //      cornersDeviceTr = CheckPerspectiveWrap(cornersDeviceTr, rgba);

        // 7. Strech these points to the corners of the image.
        MatOfPoint2f cornersScreen = create4Points(rgba.size().width, 0, 0, 0, 0, rgba.size().height, rgba.size().width, rgba.size().height);
        Mat point2CornersTransform = Imgproc.getPerspectiveTransform(corr_corners, cornersScreen);
        Log.d("FinalTransform",point2CornersTransform.dump());
        if (H1.size().height > 0 && H1.size().width > 2 && point2CornersTransform.size().height>0)  {
            Mat dst = new Mat(rgba.size(), CvType.CV_64FC1);
            Imgproc.warpPerspective(rgba, dst, point2CornersTransform, rgba.size());
            return dst;
        }
        else return rgba;
    }

    // TODO Do we want to keep this?
    /**
     * Method to restricts large changes in 4 input points in image rgba.
     * @param points
     * @param rgba
     * @return Return restricted points.
     */
    private MatOfPoint2f CheckPerspectiveWrap (MatOfPoint2f points, Mat rgba) {
        Point[] corners = points.toArray();
        Point[] corners_checked = new Point[4];
        double penalty = rgba.rows()*0.2;
        if (corners_b != null) { // check that earlier corners exist
            double distb1 = Math.abs(Math.pow(Math.pow(corners[0].x - corners_b[0].x,2)+Math.pow(corners[0].y - corners_b[0].y,2),0.5));
            double distb2 = Math.abs(Math.pow(Math.pow(corners[1].x - corners_b[1].x,2)+Math.pow(corners[1].y - corners_b[1].y,2),0.5));
            double distb3 = Math.abs(Math.pow(Math.pow(corners[2].x - corners_b[2].x,2)+Math.pow(corners[2].y - corners_b[2].y,2),0.5));
            double distb4 = Math.abs(Math.pow(Math.pow(corners[3].x - corners_b[3].x,2)+Math.pow(corners[3].y - corners_b[3].y,2),0.5));
            if (distb1 > penalty || distb2 > penalty || distb3 > penalty || distb4 > penalty) { // check if distance from current corners to corners from before is too far
                if (corners_bb != null) { // check if even earlier corners exist
                    if (test_b == 0) { // corners_b and corners_bb were similar, but current corners too far --> current corners are wrong
                        corners_checked = corners_b;
                        test_bb = test_b;
                        test_b = 1;
                    }
                    else { // corners_b were far from corners_bb --> check if current corners close to corners_bb
                        double distbb1 = Math.abs(Math.pow(Math.pow(corners[0].x - corners_bb[0].x,2)+Math.pow(corners[0].y - corners_bb[0].y,2),0.5));
                        double distbb2 = Math.abs(Math.pow(Math.pow(corners[1].x - corners_bb[1].x,2)+Math.pow(corners[1].y - corners_bb[1].y,2),0.5));
                        double distbb3 = Math.abs(Math.pow(Math.pow(corners[2].x - corners_bb[2].x,2)+Math.pow(corners[2].y - corners_bb[2].y,2),0.5));
                        double distbb4 = Math.abs(Math.pow(Math.pow(corners[3].x - corners_bb[3].x,2)+Math.pow(corners[3].y - corners_bb[3].y,2),0.5));
                        if (distbb1 > penalty || distbb2 > penalty || distbb3 > penalty || distbb4 > penalty) { // check if current corners far from corners_bb
                            corners_checked = corners;
                            test_bb = test_b;
                            test_b = 1;
                            Log.d("Corners_check", "Noisy measurements, corners are very different from both iterations before");
                        } else { // corners are similar to corners_bb and corners_b were wrong
                            corners_checked = corners;
                            test_bb = test_b;
                            test_b = 0;
                        }
                    }
                }
                else {
                    corners_checked = corners_b;
                    test_bb = test_b;
                    test_b = 1;
                }
            }
            else { // current corners are close to corners from before
                corners_checked = corners;
                test_bb = test_b;
                test_b = 0;
            }
            corners_bb = corners_b;
        } else {
            corners_checked = corners;
            test_b = 0;
        }
        corners_b = corners;

        Vector<Point> allcorners = new Vector<Point>();
        allcorners.add(corners_checked[0]);
        allcorners.add(corners_checked[1]);
        allcorners.add(corners_checked[2]);
        allcorners.add(corners_checked[3]);
        MatOfPoint2f cornersDeviceTr_checked = new MatOfPoint2f();
        cornersDeviceTr_checked.fromList(allcorners);
        return cornersDeviceTr_checked;
    }
    private Mat getEye2CamTransform(Mat rgba, Marker marker, double markerSize, float[] mCoordinates) {
        // The estimated 4 corner points in 3D marker frame:
        MatOfPoint3f cornerPointsCam = getArucoPoints(markerSize, marker);
        Log.d("Marker corners 3D:",cornerPointsCam.dump());

        // Project points into camera image:
        MatOfPoint2f cornerPointsCamProj = new MatOfPoint2f();
        Calib3d.projectPoints(cornerPointsCam, marker.getRvec(), marker.getTvec(), camParams.getCameraMatrix(), camParams.getDistCoeff(), cornerPointsCamProj);
        Log.d("Marker corners proj:",cornerPointsCamProj.dump());

        Log.d("Camera matrix:", camParams.getCameraMatrix().dump());

        // Create translation vector from camera to the focus point of the pinhole camera constituted by the eyes and camera screen.
        Mat tEye2Device = Mat.zeros(3, 1, CvType.CV_64FC1);
        //tEye2Device.put(0, 0, -halfwidth);  // X
        //tEye2Device.put(1, 0, halfHeight);  // Y (mCoordinates is from camera view, y needs to be inversed)
        tEye2Device.put(2, 0, mCoordinates[2]);  // Z (backwards)
        Mat tDevice2Cam = Mat.zeros(3, 1, CvType.CV_64FC1);
        tDevice2Cam.put(0, 0, ShiftBackFront[0]);  // X (shift from front to back camera)
        tDevice2Cam.put(1, 0, ShiftBackFront[1]);  // Y
        tDevice2Cam.put(2, 0, ShiftBackFront[2]);  // Z
        Mat tEye2Cam = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.add(tEye2Device, tDevice2Cam, tEye2Cam);

        // Get translation vectors from marker to EyeCamera. Therefore we have the definite extrinsic matrix since the rotation vector.
        Mat tEye2Marker = Mat.zeros(3, 1, CvType.CV_64FC1);
        Core.add(tEye2Cam, marker.getTvec(), tEye2Marker);

        // Get rotation of phone
        double theta_x = Math.atan2(mCoordinates[1]-halfHeight,mCoordinates[2]);
        double theta_y = Math.atan2(mCoordinates[0]+halfwidth,mCoordinates[2]);
        Mat rot_x = Mat.zeros(3,1,CvType.CV_64FC1);
        rot_x.put(0,0,theta_x);
        Mat rot_y = Mat.zeros(3,1,CvType.CV_64FC1);
        rot_y.put(1,0,theta_y);

        // Get rotation from eye to marker
        Mat RVEC = new Mat(3,3,CvType.CV_64FC1);
        Calib3d.Rodrigues(marker.getRvec(),RVEC);
        Mat ROT_X = new Mat(3,3,CvType.CV_64FC1);
        Calib3d.Rodrigues(rot_x,ROT_X);
        Mat ROT_Y = new Mat(3,3,CvType.CV_64FC1);
        Calib3d.Rodrigues(rot_y,ROT_Y);
        Mat ROT_PHONE = new Mat(3,3,CvType.CV_64FC1);
        Core.gemm(ROT_X,ROT_Y,1,Mat.zeros(3,3,CvType.CV_64FC1),0,ROT_PHONE);
        Mat ROT_FIN = new Mat(3,3,CvType.CV_64FC1);
        Core.gemm(ROT_PHONE,RVEC,1,Mat.zeros(3,3,CvType.CV_64FC1),0,ROT_FIN);
        Mat rot_fin = new Mat(3,1,CvType.CV_64FC1);
        Calib3d.Rodrigues(ROT_FIN,rot_fin);

        // Create estimation of intrinsic camera matrix for the EyeCamera.
        //Mat EyeCamMatrix = createCameraMatrix(EyeResolution*mCoordinates[2],EyeResolution*mCoordinates[2],EyeResolution*(mCoordinates[0]+halfwidth),EyeResolution*(halfHeight-mCoordinates[1]));
        Mat EyeCamMatrix = createCameraMatrix(EyeResolution*mCoordinates[2],EyeResolution*mCoordinates[2],0,0);
        Log.d("EyeCameraMatrix:", EyeCamMatrix.dump());

        // Project Aruco points onto the screen through the Eye Camera matrix.
        MatOfPoint2f cornerPointsEyeProj = new MatOfPoint2f();
        Calib3d.projectPoints(cornerPointsCam, rot_fin, tEye2Marker, EyeCamMatrix, new MatOfDouble(0,0,0,0,0,0,0,0), cornerPointsEyeProj); // camParams.getCameraMatrix()
        Log.d("dstpoints",cornerPointsEyeProj.dump());

        // Check corners with Kalman
        Mat kalman_corners = new Mat(4,2,CvType.CV_64FC1);
        kalman_corners.put(0,0, cornerPointsEyeProj.get(0,0)[0]);
        kalman_corners.put(0,1, cornerPointsEyeProj.get(0,0)[1]);
        kalman_corners.put(1,0, cornerPointsEyeProj.get(1,0)[0]);
        kalman_corners.put(1,1, cornerPointsEyeProj.get(1,0)[1]);
        kalman_corners.put(2,0, cornerPointsEyeProj.get(2,0)[0]);
        kalman_corners.put(2,1, cornerPointsEyeProj.get(2,0)[1]);
        kalman_corners.put(3,0, cornerPointsEyeProj.get(3,0)[0]);
        kalman_corners.put(3,1, cornerPointsEyeProj.get(3,0)[1]);

        Mat kalman_corners_vector = kalman_corners.reshape(0,8);
        Log.d("kalman measurement", kalman_corners_vector.dump());
        if (kalmanInitialized) {
            kalman_corners_vector = kalman.correct(kalman_corners_vector);
        } else {
            kalman = initKalman(kalman_corners_vector);
            kalmanInitialized = true;
        }
        Log.d("kalman correct", kalman_corners_vector.dump());

        kalman.predict();

        MatOfPoint2f corr_corners = new MatOfPoint2f();
        Vector<Point> Points = new Vector<Point>();
        Points.add(new Point(kalman_corners_vector.get(0,0)[0], kalman_corners_vector.get(1,0)[0]));
        Points.add(new Point(kalman_corners_vector.get(2,0)[0], kalman_corners_vector.get(3,0)[0]));
        Points.add(new Point(kalman_corners_vector.get(4,0)[0], kalman_corners_vector.get(5,0)[0]));
        Points.add(new Point(kalman_corners_vector.get(6,0)[0], kalman_corners_vector.get(7,0)[0]));
        corr_corners.fromList(Points);

        // Use getPerspectiveTransform to get a transform matrix between phone image and EyeCamera image.
        Mat cam2EyeTransform = Imgproc.getPerspectiveTransform(cornerPointsCamProj, corr_corners);
        Log.d("Cam2EyeTransform",cam2EyeTransform.dump());

        //Make this entire section about drawing squares into its own method.
        List<Point> cornerPointsCamProjList = new Vector<Point>();
        cornerPointsCamProjList = cornerPointsCamProj.toList();

        List<Point> cornerPointsEyeProjList = new Vector<Point>();
        cornerPointsEyeProjList = cornerPointsEyeProj.toList();

        // Draw squares:
        /*
        Scalar color = new Scalar(255,255,0);
        for (int i = 0; i < 4; i++){
            Imgproc.line(rgba, cornerPointsCamProjList.get(i), cornerPointsCamProjList.get((i+1)%4), color, 2);
            Imgproc.line(rgba, cornerPointsEyeProjList.get(i), cornerPointsEyeProjList.get((i+1)%4), color, 2);
            Imgproc.line(rgba, cornerPointsEyeProjList.get(i), cornerPointsCamProjList.get(i), color, 2);
        }
       */
        return cam2EyeTransform;
    }
    public Mat fixPerspectiveSingleMarker(Mat rgba, Marker marker, double markerSize, float[] mCoordinates) {
        Size rgbaSize = rgba.size();

        if (draw_cubes) {
            marker.drawCubeBottom(rgba, camParams, colorsBase.get(0));
            marker.draw3dCube(rgba, camParams, colorsCube.get(0));
        }

        Mat cam2EyeTransform = getEye2CamTransform(rgba, marker, markerSize,mCoordinates);

        // Transform frame from camera to eye:
        MatOfPoint2f frameCam2EyeTransformed = new MatOfPoint2f();
        Imgproc.warpPerspective(rgba, frameCam2EyeTransformed, cam2EyeTransform, rgbaSize);

        return frameCam2EyeTransformed;
    }

}