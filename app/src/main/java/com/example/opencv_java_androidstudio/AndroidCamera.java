package com.example.opencv_java_androidstudio;

/**
 * Created by shashwat on 01/09/16.
 */
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;
import android.widget.ImageView;

public class AndroidCamera extends Activity implements SurfaceHolder.Callback{

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    String stringPath = "/sdcard/samplevideo.3gp";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_camera);

        //Button buttonStartCameraPreview = (Button)findViewById(R.id.startcamerapreview);
        //Button buttonStopCameraPreview = (Button)findViewById(R.id.stopcamerapreview);
        Button buttonTakePicture = (Button)findViewById(R.id.takepicture);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        buttonTakePicture.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                camera.takePicture(null, null, myPicCall);
            }
        });

        //Intent intent = new Intent(AndroidCamera.this, DisplayActivity.class);
        //startActivity(intent);

    }

    public void forceCrash(View view) {
        throw new RuntimeException("This is a crash");
    }


    public Bitmap drawMask(Bitmap orig, Bitmap mask){
        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(result);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        tempCanvas.drawBitmap(orig, 0, 0, null);
        tempCanvas.drawBitmap(mask, 0, 0, paint);

        return result;
    }

    public void drawTransparentCircle(Bitmap inp, int width, int height, Bitmap overlay){
        int radius = Math.min(width, height)/2;
        Canvas mCanvas = new Canvas(inp);

        // Instead of crop, draw the 80% circle here, don't worry about transparency for now
        // draw black rectangle + add cicle later
        Paint myPaint2 = new Paint();
        myPaint2.setARGB(255, 0, 0, 0);
        myPaint2.setStrokeWidth(2);
        mCanvas.drawRect(0, 0, width, height, myPaint2);

        Paint myPaint = new Paint();
        myPaint.setAntiAlias(true);
        myPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCanvas.drawCircle(width/2, height/2, radius, myPaint);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus){
            ImageView imgView = (ImageView) findViewById(R.id.imageview);

            Bitmap result = Bitmap.createBitmap(imgView.getWidth(), imgView.getHeight(), Bitmap.Config.ARGB_8888);
            drawTransparentCircle(result, imgView.getWidth(), imgView.getHeight(), null);

            //mCanvas.drawRect(0, 0, imgView.getWidth(), imgView.getHeight(), myPaint);
            //Bitmap crop = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            //mCanvas.drawBitmap(crop, 0, 0, null);

            imgView.setImageBitmap(result);
        }
    }

    private Camera.PictureCallback myPicCall = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera cm){
            //Debug.startMethodTracing();
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            Bitmap orig = BitmapFactory.decodeByteArray(data , 0, data.length);
//
            Bitmap mask = Bitmap.createBitmap(orig.getWidth(), orig.getHeight(), Bitmap.Config.ARGB_8888);
            drawTransparentCircle(mask, orig.getWidth(), orig.getHeight(), orig);
            Bitmap finalImg = drawMask(orig, mask);

            // Fucking slow mate, why convert?
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            finalImg.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] data2 = stream.toByteArray();

            if (pictureFile == null){
                Log.d("asgard", "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data2);
                fos.close();

                orig.recycle();
                mask.recycle();
                finalImg.recycle();

                Intent intent = new Intent(AndroidCamera.this, MainActivity.class);
                intent.putExtra("image", pictureFile.toString());
                startActivity(intent);

            } catch (FileNotFoundException e) {
                Log.d("error", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("error", "Error accessing file: " + e.getMessage());
            }
            //Debug.stopMethodTracing();
        }
    };

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        // TODO Auto-generated method stub
        if(!previewing){
            camera = Camera.open();
            if (camera != null){
                try {
                    camera.setDisplayOrientation(90);
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.startPreview();
                    previewing = true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        if(!previewing){
            camera = Camera.open();
            if (camera != null){
                try {
                    camera.setDisplayOrientation(90);
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.startPreview();
                    previewing = true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        if(previewing){
            previewing = false;
            camera.stopPreview();
            camera.release();
        }
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}