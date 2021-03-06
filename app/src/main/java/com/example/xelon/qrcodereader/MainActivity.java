/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 *
 * Created by lisah0 on 2012-02-24
 */
package com.example.xelon.qrcodereader;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.example.xelon.qrcodereader.libs.NxtBluetoothController;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends Activity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    Button connectButton;
    EditText nxtName;

    NxtBluetoothController nxt;

    ImageScanner scanner;

    private int y = 0, m = 0, d = 0;

    private boolean barcodeScanned = false;
    private boolean previewing = true;

    private Thread th;
    private Date date1;
    private FrameLayout preview;

    static {
        System.loadLibrary("iconv");
    }

    //инициализация объектов (компоненты активити) и переменных

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyy/MM/dd");
        String strDate = mdformat.format(calendar.getTime());
        String[] arr = strDate.split("/");
        date1 = new Date();
        date1.setYear(Integer.parseInt(arr[0]));
        date1.setMonth(Integer.parseInt(arr[1]));
        date1.setDate(Integer.parseInt(arr[2]));

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        //инциализация сканнера QR кода
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        //инициализцая компонентов активити
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
        nxtName = (EditText) findViewById(R.id.editText);

        connectButton = (Button) findViewById(R.id.button);
        connectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                nxt = new NxtBluetoothController(NxtBluetoothController.findDeviceByName(nxtName.getText().toString()));
                try {
                    nxt.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //создание нового потока для постостоянного сканирования кода

        th = new Thread(new Runnable() {
            @Override
            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        while (true) {
//                            if (preview.getVisibility() == View.GONE || preview.getVisibility() == View.INVISIBLE) {
//                                preview.setVisibility(View.VISIBLE);
//                            }
//                        }
//                    }
//                });
                while (true) {

                    if (barcodeScanned) {
                        barcodeScanned = false;
                        mCamera.setPreviewCallback(previewCb);
                        mCamera.startPreview();
                        previewing = true;
                        mCamera.autoFocus(autoFocusCB);
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th.start();
    }

    //обработка событий приложеня

    public void onPause() {
        super.onPause();
        //th.stop();
        releaseCamera();
    }

    public void onResume() {
        super.onResume();
        //th.start();
    }

    //инициализация камеры

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
        }
        return c;
    }

    //условие отчиски камеры

    private void releaseCamera() {
        if (mCamera != null) {

            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    boolean stat = true;

    //обработка события на камере

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);
            stat = true;
            //если найден QR код
            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                SymbolSet syms = scanner.getResults();
                String text = null;
                for (Symbol sym : syms) {
                    text = sym.getData();
                    Log.d("QRS", text);
                    String[] strs = text.toString().split("\\.");
                    if (text != null && strs.length == 3) {
                        Date d = new Date();
                        d.setYear(Integer.parseInt(strs[2]));
                        d.setMonth(Integer.parseInt(strs[1]));
                        d.setDate(Integer.parseInt(strs[1]));

                        if (d.getTime() > date1.getTime()) {
                            /*try {
                                nxt.sendDirectCommand(new byte[]{(byte) 0x80, (byte) 0x09, 0, 58, 1});
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }*/
                            Log.d("BT", "sent 1!" + stat);
                        } else {
                            /*try {
                                nxt.sendDirectCommand(new byte[]{(byte) 0x80, (byte) 0x09, 0, 58, 0});
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }*/
                            preview.setVisibility(View.GONE);
                            stat = false;
                            Log.d("BT", "sent 0!" + stat);
                        }
                    }

                    barcodeScanned = true;
                    stat = true;
                }

            } else {
                preview.setVisibility(View.VISIBLE);
            }
        }
    };


    // Объявление использования автофокуса
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
}
