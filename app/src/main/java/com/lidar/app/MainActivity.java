package com.lidar.app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.icu.number.Precision;
import android.icu.text.AlphabeticIndex;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import java.io.Console;
import java.lang.Math;
public class MainActivity extends AppCompatActivity {

    private static final double DEG = 1.8;//will maybe make it selectable not now
    private static final int NUMBER_OF_POINTS = (int) (360 / DEG);//must be a whole number
    private static final double ANGLE_INCREASE_RAD = Math.toRadians(DEG);
    private static final CosSin COS_SIN_TABLE = new CosSin();
    //no idea how to do it better in java, there's no preprocessor :G

    private Button btnStart = null;
    private ImageView ivDisplay = null;
    private TableLayout tblPoints = null;
    private TableLayout tblBtn = null;
    private boolean isTableShowed = true;
    private boolean didTheLidarRunAtLeastOnce = false;//long

    private static final class CosSin
    {
        public CosSin()
        {
            for(double[] row : m_table)
            {
                row[0] = Math.round(Math.cos(angleRad) * 10000) / 10000d;
                row[1] = Math.round(-Math.sin(angleRad) * 10000) / 10000d;
                angleRad += ANGLE_INCREASE_RAD;
            }
        }
        public double Get(int x,int y)
        {
            return m_table[x][y];
        }
        public void Test()
        {
            angleRad = 0;
            double deg = 0;

            for(double[] row : m_table)
            {
                System.out.println("deg: " + deg + ", rad: " + angleRad + ", cos: " + row[0] + ", sin: " + row[1]);

                angleRad += ANGLE_INCREASE_RAD;
                deg+=DEG;
            }
        }
        private double m_table[][] = new double[NUMBER_OF_POINTS][2];
        private double angleRad = 0;
    }

    private record LidarPoint(int id, double x, double y, double distance) { }

    private LidarPoint CreateLidarPoint(int id, double distance)
    {
        double x = ivDisplay.getWidth() / 2f + COS_SIN_TABLE.Get(id,1) * distance;
        double y = ivDisplay.getHeight() / 2f + COS_SIN_TABLE.Get(id,0) * distance;
        return new LidarPoint(id,x,y,distance);
    }
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
        tblPoints.setVisibility(TableLayout.VISIBLE);
        tblPoints.animate().translationY(0).setListener(null);
        tblBtn.setVisibility(TableLayout.GONE);
        isTableShowed = true;
    }

    private void HideTable()
    {
        tblPoints.animate().translationY(tblPoints.getHeight()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                tblPoints.setVisibility(TableLayout.GONE);
                tblBtn.setVisibility(TableLayout.VISIBLE);
            }
        });

        isTableShowed = false;
    }

    //TO DO: this V
    private void addTableElement(LidarPoint point)
    {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        btnStart = findViewById(R.id.btnStart);
        ivDisplay = findViewById(R.id.ivDisplay);
        tblPoints = findViewById(R.id.tblPoints);
        tblBtn = findViewById(R.id.tblBtn);

        tblPoints.post(() -> {
            tblPoints.animate().translationY(tblPoints.getHeight());
            tblPoints.setVisibility(LinearLayout.GONE);
            tblBtn.setVisibility(TableLayout.VISIBLE);
            isTableShowed = false;
        });//this needs to be like that bc the first time it shows up the height is measured as 0 cuz its "gone"



        //setting up dialog box builder for errors
        AlertDialog.Builder dlgConnectionError_builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        dlgConnectionError_builder.setMessage("Bluetooth connection error.\nMake sure the device is connected.");
        dlgConnectionError_builder.setTitle("Connection Error");
        dlgConnectionError_builder.setNeutralButton("OK",null);

        AlertDialog.Builder dlgUnexpectedError_builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        dlgUnexpectedError_builder.setMessage("An Unexpected error occured.\nThis shouldn't have happened.");
        dlgUnexpectedError_builder.setTitle("Unexpected Error");
        dlgUnexpectedError_builder.setNeutralButton("OK",null);

        btnStart.setOnClickListener((view) -> {
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

                    canvas.drawCircle(bitmap.getWidth()/2f,bitmap.getHeight()/2f,10,paint);
                    ivDisplay.setImageBitmap(bitmap);

                    if(!isTableShowed) ShowTable();

                }
        });

        ivDisplay.setOnClickListener((view) -> {
            if(isTableShowed) HideTable();
            else ShowTable();

        });
    }



}