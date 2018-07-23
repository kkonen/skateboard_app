package de.kkonen.skateboard.skateboard;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import io.moquette.BrokerConstants;
import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;


public class MainActivity extends AppCompatActivity {

    final Server server = new Server();
    MqttClient client;

    private static final String TAG = "MainActivity";
    public static final int EXTERNAL_STORAGE_REQUEST = 80085;
    public static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "Skateboard" + File.separator;

    Button start_broker_button, request_permission_button,
            start_mqtt_client_button, change_skater_button, record_button;

    TextView permission_status_textView, broker_status_textView, client_status_textView,
            skater_name_textView;

    RadioGroup trick_radio_group;
    RadioButton plain_data_radio_button, ollie_radio_button, nollie_radio_button,
            pop_shuv_it_radio_button, kickflip_radio_button, heelflip_radio_button;

    String skater_name = "Skater";
    String trick = "Plain-Data";

    Boolean recording = false;

    File recording_file;

    FileOutputStream fOut;
    OutputStreamWriter outWriter;

    DateFormat df;
    Calendar calobj;

    protected void onCreate(Bundle savedInstanceState) {

        super.setTitle("Skateboard-Trick Classification");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        df = new SimpleDateFormat("dd_MM_yy-HH_mm_ss");
        calobj = Calendar.getInstance();


        File f = new File(PATH);
        if (!f.exists()) {
            f.mkdirs();
        }
        //LOGGER
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR); //SET TO DEBUG/INFO/ERROR WHATEVER IF NEEDED

        //CONNECT VIEW ELEMENTS
        start_broker_button = findViewById(R.id.start_broker_button);
        request_permission_button = findViewById(R.id.request_permission_button);
        start_mqtt_client_button = findViewById(R.id.start_mqtt_client_button);
        change_skater_button = findViewById(R.id.change_skater_button);
        record_button = findViewById(R.id.record_button);
        permission_status_textView = findViewById(R.id.permission_status_text);
        broker_status_textView = findViewById(R.id.broker_status_text);
        client_status_textView = findViewById(R.id.client_status_text);
        skater_name_textView = findViewById(R.id.skater_name_textView);

        trick_radio_group = findViewById(R.id.trick_radio_group);
        plain_data_radio_button = findViewById(R.id.plain_data_radio_button);
        ollie_radio_button = findViewById(R.id.ollie_radio_button);
        nollie_radio_button = findViewById(R.id.nollie_radio_button);
        pop_shuv_it_radio_button = findViewById(R.id.pop_shuv_it_radio_button);
        kickflip_radio_button = findViewById(R.id.kickflip_it_radio_button);
        heelflip_radio_button = findViewById(R.id.heelflip_it_radio_button);


        permissionCheck(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        createButtonListener();

    }

    public void permissionCheck(String permission) {

        if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            permission_status_textView.setText("Permission granted!");
            start_broker_button.setEnabled(true);
        } else {
            permission_status_textView.setText("Permission NOT granted!");
        }

    }

    public void createMqttClient() {

        try {
            if (client == null) {
                MqttDefaultFilePersistence mdfp = new MqttDefaultFilePersistence(PATH + "mqttclient");

                final MqttConnectOptions mco = new MqttConnectOptions();
                mco.setCleanSession(true);


                client = new MqttClient("tcp://localhost:1883", "simple_client", mdfp);
                client.setCallback(new MqttCallback() {

                    @Override
                    public void connectionLost(Throwable cause) { //Called when the client lost the connection to the broker
                        Log.d("CONNECTION LOST!:", "TRYING TO RECONNECT! " + cause.getMessage());
                        cause.printStackTrace();
                        client_status_textView.setText("Client disconnected! Reconnecting...");

                        try {
                            client.connect(mco);
                            client_status_textView.setText("Client connected!");
                        } catch (MqttException e) {
                            Log.d("CONNECTION LOST!:", "CANT RECONNECT! " + cause.getMessage());
                            e.printStackTrace();
                            client_status_textView.setText("Client disconnected!");

                        }
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
//                        bb.put(message.getPayload());
                        //1 unsigned long 6 shorts
                        long tstamp;
                        short ax, ay, az, gx, gy, gz;

                        ByteBuffer bb = ByteBuffer.wrap(message.getPayload());
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        tstamp = bb.getInt() & 0xffffffffl;
                        ax = bb.getShort();
                        ay = bb.getShort();
                        az = bb.getShort();
                        gx = bb.getShort();
                        gy = bb.getShort();
                        gz = bb.getShort();
//                        Log.d("MQTT_CALLBACK", topic + ": " + tstamp + "," + ax + "," + ay + "," + az + "," + gx + "," + gy + "," + gz);
                        if (recording)
                            outWriter.append(tstamp + "," + ax + "," + ay + "," + az + "," + gx + "," + gy + "," + gz + "\n");
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {//Called when a outgoing publish is complete
                    }
                });
                client.connect(mco);
                client.subscribe("espdata", 0);
                Log.d(TAG, "Client started!");
                client_status_textView.setText("Client is online!");
                record_button.setEnabled(true);
            }
        } catch (MqttException e) {
            e.printStackTrace();
            client_status_textView.setText("Client can't be created!");
        }

    }

    public void createMqttBroker() {

        try {
            if (!start_mqtt_client_button.isEnabled()) {
                MemoryConfig memoryConfig = new MemoryConfig(new Properties());
                memoryConfig.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, "");
                memoryConfig.setProperty(BrokerConstants.HOST, "localhost");
                server.startServer(memoryConfig);

                Log.d(TAG, "Broker started!");
                broker_status_textView.setText("Broker is online!");
                start_mqtt_client_button.setEnabled(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            broker_status_textView.setText("Broker can't be created!");

        }

    }

    public void createButtonListener() {

        start_broker_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createMqttBroker();
            }
        });

        start_mqtt_client_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createMqttClient();
            }
        });

        request_permission_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST);
            }
        });

        change_skater_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Who's doing them sick flips?");

                final EditText input = new EditText(MainActivity.this);

                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        skater_name = input.getText().toString();
                        skater_name_textView.setText(skater_name);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

        record_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (!recording) {
                    record_button.setText("Stop Recording");

                    int checked_button_id = trick_radio_group.getCheckedRadioButtonId();
                    if (checked_button_id == -1 || checked_button_id == R.id.plain_data_radio_button)
                        trick = "Plain-Data";
                    else
                        trick = ((RadioButton) findViewById(trick_radio_group.getCheckedRadioButtonId())).getText().toString();

                    recording_file = new File(PATH + File.separator + trick);
                    if (!recording_file.exists())
                        recording_file.mkdirs();

                    calobj = Calendar.getInstance();
                    recording_file = new File(PATH + File.separator + trick, skater_name + "-" + df.format(calobj.getTime()) + ".txt");
                    fOut = null;
                    try {
                        recording_file.createNewFile();
                        fOut = new FileOutputStream(recording_file);
                        outWriter = new OutputStreamWriter(fOut);
                        recording = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("RecordCallback", " Can't start recording!");
                    }
                } else {
                    recording = false;

                    try {
                        outWriter.close();
                        fOut.flush();
                        fOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setTitle("");
                    builder.setMessage("Did he land that trick?");

                    builder.setPositiveButton("For sure!", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing because he landed the trick
                            dialog.dismiss();
                        }
                    });

                    builder.setNegativeButton("Nope...", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //rename file to failed
                            calobj = Calendar.getInstance();
                            File failed_trick = new File(PATH + File.separator + trick, skater_name + "-" + df.format(calobj.getTime()) + "_FAILED" + ".txt");
                            recording_file.renameTo(failed_trick);
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();

                    record_button.setText("Start Recording");
                }
            }
        });

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == EXTERNAL_STORAGE_REQUEST) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permission_status_textView.setText("Permission granted!");
                start_broker_button.setEnabled(true);
                Log.d(TAG, "WRITE_EXTERNAL_PEERMISSION has been granted.");
            } else {
                Log.d(TAG, "WRITE_EXTERNAL_PERMISSION has NOT been granted.");
                permission_status_textView.setText("Come on! Grant the permission!");
            }
        }
    }

}
