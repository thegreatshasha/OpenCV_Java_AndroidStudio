package com.example.opencv_java_androidstudio;

import android.util.Log;
import android.util.Pair;

import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;

//import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;


import java.util.*;

public class HelloCv extends Test{
    static{
        System.loadLibrary("opencv_java3");
    }

    public static Pair<Mat, Integer> countColonies(Mat inp){

            // Initialize matrices
            Mat source = inp.clone();
            Mat destination = source;
            Mat gray = new Mat(source.rows(), source.cols(), CvType.CV_8UC1);
            Mat blur = new Mat(source.rows(), source.cols(), CvType.CV_8UC1);
            Mat tophat = new Mat(gray.rows(), gray.cols(), CvType.CV_8UC1);
            Mat dt = new Mat(gray.rows(), gray.cols(), CvType.CV_8UC1);
            Mat mask = new Mat(gray.rows(), gray.cols(), CvType.CV_8UC1, Scalar.all(0));
            Mat tophat_mask = new Mat(gray.rows(), gray.cols(), CvType.CV_8UC1);
            Mat rm = new Mat(gray.rows(), gray.cols(), CvType.CV_8UC1);
            destination = source;

            // RGB to gray
            Imgproc.cvtColor(destination, gray, Imgproc.COLOR_BGR2GRAY);

            // Do top hat filtering to correct for uneven illumination, does it work for all images? Let's hope so or we'll implement rolling ball algorithm
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(50,50));
            Imgproc.morphologyEx(gray, tophat, Imgproc.MORPH_TOPHAT, kernel);
            Test.saveImg("tophat.jpg", tophat);

            // Blur before thresholding
            Imgproc.GaussianBlur(tophat, blur, new Size(5,5), 0);
            Test.saveImg("blurred.jpg", blur);
            //blur = tophat;

            // Apply mask
            Point center = new Point(source.cols()/2, source.rows()/2);
            Scalar maskColor = new Scalar(255, 255, 255);

            Imgproc.circle(mask, center, Math.min(source.rows()/2, source.cols()/2) - 15, maskColor, -1);
            Test.saveImg("mask.jpg", mask);
            blur.copyTo(tophat_mask, mask);
            Core.bitwise_and(blur, blur, tophat_mask, mask);
            Test.saveImg("tophat_mask.jpg", tophat_mask);

            // Otsu thresholding on the tophat image
            Imgproc.threshold(tophat_mask,gray,0,255,Imgproc.THRESH_BINARY|Imgproc.THRESH_OTSU);

            // Save everything
            Test.saveImg("threshold.png", gray);

            // Find contours
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>(); //
            List<MatOfPoint> cnts = new ArrayList<MatOfPoint>();
            Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            System.out.println("Total Contours: "+contours.size());

            // Iterate over contours and print only the ones who have a circularity greater than 5
            for (int i=0; i<contours.size(); i++){
                MatOfPoint2f cont = new MatOfPoint2f(contours.get(i).toArray());
                double perimeter = Imgproc.arcLength(cont, true);
                double area = Imgproc.contourArea(cont);

                if(perimeter ==0){
                    continue;
                }

                double circ = (4*Math.PI*area)/(Math.pow(perimeter,2));

                if(area>100){
                    cnts.add(contours.get(i));
                }
            }

            Mat black = Mat.zeros(gray.rows(), gray.cols(), CvType.CV_8UC1);
            Imgproc.drawContours(black, cnts, -1, new Scalar(255,255,255), -1);
            Test.saveImg("black_contours.png", black);

            // Do the distance trnasform and count
            Imgproc.distanceTransform(black, dt, Imgproc.CV_DIST_L2, Imgproc.CV_DIST_MASK_PRECISE);
            Test.saveImg("distance_transform.png", dt);

            rm = Test.regional_maxima(dt);
            //Test.saveImg("regional_maxima.png", rm);

            Test.label(rm);
            //Test.saveImg("label.png", rm);
            //System.out.println(rm.dump());
            Core.MinMaxLocResult mmr = Core.minMaxLoc(rm);
            int count = (int)mmr.maxVal-1;
            System.out.println(count);

            Log.v("channels", Integer.toString(inp.channels()));
            Imgproc.drawContours(black, cnts, -1, new Scalar(255,0,255), 2);
            //inp = black;
            //Test.saveImg("final.png", source);
            Pair<Mat,Integer> p = Pair.create(black, count);

            // Release all memory matrices
            destination.release();
            gray.release();
            blur.release();
            tophat.release();
            dt.release();
            mask.release();
            tophat_mask.release();
            rm.release();

            return p;
    }

    public static Mat cropContour(Mat img, List<MatOfPoint> contours, int idx){
        Mat black = Mat.zeros(img.rows(), img.cols(), CvType.CV_8UC1);
        //System.out.println(black.rows()+","+black.cols());
        Imgproc.drawContours(black, contours, idx, new Scalar(255,255,255), -1);
        //Highgui.imwrite("5.jpg", black);
        //System.out.println(contours.get(idx));
        Rect bounds = Imgproc.boundingRect(contours.get(idx));
        //System.out.println(img.rows()+" "+img.cols());
        //System.out.println(bounds.x+","+bounds.y+","+bounds.width+","+bounds.height);
        return black.submat(bounds.y-1, bounds.y + bounds.height+1, bounds.x-1, bounds.x+bounds.width+1);
        //return black(Rect(0,0,10,10));
    }
}
