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
import android.os.Handler;
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

import io.github.controlwear.virtual.joystick.android.JoystickView;
import me.aflak.bluetooth.Bluetooth;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback {
    private String name;
    private Bluetooth b;
    private EditText message;
    private Button send;
    private TextView text;
    private ImageView distance;
    private ImageView temperature;

    //TODO
    //Headlights Status
    private boolean headlights;

    private TextView temp;
    private TextView dist;
    private ScrollView scrollView;
    private boolean registered=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        headlights = false;

        text = (TextView)findViewById(R.id.text);
        temp = (TextView)findViewById(R.id.temp);
        dist = (TextView)findViewById(R.id.dist);
        message = (EditText)findViewById(R.id.message);
        send = (Button)findViewById(R.id.send);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        distance = (ImageView)findViewById(R.id.distance_img);
        temperature = (ImageView)findViewById(R.id.temp_img);

        temp.setText("?°");
        dist.setText("? cm");

        text.setMovementMethod(new ScrollingMovementMethod());
        send.setEnabled(false);

        JoystickView joystick = (JoystickView) findViewById(R.id.joystick);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                message.setText("AN:" + angle + "-ST:" + strength);
            }
        });

        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");
        name = b.getPairedDevices().get(pos).getName();

        Display("Connecting...");
        b.connectToDevice(b.getPairedDevices().get(pos));

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = message.getText().toString();
                message.setText("");
                b.send(msg);
                Display("You: "+msg);
            }
        });

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.close:
                b.removeCommunicationCallback();
                b.disconnect();
                Intent intent = new Intent(this, Select.class);
                startActivity(intent);
                finish();
                return true;

            case R.id.rate:
                Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                send.setEnabled(true);
            }
        });
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Display("Disconnected!");
        Display("Connecting again...");
        b.connectToDevice(device);
    }

    @Override
    public void onMessage(String message) {

        switch(message.charAt(0)){
            case 'T':
                final String temp_p = message;
                if(!temp_p.matches("(.*)p =(.*)")){
                    Log.d("Temperatura ", temp_p.substring(2));
                    //DisplayT(message.substring(2)+ "°");

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


                //Log.d("Temperatura ", temp_p.substring(2));
                //DisplayT(message.substring(2)+ "°");
                break;
            case 'D':
                final String  dist_p = message;
                final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                Log.d("kha:",dist_p);
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
            for (int i = 0;i<5;i++){
                b.send("o\n");
                Thread.sleep(1);
            }

        }
        else{
            car.setBackground(getResources().getDrawable(R.drawable.velar_lights_on));
            for(int i=0; i<5; i++) {
                b.send("e\n");
                Thread.sleep(1);
            }
            headlights = true;
        }
    }
}
