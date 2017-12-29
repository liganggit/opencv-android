package com.example.ligang.myapplication;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;

import static android.os.AsyncTask.Status.RUNNING;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "test";
    private double max_size = 1024;
    private int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private Bitmap selectbp;
    private Uri uri;
    private BilateralFilterTask bilateralFilterTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        staticLoadCVLibraries();
        bilateralFilterTask = new BilateralFilterTask();
        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (imageView != null) {
                    readFile();
                    convertGray(seekBar.getProgress() / 100);
                }
            }
        });
    }

    //OpenCV库静态加载并初始化
    private void staticLoadCVLibraries(){
        boolean load = OpenCVLoader.initDebug();
        if(load) {
            Log.i("CV", "Open CV Libraries loaded...");
        }
    }
    private class BilateralFilterTask extends AsyncTask<Float, Bitmap, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Bitmap doInBackground(Float... bilityTraversal) {
            Bitmap processbp = Bitmap.createBitmap(selectbp);

            if (bilityTraversal[0] > 0) {

/*
    双边滤波 计算量太大
                int width = processbp.getWidth();
                int height =  processbp.getHeight();
                Mat src = new Mat(width, height, CvType.CV_8UC3);
                Mat tmp = new Mat (width, height, CvType.CV_8UC3);
                Mat dst = tmp.clone();
                Utils.bitmapToMat(processbp, src);
                Imgproc.cvtColor(src, tmp, Imgproc.COLOR_BGRA2BGR);
                Imgproc.bilateralFilter(tmp, dst,bilityTraversal[0] * 25,bilityTraversal[0] * 25/2,bilityTraversal[0] * 25/2);
                Utils.matToBitmap(dst, processbp);
*/

// 磨皮美颜算法
                int dx = (int)bilityTraversal[0].floatValue() * 5; // 双边滤波参数之一
                double fc = bilityTraversal[0] * 12.5; // 双边滤波参数之一
                double p = 0.1f; // 透明度
                Mat image = new Mat(), dst = new Mat(), matBilFilter = new Mat(), matGaussSrc = new Mat(), matGaussDest = new Mat(), matTmpDest = new Mat(), matSubDest = new Mat(), matTmpSrc = new Mat();

                // 双边滤波
                Utils.bitmapToMat(processbp, image);
                Imgproc.cvtColor(image, image, Imgproc.COLOR_BGRA2BGR);
                Imgproc.bilateralFilter(image, matBilFilter, dx, fc, fc);

                Core.subtract(matBilFilter, image, matSubDest);
                Core.add(matSubDest, new Scalar(128, 128, 128, 128), matGaussSrc);
                // 高斯模糊
                Imgproc.GaussianBlur(matGaussSrc, matGaussDest, new Size(2 * bilityTraversal[0] - 1, 2 * bilityTraversal[0] - 1), 0, 0);
                matGaussDest.convertTo(matTmpSrc, matGaussDest.type(), 2, -255);
                Core.add(image, matTmpSrc, matTmpDest);
                Core.addWeighted(image, p, matTmpDest, 1 - p, 0.0, dst);

                Core.add(dst, new Scalar(10, 10, 10), dst);
                Utils.matToBitmap(dst, processbp);

            }

            return processbp;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }
    }
    private void convertGray(float bilityTraversal) {
        bilateralFilterTask.cancel(true);
        bilateralFilterTask = new BilateralFilterTask();
        bilateralFilterTask.execute(bilityTraversal);
    }

    private void readFile() {
        try {
            Log.d("image-tag", "start to decode selected image now...");
            InputStream input = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            int raw_width = options.outWidth;
            int raw_height = options.outHeight;
            int max = Math.max(raw_width, raw_height);
            int newWidth = raw_width;
            int newHeight = raw_height;
            int inSampleSize = 1;
            if(max > max_size) {
                newWidth = raw_width / 2;
                newHeight = raw_height / 2;
                while((newWidth/inSampleSize) > max_size || (newHeight/inSampleSize) > max_size) {
                    inSampleSize *=2;
                }
            }

            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            selectbp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uri = data.getData();
            readFile();
            imageView.setImageBitmap(selectbp);
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"选择图像..."), PICK_IMAGE_REQUEST);
    }
}