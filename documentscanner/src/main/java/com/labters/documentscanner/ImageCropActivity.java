/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */

package com.labters.documentscanner;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.labters.documentscanner.base.CropperErrorType;
import com.labters.documentscanner.base.DocumentScanActivity;
import com.labters.documentscanner.helpers.ScannerConstants;
import com.labters.documentscanner.libraries.PolygonView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ImageCropActivity extends DocumentScanActivity {

    private FrameLayout holderImageCrop;
    private ImageView imageView;
    private PolygonView polygonView;
//    private boolean isBW;

    private ProgressBar progressBar;
    private Bitmap cropImage;
    private OnClickListener btnImageEnhanceClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage = getCroppedImage(cropImage);
                        if (cropImage == null)
                            return false;
                        if (ScannerConstants.saveStorage)
                            saveToInternalStorage(cropImage);
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                if (cropImage != null) {
                                    ScannerConstants.selectedImageBitmap = cropImage;
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            })
            );
        }
    };
    private OnClickListener btnRebase = v -> {
        cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
        startCropping();
    };

    private OnClickListener btnColor = v -> {
        cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
        Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
        imageView.setImageBitmap(scaledBitmap);
    };

    private OnClickListener btnCloseClick = v -> finish();
    private OnClickListener btnBWColor = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setBW();
        }
    };

    private OnClickListener btnGrayColor = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setGray();
        }
    };

    private OnClickListener onRotateClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage = rotateBitmap(cropImage, 90);
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                startCropping();
                            })
            );
        }
    };


    protected void setBW() {
        showProgressBar();
        disposable.add(
                Observable.fromCallable(() -> {
                    bwColor();
                    return false;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    hideProgressBar();
                    Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                    imageView.setImageBitmap(scaledBitmap);
                })
        );
    }

    protected void setGray() {

        showProgressBar();
        disposable.add(
                Observable.fromCallable(() -> {
                    grayColor();
                    return false;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((result) -> {
                            hideProgressBar();
                            Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                            imageView.setImageBitmap(scaledBitmap);
                        })
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        cropImage = ScannerConstants.selectedImageBitmap;
//        isBW = false;

        if (ScannerConstants.selectedImageBitmap != null)
            initView();
        else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected FrameLayout getHolderImageCrop() {
        return holderImageCrop;
    }

    @Override
    protected ImageView getImageView() {
        return imageView;
    }

    @Override
    protected PolygonView getPolygonView() {
        return polygonView;
    }

    @Override
    protected void showProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void hideProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void showError(CropperErrorType errorType) {
        final Context context = this;
        switch (errorType) {
            case CROP_ERROR:
                runOnUiThread(() -> Toast.makeText(context, ScannerConstants.cropError, Toast.LENGTH_LONG).show());
                break;
        }
    }

    @Override
    protected Bitmap getBitmapImage() {
        return cropImage;
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    private void initView() {
        ImageView btnImageCrop = findViewById(R.id.btnImageCrop);
        ImageView btnClose = findViewById(R.id.btnClose);
        holderImageCrop = findViewById(R.id.holderImageCrop);
        imageView = findViewById(R.id.imageView);
        ImageView ivRotate = findViewById(R.id.ivRotate);
        ImageView ivBW = findViewById(R.id.ivInvert);
        ImageView ivColor = findViewById(R.id.ivColor);
        ImageView ivGray = findViewById(R.id.ivGray);

        ImageView ivRebase = findViewById(R.id.ivRebase);

        polygonView = findViewById(R.id.polygonView);
        progressBar = findViewById(R.id.progressBar);

        if (!ScannerConstants.showEditButtons) {
            RelativeLayout bottomLayout = findViewById(R.id.bottom_parent_rl);
            bottomLayout.setVisibility(View.GONE);
        }

        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);

        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);

        ivRotate.setOnClickListener(onRotateClick);
        ivBW.setOnClickListener(btnBWColor);
        ivColor.setOnClickListener(btnColor);
        ivRebase.setOnClickListener(btnRebase);
        ivGray.setOnClickListener(btnGrayColor);
        startCropping();
    }

    private void bwColor() {
//        if (!isBW) {
        try {
            Mat adaptiveTh = new Mat();
            Utils.bitmapToMat(cropImage, adaptiveTh);

            Imgproc.cvtColor(adaptiveTh, adaptiveTh, Imgproc.COLOR_BGR2GRAY);

            Imgproc.adaptiveThreshold(adaptiveTh, adaptiveTh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY, 75, 10);

            cropImage = Bitmap.createBitmap(adaptiveTh.cols(), adaptiveTh.rows(),
                    Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(adaptiveTh, cropImage);

            cropImage = cropImage.copy(cropImage.getConfig(), true);
        } catch (Exception e) {

        }

//            isBW = !isBW;
//        }
    }

    private void grayColor() {
//        if (!isBW) {
        try {
            cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);

            Mat adaptiveTh = new Mat();
            Utils.bitmapToMat(cropImage, adaptiveTh);

            Imgproc.cvtColor(adaptiveTh, adaptiveTh, Imgproc.COLOR_BGR2GRAY);

            cropImage = Bitmap.createBitmap(adaptiveTh.cols(), adaptiveTh.rows(),
                    Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(adaptiveTh, cropImage);

            cropImage = cropImage.copy(cropImage.getConfig(), true);
        } catch (Exception e) {

        }

//            isBW = !isBW;
//        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "cropped_" + timeStamp + ".png";
        File mypath = new File(directory, imageFileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
}
