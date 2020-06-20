package com.example.fixingar;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import es.ava.aruco.CameraParameters;

public class Calibration extends AppCompatActivity {
    private Button mReturnButton;
    private Button mFrontCalibButton;
    private Button mBackCalibButton;
    private double DotDist = 0;
    private TextView CalibInfo;
    private EditText DotDistText;
    private CameraParameters camParamsBack;
    private CameraParameters camParamsFront;


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
        camParamsFront = new CameraParameters("Front");
        camParamsFront.read(this);
        if (camParamsFront == null){
            CalibInfo.setText(R.string.CalibrateFront);
        }
        else {
            camParamsBack = new CameraParameters("Back");
            camParamsBack.read(this);
            if (camParamsBack == null) {
                CalibInfo.setText(R.string.CalibrateBack);
            }
            else {
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
