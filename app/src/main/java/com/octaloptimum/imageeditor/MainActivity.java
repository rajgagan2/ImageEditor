package com.octaloptimum.imageeditor;

import android.Manifest;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int maxPixelCount = 2048;
    private static int REQUEST_PERMISSIONS = 1234;
    private static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static int permissions_count = 2;
    private static int REQUEST_PICK_IMAGE = 12345;
    InputStream input = null;
    OutputStream outputStream;
    private Uri imageUri;
    private ImageView imageView;
    private Button imageInfo, saveImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();


    }

    private void init() {
        final Button selectImageBtn = findViewById(R.id.selectImage);
        imageView = findViewById(R.id.imageView);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity(imageUri)
                        .start(MainActivity.this);
            }
        });

        imageInfo = findViewById(R.id.info);
        imageInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageInfo(imageUri);
            }
        });

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recreate();
            }
        });

        saveImage = findViewById(R.id.saveImage);
        saveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final DialogInterface.OnClickListener dialogInterfaceListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            final File outFile = createImageFile();
                            try (FileOutputStream out = new FileOutputStream(outFile)) {
                                Bitmap bitmap = getContactBitmapFromURI(imageUri);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                imageUri = Uri.parse("file://" + outFile.getAbsolutePath());
                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
                                Toast.makeText(MainActivity.this, "Image was saved", Toast.LENGTH_SHORT).show();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                builder.setMessage("Save to gallery?").
                        setPositiveButton("Yes", dialogInterfaceListener).
                        setNegativeButton("No", dialogInterfaceListener).show();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkPermissions()) {

            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkPermissions()) {
                ((ActivityManager) this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                recreate();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkPermissions() {
        for (int i = 0; i < permissions_count; i++) {
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }


        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                imageUri = result.getUri();
                imageView.setImageURI(imageUri);
//                Toast.makeText(this, "Cropped Image", Toast.LENGTH_SHORT).show();
                findViewById(R.id.welcomeScreen).setVisibility(View.GONE);
                findViewById(R.id.editScreen).setVisibility(View.VISIBLE);


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }


    }

    private void getImageInfo(Uri imageUri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(this.imageUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

                /*
                ExifInterface (FileDescriptor fileDescriptor) added in API level 24
                 */
            ExifInterface exifInterface = new ExifInterface(fileDescriptor);
            String exif = "Exif: " + fileDescriptor.toString();
            exif += "\nIMAGE_LENGTH: " +
                    exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            exif += "\nIMAGE_WIDTH: " +
                    exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            exif += "\n DATETIME: " +
                    exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            exif += "\n TAG_MAKE: " +
                    exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            exif += "\n TAG_MODEL: " +
                    exifInterface.getAttribute(ExifInterface.TAG_MODEL);
            exif += "\n TAG_ORIENTATION: " +
                    exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            exif += "\n TAG_WHITE_BALANCE: " +
                    exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
            exif += "\n TAG_FOCAL_LENGTH: " +
                    exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            exif += "\n TAG_FLASH: " +
                    exifInterface.getAttribute(ExifInterface.TAG_FLASH);
            exif += "\nGPS related:";
            exif += "\n TAG_GPS_DATESTAMP: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
            exif += "\n TAG_GPS_TIMESTAMP: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
            exif += "\n TAG_GPS_LATITUDE: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            exif += "\n TAG_GPS_LATITUDE_REF: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            exif += "\n TAG_GPS_LONGITUDE: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            exif += "\n TAG_GPS_LONGITUDE_REF: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            exif += "\n TAG_GPS_PROCESSING_METHOD: " +
                    exifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);

            parcelFileDescriptor.close();

            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Exif Information")
                    .setMessage(exif)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            Log.i("Hey", " " + exifInterface);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    "Something wrong:\n" + e.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    "Something wrong:\n" + e.toString(),
                    Toast.LENGTH_LONG).show();
        }

        String strPhotoPath = this.imageUri.getPath();

    }

    public Bitmap getContactBitmapFromURI(Uri uri) {
        try {

            InputStream input = MainActivity.this.getContentResolver().openInputStream(uri);
            if (input == null) {
                return null;
            }
            return BitmapFactory.decodeStream(input);
        } catch (FileNotFoundException e) {

        }
        return null;

    }

//    public void saveBitmapIntoSDCardImage(Uri uri) throws IOException {
//
//        final Bitmap bitmap = getContactBitmapFromURI(MainActivity.this, uri);
//
//
//        File filepath = Environment.getDataDirectory();
//        File dir = new File(filepath.getAbsoluteFile() + "/Img/");
//        dir.mkdir();
//        File file = new File(dir, System.currentTimeMillis() + ".jpg");
//        try {
//            outputStream = new FileOutputStream(file);
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        Toast.makeText(getApplicationContext(), "Image Saved to internal !!", Toast.LENGTH_SHORT).show();
//        outputStream.flush();
//        outputStream.close();
//
//
//    }

    private File createImageFile() {
        final String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName = "/JPEG_" + timestamp + ".jpg";
        final File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir + imageFileName);

    }
}



