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
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.esimtek.fsdk.FSdk;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
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
    private Callback errorCallback;

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
                int ret;
                try {
                    if (mPrinter.isConnect()) mPrinter.disconnect();

                    InterfaceAPI io = new USBAPI(getCurrentActivity());
                    ret = mPrinter.connect(io);
                    if (PrinterAPI.SUCCESS == ret)
                        errorMsg = "CONNECTED";
                    else
                        errorMsg = "DISCONNECTED";
                } catch (Exception ex) {
                    errorMsg = "Request printer permission fail: " + ex.getMessage();
                }
                ShowMsg(errorMsg);
            }
        };
        new Thread(runnable).start();
    }

    @ReactMethod
    public void PrintBarcode (String code, Integer barcodeWidth, Callback c){
        if (c != null) callback = c;
        if (code == null){
            ShowMsg("Barcode content cannot be empty!");
            return;
        }
        barcodeWidth = barcodeWidth != null ? barcodeWidth : 2;

        final String barcode = code;
        final Integer width = barcodeWidth;

        runnable = new Runnable() {
            @Override
            public void run() {
                int ret;
                try {
                    if (!mPrinter.isConnect()) {
                        ShowMsg("DISCONNECTED");
                        return;
                    }

                    mPrinter.setBarCodeWidth(width);
                    String barCode = barcode;
                    ret = mPrinter.printBarCode(73, barCode.length(), barCode);
                    if (PrinterAPI.SUCCESS == ret) {
                        Log.e("print","Print bar code success");
                    } else {
                        ShowMsg("Print bar code fail, ret = " + ret);
                        return;
                    }
                } catch (IOException e) {
                    ShowMsg("Print bar code catch exception: " + e.getMessage());
                    return;
                }
            }
        };
        new Thread(runnable).start();
    }

    @ReactMethod
    public void PrintQRCode (String code, Integer qrSize, Callback c){
        if (c != null) callback = c;
        if (code == null){
            ShowMsg("Barcode content cannot be empty!");
            return;
        }
        qrSize = qrSize != null ? qrSize : 5;

        final String qrcode = code;
        final Integer size = qrSize;

        runnable = new Runnable() {
            @Override
            public void run() {
                int ret;
                try {
                    if (!mPrinter.isConnect()) {
                        ShowMsg("DISCONNECTED");
                        return;
                    }

                    String qrCode = qrcode;
                    ret = mPrinter.printQRCode(qrCode, size, false);
                    if (PrinterAPI.SUCCESS == ret) {
                        mPrinter.printFeed();
                        Log.e("print","Print QR code success");
                    } else {
                        ShowMsg("Print QR code fail, ret = " + ret);
                        return;
                    }
                } catch (Exception e) {
                    ShowMsg("Print QR code catch exception: " + e.getMessage());
                    return;
                }
            }
        };
        new Thread(runnable).start();
    }

    @ReactMethod
    public void PrintText (String t, Callback c){
        if (c != null) callback = c;
        if (t == null || t.isEmpty()){
            ShowMsg("Text content cannot be empty!");
            return;
        }
        final String text = t;
        runnable = new Runnable() {
            @Override
            public void run() {
                int ret;
                try {
                    if (!mPrinter.isConnect()) {
                        ShowMsg("DISCONNECTED");
                        return;
                    }

                    mPrinter.setFontStyle(0);
                    ret = mPrinter.printString(text);
                    if (PrinterAPI.SUCCESS == ret) {
                        Log.i("print", "Print text success");
                    } else {
                        ShowMsg("Print text fail, ret = " + ret);
                        return;
                    }
                } catch (UnsupportedEncodingException e) {
                    ShowMsg("Print text catch exception: " + e.getMessage());
                    return;
                }
                ShowMsg("SUCCESS");
            }
        };
        new Thread(runnable).start();
    }

    @ReactMethod
    public void printReceipt(final ReadableMap paymentType, final ReadableMap brandInfo, final ReadableArray items, final ReadableMap cost, final ReadableMap dates, Callback c){
        if (c != null) callback = c;

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!mPrinter.isConnect()) {
                    ShowMsg("DISCONNECTED");
                    return;
                }
                try {
                    String brand = brandInfo.getString("client");
                    String branch = brandInfo.getString("branch");
                    String dateNow = dates.getString("dateNow");
                    String nextDay = dates.getString("nextDay");
                    String totalPrice = cost.getString("totalPrice");
                    String logisticPrice = cost.getString("logisticPrice");
                    String totalAll = cost.getString("totalAll");
                    String payment = paymentType.getString("type");
                    String paymentData = paymentType.getString("data");
                    String paymentString = (payment == "midtrans") ? "Transver VA ( " + paymentData + " )" : paymentData;

                    // try {
                    //     byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                    //     Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    //     mPrinter.printRasterBitmap(decodedByte, false);
                    // } catch (IOException e) {
                    //     Log.e("print", "Print image catch exception: " + e.getMessage());
                    // }

                    mPrinter.setFontStyle(0);
                    mPrinter.setAlignMode(1);
                    mPrinter.printString(brand);
                    mPrinter.printFeed();
                    mPrinter.printString("In partnership with Dav");
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(0);
                    mPrinter.printString(brand + " - " + branch);
                    mPrinter.printFeed();
                    mPrinter.printString(dateNow);
                    mPrinter.printFeed();

                    mPrinter.printString("------------------------------------------------");
                    mPrinter.printFeed();

                    for (int i = 0; i < items.size(); i++){
                        ReadableMap rmItem = items.getMap(i);
                        String name = rmItem.getString("name");
                        String qty = rmItem.getString("qty");
                        String price = rmItem.getString("price");

                        mPrinter.setAlignMode(0);
                        mPrinter.printString(qty + " X " + name.substring(0, 25));
                        mPrinter.printFeed();
                        mPrinter.setAlignMode(2);
                        mPrinter.printString("Rp." + price);
                        mPrinter.printFeed();
                    }

                    mPrinter.setAlignMode(0);
                    mPrinter.printString("--  --  --  --  --  --  --  --  --  --  --  --  ");
                    mPrinter.printFeed();

                    mPrinter.printString("Total Harga Produk                          Rp." + totalPrice);
                    mPrinter.printFeed();

                    mPrinter.printString("Biaya Pengiriman                            Rp." + logisticPrice);
                    mPrinter.printFeed();

                    mPrinter.printString("--  --  --  --  --  --  --  --  --  --  --  --  ");
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(2);
                    mPrinter.printString("TOTAL Rp." + totalAll);
                    mPrinter.printFeed();
                    mPrinter.printString("Harga sudah termasuk PPN 10%");
                    mPrinter.printFeed();

                    if (payment.equals("midtrans")) {
                        mPrinter.setAlignMode(0);
                        mPrinter.printString("Metode Pembayaran       : " + paymentString);
                        mPrinter.printFeed();
                    } else {
                        mPrinter.setAlignMode(1);
                        mPrinter.printString("Metode Pembayaran");
                        mPrinter.printFeed();
                        mPrinter.printQRCode(paymentString, 100, false);
                        mPrinter.printFeed();
                        mPrinter.setAlignMode(0);
                    }

                    mPrinter.printString("Batas Waktu Pembayaran  : " + nextDay);
                    mPrinter.printFeed();

                    mPrinter.printString("------------------------------------------------");
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(1);
                    mPrinter.printString("Jika ada pertanyaan lanjutan silahkan hubungi");
                    mPrinter.printFeed();
                    mPrinter.printString("Customer Service di");
                    mPrinter.printFeed();
                    mPrinter.printString("1500 860");
                    mPrinter.printFeed();
                    mPrinter.printString("atau");
                    mPrinter.printFeed();
                    mPrinter.printString("cs@alfadigital.id");
                    mPrinter.printFeed();

                    mPrinter.cutPaper(66, 0);

                } catch (UnsupportedEncodingException e) {
                    ShowMsg("Print text catch exception: " + e.getMessage());
                    return;
                }
                ShowMsg("SUCCESS");
            }
        };
        new Thread(runnable).start();
    }

    @ReactMethod
    public void CutPaper (){
        String errorMsg;
        int ret = mPrinter.cutPaper(66, 0);
        if (PrinterAPI.SUCCESS == ret) {
            errorMsg = "CUTPAPER SUCCESS";
        } else {
            errorMsg = "Cut paper fail, ret = " + ret;
        }
        ShowMsg(errorMsg);
    }

    private void ShowMsg(final String msg){
        this.callback.invoke(msg);
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
