package com.example.stepcounter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;

//import com.github.mikephil.charting.charts.LineChart;
//import com.github.mikephil.charting.components.XAxis;
//import com.github.mikephil.charting.data.Entry;
//import com.github.mikephil.charting.data.LineData;
//import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView stepCountTextView;
    private ImageView resetButton;
    private ImageView showDistanceButton;
    private double stepLengthCm = 0.0;
    private DBHelper dbHelper;
    private int stepCount = 0;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private String todayDate;
    private boolean isSensorAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepCountTextView = findViewById(R.id.stepCountTextView);
        resetButton = findViewById(R.id.resetButton);

        dbHelper = new DBHelper(this);
        initSensor();

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.clearStepData();
                stepCount = 0;
                updateStepCount();
            }
        });

        showDistanceButton = findViewById(R.id.showDistanceButton);

        showDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDistanceInputDialog();
            }
        });

        loadSavedStepCount();
    }

    public void openLinkedInPage(View view) {
        String linkedInProfile = "nerkar-mansi";
        String linkedInUrl = "https://www.linkedin.com/in/" + linkedInProfile;

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the case where the LinkedIn app or website is not available
            // You may want to open the Play Store to prompt the user to install the LinkedIn app
            // or open the LinkedIn website in a web browser
        }
    }



    public void openGitHub(View view) {
        String gitHubProfile = "mansinerkar-11";
        String gitHubUrl = "https://github.com/" + gitHubProfile;

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(gitHubUrl));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the case where the LinkedIn app or website is not available
            // You may want to open the Play Store to prompt the user to install the LinkedIn app
            // or open the LinkedIn website in a web browser
        }
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (stepSensor != null) {
            isSensorAvailable = true;
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            isSensorAvailable = false;
            Toast.makeText(this, "Step detector sensor not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedStepCount() {
        // Load today's step count from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        stepCount = sharedPreferences.getInt(todayDate, 0);
        updateStepCount();
    }

    private void updateStepCount() {
        stepCountTextView.setText("Steps: " + stepCount);
    }

    private final SensorEventListener stepListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                // Increment the step count for each step detected
                stepCount++;
                updateStepCount();

                // Save the step count in the database with today's date and time
                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                dbHelper.insertStepData(todayDate, currentTime, stepCount); // Use todayDate and currentTime here
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not needed for step counting
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        // Save the current step count in SharedPreferences
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SharedPreferences sharedPreferences = getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(todayDate, stepCount);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the sensor listener to prevent battery drain
        if (isSensorAvailable) {
            sensorManager.unregisterListener(stepListener, stepSensor);
        }
    }

    private void showDistanceInputDialog() {
        // Inflate the distance input dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_distance_input, null);

        // Find views in the dialog layout
        EditText stepLengthEditText = dialogView.findViewById(R.id.stepLengthEditText);
        TextView calculateDistanceButton = dialogView.findViewById(R.id.calculateDistanceButton);

        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Enter Step Length (cm)");
        builder.setView(dialogView);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Set a click listener for the "Calculate Distance" button
        calculateDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve the step length entered by the user
                String stepLengthStr = stepLengthEditText.getText().toString().trim();
                if (!stepLengthStr.isEmpty()) {
                    try {
                        // Parse the step length as a double
                        stepLengthCm = Double.parseDouble(stepLengthStr);

                        // Calculate the distance based on step count and step length
                        double distanceMeters = (stepCount * stepLengthCm) / 100.0; // Convert cm to meters

                        // Display the distance in a popup
                        showDistanceResultPopup(distanceMeters);

                        // Dismiss the input dialog
                        dialog.dismiss();
                    } catch (NumberFormatException e) {
                        // Handle invalid input
                        Toast.makeText(MainActivity.this, "Invalid step length", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Show the input dialog
        dialog.show();
    }

    private void showDistanceResultPopup(double distanceMeters) {
        // Create a new AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Distance Calculation Result");
        builder.setMessage("Distance: " + distanceMeters + " meters\n\nYou are doing Great! Keep Going.");

        // Set a "Close" button and its action
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Dismiss the dialog when the "Close" button is clicked
            }
        });

        // Create the AlertDialog
        AlertDialog resultDialog = builder.create();
        resultDialog.show();
    }
}