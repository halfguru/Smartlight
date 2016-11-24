package com.example.mete.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Constants {

    /*
     * You should replace these values with your own. See the README for details
     * on what to fill in.
     */
    public static final String COGNITO_POOL_ID = "us-east-1:0d570053-f3d4-450a-acea-3768250892e7";

    /*
     * Note, you must first create a bucket using the S3 console before running
     * the sample (https://console.aws.amazon.com/s3/). After creating a bucket,
     * put it's name in the field below.
     */
    public static final String BUCKET_NAME = "ecseproject-userfiles-mobilehub-1616544663";
}

/***
public class AWSConfiguration {

    // AWS MobileHub user agent string
    public static final String AWS_MOBILEHUB_USER_AGENT =
            "MobileHub 1630b360-2d64-4f8b-b091-63970f8726c3 aws-my-sample-app-android-v0.10";
    // AMAZON COGNITO
    public static final Regions AMAZON_COGNITO_REGION =
            Regions.fromName("us-east-1");
    public static final String  AMAZON_COGNITO_IDENTITY_POOL_ID =
            "us-east-1:0d570053-f3d4-450a-acea-3768250892e7";
    // S3 BUCKET
    public static final String AMAZON_S3_USER_FILES_BUCKET =
            "ecseproject-userfiles-mobilehub-1616544663";
    // S3 BUCKET REGION
    public static final Regions AMAZON_S3_USER_FILES_BUCKET_REGION =
            Regions.fromName("us-east-1");
    public static final String AMAZON_COGNITO_USER_POOL_ID =
            "us-east-1_bJOiakccz";
    public static final String AMAZON_COGNITO_USER_POOL_CLIENT_ID =
            "60n8ebs26hc5s00gjsde9hgkn7";
    public static final String AMAZON_COGNITO_USER_POOL_CLIENT_SECRET =
            "1gr4t07jhk8ql1an0in9aa8q04m4ribb7d79vlee31uvog1ki0dr";
}
 ***/