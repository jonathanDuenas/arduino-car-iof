package me.aflak.bluetoothterminal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import me.aflak.bluetooth.Bluetooth;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback {
    private String name;
    private Bluetooth b;
    private TextView text;
    private ImageView distance;
    private ImageView temperature;

    private boolean headlights;

    private TextView temp;
    private TextView direction;
    private TextView speed;

    private TextView dist;
    private ScrollView scrollView;
    private boolean registered=false;

    private boolean stop = true;
    private boolean ahead = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        headlights = false;

        text = (TextView)findViewById(R.id.text);
        temp = (TextView)findViewById(R.id.temp);
        dist = (TextView)findViewById(R.id.dist);

        direction = (TextView)findViewById(R.id.direction);
        speed = (TextView)findViewById(R.id.speed);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        distance = (ImageView)findViewById(R.id.distance_img);
        temperature = (ImageView)findViewById(R.id.temp_img);

        temp.setText("?°");
        dist.setText("? cm");

        text.setMovementMethod(new ScrollingMovementMethod());


        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");
        name = b.getPairedDevices().get(pos).getName();

        Display("Connecting...");
        b.connectToDevice(b.getPairedDevices().get(pos));

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered=true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(registered) {
            unregisterReceiver(mReceiver);
            registered=false;
        }
    }

    public void Display(final String s){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.append(s + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public void DisplayT(final String s){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                temp.setText(s);
            }
        });
    }

    public void DisplayD(final String s){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dist.setText(s);
            }
        });
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Display("Connected to "+device.getName()+" - "+device.getAddress());
        startTimer();
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Display("Disconnected!");
        Display("Connecting again...");
        b.connectToDevice(device);
    }

    @Override
    public void onMessage(String message) {
        switch(message.substring(0,2)){
            case "T:":
                final String temp_p = message.substring(0);
                if(true){
                    Log.d("Temperatura ", temp_p.substring(2));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(Float.parseFloat(temp_p.substring(2)) < 15.00) {
                                temperature.setImageDrawable(getResources().getDrawable(R.drawable.thermo_blue));
                                temp.setTextColor(Color.BLUE);
                            }
                            else if (Float.parseFloat(temp_p.substring(2))>=15.00 && Float.parseFloat(temp_p.substring(2))<=37.00){
                                temperature.setImageDrawable(getResources().getDrawable(R.drawable.thermo_white));
                                temp.setTextColor(Color.WHITE);
                            }
                            else {
                                temperature.setImageDrawable(getResources().getDrawable(R.drawable.thermo_red));
                                temp.setTextColor(Color.RED);
                            }

                            DisplayT(temp_p.substring(2)+ "°");
                        }
                    });
                }

                break;
            case "D:":
                final String  dist_p = message;
                final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(Integer.parseInt(dist_p.substring(2)) < 6) {
                            distance.setImageDrawable(getResources().getDrawable(R.drawable.distance_red));
                            dist.setTextColor(Color.RED);
                            vibrator.vibrate(250);
                        }
                        else {
                            distance.setImageDrawable(getResources().getDrawable(R.drawable.distance_white));
                            dist.setTextColor(Color.WHITE);
                        }
                        Log.d("kha:",dist_p.substring(2));
                        DisplayD(dist_p.substring(2)+ " cm");
                    }
                });

                break;
            case "P:":
                final String directionT = message.substring(2);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        direction.setText(directionT);
                    }
                });
                break;
            case "V:":
                final String speedT = message.substring(2);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speed.setText(speedT + "km/s");
                    }
                });
                break;
            case "M:":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stop = true;
                    }
                });
                break;
            default:
                Display(name+": "+message);
                break;
        }
    }

    @Override
    public void onError(String message) {
        Display("Error: "+message);
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
        Display("Error: "+message);
        Display("Trying again in 3 sec.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.connectToDevice(device);
                    }
                }, 2000);
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(Chat.this, Select.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }
    };

    //todo send bluetooth string from lights on
    public void turnOnLights(View view) throws InterruptedException {
        LinearLayout car;
        car = (LinearLayout)findViewById(R.id.car_image);
        if (headlights){
            headlights = false;
            car.setBackground(getResources().getDrawable(R.drawable.velar));

        }
        else{
            car.setBackground(getResources().getDrawable(R.drawable.velar_lights_on));

            headlights = true;
        }
    }

    public void drive(View view) throws InterruptedException {
        this.stop = false;
        this.ahead = true;
    }

    public void reverse(View view) throws InterruptedException {
        this.stop = false;
        this.ahead = false;
    }

    public void stopCar(View view) throws InterruptedException {
        this.stop = true;
    }


    private void startTimer(){

        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            public void run() {
                CountDownTimer counter = new CountDownTimer(30000, 500){
                    public void onTick(long millisUntilDone){
                        Calendar rightNow = Calendar.getInstance();
                        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
                        if(currentHour >= 18 || currentHour <= 5){
                            if(!headlights){
                                headlights = true;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        LinearLayout car;
                                        car = (LinearLayout)findViewById(R.id.car_image);
                                        car.setBackground(getResources().getDrawable(R.drawable.velar_lights_on));
                                    }
                                });
                            }
                        }

                        if(b.isConnected()){
                            String val = "";

                            if(stop)
                                val+="s";
                            else{
                                if(ahead)
                                    val+="a";
                                else
                                    val+="r";
                            }
                            if(headlights)
                                val+="e";
                            else
                                val+="o";

                            b.send("&" + val + "&"+"\n");
                        }
                    }
                    public void onFinish() {
                        startTimer();
                    }
                }.start();
            }
        });
    }
}