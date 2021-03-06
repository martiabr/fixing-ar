package com.example.fixingar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class Settings extends AppCompatActivity {
    private Button mCalibrateButton;
    private Button mReturnButton;
    private TextView VarInfo;
    private EditText MarkerSize;
    private EditText FaceWidth;
    private EditText EyeDist;
    private Switch DrawCubes;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    public Settings() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPref = this.getSharedPreferences("variables", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        mCalibrateButton = findViewById(R.id.button_calibrate);
        mCalibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoToCalibration();
            }
        });
        mReturnButton = findViewById(R.id.button_return);
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnToMain();
            }
        });

        MarkerSize = findViewById(R.id.marker_size);
        if (MarkerSize.getText().toString().isEmpty() && sharedPref.contains("MarkerSize")) {
            MarkerSize.setText(Float.toString(sharedPref.getFloat("MarkerSize", 0)));
        }
        MarkerSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (MarkerSize.getText().toString() != "") {
                    editor.putFloat("MarkerSize", Float.parseFloat(MarkerSize.getText().toString()));
                }
                else {editor.remove("MarkerSize");}
                editor.commit();
            }
        });

        FaceWidth = findViewById(R.id.facewidth);
        if (FaceWidth.getText().toString().isEmpty() && sharedPref.contains("FaceWidth")) {
            FaceWidth.setText(Float.toString(sharedPref.getFloat("FaceWidth", 0)));
        }
        FaceWidth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (FaceWidth.getText().toString() != "") {
                    editor.putFloat("FaceWidth", Float.parseFloat(FaceWidth.getText().toString()));
                }
                else {editor.remove("FaceWidth");}
                editor.commit();
            }
        });

        EyeDist = findViewById(R.id.eyedistance);
        if (EyeDist.getText().toString().isEmpty() && sharedPref.contains("EyeDist")) {
            EyeDist.setText(Float.toString(sharedPref.getFloat("EyeDist", 0)));
        }
        EyeDist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (EyeDist.getText().toString() != "") {
                    editor.putFloat("EyeDist", Float.parseFloat(EyeDist.getText().toString()));
                }
                else {editor.remove("EyeDist");}
                editor.commit();
            }
        });

        DrawCubes = findViewById(R.id.switch_draw_cubes);
        DrawCubes.setChecked(sharedPref.getBoolean("DrawCubes", false));
        DrawCubes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean("DrawCubes", isChecked);
                editor.commit();
            }
        });

        VarInfo = findViewById(R.id.warning_variables);
        VarInfo.setText("");
    }

    public void GoToCalibration() {
        Intent intent = new Intent(this, Calibration.class);
        startActivity(intent);
    }

    public void ReturnToMain() {
        if (sharedPref.getFloat("MarkerSize", 0) == 0) {
            VarInfo.setText("Please fill in the Marker Size.");
        }
        else {
            if (sharedPref.getFloat("FaceWidth", 0) == 0) {
                VarInfo.setText("Please fill in the Face Width.");
            }
            else {
                if (sharedPref.getFloat("EyeDist", 0) == 0) {
                    VarInfo.setText("Please fill in the Eye Distance.");
                }
                else {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
            }
        }
    }

    public float[] GetVariables(Activity activity) {
        sharedPref = activity.getSharedPreferences("variables", Context.MODE_PRIVATE);
        float[] variables = new float[3];
        variables[0] = sharedPref.getFloat("MarkerSize", 0);
        variables[1] = sharedPref.getFloat("FaceWidth", 0);
        variables[2] = sharedPref.getFloat("EyeDist", 0);
        return variables;
    }

    public boolean GetDrawCubes(Activity activity) {
        sharedPref = activity.getSharedPreferences("variables", Context.MODE_PRIVATE);
        return sharedPref.getBoolean("DrawCubes", false);
    }

}
