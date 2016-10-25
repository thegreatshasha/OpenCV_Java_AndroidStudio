package com.example.opencv_java_androidstudio;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.os.Environment;

        import android.app.Activity;
        import android.content.Intent;
        import android.database.Cursor;
        import android.graphics.BitmapFactory;
        import android.graphics.Bitmap;
        import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.ContentResolver;
        import android.provider.MediaStore;
import android.util.Pair;
import android.view.View;
import java.io.File;
import java.io.InputStream;
import android.util.Log;

import android.widget.Button;
        import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends Activity {

    static {
        // If you use opencv 2.4, System.loadLibrary("opencv_java")
        System.loadLibrary("opencv_java3");
    }



    private static int RESULT_LOAD_IMAGE = 1;
    private static int RESULT_CAPTURE_IMAGE = 2;
    private Uri imageUri;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

//        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
//        buttonLoadImage.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View arg0) {
//
//                Intent i = new Intent(
//                        Intent.ACTION_PICK,
//                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//
//                startActivityForResult(i, RESULT_LOAD_IMAGE);
//            }
//        });

        Button buttonTakeImage = (Button) findViewById(R.id.buttonTakePicture);
        buttonTakeImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent intent = new Intent(MainActivity.this, AndroidCamera.class);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        String img = intent.getStringExtra("image");

        if(img != null){
            Bitmap bmp = BitmapFactory.decodeFile(img);
            Mat pic1 = new Mat();
            Utils.bitmapToMat(bmp,pic1);
            new AsyncImageProcess().execute(pic1);
        }
    }

//    public void forceCrash(View view) {
//        throw new RuntimeException("This is a crash");
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_CAPTURE_IMAGE && resultCode == RESULT_OK) {
            Uri selectedImage = imageUri;
            Bitmap myBitmap;
            getContentResolver().notifyChange(selectedImage, null);
            ContentResolver cr = getContentResolver();

            try {
                myBitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, imageUri);
                ImageView imageView = (ImageView) findViewById(R.id.imgView);
                TextView textView = (TextView) findViewById(R.id.textView1);
                Mat pic1 = new Mat();
                Utils.bitmapToMat(myBitmap,pic1);
                new AsyncImageProcess().execute(pic1);

            } catch (Exception e) {
                Log.e("Camera", e.toString());
            }
        }

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.imgView);
            TextView textView = (TextView) findViewById(R.id.textView1);
            Bitmap myBitmap = BitmapFactory.decodeFile(picturePath);
            Mat pic1 = new Mat();
            Utils.bitmapToMat(myBitmap,pic1);
            new AsyncImageProcess().execute(pic1);
        }
    }

    private class AsyncImageProcess extends AsyncTask<Mat, Integer, Pair<Mat,Integer>>{
        @Override
        protected void onPreExecute(){
            // Show the circular loader
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        }

        @Override
        protected Pair<Mat,Integer> doInBackground(Mat... params){
            Mat inp = params[0];
            // Do image processing
            Mat resized = inp;
            //Mat resized = Test.scaledResize(inp, 1000);
            return HelloCv.countColonies(resized);

            //Pair<Mat,Integer> result = Pair.create(resized, count);
            //return result;
        }

        @Override
        protected void onProgressUpdate(Integer... i){
            // Hide the circular loader
        }

        @Override
        protected void onPostExecute(Pair values){
            Mat resized = (Mat)values.first;
            int count = (int) values.second;

            // Update the ui with the count
            ImageView imageView = (ImageView) findViewById(R.id.imgView);
            TextView textView = (TextView) findViewById(R.id.textView1);

            Bitmap bmp = Bitmap.createBitmap(resized.cols(), resized.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resized, bmp);

            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            imageView.setImageBitmap(bmp);
            textView.setText(Integer.toString(count));

            return;
        }
    }
}