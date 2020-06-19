package com.example.fixingar;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Calibration extends AppCompatActivity {
    private Button mReturnButton;
    private Button mFrontCalibButton;
    private Button mBackCalibButton;

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
    }

    public void ReturnToSettings() {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    public void GoToFrontCalibration() {
        Intent intent = new Intent(this, FrontCalibration.class);
        startActivity(intent);
    }

    public void GoToBackCalibration() {
        Intent intent = new Intent(this, BackCalibration.class);
        startActivity(intent);
    }
}
