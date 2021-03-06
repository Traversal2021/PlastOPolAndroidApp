package com.example.litterdetection;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadServiceSingleBroadcastReceiver;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements UploadStatusDelegate {

    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int PermissionRequestCode = 1;
    private static final int SELECTION_CHOICE_FROM_ALBUM_REQUEST_CODE = 2; // album selection requestCode
    private static final int EDIT_CHOICE_FROM_ALBUM_REQUEST_CODE = 3; // album selection requestCode
    private static final int REQUEST_IMAGE_CAPTURE = 4;
    private static final int REQUEST_TAKE_PHOTO = 5;
    private Uri currentPhotoUri;
    private String ImageName;
    private String currentPhotoPath;

    private Button mainCapture;
    private Button mainUpload;
    private Button mainEdit;

    private static final int pic_id = 123;

    public static final String UPLOAD_URL = "http://158.38.66.238/plastOPol/Api.php?apicall=upload";

    private UploadServiceSingleBroadcastReceiver uploadReceiver;

    String currentPhotoPathCapture;



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


        uploadReceiver = new UploadServiceSingleBroadcastReceiver(this);


        if (checkPermissions()){


            mainCapture= findViewById(R.id.MainCaptureButton);
            mainCapture.setOnClickListener(clickListener);

            mainUpload=findViewById(R.id.MainUploadButton);
            mainUpload.setOnClickListener(clickListener);

            mainEdit=findViewById(R.id.MainEditButton);
            mainEdit.setOnClickListener(clickListener);
        }

        createFile();// ?????????????????????

    }

    private void createFile() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"LitterDetection");

        if (!file.exists()){

            if(file.mkdirs()){
                Toast.makeText(MainActivity.this,"Successful",Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(MainActivity.this,"fail",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            } else {Log.d("PERMISSIONS", "Permission already granted: " + p);}
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PermissionRequestCode);
            return false;
        }
        Toast.makeText(this, "All Permission Granted.", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionRequestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied.", Toast.LENGTH_SHORT).show();
                    closeNow();
                }
            }
        }
    }

    private void closeNow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity();
        } else {
            finish();
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.MainCaptureButton:
                    OpenCamera();
                    break;
                case R.id.MainUploadButton:
                    selectionChoiceFromAlbum();
                    break;
                case R.id.MainEditButton:
                    editChoiceFromAlbum();
                    break;
                default:
                    break;
            }
        }
    };

    private void OpenCamera() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = getImageContentUri(this, photoFile);
                currentPhotoPathCapture = photoURI.toString();
                Log.d("uri", "picture Uri is "+ photoURI.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"LitterDetection");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPathCapture = image.getAbsolutePath();
        return image;
    }
//
//    private File createImageFile() throws IOException {
//        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "IMAG_"+timeStamp;
//
//        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"LitterDetection");
//
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".png",         /* suffix */
//                storageDir      /* directory */
//        );
//
//        currentPhotoPath=image.getAbsolutePath();
//        ImageName=image.getName();
//        Log.d("imageFile", "image File Location"+ImageName);
//        return image;
//    }

    public static Uri getImageContentUri(Context context, File imageFile) {

        String filePath = imageFile.getAbsolutePath();

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,

                new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ",

                new String[] { filePath }, null);

        if (cursor != null && cursor.moveToFirst()) {

            @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));

            Uri baseUri = Uri.parse("content://media/external/images/media");

            return Uri.withAppendedPath(baseUri, "" + id);

        } else {

            if (imageFile.exists()) {

                ContentValues values = new ContentValues();

                values.put(MediaStore.Images.Media.DATA, filePath);

                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            } else {

                return null;

            }

        }

    }


    private void selectionChoiceFromAlbum() {

        // ????????????????????? Action????????????: "android.intent.action.GET_CONTENT"
        Intent choiceFromAlbumIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        choiceFromAlbumIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        //choiceFromAlbumIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //choiceFromAlbumIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        // ?????????????????????????????????
        choiceFromAlbumIntent.setType("*/*");
        startActivityForResult(choiceFromAlbumIntent, SELECTION_CHOICE_FROM_ALBUM_REQUEST_CODE);
    }

    private void editChoiceFromAlbum() {
        // ????????????????????? Action????????????: "android.intent.action.GET_CONTENT"
        Intent choiceFromAlbumIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        choiceFromAlbumIntent.addCategory(Intent.CATEGORY_OPENABLE);
        // ?????????????????????????????????
        choiceFromAlbumIntent.setType("image/*");
        startActivityForResult(choiceFromAlbumIntent, EDIT_CHOICE_FROM_ALBUM_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // ???????????????????????????????????????????????????
            switch (requestCode) {
                // album selection
                case EDIT_CHOICE_FROM_ALBUM_REQUEST_CODE:

                    Uri uri = data.getData();

                    String absolutePath =getRealPathFromUri(this,uri);
                    Uri mediaUri = getMediaUriFromPath(this,absolutePath);

                    Intent intent = new Intent();
                    intent.setDataAndType(uri,"image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.putExtra("imagePath", mediaUri.toString());
                    intent.setClass(MainActivity.this, EditActivity.class);
                    startActivity(intent);
                    Log.d("chosen Uri","edited Uri is " + uri);
                    break;
                // upload image
                case SELECTION_CHOICE_FROM_ALBUM_REQUEST_CODE:
                    ClipData clipData = data.getClipData();

                    if(clipData != null && clipData.getItemCount() > 0) {
                        for (int i=0; i<clipData.getItemCount(); i=i+2){
                        ClipData.Item item1 = clipData.getItemAt(i);
                        Uri upload_uri = item1.getUri();
                        ClipData.Item item2 = clipData.getItemAt(i+1);
                        Uri json_uri = item2.getUri();
                        uploadMultipart(upload_uri.toString(),json_uri.toString());
                        }
                    }

                    break;
                case REQUEST_TAKE_PHOTO:
                    Intent intent2 = new Intent();
                    intent2.putExtra("imagePath", currentPhotoPathCapture.toString());
                    intent2.putExtra("imageName", ImageName);
                    intent2.setClass(MainActivity.this, EditActivity.class);
                    startActivity(intent2);
                    Log.d("chosen Uri","capture Uri is " + currentPhotoPathCapture);
                    Toast.makeText(getBaseContext(), "Image Saved to " + currentPhotoPathCapture, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    @SuppressLint("NewApi")
    private static String getRealPathFromUri(Context context, Uri uri) {
        String filePath = null;
        String wholeID = null;

        wholeID = DocumentsContract.getDocumentId(uri);

        // ??????':'??????
        String id = wholeID.split(":")[1];

        String[] projection = { MediaStore.Images.Media.DATA };
        String selection = MediaStore.Images.Media._ID + "=?";
        String[] selectionArgs = { id };

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                selection, selectionArgs, null);
        int columnIndex = cursor.getColumnIndex(projection[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    @SuppressLint("Range")
    public static Uri getMediaUriFromPath(Context context, String path) {
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(mediaUri,
                null,
                MediaStore.Images.Media.DISPLAY_NAME + "= ?",
                new String[] {path.substring(path.lastIndexOf("/") + 1)},
                null);

        Uri uri = null;
        if(cursor.moveToFirst()) {
            uri = ContentUris.withAppendedId(mediaUri,
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
        }
        cursor.close();
        return uri;
    }

    private void uploadMultipart(String filePath, String jsonPath) {
        try {
            Log.d("file path", "Main file path is "+filePath);
            String uploadId = UUID.randomUUID().toString();
            uploadReceiver.setUploadID(uploadId);

            new MultipartUploadRequest(this, uploadId, UPLOAD_URL)
                    .setMethod("POST")
                    .addFileToUpload(filePath, "image")
                    .addFileToUpload(jsonPath, "desc")
                    .setNotificationConfig(new UploadNotificationConfig())
                    .setMaxRetries(0)
                    .startUpload();

        } catch (Exception exc) {
            Log.d("Upload start error", "upload error" + exc.getMessage() + exc);
        }
        finally {
            Toast.makeText(getBaseContext(), "Image Uploaded",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProgress(Context context, UploadInfo uploadInfo) {
        showMessage("Progress: " +  uploadInfo.getProgressPercent());
    }

    @Override
    public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
        showMessage("Error uploading. Server response code: " +  serverResponse.getHttpCode() + ", body: " + serverResponse.getBodyAsString());
    }

    @Override
    public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
        showMessage("Completed. Server response code: " +  serverResponse.getHttpCode() + ", body: " + serverResponse.getBodyAsString());
    }

    @Override
    public void onCancelled(Context context, UploadInfo uploadInfo) {
        showMessage("Upload cancelled");
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.i("Message", message);
    }



}