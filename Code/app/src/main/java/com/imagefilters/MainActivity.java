package com.imagefilters;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

/**
 * Activity to choose image to filter
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Package name
     */
    public static final String PACKAGE_NAME = "com.imagefilters";

    /**
     * Views to interact with buttons
     */
    private Button mDownloadGalleryButton, mDownloadUserUrlButton, nDownloadDefaultUrlButton;

    /**
     * View to interact with entered URL
     */
    private EditText mUrlText;

    /**
     * View to interact with progress bar
     */
    private ProgressBar mProgressBar;

    /**
     * Default URL
     */
    private String DEFAULT_URL;

    /**
     * Permission names in String[] format
     */
    private final String[] READ_PERMISSION = { Manifest.permission.READ_EXTERNAL_STORAGE },
        WRITE_PERMISSION = { Manifest.permission.WRITE_EXTERNAL_STORAGE };

    /**
     * Download ID to check if DownloadManager finished loading our image
     */
    private long mResultCode;

    /**
     * Tag for logging
     */
    private final String TAG = "MainActivity";

    /**
     * Manager that downloads images from URL
     */
    private DownloadManager mDownloadManager;

    /**
     * Callback to pass downloaded image from gallery to FilterImage Activity
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Processing finished download");
            if(mResultCode==intent
                    .getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)) {
                Intent data = new Intent();
                Uri path = mDownloadManager.getUriForDownloadedFile(mResultCode);
                if(path!=null) {
                    data.setData(path);
                    data.setClass(MainActivity.this, FilterImage.class);
                    startActivity(data);
                }
                else {
                    Log.w(TAG, getString(R.string.load_url_error));
                    Toast.makeText(MainActivity.this,
                            getString(R.string.load_url_error),
                            Toast.LENGTH_LONG).show();
                    changeLoadMode(false);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();

        //Getting default URL to use for third load option
        DEFAULT_URL = getString(R.string.default_url);

        //Registering BroadcastReceiver to process finished download from URL
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(mReceiver, filter);

        mDownloadGalleryButton.setOnClickListener(e -> {
            //Checking if we have permission to read storage
            if(hasPermission(this, READ_PERMISSION[0], TAG)) {
                //Start Gallery to choose image
                changeLoadMode(true);
                startGalleryActivity();
            }
            else {
                Log.i(TAG, "Permission denied, asking for permission");
                requestPermissions(this, READ_PERMISSION, TAG);
            }
        });

        mDownloadUserUrlButton.setOnClickListener(e -> {
            // Checking if provided URL is valid
            if(!Patterns.WEB_URL.matcher(mUrlText.getText().toString()).matches()) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.invalid_url), Toast.LENGTH_LONG).show();
            }
            else {
                //Checking if we have permission to write to storage
                if(hasPermission(this, WRITE_PERMISSION[0], TAG)) {
                    Log.i(TAG, "Permission granted, starting download from user URL");

                    //Start DownloadService to download image
                    downloadImageByUrl(Uri.parse(mUrlText.getText().toString()));
                }
                else {
                    Log.i(TAG, "Permission denied, asking for permission");
                    requestPermissions(this, WRITE_PERMISSION, TAG);
                }
            }
        });

        nDownloadDefaultUrlButton.setOnClickListener(e -> {
            //Checking if we have permission to write to storage
            if(hasPermission(this, WRITE_PERMISSION[0], TAG)) {
                Log.i(TAG, "Permission granted, starting download from default URL");

                //Start DownloadService to download image
                downloadImageByUrl(Uri.parse(DEFAULT_URL));
            }
            else {
                Log.i(TAG, "Permission denied, asking for permission");
                requestPermissions(this, WRITE_PERMISSION, TAG);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        changeLoadMode(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    /**
     * Sets up Views by unique ID
     */
    private void setupViews() {
        Log.i(TAG, "Setting up Views to interact with elements");
        mDownloadGalleryButton = findViewById(R.id.select_gallery_button);
        mDownloadUserUrlButton = findViewById(R.id.download_url_user_button);
        nDownloadDefaultUrlButton = findViewById(R.id.download_url_default_button);
        mUrlText = findViewById(R.id.download_url_value);
        mProgressBar = findViewById(R.id.progress_loader);
        setButtonsMode(true);
        setProgressBarMode(false);
    }

    /**
     * Change whether buttons on screen can be pressed
     * @param areEnabled False to block buttons, true to unblock
     */
    private void setButtonsMode(boolean areEnabled) {
        mDownloadGalleryButton.setEnabled(areEnabled);
        mDownloadUserUrlButton.setEnabled(areEnabled);
        nDownloadDefaultUrlButton.setEnabled(areEnabled);
    }

    /**
     * Change whether progress bar can be seen
     * @param isVisible True to show, false to hide
     */
    private void setProgressBarMode(boolean isVisible) {
        int visibilityCode = (isVisible ? ProgressBar.VISIBLE : ProgressBar.INVISIBLE);
        mProgressBar.setVisibility(visibilityCode);
    }

    /**
     * Helper method to change view when image selection or load is enabled
     * @param isLoadInProgress True when image load is started, false when finished
     */
    private void changeLoadMode(boolean isLoadInProgress) {
        setButtonsMode(!isLoadInProgress);
        setProgressBarMode(isLoadInProgress);
    }

    /**
     * Checks if application has specified permission
     * @param context Caller's Context
     * @param permission Permission to check
     * @param logTag Name of caller component
     * @return True if application has given permission
     */
    public static boolean hasPermission(Context context, String permission, String logTag) {
        if(context.getPackageName().equals(MainActivity.PACKAGE_NAME)) {
            Log.i(logTag, "Checking permission for: " + permission);
            int res = ContextCompat.checkSelfPermission(context, permission);
            return res == PackageManager.PERMISSION_GRANTED;
        }
        else {
            Log.w(logTag, context + " has no right to ask permission " + permission);
            return false;
        }
    }

    /**
     * Requests for specified permission
     * @param activity Caller's Activity
     * @param permission Permissions to ask for
     * @param logTag Name of caller component
     */
    public static void requestPermissions(Activity activity, String [] permission,
                                                    String logTag) {
        if(activity.getPackageName().equals(MainActivity.PACKAGE_NAME)) {
            AlertDialog.Builder rationale = new AlertDialog.Builder(activity);
            rationale.setTitle(R.string.rationale_storage_title)
                    .setMessage(R.string.rationale_storage_text)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        Log.i(logTag, "Requesting permission " + Arrays.toString(permission));
                        ActivityCompat.requestPermissions(activity, permission, RESULT_FIRST_USER);
                    }).show();
        }
        else
            Log.w(logTag, activity + " has no right to ask permission "
                    + Arrays.toString(permission));
    }

    /**
     * Starting Gallery to choose image from storage
     */
    private void startGalleryActivity() {
        Log.i(TAG, "Permission " + READ_PERMISSION[0] +
                " granted, starting gallery to choose image");
        changeLoadMode(true);
        Intent in = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getImage.launch(in);
    }

    /**
     * Callback to pass selected image from gallery to FilterImage Activity
     */
    ActivityResultLauncher<Intent> getImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                changeLoadMode(false);
                if(result.getResultCode()==RESULT_OK) {
                    Log.i(TAG, "Passing image URI from gallery to FilterImage Activity");
                    Intent dataToPass = result.getData();
                    assert dataToPass != null;
                    dataToPass.setClass(MainActivity.this, FilterImage.class);
                    startActivity(dataToPass);
                }
                else {
                    Log.w(TAG, getString(R.string.load_gallery_error));
                    Toast.makeText(MainActivity.this,
                            getString(R.string.load_gallery_error),
                            Toast.LENGTH_LONG).show();
                }
            }
    );

    /**
     * Starts download from URL
     * @param source URL from where to download image
     */
    private void downloadImageByUrl(Uri source) {
        //Disabling buttons and showing progress bar
        changeLoadMode(true);

        //Setting up DownloadManager that will do the download
        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        //Setting up filename of result image
        String filename = ((Long)System.currentTimeMillis()).toString() + ".jpg";

        //Formulating request to download
        DownloadManager.Request request =
                new DownloadManager.Request(source);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(filename)
                .setMimeType("image/jpeg")
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                        File.separator + filename)
                .allowScanningByMediaScanner();

        //Starting download
        mResultCode = mDownloadManager.enqueue(request);
    }
}