package com.example.fixingar;

import android.app.Activity;

public class Variables {
    private String who;
    // Julia
    // face detection variables
    private double focallength_J = 3.75*0.001;
    private double resolution_J = 1920;
    private double est_J = 1.3;
    private float FaceWidth_J = 0.14f;
    private float EyeDist_J = 0.06f;
    // perspective fixer variables
    private float MarkerSize_J = 0.03f;
    private double halfwidth_J = 0.103/2;
    private double halfHeight_J = 0.058/2;
    // Position of front camera.
    private double camTo00CornerX_J = 0;
    private double camTo00CornerY_J = 0.066/2; // y vector from camera to (0,0) image corner (top-left). Positive direction is downwards.
    private double[] ShiftFrontBackCamera_J = {0.012, -0.025, 0.008};
    // Martin
    // face detection variables
    private double focallength_M = 2.95*0.001;
    private double resolution_M = 2560;
    private double est_M = 0.75;
    private float FaceWidth_M = 0.14f;
    private float EyeDist_M = 0.06f;
    // perspective fixer variables
    private float MarkerSize_M = 0.08f;
    private double halfwidth_M = 0.14/2;
    private double halfHeight_M = 0.066/2;
    // Position of front camera.
    private double camTo00CornerX_M = 0;
    private double camTo00CornerY_M = 0.066/2; // y vector from camera to (0,0) image corner (top-left). Positive direction is downwards.
    private double[] ShiftFrontBackCamera_M = {0.012, 0.025, 0.008};
    // Oskar
    // face detection variables
    private double focallength_O = 3.26*0.001;
    private double resolution_O = 1920;//3840;
    private double est_O = 1.2  ;
    private float FaceWidth_O = 0.14f;
    private float EyeDist_O = 0.065f;
    // perspective fixer variables
    private float MarkerSize_O = 0.111f;
    private double halfwidth_O = 0.105/2;
    private double halfHeight_O = 0.059/2;
    // Position of front camera.
    private double camTo00CornerX_O = 0.012 ;
    private double camTo00CornerY_O = -0.002; // y vector from camera to (0,0) image corner (top-left). Positive direction is downwards.
    private double[] ShiftFrontBackCamera_O = {0.018, 0.016, 0.005};
    // Caroline
    // face detection variables
    private double focallength_C = 3.75*0.001;
    private double resolution_C = 1920;
    private double est_C = 1.3;
    private float FaceWidth_C = 0.14f;
    private float EyeDist_C = 0.06f;
    // perspective fixer variables
    private float MarkerSize_C = 0.08f;
    private double halfwidth_C = 0.14/2;
    private double halfHeight_C = 0.066/2;
    // Position of front camera.
    private double camTo00CornerX_C = 0;
    private double camTo00CornerY_C = 0.066/2; // y vector from camera to (0,0) image corner (top-left). Positive direction is downwards.
    private double[] ShiftFrontBackCamera_C = {0.012, 0.025, 0.008};



    public Variables(String WHO){
        who = WHO;
    }

    public double[] getShiftFrontBackCamera(){
        double[] Shift = ShiftFrontBackCamera_J;
        if (who == "Martin") {
            Shift = ShiftFrontBackCamera_M;
        }
        if (who == "Caroline") {
            Shift = ShiftFrontBackCamera_C;
        }
        if (who == "Oskar") {
            Shift = ShiftFrontBackCamera_O;
        }
        return Shift;
    }

    public double getCamTo00CornerY(){
        double CamTo00CornerY = camTo00CornerY_J;
        if (who == "Martin") {
            CamTo00CornerY = camTo00CornerY_M;
        }
        if (who == "Caroline") {
            CamTo00CornerY = camTo00CornerY_C;
        }
        if (who == "Oskar") {
            CamTo00CornerY = camTo00CornerY_O;
        }
        return CamTo00CornerY;
    }

    public double getCamTo00CornerX(){
        double CamTo00CornerX = camTo00CornerX_J;
        if (who == "Martin") {
            CamTo00CornerX = camTo00CornerX_M;
        }
        if (who == "Caroline") {
            CamTo00CornerX = camTo00CornerX_C;
        }
        if (who == "Oskar") {
            CamTo00CornerX = camTo00CornerX_O;
        }
        return CamTo00CornerX;
    }

    public double getHalfHeight(){
        double halfheight = halfHeight_J;
        if (who == "Martin") {
            halfheight = halfHeight_M;
        }
        if (who == "Caroline") {
            halfheight = halfHeight_C;
        }
        if (who == "Oskar") {
            halfheight = halfHeight_O;
        }
        return halfheight;
    }

    public double getHalfWidth(){
        double halfwidth = halfwidth_J;
        if (who == "Martin") {
            halfwidth = halfwidth_M;
        }
        if (who == "Caroline") {
            halfwidth = halfwidth_C;
        }
        if (who == "Oskar") {
            halfwidth = halfwidth_O;
        }
        return halfwidth;
    }

    public double getFocallength(){
        double focallength = focallength_J;
        if (who == "Martin") {
            focallength = focallength_M;
        }
        if (who == "Caroline") {
            focallength = focallength_C;
        }
        if (who == "Oskar") {
            focallength = focallength_O;
        }
        return focallength;
    }

    public double getResolution(){
        double resolution = resolution_J;
        if (who == "Martin") {
            resolution = resolution_M;
        }
        if (who == "Caroline") {
            resolution = resolution_C;
        }
        if (who == "Oskar") {
            resolution = resolution_O;
        }
        return resolution;
    }

    public double getEst(){
        double est = est_J;
        if (who == "Martin") {
            est = est_M;
        }
        if (who == "Caroline") {
            est = est_C;
        }
        if (who == "Oskar") {
            est = est_O;
        }
        return est;
    }

    public float getFaceWidth(Activity activity){
        Settings settings = new Settings();
        float[] variables = settings.GetVariables(activity);
        float FaceWidth = variables[1];
        return FaceWidth;
    }

    public float getEyeDist(Activity activity){
        Settings settings = new Settings();
        float[] variables = settings.GetVariables(activity);
        float EyeDist = variables[2];
        return EyeDist;
    }

    public float getMarkerSize(Activity activity){
        Settings settings = new Settings();
        float[] variables = settings.GetVariables(activity);
        float MarkerSize = variables[0];
        return MarkerSize;
    }

}
