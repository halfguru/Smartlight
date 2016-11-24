package com.example.mete.myapplication;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.fromString;

public class BLEService extends Service {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt sensorTagGatt;
    private LocalBroadcastManager localBroadcastManager;

    public final static UUID

            UUID_DEVINFO_SERV = fromString("0000180a-0000-1000-8000-00805f9b34fb"),
            UUID_DEVINFO_FWREV = fromString("00002A26-0000-1000-8000-00805f9b34fb"),

    UUID_IRT_SERV = fromString("f000aa00-0451-4000-b000-000000000000"),
            UUID_IRT_DATA = fromString("f000aa01-0451-4000-b000-000000000000"),
            UUID_IRT_CONF = fromString("f000aa02-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_IRT_PERI = fromString("f000aa03-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_ACC_SERV = fromString("f000aa10-0451-4000-b000-000000000000"),
            UUID_ACC_DATA = fromString("f000aa11-0451-4000-b000-000000000000"),
            UUID_ACC_CONF = fromString("f000aa12-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_ACC_PERI = fromString("f000aa13-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_HUM_SERV = fromString("f000aa20-0451-4000-b000-000000000000"),
            UUID_HUM_DATA = fromString("f000aa21-0451-4000-b000-000000000000"),
            UUID_HUM_CONF = fromString("f000aa22-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_HUM_PERI = fromString("f000aa23-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_MAG_SERV = fromString("f000aa30-0451-4000-b000-000000000000"),
            UUID_MAG_DATA = fromString("f000aa31-0451-4000-b000-000000000000"),
            UUID_MAG_CONF = fromString("f000aa32-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_MAG_PERI = fromString("f000aa33-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_OPT_SERV = fromString("f000aa70-0451-4000-b000-000000000000"),
            UUID_OPT_DATA = fromString("f000aa71-0451-4000-b000-000000000000"),
            UUID_OPT_CONF = fromString("f000aa72-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_OPT_PERI = fromString("f000aa73-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_BAR_SERV = fromString("f000aa40-0451-4000-b000-000000000000"),
            UUID_BAR_DATA = fromString("f000aa41-0451-4000-b000-000000000000"),
            UUID_BAR_CONF = fromString("f000aa42-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_BAR_CALI = fromString("f000aa43-0451-4000-b000-000000000000"), // Calibration characteristic
            UUID_BAR_PERI = fromString("f000aa44-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_GYR_SERV = fromString("f000aa50-0451-4000-b000-000000000000"),
            UUID_GYR_DATA = fromString("f000aa51-0451-4000-b000-000000000000"),
            UUID_GYR_CONF = fromString("f000aa52-0451-4000-b000-000000000000"), // 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z
            UUID_GYR_PERI = fromString("f000aa53-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_MOV_SERV = fromString("f000aa80-0451-4000-b000-000000000000"),
            UUID_MOV_DATA = fromString("f000aa81-0451-4000-b000-000000000000"),
                UUID_MOV_NOTIF = fromString("00002902-0000-1000-8000-00805f9b34fb"),    //notif. Took forever to find this
            UUID_MOV_CONF = fromString("f000aa82-0451-4000-b000-000000000000"), // 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z
            UUID_MOV_PERI = fromString("f000aa83-0451-4000-b000-000000000000"), // Period in tens of milliseconds

    UUID_TST_SERV = fromString("f000aa64-0451-4000-b000-000000000000"),
            UUID_TST_DATA = fromString("f000aa65-0451-4000-b000-000000000000"), // Test result

    UUID_KEY_SERV = fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
            UUID_KEY_DATA = fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    /** interface for clients that bind */
    private final IBinder serviceBinder = new BLEServBinder();

    /** indicates whether onRebind should be used */
    private final boolean mAllowRebind = true;

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
        super.onCreate();
        //Initialize bluetooth manager
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    /** A client is binding to the service with bindService() */
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    /** Called when The service is no longer used and is being destroyed */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
     * The function below scans for bluetooth LE devices. The snippet provides support using both
     * the deprecated and updated APIs depending on the SDK build version
     *
     * @param {Boolean} enable : Indicates whether scanning should be enabled or not
     * @return void
     */
    private void scanLeDevice(final boolean enable) {
        Log.e("scanLeDevice", "scanLeDevice");
        if (enable) {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }
    /*
     * The function below is the callback function for scanning devices for the updated API.
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.e("onScanResult", "onScanResult");
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            String btDeviceName = btDevice.getName();

            Log.d("BLE Device Found.", "Adding to list");
            Intent addDeviceToList = new Intent(MainActivity.BLE_DEV_FOUND);
            addDeviceToList.putExtra("DeviceName", btDeviceName);
            addDeviceToList.putExtra("Device", btDevice);

            localBroadcastManager.sendBroadcast(addDeviceToList);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.e("onBatchScanResults", "onBatchScanResults");
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };
    /*
     * The function below implements the callback using the deprecated API
     *
     * @return void
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.e("onLeScan", "onLeScan");

            String btDeviceName = device.getName();

            Log.d("BLE Device Found.", "Adding to list");
            Intent addDeviceToList = new Intent(MainActivity.BLE_DEV_FOUND);
            addDeviceToList.putExtra("DeviceName", btDeviceName);
            addDeviceToList.putExtra("Device", device);

            localBroadcastManager.sendBroadcast(addDeviceToList);
        }
    };

    /** Function calls accessible by Activity */
    public void runBLEScan(Boolean run){
        if (run) {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
                filters = new ArrayList<>();
            }
        }
        this.scanLeDevice(run);
    }

    public BluetoothGatt connectTo(BluetoothDevice device){
        sensorTagGatt = device.connectGatt(this, false, gattCallback);
        return sensorTagGatt;
    }

    public void disconectDevice(){
        sensorTagGatt.disconnect();
        sensorTagGatt.close();
        sensorTagGatt = null;
    }

    /*
     * The function implements the bluetooth gatt callback for the connectTo function.
     *
     * @return void
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e("onConnectionStateChange", "onConnectionStateChange");
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e("onServicesDiscovered", "onServicesDiscovered");
            //Now that we have discovered services, we want to get the accel service
            BluetoothGattService movService = gatt.getService(UUID_MOV_SERV);
            Log.e("FOUND SERVICE", "Service details: " + movService.toString());
            //Now that we have the movement service, we want to enable and use the accelerometer

            BluetoothGattCharacteristic enable = movService.getCharacteristic(UUID_MOV_CONF);

            byte[] enableAccels = {-72, 0b00000001};  //enable 3 axes and "wake-on-motion", and set range to 2   0b10111000 -> -72. Need to do this because Java....
            enable.setValue(enableAccels);

            gatt.writeCharacteristic(enable);   //sensor should be enabled, and values should be reading. Once done, set the period
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            Log.e("onCharacteristicChanged", "onCharacteristicChanged");
            Log.i("onCharacteristicChanged", characteristic.toString());
            //Should not disconnect Gatt. Want consistent values
            if (characteristic.getUuid().equals(UUID_MOV_DATA)){
                byte[] movValue = characteristic.getValue();
                ByteBuffer movValueResults = ByteBuffer.wrap(movValue); //stored as big endian.
                int rawX = movValueResults.getInt(6);   //retrieves 6&7
                int rawY = movValueResults.getInt(8);   //retrieves 8&9
                int rawZ = movValueResults.getInt(10);   //retrieves 10&11
                //Since we hardcoded the accuracy to 4G, conversion factor is as follows:
                float xInGs = rawX  / (32768/4);
                float yInGs = rawY  / (32768/4);
                float zInGs = rawZ  / (32768/4);

                Intent sendAccelValues = new Intent(SensortagActivity.SENSOR_TAG_ACCEL);
                sendAccelValues.putExtra("x", xInGs);
                sendAccelValues.putExtra("y", yInGs);
                sendAccelValues.putExtra("z", zInGs);

                localBroadcastManager.sendBroadcast(sendAccelValues);

            } else {
                Log.e("onCharacteristicRead", "Got UUID that's not UUID_ACC_DATA. UUID: " + characteristic.getUuid());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            Log.e("onCharacteristicWrite", "onCharacteristicWrite");
            Log.i("onCharacteristicWrite", characteristic.toString());

            UUID characUuid = characteristic.getUuid();
            BluetoothGattService movService = gatt.getService(UUID_MOV_SERV);
            if (characUuid.equals(UUID_MOV_CONF)){
                BluetoothGattCharacteristic period = movService.getCharacteristic(UUID_MOV_PERI);
                byte[] periodInTensOfMs = {50}; //500 milliseconds
                period.setValue(periodInTensOfMs);
                gatt.writeCharacteristic(period);   //sensor should be configured to return results every 500 ms

            } else if (characUuid.equals(UUID_MOV_PERI)) {
                BluetoothGattCharacteristic data = movService.getCharacteristic(UUID_MOV_DATA);
                gatt.setCharacteristicNotification(data, true);     //enable notifications locally
                BluetoothGattDescriptor notif = data.getDescriptor(UUID_MOV_NOTIF);
                byte[] enableNotif = {0x01, 0x00};
                notif.setValue(enableNotif);
                gatt.writeDescriptor(notif);     //enable notifications
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            Log.e("onDescriptorWrite", "onDescriptorWrite");
            Log.i("onDescriptorWrite", descriptor.toString());
        }
    };

    /** Getters for the activity to perform appropriate checks */
    public BluetoothAdapter getBluetoothAdapter(){
        return this.mBluetoothAdapter;
    }
    public BluetoothGatt getBluetoothGatt(){
        return this.sensorTagGatt;
    }
    //Below class definition helps in retrieving instance of the pedometer service in main activity
    //It is used to help acquire access to service methods (namely the counters) to view steps
    public class BLEServBinder extends Binder {
        BLEService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BLEService.this;
        }
    }
}
