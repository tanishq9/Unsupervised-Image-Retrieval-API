package com.boss.ksimilar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_CODE = 101;
    private static final int IMAGE_PICK_CODE = 100;
    private static final int RESET_VIEWS = 102;
    private static final String IP = "http://192.168.1.4:3000/file_upload";

    ImageView imageView;
    Button button;
    ProgressDialog progressDialog;

    Boolean userSelectedImage = false;
    private static final String TAG = "MainActivity";
    private String image_path = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        clickListeners();
    }

    private void clickListeners() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, String.valueOf(userSelectedImage));
                if (userSelectedImage) {
                    getKSimilar();
                } else {
                    Toast.makeText(MainActivity.this, "No grayscale Image Selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("TAG", String.valueOf(userSelectedImage));
                //check runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        //permission not granted, requesting permission
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        //show popup for runtime permission
                        requestPermissions(permissions, PERMISSION_CODE);
                    } else {
                        //permission already granted
                        uploadImage();
                    }
                } else {
                    // system os is less than marshmallow
                    uploadImage();
                }
            }
        });
    }

    private void uploadImage() {
        //intent to upload image
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    private void getKSimilar() {
        Log.e(TAG, "Inside similar category");
        File file = new File(image_path);
        final String file_path = file.getAbsolutePath();
        // extension can be .png/.PNG/.jpg/.JPG/.jpeg/.JPEG
        String extension = file_path.substring(file_path.length() - 3);
        Log.e(TAG, extension);
        if (extension.equals("png") || extension.equals("PNG")) {
            makeRequest("png");
        } else if (extension.equals("jpg") || extension.equals("JPG")) {
            makeRequest("jpg");
        } else if (extension.equals("peg") || extension.equals("PEG")) {
            makeRequest("jpeg");
        } else {
            Toast.makeText(MainActivity.this, "Incorrect File Format", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeRequest(String extension) {
        Log.e(TAG, "Inside make request");

        // Disable the button for multiple requests

        button.setClickable(false);
        button.setAlpha(.5f);
        imageView.setClickable(false);

        // make a loader on the main thread
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Analyzing");
        progressDialog.setCancelable(false);
        progressDialog.show();

        File file = new File(image_path);
        final String file_path = file.getAbsolutePath();

        RequestBody file_body = RequestBody.create(MediaType.parse("image/" + extension), file);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(1, TimeUnit.MINUTES) // connect timeout
                .writeTimeout(1, TimeUnit.MINUTES) // write timeout
                .readTimeout(1, TimeUnit.MINUTES); // read timeout

        OkHttpClient client = builder.build();

        RequestBody request_body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file_path.substring(file_path.lastIndexOf("/") + 1), file_body)
                .build();

        Request request = new Request.Builder()
                .url(IP)
                .post(request_body)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                // Hide Progress dialog
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Internet/Input Problem", Toast.LENGTH_LONG).show();
                        resetViews();
                    }
                });
                Log.e(TAG, "ERROR:" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // Hide Progress dialog
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setMessage("DONE");
                        progressDialog.dismiss();
                        String jsonResponse = null;
                        try {
                            jsonResponse = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.e(TAG, "RESPONSE:" + jsonResponse);

                        // check if JSON Response is valid
                        if (jsonResponse.toString().contains("MNIST")) {
                            Toast.makeText(MainActivity.this, "Only Grayscale Images are allowed", Toast.LENGTH_SHORT).show();
                            resetViews();
                        } else {
                            launchResultActivity(jsonResponse);
                        }
                    }
                });
            }

        });
    }

    private void launchResultActivity(String jsonResponse) {
        // start new activity and send JSON to this activity
        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        intent.putExtra("response", jsonResponse);
        // we will reset the imageView in OnActivityResult
        startActivityForResult(intent, RESET_VIEWS);
    }

    //handling request of runtime permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    uploadImage();
                } else {
                    //permission denied
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // handling result of pick image and going back from result activity to main activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case IMAGE_PICK_CODE: {
                    // Log.e(TAG, data.getData().getPath().split(":")[1]); -- this log statement is producing a bug, so commenting out

                    userSelectedImage = true;
                    Uri selectedImage = data.getData();

                    try {
                        // A ParcelFileDescriptor is used here to open the selected file.
                        // It will work fine regardless of any Android OS API level
                        ParcelFileDescriptor parcelfileDescriptor = this.getContentResolver().openFileDescriptor(selectedImage, "r");
                        FileDescriptor fileDescriptor = parcelfileDescriptor.getFileDescriptor();

                        // FileInputStream and InputStreamChannel are created for incoming data from the ParcelFileDescriptor
                        FileInputStream inputStream = new FileInputStream(fileDescriptor);
                        FileChannel inputStreamChannel = inputStream.getChannel();

                        // Image file is given a name
                        String nameOfFile = "copyImageFile.jpg";

                        // A empty copy of the original selected file is created here
                        File copyImageFile = new File(getCacheDir(), nameOfFile);

                        // FileOutputStream and OutputStreamChannel are created for outgoing data to the copied file
                        FileOutputStream outputStream = new FileOutputStream(copyImageFile);
                        FileChannel outputStreamChannel = outputStream.getChannel();

                        // The data is transferred to the copied file
                        inputStreamChannel.transferTo(0, inputStreamChannel.size(), outputStreamChannel);

                        inputStream.close();
                        outputStream.close();

                        // Here we have obtained the copied file Uri
                        Uri copyImage = Uri.fromFile(copyImageFile);
                        imageView.setImageURI(copyImage);

                        image_path = copyImage.getPath();
                        Log.e(TAG, "PIC PATH :" + image_path);
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                case RESET_VIEWS: {
                    // reset the boolean variable and imageView
                    Log.e(TAG, "Back Inside Main Activity");
                    resetViews();
                    break;
                }
            }

        }
    }

    private void resetViews() {
        userSelectedImage = false;
        // reset the upload button
        imageView.setImageResource(R.drawable.ic_file_upload_black_24dp);
        button.setClickable(true);
        button.setAlpha(1f);
        // reset the imageView
        imageView.setClickable(true);
    }


    private void bindViews() {
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button);
    }


}
