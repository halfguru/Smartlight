package com.example.mete.myapplication;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DisplayTimer extends AppCompatActivity {
    private TransferUtility transferUtility;
    private AWSConfiguration sAWSConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_timer);
        transferUtility = Util.getTransferUtility(this);
        downloadFile();
    }

    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {

        Intent intent = new Intent(this, SensortagActivity.class);
        Log.e("TAG", "Go back to sensortag activity");
        startActivity(intent);
    }

    public void downloadFile() {
        String path = Environment.getExternalStorageDirectory() + File.separator + "Accelerometer Data";
        File folder = new File(path);
        folder.mkdirs();
        File file = new File(folder, "timer.txt");
        try{
            if(!file.exists()){
                Log.e("TAG","We had to make a new file.");
                file.createNewFile();
            }

            Log.e("TAG", "Downloading file");
        } catch(IOException e) {
            Log.e("TAG", "Couldn't download to file");
        }
        //TransferObserver observer = transferUtility.download(sAWSConfiguration.AMAZON_S3_USER_FILES_BUCKET, file.getName(), file);

        //Read text from file
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
        }
        //Find the view by its id
        TextView tv = (TextView)findViewById(R.id.DisplayTimer);

        //Set the text
        tv.setText(text);



        }

    }
