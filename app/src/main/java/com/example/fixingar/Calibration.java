package com.example.fixingar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.opencv.core.Mat;

import es.ava.aruco.CameraParameters;

public class Calibration extends AppCompatActivity {
    private Button mReturnButton;
    private Button mFrontCalibButton;
    private Button mBackCalibButton;
    private double DotDist = 0;
    private TextView CalibInfo;
    private EditText DotDistText;
    private CameraParameters camParamsFront;
    private CameraParameters camParamsBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        mReturnButton = findViewById(R.id.button_return2);
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnToSettings();
            }
        });
        mFrontCalibButton = findViewById(R.id.calibrate_front);
        mFrontCalibButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoToFrontCalibration();
            }
        });
        mBackCalibButton = findViewById(R.id.calibrate_back);
        mBackCalibButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoToBackCalibration();
            }
        });
        DotDistText = findViewById(R.id.calibration_dot_distance);
        if (DotDistText.getText().toString().isEmpty() == false) {
        DotDist = Double.parseDouble(DotDistText.getText().toString());
        }
        else {
            if (DotDist != 0){
                DotDistText.setText(Double.toString(DotDist));
            }
        }
        DotDistText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (DotDistText != null) {
                    DotDist = Double.parseDouble(DotDistText.getText().toString());}
                else {DotDist = 0;}
            }
        });
        CalibInfo = findViewById(R.id.calibrate_warning);
        CalibInfo.setText("");
    }

    public void ReturnToSettings() {
        camParamsFront = new CameraParameters("front");
        camParamsFront.read(this);
        Mat cpf = camParamsFront.getCameraMatrix();
        if (cpf.get(1,1)[0] == -1 &&  cpf.get(2,2)[0] == -1) {
            CalibInfo.setText("Please calibrate the front camera.");
        }
        else {
            camParamsBack = new CameraParameters("Back");
            camParamsBack.read(this);
            Mat cpb = camParamsBack.getCameraMatrix();
            if (cpb.get(1,1)[0] == -1 &&  cpb.get(2,2)[0] == -1) {
                CalibInfo.setText("Please calibrate the back camera.");
            } else {
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
            }
            }
    }

    public void GoToFrontCalibration() {
        if (DotDist == 0){
            CalibInfo.setText("Please specify half the distance in between dots on the calibration sheet.");
        }
        else {
        Intent intent = new Intent(this, FrontCalibration.class);
        startActivity(intent);}
    }

    public void GoToBackCalibration() {
        if (DotDist == 0){
            CalibInfo.setText("Please specify half the distance in between dots on the calibration sheet.");
        }
        else {
        Intent intent = new Intent(this, BackCalibration.class);
        startActivity(intent);}
    }
}
