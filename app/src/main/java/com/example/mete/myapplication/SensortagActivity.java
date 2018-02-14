package com.example.mete.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobile.AWSConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SensortagActivity extends Fragment {
    private TextView xReading, yReading, zReading;
    private Button btnUpload, btnSensor, btnTimer;
    private boolean bleServiceConnected;
    private boolean sensorToCloud;
    private Intent bleServiceIntent;
    BLEService bleService;
    public static final String SENSOR_TAG_ACCEL = "Accelerometer Values Received";
    private String[] accToFile = new String[3];
    private String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    // The TransferUtility is the primary class for managing transfer to S3
    private TransferUtility transferUtility;
    private AWSConfiguration sAWSConfiguration;
    LocalBroadcastManager bManagerSensortag;
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SENSOR_TAG_ACCEL)){
                Bundle accelInfo = intent.getExtras();
                final float x = (float)accelInfo.get("x");
                final float y = (float)accelInfo.get("y");
                final float z = (float)accelInfo.get("z");

                getActivity().runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        xReading.setText(x + "Gs");
                        yReading.setText(y + "Gs");
                        zReading.setText(z + "Gs");
                            accToFile[0] = "x: " + Float.toString(x);
                            accToFile[1] = "y: " + Float.toString(y);
                            accToFile[2] = "z: " + Float.toString(z);
                            if (sensorToCloud == true){
                            Log.e("TAG", "Sending to Amazon S3");
                            for( int i = 0; i <= accToFile.length - 1; i++){
                                writeToFile(accToFile[i]);
                        }

                        }
                    }
                });
            }
        }
    };
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Initializes TransferUtility, always do this before using it.
        transferUtility = Util.getTransferUtility(getActivity());
        sensorToCloud = false;
        View rootView = inflater.inflate(R.layout.sensortag_activity, container, false);

        Thread displayUpdateThread = getUpdateStepDisplayThread();
        displayUpdateThread.start();
        bleServiceIntent = new Intent(getActivity(), BLEService.class);   //this should never be re-assigned
        getActivity().bindService(bleServiceIntent, bleServiceConnection, Context.BIND_AUTO_CREATE);
        bManagerSensortag = LocalBroadcastManager.getInstance(getActivity());


        return rootView;

    }

    public void onViewCreated(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        xReading = (TextView)getView().findViewById(R.id.xReading);
        yReading = (TextView)getView().findViewById(R.id.yReading);
        zReading = (TextView)getView().findViewById(R.id.zReading);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SENSOR_TAG_ACCEL);
        bManagerSensortag.registerReceiver(bReceiver, intentFilter);


    }
    /*
     * The function below implements the onResume callback for the activity
     *
     * @return void
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().bindService(bleServiceIntent, bleServiceConnection, 0);
        Log.e("onResume", "onResume");
    }
    /*
     * The function below implements the onPause callback for the activity. It pauses device scanning
     *
     * @return void
     */
    @Override
    public void onPause() {
        super.onPause();
        //unbind from service
        if (bleServiceConnected) {
            BluetoothAdapter bleAdapter = bleService.getBluetoothAdapter();
            if (bleAdapter != null && bleAdapter.isEnabled()) {
                bleService.runBLEScan(false);
            }
            getActivity().unbindService(bleServiceConnection);
            bleServiceConnected = false;
        }
    }
    /*
     * The function below implements the onDestroy callback for the activity. It properly handles
     * program exits
     *
     * @return void
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("onDestroy", "onDestroy");
        if (bleService.getBluetoothGatt() != null) {
            bleService.disconectDevice();
        }
        bManagerSensortag.unregisterReceiver(bReceiver);
        getActivity().stopService(bleServiceIntent);
    }

    protected ServiceConnection bleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
//            Log.d(LOG_TAG, "onServiceConnected");

            // We've bound to pedometerService, cast the IBinder and get the instance of it
            bleService = ((BLEService.BLEServBinder) service).getService();
            bleServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
//            Log.d(LOG_TAG, "onServiceDisconnected");
            bleServiceConnected = false;
        }
    };

    public void onDisconnectClick(View v){
        bleService.disconectDevice();
        Intent moveToMainActivity = new Intent(getActivity(), MainActivity.class);
        startActivity(moveToMainActivity);
    }

    public void writeToFile(String data) {
        // Get the directory for the user's public pictures directory.
        String path =
                Environment.getExternalStorageDirectory() + File.separator  + "Accelerometer Data";
        // Create the folder.
        File folder = new File(path);
        folder.mkdirs();

        // Create the file.
        File file = new File(folder, "config.txt");

        //Upload file to S3 user cloud
        TransferObserver observer = transferUtility.upload(sAWSConfiguration.AMAZON_S3_USER_FILES_BUCKET, file.getName(), file);
        try{
            if(!file.exists()){
                Log.e("TAG","We had to make a new file.");
                file.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(timeStamp.toString() + " " + data + "\n");
            bufferedWriter.close();

            Log.e("TAG", "Writing to file");
        } catch(IOException e) {
            Log.e("TAG", "Couldn't log to file");
        }

        /*** Save your stream, don't forget to flush() it before closing it.
        try {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
         ***/
    }

    private Thread getUpdateStepDisplayThread(){
        Thread updateDisplay = new Thread(){
            @Override
            public void run(){
                try {
                    while (!isInterrupted()){
                        Thread.sleep(250);
                        getActivity().runOnUiThread(new Runnable(){
                            @Override
                            public void run(){
                                //Include logic to set the fields
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.e("TAG", "Error: Thread interrupted");
                }
            }
        };

        return updateDisplay;
    }
}
