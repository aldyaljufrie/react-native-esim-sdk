package com.esimsdk;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.esimtek.fsdk.FSdk;
import com.szsicod.print.escpos.PrinterAPI;
import com.szsicod.print.io.InterfaceAPI;
import com.szsicod.print.io.USBAPI;
import com.szsicod.print.utils.BitmapUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class RNEsimSdkModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private Runnable runnable;
    private Callback callback;

    public PrinterAPI mPrinter;

    public RNEsimSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNEsimSdk";
    }

    // Init and connect to printer
    @ReactMethod
    public void InitPrinter(Callback c){
        if (c != null) callback = c;

        mPrinter = PrinterAPI.getInstance();
        runnable = new Runnable() {
            @Override
            public void run() {
                String errorMsg;
                try {
                    if (mPrinter.isConnect()) {
                        mPrinter.disconnect();
                    }

                    InterfaceAPI io = new USBAPI(getCurrentActivity());
                    int ret = mPrinter.connect(io);
                    if (PrinterAPI.SUCCESS == ret) {
                        errorMsg = "Connect printer success";
                    } else {
                        errorMsg = "Connect printer fail, ret = " + ret;
                    }
                } catch (Exception ex) {
                    errorMsg = "Request printer permission fail: " + ex.getMessage();
                }
                ShowErrorMsg(errorMsg);
            }
        };
        new Thread(runnable).start();
    }

    @ReactMethod
    public void TestPrint(Callback c){
        if (c != null) callback = c;

        runnable = new Runnable() {
            @Override
            public void run() {
                int ret;
                try {
                    if (!mPrinter.isConnect()) {
                        ShowErrorMsg("Printer do not connect, please check the hardware and try restart app");
                        return;
                    }
                    //mPrinter.setAlignMode(1);
                    mPrinter.setFontStyle(0);
                    ret = mPrinter.printString(getCurrentActivity().getString(R.string.print_text), "GBK", true);
                    if (PrinterAPI.SUCCESS == ret) {
                        Log.i("print", "Print text success");
                    } else {
                        ShowErrorMsg("Print text fail, ret = " + ret);
                        return;
                    }
                } catch (UnsupportedEncodingException e) {
                    ShowErrorMsg("Print text catch exception: " + e.getMessage());
                    return;
                }

//                 try {
//                     mPrinter.initAllPrinter(2);
//                     @SuppressLint("ResourceType") InputStream is = reactContext.getResources().openRawResource(R.drawable.test);
//                     Bitmap drawingCache = BitmapFactory.decodeStream(is);
//                     // The appropriate pixel width of printer paper is about 500
//                     // Scale the image width and height according the pixel width of printer paper firstly
//                     Bitmap scaleCache = scaleBitmapByWidth(drawingCache, 500);
//                     ret = mPrinter.printRasterBitmap(scaleCache, true, 2000, false);
// //                    ret = mPrinter.printRasterBitmap(BitmapUtils.toGrays(drawingCache));
//                     if (PrinterAPI.SUCCESS == ret) {
//                         ShowErrorMsg("Print image success");
//                     } else {
//                         ShowErrorMsg("Print image fail, ret = " + ret);
//                     }
//                 } catch (IOException e) {
//                     ShowErrorMsg("Print image catch exception: " + e.getMessage());
//                 }

                mPrinter.printFeed();
                mPrinter.initAllPrinter(2);

                try {
                    mPrinter.setBarCodeWidth(2);
                    String barCode = getCurrentActivity().getString(R.string.print_bar_code_73);
                    ret = mPrinter.printBarCode(73, barCode.length(), barCode);
                    if (PrinterAPI.SUCCESS == ret) {
                        Log.e("print","Print bar code success");
                    } else {
                        ShowErrorMsg("Print bar code fail, ret = " + ret);
                        return;
                    }
                } catch (IOException e) {
                    ShowErrorMsg("Print bar code catch exception: " + e.getMessage());
                    return;
                }
//
                mPrinter.printFeed();

                try {
                    mPrinter.setBarCodeWidth(2);
                    String qrCode = getCurrentActivity().getString(R.string.print_qr_code);
                    ret = mPrinter.printQRCode(qrCode, 5, false);
                    if (PrinterAPI.SUCCESS == ret) {
                        mPrinter.printFeed();
                        Log.e("print","Print QR code success");
                    } else {
                        ShowErrorMsg("Print QR code fail, ret = " + ret);
                        return;
                    }
                } catch (Exception e) {
                    ShowErrorMsg("Print QR code catch exception: " + e.getMessage());
                    return;
                }

                ret = mPrinter.cutPaper(66, 0);
                if (PrinterAPI.SUCCESS == ret) {
                    Log.e("print","Cut paper success");
                } else {
                    ShowErrorMsg("Cut paper fail, ret = " + ret);
                    return;
                }

                ShowErrorMsg("Print runner is done!");
            }
        };
        new Thread(runnable).start();
    }

    private void ShowErrorMsg(final String error){
        this.callback.invoke(error);
    }

    public static Bitmap scaleBitmapByWidth(Bitmap bitmap, int mWidth) {
        int mHeight = bitmap.getHeight() / (bitmap.getWidth() / mWidth);
        Bitmap newBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);//创建和目标相同大小的空Bitmap
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();
        Bitmap temp = bitmap;

        PaintFlagsDrawFilter pfd= new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        canvas.setDrawFilter(pfd);
        Rect rect = new Rect(0, 0, mWidth, mHeight);
        canvas.drawBitmap(temp, null, rect, paint);

        return newBitmap;
    }
}
