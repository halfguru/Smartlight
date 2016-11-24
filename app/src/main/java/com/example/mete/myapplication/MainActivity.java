package com.example.mete.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.widget.AdapterView;
import android.util.Log;
import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * The class below implements a simple activity to allow the user to scan for bluetooth LE devices,
 * and connect to them via clicks. It then enables simple acquisition of the device's GATT
 *
 * @author Mete Aykul
 */
public class MainActivity extends AppCompatActivity{
    private Intent bleServiceIntent;
    private boolean scanningForDevices, bleServiceConnected;
    private Map<String, BluetoothDevice> mapOfNameToDevice;
    BLEService bleService;

    private Button startScanButton;
    private ListView listViewOfDevices;
    private List<BluetoothDevice> pairedDevices;
    private ArrayList<String> mLeDevices;
    private ArrayAdapter<String> devicesListAdapter;

    private int REQUEST_ENABLE_BT = 1;
    private BluetoothGatt mGatt;

    private static final String SENSOR_TAG_NAME_1 = "SensorTag2";
    private static final String SENSOR_TAG_NAME_2 = "CC2650 SensorTag";

    /** Broadcast receiver used to process and handle messages coming from service */
    //String to which it will respond
    public static final String BLE_DEV_FOUND = "Add new BLE Device to list";

    // for bluetooth to work on SDK 23
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BLE_DEV_FOUND)){
                Bundle btDeviceInfo = intent.getExtras();
                String btDeviceName = (String)btDeviceInfo.get("DeviceName");
                BluetoothDevice btDevice = (BluetoothDevice)(btDeviceInfo.get("Device"));
                if (btDeviceName == null || btDeviceName.isEmpty()){
                    Log.d("TAG", "Bluetooth Device name is null");
                    btDeviceName = btDevice.getAddress();
                }

                if (mapOfNameToDevice.get(btDeviceName) == null){
                    mapOfNameToDevice.put(btDeviceName, btDevice);
                    mLeDevices.add(btDeviceName);
                    devicesListAdapter.notifyDataSetChanged();
                } else if (mapOfNameToDevice.get(btDeviceName) != null){
                    Log.e("TAG", "Device already in map. Not adding to list");
                }
            }
        }
    };

    LocalBroadcastManager bManager;

    /*
     * The function below implements the onCreate callback for the activity
     *
     * @param {Bundle} savedInstanceState
     * @return void
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e("onCreate", "onCreate");


        //SDK 23
        verifyStoragePermissions(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
            builder.show();
        }
        }

    //Check if BLE supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e("BLE NOT SUPPORTED", "BLE NOT SUPPORTED");
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Log.e("BLE Status", "BLE SUPPORTED");
        }

        /** Setup broadcast receiver */
        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_DEV_FOUND);
        bManager.registerReceiver(bReceiver, intentFilter);

        /** Start BLE service */
        bleServiceIntent = new Intent(this, BLEService.class);   //this should never be re-assigned
        startService(bleServiceIntent);
        bindService(bleServiceIntent, bleServiceConnection, Context.BIND_AUTO_CREATE);

        //Initialize scanning button
        startScanButton = (Button) findViewById(R.id.start_scan);
        startScanButton.setText("Click to Start Scanning");
        scanningForDevices = false;

        //Initialize device list
        mLeDevices = new ArrayList<>();
        devicesListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mLeDevices);
        mapOfNameToDevice = new HashMap<>();    //used to keep track of name -> bluetooth device

        //Initialize listview item clicks to enable connection
        listViewOfDevices = (ListView)findViewById(R.id.listview_devices);
        listViewOfDevices.setAdapter(devicesListAdapter);   //attach adapter
        listViewOfDevices.setClickable(true);
        listViewOfDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String deviceName = (String)parent.getItemAtPosition(position);
                BluetoothDevice device = mapOfNameToDevice.get(deviceName);
                //BluetoothDevice device = (BluetoothDevice)((ListView)parent).getItemAtPosition(position);
                connectToDevice(device, deviceName);    //connects to item when it is clicked
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("coarse location", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    /*
     * The function below implements the onResume callback for the activity
     *
     * @return void
     */
    @Override
    protected void onResume() {
        if (!bleServiceConnected){
            bindService(bleServiceIntent, bleServiceConnection, 0);
        }
        super.onResume();
        Log.e("onResume", "onResume");
    }
    /*
     * The function below implements the onPause callback for the activity. It pauses device scanning
     *
     * @return void
     */
    @Override
    protected void onPause() {
        super.onPause();
        //Clear list and map
        mapOfNameToDevice.clear();
        mLeDevices.clear();
        devicesListAdapter.notifyDataSetChanged();

        //unbind from service
        if (bleServiceConnected) {
            BluetoothAdapter bleAdapter = bleService.getBluetoothAdapter();
            if (bleAdapter != null && bleAdapter.isEnabled()) {
                bleService.runBLEScan(false);
            }
            unbindService(bleServiceConnection);
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
    protected void onDestroy() {
        super.onDestroy();
        Log.e("onDestroy", "onDestroy");
        if (bleService.getBluetoothGatt() != null) {
            bleService.disconectDevice();
        }
        bManager.unregisterReceiver(bReceiver);
        mGatt = null;
        stopService(bleServiceIntent);
    }
    /*
     * The function below implements the onActivityResult callback for the activity. It requests
     * the user to enable bluetooth when isn't already done so. It exits when the user doesn't
     * acknowledge the request
     *
     * @param {int} requestCode
     * @param{int} resultCode
     * @param{Intent} data
     *
     * @return void
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("onActivityResult", "onActivityResult");
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

            mapOfNameToDevice.clear();
            mLeDevices.clear();
            devicesListAdapter.notifyDataSetChanged();
        }
    };

    public void onButtonClick(View v){
        if (bleServiceConnected) {
            switch (v.getId()){
                case (R.id.start_scan):
                    //scan button was pressed. Check if already scanning or not

                    if (scanningForDevices){    //we're currently scanning. So stop scanning
                        bleService.runBLEScan(false);
                        startScanButton.setText("Click to Start Scanning");
                    } else if (!scanningForDevices){    //not scanning. Enable and start
                        BluetoothAdapter bleAdapter = bleService.getBluetoothAdapter();
                        //Check if bluetooth is enabled. If not, request user to enable
                        if (bleAdapter == null || !bleAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else {
                            bleService.runBLEScan(true);
                        }
                        startScanButton.setText("Click to Stop Scanning");
                    }
                    scanningForDevices = !scanningForDevices;   //flip boolean/state
                    break;
                default:
                    break;
            }
        } else {
            Toast.makeText(this, "Error: Service not connected", Toast.LENGTH_SHORT).show();
        }
    }
    /*
     * The function below connects to the selected device. Scanning is explicitly stopped when the
     * selected device is connected.
     *
     * @param {BluetoothDevice} device : The bluetooth device that is to be connected to
     * @return void
     */
    public void connectToDevice(BluetoothDevice device, String deviceName) {
        Log.e("connectToDevice", "connectToDevice");
        if (bleService.getBluetoothGatt() == null) {
            mGatt = bleService.connectTo(device);
            //Check if scanning. If so, stop it
            if (scanningForDevices){
                scanningForDevices = !scanningForDevices;
                bleService.runBLEScan(false);     //stop after connect to gatt
                startScanButton.setText("Click to Start Scanning");
            }
        }
        if (deviceName.equals(SENSOR_TAG_NAME_1) || deviceName.equals(SENSOR_TAG_NAME_2)){
            /** Start activity for displaying SensorTag information */
            Intent moveToMainActivity = new Intent(this, SensortagActivity.class);
            moveToMainActivity.putExtra("SensorTag", device);
            startActivity(moveToMainActivity);
        }
    }
}
