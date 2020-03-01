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
    public void printReceipt(final ReadableMap paymentType, final ReadableMap userInfo, final ReadableMap brandInfo, final ReadableArray items, final ReadableMap cost, final ReadableMap dates, Callback c){
        if (c != null) callback = c;

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!mPrinter.isConnect()) {
                    ShowMsg("DISCONNECTED");
                    return;
                }
                try {
                    String encodedImage = "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";
                    
                    // user objects
                    String userName = brandInfo.getString("name");
                    String userAddress = brandInfo.getString("alamat");
                    String userPhone = brandInfo.getString("phone");

                    // brand objects
                    String brand = brandInfo.getString("client");
                    String branch = brandInfo.getString("branch");
                    String address = brandInfo.getString("address");

                    // dates objects
                    String dateNow = dates.getString("dateNow");
                    String nextDay = dates.getString("nextDay");

                    // cost objects
                    String totalPrice = cost.getString("totalPrice");
                    String diskon = cost.getString("diskon");
                    String diskonOnkir = cost.getString("diskonOnkir");
                    String logisticPrice = cost.getString("logisticPrice");
                    String totalAll = cost.getString("totalAll");

                    // payment objects
                    String orderCode = paymentType.getString("order");
                    String payment = paymentType.getString("type");
                    String paymentData = paymentType.getString("data");
                    String bank = paymentType.getString("bank");
                    String paymentString = (payment == "nicepay_va") ? "Transver VA ( " + paymentData + " )" : paymentData;

                    mPrinter.setAlignMode(1);

                    try {
                        Bitmap logo = BitmapFactory.decodeResources(context.getResources(), R.drawable.logo_black)
                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        mPrinter.printRasterBitmap(decodedByte, true);
                    } catch (IOException e) {
                        Log.e("print", "Print image catch exception: " + e.getMessage());
                    }

                    mPrinter.printString("MEDIVSCREEN");
                    mPrinter.printFeed();
                    mPrinter.printString(brand + " " + branch);
                    mPrinter.printFeed();
                    mPrinter.printString(address);
                    mPrinter.printFeed();
                    mPrinter.printString(dateNow);

                    mPrinter.printFeed();
                    mPrinter.printFeed();
                    mPrinter.printString("================================================");
                    mPrinter.setAlignMode(0);
                    mPrinter.printString(userName);
                    mPrinter.printFeed();
                    mPrinter.printString(userAddress);
                    mPrinter.printFeed();
                    mPrinter.printString(userPhone);
                    mPrinter.printFeed();
                    mPrinter.setAlignMode(1);
                    mPrinter.printString("================================================");
                    mPrinter.printFeed();
                    mPrinter.printFeed();

                    for (int i = 0; i < items.size(); i++){
                        ReadableMap rmItem = items.getMap(i);
                        String name = rmItem.getString("name");
                        String qty = rmItem.getString("qty");
                        String price = rmItem.getString("price");
                        boolean subName = (name.length() >= 55) ? true : false;

                        mPrinter.setAlignMode(0);
                        mPrinter.printString(qty + " X " + (subName ? name.substring(0, 55) + "..." : name));
                        mPrinter.printFeed();
                        
                        mPrinter.setAlignMode(2);
                        mPrinter.printString("Rp." + price);
                        mPrinter.printFeed();
                    }

                    mPrinter.setAlignMode(0);
                    mPrinter.printString("------------------------------------------------");
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(2);
                    mPrinter.printString("Subtotal : Rp." + totalPrice);
                    mPrinter.printFeed();
                    mPrinter.printString("Biaya Antar : Rp." + logisticPrice);
                    mPrinter.printFeed();
                    mPrinter.printString("Diskon : -Rp." + diskon);
                    mPrinter.printFeed();
                    mPrinter.printString("Diskon Ongkir : -Rp." + diskonOnkir);
                    mPrinter.printFeed();


                    mPrinter.printString("------------------------------------------------");

                    mPrinter.printFeed();
                    mPrinter.printString("Total : Rp." + totalAll);
                    mPrinter.printFeed();
                    mPrinter.printString("Harga sudah termasuk PPN 10%");
                    mPrinter.printFeed();
                    mPrinter.printFeed();

                    mPrinter.setFontStyle(1)
                    mPrinter.printString("================================================");
                    mPrinter.printFeed();
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(0);
                    mPrinter.printString("Metode Pembayaran");
                    mPrinter.printFeed();

                    if (payment.equals("nicepay_va")) {
                        mPrinter.printString("Transfer Virtual Account :");
                        mPrinter.printFeed();
                        mPrinter.printString(bank + " " + paymentData);
                    } else {
                        mPrinter.printQRCode(paymentString, 100, false);
                    }
                    mPrinter.printFeed();
                    mPrinter.printString("Batas Waktu Pembayaran  : " + nextDay);

                    mPrinter.printFeed();
                    mPrinter.printFeed();
                    mPrinter.printString("================================================");
                    mPrinter.printFeed();
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(1);
                    mPrinter.printString("Cek Email Anda untuk melihat detail pembelanjaan Anda.");

                    mPrinter.printFeed();
                    mPrinter.printFeed();
                    mPrinter.printString("================================================");

                    mPrinter.printFeed();
                    mPrinter.printFeed();
                    mPrinter.printString("Terima Kasih");
                    mPrinter.printFeed();
                    mPrinter.printString("Scan QR ini untuk download aplikasi MEDIV.");
                    mPrinter.printFeed();
                    mPrinter.printString("Anda bisa merasakan kemudahan dan keuntungan");
                    mPrinter.printFeed();
                    mPrinter.printString("berbelanja di MEDIV.");
                    mPrinter.printFeed();
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
