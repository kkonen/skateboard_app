package de.kkonen.skateboard.skateboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import io.moquette.BrokerConstants;
import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;


public class MainActivity extends AppCompatActivity {

    final Server server = new Server();
    MqttClient client;

    private static final String TAG = "MainActivity";
    public static final int EXTERNAL_STORAGE_REQUEST = 30035;

    Button start_broker_button;
    Button request_permission_button;
    Button start_mqtt_client_button;
    Button test_publish_button;
    TextView permission_status_text;
    TextView broker_status_text;
    TextView client_status_text;
    TextView listener_text;
    String listener_string = "listener text";

    Handler handler;
    Runnable r;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator, "Skateboard");
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
        test_publish_button = findViewById(R.id.test_publish_button);
        permission_status_text = findViewById(R.id.permission_status_text);
        broker_status_text = findViewById(R.id.broker_status_text);
        client_status_text = findViewById(R.id.client_status_text);
        listener_text = findViewById(R.id.listener_text);

        permissionCheck(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        createButtonListener();

        handler = new Handler();
        r = new Runnable() {
            public void run() {
                handler.postDelayed(this, 100);
                listener_text.setText(listener_string);
            }
        };
        handler.postDelayed(r, 0000);

    }

    public void permissionCheck(String permission) {

        if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            permission_status_text.setText("Permission granted!");
            start_broker_button.setEnabled(true);
        } else {
            permission_status_text.setText("Permission NOT granted!");
        }

    }

    public void createMqttClient() {
        try {
            MqttDefaultFilePersistence mdfp = new MqttDefaultFilePersistence(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "Skateboard" + File.separator + "mqttclient");

            client = new MqttClient("tcp://localhost:1883", "simple_client", mdfp);

            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) { //Called when the client lost the connection to the broker
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
//                    Log.d("MQTT_CALLBACK", topic + ": " + Arrays.toString(message.getPayload()));
                    listener_string = Arrays.toString(message.getPayload());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {//Called when a outgoing publish is complete
                }
            });

            client.connect();
            client.subscribe("espdata", 0);

            Log.d(TAG, "Client started!");
            client_status_text.setText("Client is online!");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void createButtonListener() {

        start_broker_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    MemoryConfig memoryConfig = new MemoryConfig(new Properties());
                    memoryConfig.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, "");
                    memoryConfig.setProperty(BrokerConstants.HOST, "localhost");

                    server.startServer(memoryConfig);

                    Log.d(TAG, "Broker started!");
                    broker_status_text.setText("Broker online!");
                    start_mqtt_client_button.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

//         test_publish_button.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                listener_text.setText(listener_string);
//            }
//        });

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == EXTERNAL_STORAGE_REQUEST) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permission_status_text.setText("Permission granted!");
                start_broker_button.setEnabled(true);
                Log.d(TAG, "WRITE_EXTERNAL_PEERMISSION has been granted.");
            } else {
                Log.d(TAG, "WRITE_EXTERNAL_PERMISSION has NOT been granted.");
                permission_status_text.setText("Come on! Grant the permission!");
            }
        }
    }

}
