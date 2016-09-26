package com.example.opencv_java_androidstudio;

/**
 * Created by shashwat on 03/08/16.
 */
import org.opencv.core.Core;
import org.opencv.core.Size;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import java.util.Date;
import android.util.Log;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.MatOfPoint2f;
//import org.opencv.core.Scalar;

import java.util.Stack;
import java.io.File;

import android.os.Environment;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Test {
    static{
        System.loadLibrary("opencv_java3");
    }

//    public static void main(String[] args) {
//        //System.out.println("Hello, OpenCV");
//
//        // Load the native library.
//        Mat source = Highgui.imread("/Users/shashwat/Downloads/watershed1.tiff");
//        Imgproc.cvtColor(source, source , Imgproc.COLOR_BGR2GRAY);
//        Imgproc.threshold(source,source,0,255,Imgproc.THRESH_BINARY_INV|Imgproc.THRESH_OTSU);
//        System.out.println(count(source));
//        // Now to label and print connected components
//
//        //label(source);
//    }

    public static int getPixel(Mat img, int i, int j){
        try{
            int val = (int)img.get(i,j)[0];
            return val;
        }
        catch(Exception e){
            return 0;
        }
    }

    public static Mat scaledResize(Mat img, int w_new){
        Mat resized_image = new Mat();
        int h_new = (img.rows()*w_new)/img.cols();
        Imgproc.resize(img, resized_image, new Size(w_new, h_new));
        return resized_image;
    }

    public static void saveImg (String name, Mat mat) {
        Mat img2 = mat.clone();
        Core.normalize(mat, img2, 0, 255, Core.NORM_MINMAX,  CvType.CV_8UC1);
        //Imgproc.cvtColor(mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);
        Log.i("saveImg", Environment.getExternalStorageDirectory().toString());
        File path = new File(Environment.getExternalStorageDirectory() + "/");
        //path.mkdirs();
        File file = new File(path, name);

        String filename = file.toString();
        Boolean bool = Imgcodecs.imwrite(filename, img2);

        if (bool)
            Log.i("success", "SUCCESS writing image to external storage");
        else
            Log.i("failure", "Fail writing image to external storage");
    }

    public static int count(Mat inpu, int index){

        Mat source = inpu.clone();
        //Mat source = inp.clone();
        Mat dt = new Mat(source.rows(), source.cols(), CvType.CV_8UC1);
        //Imgproc.cvtColor(source, source , Imgproc.COLOR_BGR2GRAY);
        //Imgproc.threshold(source,source,0,255,Imgproc.THRESH_BINARY|Imgproc.THRESH_OTSU);
        String name1 = "thresh"+String.valueOf(index)+".png";
        String name2 = "dt"+String.valueOf(index)+".png";
        String name3 = "max"+String.valueOf(index)+".png";
        //Log.v("success", name1);
        //Log.v("success", name2);
        //Log.v("success", name2);
        saveImg(name1, source);

        //System.out.println(source.dump());
        // Operate on the distance transform image now
        Imgproc.distanceTransform(source, dt, Imgproc.CV_DIST_L2, 3);
        Core.normalize(dt, dt, 0, 255, Core.NORM_MINMAX,  CvType.CV_8UC1);
        saveImg(name2, dt);
        // Cool so distance transform works fine
        Mat rm = regional_maxima(source);
        //System.out.println(source.dump());

        Imgproc.threshold(rm,rm,0,1,Imgproc.THRESH_BINARY);

        Mat element1 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new  Size(2,2));
        Imgproc.dilate(rm, rm, element1);

        //System.out.println(rm.dump());
        label(rm);
        //Imgproc.dilate(rm, )
        //Core.normalize(rm, rm, 0, 255, Core.NORM_MINMAX,  CvType.CV_8UC1);
        //Highgui.imwrite("3.jpg", rm);

        //System.out.println(rm.dump());
        //saveImg("3.jpg", rm);



        Core.MinMaxLocResult mmr = Core.minMaxLoc(rm);
        //System.out.println(rm);
        int count = (int)mmr.maxVal-1;
        saveImg(name3, rm);
        return count;
        //return 0;
    }

    //Returns 1 for pixels marked as regional maxima and 0 for non maxima pixels
    public static void dfsset(Mat inp, int i, int j, int pixel) {
        Node parent = new Node(i, j);

        Stack<Node> st = new Stack<Node>();

        st.push(parent);

        while(!st.empty()){
            // Equivalent dfsset code here
            Node node = st.pop();

            // Color all pixels with the value=pixel
            if(getPixel(inp, node.i, node.j) == pixel){
                // Set this node as black
                inp.put(node.i, node.j, 0);

                if(node.j>0)
                    st.push(new Node(node.i, node.j-1));
                if(node.j<inp.cols()-1)
                    st.push(new Node(node.i, node.j+1));
                if(node.i>0)
                    st.push(new Node(node.i-1, node.j));
                if(node.i<inp.rows()-1)
                    st.push(new Node(node.i+1, node.j));
            }
        }
    }

    // Regional maxima detection using
    public static Mat regional_maxima(Mat inp){
        Mat out = inp.clone();

        // Iterate over all pixels
        for(int i = 1; i< inp.rows()-1; i++){
            for(int j=1; j<inp.cols()-1; j++){
                int pixel = getPixel(inp, i, j);

                // If this pixel has already been marked as non minima, screw this
                if((int)out.get(i,j)[0] == 0)
                    continue;

                // Compare pixel with its neighbours in input, if small, do dfs and set to 255
                if(check(inp, i, j) == false)
                    dfsset(out, i, j, pixel);
            }
        }
        Imgproc.threshold(out, out, 0,1, Imgproc.THRESH_BINARY);
        return out;
    }

    public static Boolean check(Mat inp, int i_p, int j_p){
        int max = -1;

        for(int i = i_p-1; i<=i_p+1; i++){
            for(int j=j_p-1; j<=j_p+1; j++){
                int pxl = getPixel(inp, i, j);
                max = Math.max(max, pxl);
            }
        }

        if(getPixel(inp, i_p, j_p) == max)
            return true;
        else
            return false;
    }

    public static void dfsset2(Mat damon, int i, int j, int label){
        Node parent = new Node(i, j);

        Stack<Node> st = new Stack<Node>();

        st.push(parent);

        while(!st.empty()){
            // Process current pixel node
            Node node = st.pop();

            int pixel = getPixel(damon, node.i, node.j);

            // If it's a white connected pixel, label it and add it's neighbours
            if(pixel==1){
                damon.put(node.i, node.j, label);

                // Also label the 8 connected neighbourhood of this pixel
                if(node.j>0)
                    st.push(new Node(node.i, node.j-1));
                if(node.j<damon.cols()-1)
                    st.push(new Node(node.i, node.j+1));
                if(node.i>0)
                    st.push(new Node(node.i-1, node.j));
                if(node.i<damon.rows()-1)
                    st.push(new Node(node.i+1, node.j));
                if(node.i>0 && node.j>0)
                    st.push(new Node(node.i-1, node.j-1));
                if(node.i>0 && node.j<damon.cols())
                    st.push(new Node(node.i-1, node.j+1));
                if(node.i<damon.rows() && node.j>0)
                    st.push(new Node(node.i+1, node.j-1));
                if(node.i<damon.rows() && node.j<damon.cols())
                    st.push(new Node(node.i+1, node.j+1));
            }
        }
    }

    // 4 connectivity connected componenets labelling
    public static void label(Mat damon) {
        //System.out.println(damon.rows()+","+damon.cols());
        //System.out.println(damon.dump());
        int label = 2;

        for(int i = 0; i< damon.rows(); i++){
            for(int j=0; j<damon.cols(); j++){
                int pixel = getPixel(damon, i, j);
                if(pixel==1){
                    dfsset2(damon, i, j, label);
                    label += 1;
                }
                //System.out.println((int)damon.get(i,j)[0]) ;
            }
        }

        //System.out.println(damon.dump());

    }

    public static boolean canWriteOnExternalStorage() {
        // get the state of your external storage
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // if storage is mounted return true
            Log.v("sTag", "Yes, can write to external storage.");
            return true;
        }
        return false;
    }

    public static Mat copyToBlack(Mat mat){
        Mat black = Mat.zeros(mat.rows(), mat.cols(), CvType.CV_8UC1);
        mat.copyTo(black);
        return black;
    }
}
