package com.example.virtualmouse;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    private Socket socket;
    private PrintWriter out;

    private Button btnSetip;
    private EditText edit;
    private LinearLayout pad;

    private float curX, curY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        curX = 500.0f;
        curY = 500.0f;
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        btnSetip = findViewById(R.id.setip);
        edit = findViewById(R.id.edit);
        pad = findViewById(R.id.layout_pad);
        pad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                try {
                    if (out != null) {
                        out.println(String.format("m %g %g", motionEvent.getX(), motionEvent.getY()));
                        out.flush();
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
                return false;
            }
        });
        btnSetip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            socket = new Socket(edit.getText().toString(), 2288);
                            out = new PrintWriter(socket.getOutputStream());
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        edit.setText("192.168.43.4");
        Thread netThread = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    socket = new Socket("192.168.43.4", 2288);
                    out = new PrintWriter(socket.getOutputStream());
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        netThread.start();

    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = event.values.clone();
// Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x*x + y*y + z*z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            curX += x;
            curY += y;
// Make this higher or lower according to how much
// motion you want to detect
            if(mAccel > 0.01){
                try {
                    if (out != null) {
                        out.println(String.format(Locale.ROOT, "m %d %d", (int) curX, (int) curY));
                        out.flush();
                    }
                    Log.e("coord", String.format(Locale.ROOT, "%f, %f", x, y));
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }}

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
