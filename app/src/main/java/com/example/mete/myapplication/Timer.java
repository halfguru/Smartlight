package com.example.mete.myapplication;

import android.content.Intent;
import android.hardware.Sensor;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Timer extends AppCompatActivity {
    // The TransferUtility is the primary class for managing transfer to S3
    private TransferUtility transferUtility;
    private AWSConfiguration sAWSConfiguration;
    public final static String EXTRA_MESSAGE = "com.example.mete.MESSAGE";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        transferUtility = Util.getTransferUtility(this);
    }

    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {

        Intent intent = new Intent(this, NavigationDrawer.class);
        EditText editText = (EditText) findViewById(R.id.wakeText);
        EditText editText2 = (EditText) findViewById(R.id.sleepText);
        EditText editText3 = (EditText) findViewById(R.id.workstartText);
        EditText editText4 = (EditText) findViewById(R.id.workstopText);
        String message = editText.getText().toString();
        String message2 = editText2.getText().toString();
        String message3 = editText3.getText().toString();
        String message4 = editText4.getText().toString();
        Log.e("TAG", "Sending to Amazon S3");
        writeToFile("Wake up time:  "  + message);
        writeToFile("Sleeping time:  "  + message2);
        writeToFile("Work start time:  "  + message3);
        writeToFile("Work end time:  "  + message4);
        startActivity(intent);
    }

    public void writeToFile(String data) {
        // Get the directory for the user's public pictures directory.
        String path =
                Environment.getExternalStorageDirectory() + File.separator  + "Accelerometer Data";
        // Create the folder.
        File folder = new File(path);
        folder.mkdirs();

        // Create the file.
        File file = new File(folder, "timer.txt");

        try{
            if(!file.exists()){
                Log.e("TAG","We had to make a new file.");
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(data + "\n");
            bufferedWriter.close();

            Log.e("TAG", "Writing to file");
        } catch(IOException e) {
            Log.e("TAG", "Couldn't log to file");
        }

        //Upload file to S3 user cloud
        TransferObserver observer = transferUtility.upload(sAWSConfiguration.AMAZON_S3_USER_FILES_BUCKET, file.getName(), file);

    }
}
