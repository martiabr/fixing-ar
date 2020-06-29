# fixing-ar
Fixing Augmented Reality 3D Vision Project

This project was conducted by Oskar Keding, Martin Brandt, Caroline Sauget, Julia Kenel.

The app was designed and tested on a Samsung Galaxy S10e. The app was designed for Android devices.

For the calibration, this file is needed: "\fixing-ar\acircles_pattern.png"
For the aruco markers, these pictures were used: "\fixing-ar\aruco4.pdf" in the single marker case
and "\fixing-ar\aruco12.pdf" for the multiple marker case

The application was developed on Android Studio, Version 3.6.1. OpenCV version 4.2.0 was imported. 

Below a short description of the source code in ...\src\main\java\ :
- aruco.min3d: 
- com.example.fixingar:
	- BackCalibration and FrontCalibration are based on the Calibration algorithm by OpenCV and
	 were adjusted to our needs. They are the calibration activity files.
	- CalibrationResult, CameraCalibrator and OnCameraFrameRender are complementing the BackCalibration
	 and FrontCalibration activity. They originate from the Calibration algorithm by OpenCV and were barely changed.
	- Calibration is activity file for the calibration settings menu and was created by Julia.
	- FaceDetection includes all code related to face and eye tracking and is used within the main
	 activity when the front camera is active. The face detection is based on an example project by
	 OpenCV. However, FaceDetection has been elaborated to include eye detection, to extract the 
	 coordinates and width of the eyes/face and to calculate the distance and shift of user-device. Most
	 of the code was therefore created by Caroline and Julia.
	- MainActivity was created by all group members. It includes the switching between cameras and uses
	 FaceDetection if the front camera is active and PerspectiveFixer when the back camera is active. It
	 also includes the Aruco Marker Tracking, which is originally from ...
	- PerspectiveFixer was created mainly by Oskar and Martin, later Julia added onto it. The final version
	 includes Oskar's version of "fixing the perspective". It defines the Eye-to-Device-Camera, finds
	 the homography in between pinhole cameras and warps the perspective.
	- Settings is the activity file for the settings menu, created by Julia.
	- Variables includes device specific variables of the 4 group members.

- es.ava.aruco:
    -debug
    -exceptions
    -Board, BoardConfiguraiton and BoardDetector are for the calibration of the cameras. They contain all the
    informations regarding the pattern used to calibrate.
    -CameraParameters gives the camera matrix and camera distorsion matrix
    -Code, Marker and MarkerDetector are the markers for the aruco detection
    -Utils contains various functions used in the other classes

-min3d: 
 	a minimal 3D framework for Android used by the Aruco marker tracker module.

-org.opencv.examples.facedetect:
    -DetectionBasedTracker comes with the example imported from OpenCV for face tracking.

