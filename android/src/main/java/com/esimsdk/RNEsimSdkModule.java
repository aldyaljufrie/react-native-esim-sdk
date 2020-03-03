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
                String encodedImage = "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I1ByY0BZAAAAAAAAAAAAEGNyb3BXaGVuUHJpbnRpbmdib29sAAAAAA5jcm9wUmVjdEJvdHRvbWxvbmcAAAAAAAAADGNyb3BSZWN0TGVmdGxvbmcAAAAAAAAADWNyb3BSZWN0UmlnaHRsb25nAAAAAAAAAAtjcm9wUmVjdFRvcGxvbmcAAAAAADhCSU0D7QAAAAAAEAEsAAAAAQACASwAAAABAAI4QklNBCYAAAAAAA4AAAAAAAAAAAAAP4AAADhCSU0EDQAAAAAABAAAAB44QklNBBkAAAAAAAQAAAAeOEJJTQPzAAAAAAAJAAAAAAAAAAABADhCSU0nEAAAAAAACgABAAAAAAAAAAI4QklNA/UAAAAAAEgAL2ZmAAEAbGZmAAYAAAAAAAEAL2ZmAAEAoZmaAAYAAAAAAAEAMgAAAAEAWgAAAAYAAAAAAAEANQAAAAEALQAAAAYAAAAAAAE4QklNA/gAAAAAAHAAAP////////////////////////////8D6AAAAAD/////////////////////////////A+gAAAAA/////////////////////////////wPoAAAAAP////////////////////////////8D6AAAOEJJTQQAAAAAAAACAAA4QklNBAIAAAAAAAIAADhCSU0EMAAAAAAAAQEAOEJJTQQtAAAAAAAGAAEAAAACOEJJTQQIAAAAAAAQAAAAAQAAAkAAAAJAAAAAADhCSU0EHgAAAAAABAAAAAA4QklNBBoAAAAAA0kAAAAGAAAAAAAAAAAAAABAAAAASAAAAAoAbABvAGcAbwBfAGIAbABhAGMAawAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAASAAAAEAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAQAAAAAAAG51bGwAAAACAAAABmJvdW5kc09iamMAAAABAAAAAAAAUmN0MQAAAAQAAAAAVG9wIGxvbmcAAAAAAAAAAExlZnRsb25nAAAAAAAAAABCdG9tbG9uZwAAAEAAAAAAUmdodGxvbmcAAABIAAAABnNsaWNlc1ZsTHMAAAABT2JqYwAAAAEAAAAAAAVzbGljZQAAABIAAAAHc2xpY2VJRGxvbmcAAAAAAAAAB2dyb3VwSURsb25n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cC8xLjAvIiB4bWxuczpkYz0iaHR0cDovL3B1cmwub3JnL2RjL2VsZW1lbnRzLzEuMS8iIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0RXZ0PSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VFdmVudCMiIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRlRGF0ZT0iMjAxOS0xMi0yMFQwOTo0NDozNyswNzowMCIgeG1wOk1vZGlmeURhdGU9IjIwMjAtMDMtMDNUMTE6MzI6MTIrMDc6MDAiIHhtcDpNZXRhZGF0YURhdGU9IjIwMjAtMDMtMDNUMTE6MzI6MTIrMDc6MDAiIGRjOmZvcm1hdD0iaW1hZ2UvanBlZyIgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIgcGhvdG9zaG9wOklDQ1Byb2ZpbGU9InNSR0IgSUVDNjE5NjYtMi4xIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOmQ5MGZjYjFmLTA3MTgtNDYzMC05OTA0LTliMjY2NGFmMTNmOCIgeG1wTU06RG9jdW1lbnRJRD0iYWRvYmU6ZG9jaWQ6cGhvdG9zaG9wOjM0Y2JlOGQ0LWM1YmItMDI0Yy05MjczLWZlMzkxYjExNjkwMSIgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOmFjM2YzYmNjLWRlYWYtNGI0ZC1hZjI0LTM4NTgwNTAwMTIyZCI+IDx4bXBNTTpIaXN0b3J5PiA8cmRmOlNlcT4gPHJkZjpsaSBzdEV2dDphY3Rpb249InNhdmVkIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOmFjM2YzYmNjLWRlYWYtNGI0ZC1hZjI0LTM4NTgwNTAwMTIyZCIgc3RFdnQ6d2hlbj0iMjAyMC0wMy0wM1QxMTozMTo1OCswNzowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTkgKE1hY2ludG9zaCkiIHN0RXZ0OmNoYW5nZWQ9Ii8iLz4gPHJkZjpsaSBzdEV2dDphY3Rpb249InNhdmVkIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOjYxMmRkYmQwLWY5NDgtNDFhOC1hYWZiLWNiNjJmZWY1MjUxYSIgc3RFdnQ6d2hlbj0iMjAyMC0wMy0wM1QxMTozMjoxMiswNzowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTkgKE1hY2ludG9zaCkiIHN0RXZ0OmNoYW5nZWQ9Ii8iLz4gPHJkZjpsaSBzdEV2dDphY3Rpb249ImNvbnZlcnRlZCIgc3RFdnQ6cGFyYW1ldGVycz0iZnJvbSBpbWFnZS9wbmcgdG8gaW1hZ2UvanBlZyIvPiA8cmRmOmxpIHN0RXZ0OmFjdGlvbj0iZGVyaXZlZCIgc3RFdnQ6cGFyYW1ldGVycz0iY29udmVydGVkIGZyb20gaW1hZ2UvcG5nIHRvIGltYWdlL2pwZWciLz4gPHJkZjpsaSBzdEV2dDphY3Rpb249InNhdmVkIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOmQ5MGZjYjFmLTA3MTgtNDYzMC05OTA0LTliMjY2NGFmMTNmOCIgc3RFdnQ6d2hlbj0iMjAyMC0wMy0wM1QxMTozMjoxMiswNzowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTkgKE1hY2ludG9zaCkiIHN0RXZ0OmNoYW5nZWQ9Ii8iLz4gPC9yZGY6U2VxPiA8L3htcE1NOkhpc3Rvcnk+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjYxMmRkYmQwLWY5NDgtNDFhOC1hYWZiLWNiNjJmZWY1MjUxYSIgc3RSZWY6ZG9jdW1lbnRJRD0ieG1wLmRpZDphYzNmM2JjYy1kZWFmLTRiNGQtYWYyNC0zODU4MDUwMDEyMmQiIHN0UmVmOm9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDphYzNmM2JjYy1kZWFmLTRiNGQtYWYyNC0zODU4MDUwMDEyMmQiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+ICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgPD94cGFja2V0IGVuZD0idyI/Pv/iDFhJQ0NfUFJPRklMRQABAQAADEhMaW5vAhAAAG1udHJSR0IgWFlaIAfOAAIACQAGADEAAGFjc3BNU0ZUAAAAAElFQyBzUkdCAAAAAAAAAAAAAAABAAD21gABAAAAANMtSFAgIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEWNwcnQAAAFQAAAAM2Rlc2MAAAGEAAAAbHd0cHQAAAHwAAAAFGJrcHQAAAIEAAAAFHJYWVoAAAIYAAAAFGdYWVoAAAIsAAAAFGJYWVoAAAJAAAAAFGRtbmQAAAJUAAAAcGRtZGQAAALEAAAAiHZ1ZWQAAANMAAAAhnZpZXcAAAPUAAAAJGx1bWkAAAP4AAAAFG1lYXMAAAQMAAAAJHRlY2gAAAQwAAAADHJUUkMAAAQ8AAAIDGdUUkMAAAQ8AAAIDGJUUkMAAAQ8AAAIDHRleHQAAAAAQ29weXJpZ2h0IChjKSAxOTk4IEhld2xldHQtUGFja2FyZCBDb21wYW55AABkZXNjAAAAAAAAABJzUkdCIElFQzYxOTY2LTIuMQAAAAAAAAAAAAAAEnNSR0IgSUVDNjE5NjYtMi4xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABYWVogAAAAAAAA81EAAQAAAAEWzFhZWiAAAAAAAAAAAAAAAAAAAAAAWFlaIAAAAAAAAG+iAAA49QAAA5BYWVogAAAAAAAAYpkAALeFAAAY2lhZWiAAAAAA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";

                // user objects
                String userName = userInfo.getString("name");
                String userAddress = userInfo.getString("alamat");
                String userPhone = userInfo.getString("phone");

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
                String diskonOngkir = cost.getString("diskonOngkir");
                String logisticPrice = cost.getString("logisticPrice");
                String totalAll = cost.getString("totalAll");

                // payment objects
                String orderCode = paymentType.getString("order");
                String payment = paymentType.getString("type");
                String paymentData = paymentType.getString("data");
                String bank = paymentType.getString("bank");
                String paymentString = (payment == "nicepay_va") ? "Transver VA ( " + paymentData + " )" : paymentData;

                if (!mPrinter.isConnect()) {
                    ShowMsg("DISCONNECTED");
                    return;
                }
                try {
                    mPrinter.setFontStyle(0);
                    mPrinter.setAlignMode(1);
                    try {
                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        mPrinter.printRasterBitmap(decodedByte);
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

                    mPrinter.printFeed();
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
                        String brandName = rmItem.getString("brandName");
                        String productName = rmItem.getString("productName");
                        String qty = rmItem.getString("qty");
                        String price = rmItem.getString("price");

                        mPrinter.setAlignMode(0);
                        mPrinter.printString(qty + " X " + (brandName.length() >= 50 ? brandName.substring(0, 45) + "..." : brandName));
                        mPrinter.printFeed();
                        mPrinter.printString("  " + (productName.length() >= 50 ? productName.substring(0, 45) + "..." : productName));
                        mPrinter.printFeed();

                        mPrinter.setAlignMode(2);
                        mPrinter.printString("Rp." + price);
                        mPrinter.printFeed();
                    }

                    mPrinter.setAlignMode(1);
                    mPrinter.printString("------------------------------------------------");
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(2);
                    mPrinter.printString("Subtotal : Rp." + totalPrice);
                    mPrinter.printFeed();
                    mPrinter.printString("Biaya Antar : Rp." + logisticPrice);
                    mPrinter.printFeed();
                    mPrinter.printString("Diskon : -Rp." + diskon);
                    mPrinter.printFeed();
                    mPrinter.printString("Diskon Ongkir : -Rp." + diskonOngkir);
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(1);
                    mPrinter.printString("------------------------------------------------");
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(2);
                    mPrinter.printString("Total : Rp." + totalAll);
                    mPrinter.printFeed();
                    mPrinter.printString("Harga sudah termasuk PPN 10%");
                    mPrinter.printFeed();
                    mPrinter.printFeed();

                    mPrinter.setAlignMode(1);
                    mPrinter.printString("================================================");
                    mPrinter.printFeed();
                    mPrinter.printFeed();

                    mPrinter.setFontStyle(1);
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
                    mPrinter.setFontStyle(0);

                    mPrinter.printFeed();
                    mPrinter.printFeed();
                    mPrinter.setAlignMode(1);
                    mPrinter.printString("================================================");
                    mPrinter.printFeed();
                    mPrinter.printFeed();

                    mPrinter.setFontStyle(1);
                    mPrinter.printString("Cek Email Anda untuk melihat detail pembelanjaan Anda.");
                    mPrinter.setFontStyle(0);

                    mPrinter.printFeed();
                    mPrinter.printFeed();
                    mPrinter.printString("================================================");
                    mPrinter.setFontStyle(1);

                    mPrinter.printFeed();
                    mPrinter.printFeed();

                    String qrCode = "https://play.google.com/store/apps/details?id=com.kimiafarma.mediv&hl=en";
                    mPrinter.printQRCode(qrCode, 4, false);

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
