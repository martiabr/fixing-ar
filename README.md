# fixing-ar
Fixing Augmented Reality 3D Vision Project

This project was conducted by Oskar Keding, Martin Brandt, Caroline Sauget, Julia Kenel.

The app was designed and tested on a Samsung Galaxy S10e.

For the calibration, this file is needed: "\fixing-ar\acircles_pattern.png"

Individual contributions of the different modules in the application:
- Settings Menu: Julia
- Calibration: Martin
- Aruco Marker Tracking: Martin and Oskar
- Face and Eye Detection: Caroline and Julia
- Draw cubes on markers: Martin
- PerspectiveFixer (1st attempt): Martin and Julia. In this version, the method Imgproc.warpPerspective was applied to the image, 
using the transform between the Marker corners as seen by the user and the Marker corners from the camera perspective. Rotation of the phone with respect to
the user was considered. However, the second method yielded better results, so this method was dropped.
- PerspectiveFixer (2nd attempt and final version): Oskar. In this version, the device corners are projected in the user's perspective and 
transformed to the camera perspective. Then they are defined as the new image corners. Julia and Martin helped with the Kalman filter.


