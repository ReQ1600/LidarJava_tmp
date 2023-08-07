package com.lidar.app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;

public class MainActivity extends AppCompatActivity {

    private Button btnStart;
    private ImageView ivDisplay;
    private TableLayout tblPoints;
    private boolean isTableShowed;

    private enum ScanningState
    {
        STARTED,
        NO_DEVICE,
        ERROR
    }

    //if no connected device or some shit happens return false;
    private ScanningState BeginScanning()
    {
        //everything working
        return ScanningState.STARTED;
        //if no device found

        //default return (shouldn't happen)
        //return ScanningState.ERROR;
    }

    private void ShowTable()
    {
        //tblPoints.setVisibility(TableLayout.VISIBLE);
        tblPoints.animate().translationYBy(-tblPoints.getHeight());
        isTableShowed = true;
    }

    private void HideTable()
    {
        tblPoints.animate().translationYBy(tblPoints.getHeight());
        //tblPoints.setVisibility(TableLayout.INVISIBLE);
        isTableShowed = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        btnStart = (Button) findViewById(R.id.btnStart);
        ivDisplay = (ImageView) findViewById(R.id.ivDisplay);
        tblPoints = (TableLayout) findViewById(R.id.tblPoints);

        tblPoints.post(new Runnable() {
            @Override
            public void run() {
                tblPoints.setTranslationY(tblPoints.getHeight());
            }
        });

        //setting up dialog box builder for errors
        AlertDialog.Builder dlgConnectionError_builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        dlgConnectionError_builder.setMessage("Bluetooth connection error.\nMake sure the device is connected.");
        dlgConnectionError_builder.setTitle("Connection Error");
        dlgConnectionError_builder.setNeutralButton("OK",null);

        AlertDialog.Builder dlgUnexpectedError_builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        dlgUnexpectedError_builder.setMessage("An Unexpected error occured.\nThis shouldn't have happened.");
        dlgUnexpectedError_builder.setTitle("Unexpected Error");
        dlgUnexpectedError_builder.setNeutralButton("OK",null);



        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            //sends out starting signal into lidar
            public void onClick(View view) {
                //checking if scanning has began
                ScanningState scanningState = BeginScanning();
                if (scanningState != ScanningState.STARTED)
                {
                    AlertDialog err_dlg = scanningState == ScanningState.ERROR ? dlgUnexpectedError_builder.create() : dlgConnectionError_builder.create();
                    err_dlg.show();

                    //centering the ok button
                    LinearLayout LayoutOfParent = (LinearLayout) err_dlg.getButton(AlertDialog.BUTTON_NEUTRAL).getParent();
                    LayoutOfParent.setGravity(Gravity.CENTER_HORIZONTAL);
                    LayoutOfParent.getChildAt(1).setVisibility(View.GONE);
                }
                else
                {
                    Bitmap bitmap = Bitmap.createBitmap(ivDisplay.getWidth(), ivDisplay.getHeight(),Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);

                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.FILL);

                    canvas.drawCircle(bitmap.getWidth()/2,bitmap.getHeight()/2,10,paint);
                    ivDisplay.setImageBitmap(bitmap);

                    if(isTableShowed) HideTable();
                    else ShowTable();

                }
            }
        });
    }



}