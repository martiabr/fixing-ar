package com.example.fixingar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import es.ava.aruco.CameraParameters;

public class Calibration extends AppCompatActivity {
    private static final String TAG = "CalibrationMenu";
    private Button mReturnButton;
    private Button mFrontCalibButton;
    private Button mBackCalibButton;
    private TextView CalibInfo;
    private EditText DotDistText;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    public Calibration() {
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    Mat MatTest=new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        sharedPref = this.getSharedPreferences("variables", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

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
        // TODO: there is a bug if i delete the decimal point of the dot distance. It is not possible to write afterwards
        if (DotDistText.getText().toString().isEmpty() && sharedPref.contains("DotDist")) {
            DotDistText.setText(Float.toString(sharedPref.getFloat("DotDist", 0)));
        }
        DotDistText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (DotDistText.getText().toString().isEmpty()) {
                    editor.remove("DotDist");
                }
                else {
                    editor.putFloat("DotDist", Float.parseFloat(DotDistText.getText().toString()));
                }
                editor.commit();
            }
        });
        CalibInfo = findViewById(R.id.calibrate_warning);
        CalibInfo.setText("");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void ReturnToSettings() {
        CameraParameters camParamsFront = new CameraParameters("front");
        if (camParamsFront.CheckIfItExists(this)) {
            CameraParameters camParamsBack = new CameraParameters("back");
            if (camParamsBack.CheckIfItExists(this)) {
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
            } else {
                CalibInfo.setText("Please calibrate the back camera.");
            }
        }
        else {
            CalibInfo.setText("Please calibrate the front camera.");
            }
    }

    public void GoToFrontCalibration() {
        if (sharedPref.contains("DotDist") && sharedPref.getFloat("DotDist", 0) != 0){
            Intent intent = new Intent(this, FrontCalibration.class);
            startActivity(intent);
        }
        else {
            CalibInfo.setText("Please specify half the distance in between dots on the calibration sheet.");
        }
    }

    public void GoToBackCalibration() {
        if (sharedPref.contains("DotDist") && sharedPref.getFloat("DotDist", 0) != 0){
            Intent intent = new Intent(this, BackCalibration.class);
            startActivity(intent);
        }
        else {
            CalibInfo.setText("Please specify half the distance in between dots on the calibration sheet.");
        }
    }

    public float getDotDist(Activity activity) {
        sharedPref = activity.getSharedPreferences("variables", Context.MODE_PRIVATE);
        float DotDist = sharedPref.getFloat("DotDist", 0);
        return DotDist;
    }
}
