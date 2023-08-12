package com.lidar.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.jetbrains.annotations.TestOnly;

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
    private Canvas DisplayCanvas = null;
    private Paint DisplayPaint = null;
    private Bitmap DisplayBitmap = null;
    private boolean isTableShowed = true;
    private boolean didTheLidarRunAtLeastOnce = false;//long
    private float ButtonMaxAlpha = 1f;
    private double PointsCords[][] = null;
    private boolean canDisplayBeTouched = true;
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
        public double Get(@NonNull int x, @NonNull int y)
        {
            return m_table[x][y];
        }
        @TestOnly
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

    //Creates lidar point, sets proper cords and adds them to the table and display
    private LidarPoint CreateLidarPoint(@NonNull int id, @NonNull double distance)
    {
        //rotating point around the center
        double x = ivDisplay.getWidth() / 2f + COS_SIN_TABLE.Get(id,1) * distance;
        double y = ivDisplay.getHeight() / 2f + COS_SIN_TABLE.Get(id,0) * distance;

        //addding cords to the their table and display
        PointsCords[id][0] = x;
        PointsCords[id][1] = y;
        DisplayCanvas.drawCircle((float)x,(float)y,4*getResources().getDisplayMetrics().density,DisplayPaint);
        ivDisplay.setImageBitmap(DisplayBitmap);

        //adding points to gui table
        LidarPoint point = new LidarPoint(id,x,y,distance);
        addTableElement(point);

        return point;
    }
    private enum ScanningState
    {
        STARTED,
        NO_DEVICE,
        ERROR
    }

    ////TODO: bluettoth connencting
    //if no connected device or some shit happens returns false;
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
        AlphaAnimation animation = new AlphaAnimation(ButtonMaxAlpha,0f);
        animation.setDuration(300);
        animation.setFillAfter(true);
        tblBtn.startAnimation(animation);
        tblPoints.setVisibility(TableLayout.VISIBLE);
        tblPoints.animate().translationY(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                canDisplayBeTouched = true;
            }
        });
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
                canDisplayBeTouched = true;
            }
        });
        AlphaAnimation alphaAnimation = new AlphaAnimation(0f,ButtonMaxAlpha);
        alphaAnimation.setDuration(300);
        alphaAnimation.setFillAfter(true);
        tblBtn.startAnimation(alphaAnimation);

        isTableShowed = false;
    }

    private void addTableElement(@NonNull LidarPoint point)
    {
        final float density = getResources().getDisplayMetrics().density;
        final int paddingInDP = (int)(10 * density);

        TableRow newRow = new TableRow(this);
        TextView txt_id = new TextView(this);
        TextView txt_distance = new TextView(this);
        View newSpacer = new View(this);

        newRow.setGravity(Gravity.CENTER_HORIZONTAL);
        newRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        txt_id.setText(Integer.toString(point.id));
        txt_id.setTextColor(Color.WHITE);
        txt_id.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);
        txt_id.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        txt_id.setPadding(paddingInDP, paddingInDP, paddingInDP, paddingInDP);
        newRow.addView(txt_id);

        txt_distance.setText(Integer.toString(point.id)+"mm");
        txt_distance.setTextColor(Color.WHITE);
        txt_distance.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);
        txt_distance.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        txt_distance.setPadding(paddingInDP, paddingInDP, paddingInDP, paddingInDP);
        newRow.addView(txt_distance);
        //TODO: onclcklistener V
        newRow.setOnClickListener((view)->{});
        tblPoints.addView(newRow);

        tblPoints.addView(newSpacer);
        newSpacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (2*density)));
        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) newSpacer.getLayoutParams();
        marginParams.setMargins((int)(12*density),0,(int)(12*density),0);
        newSpacer.setLayoutParams(marginParams);
        newSpacer.setBackgroundColor(Color.parseColor("#232323"));
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

        ivDisplay.post(() -> {
            DisplayBitmap = Bitmap.createBitmap(ivDisplay.getWidth(), ivDisplay.getHeight(),Bitmap.Config.ARGB_8888);
            DisplayCanvas = new Canvas(DisplayBitmap);
            DisplayPaint = new Paint();
            DisplayPaint.setColor(Color.BLACK);
            DisplayPaint.setStyle(Paint.Style.FILL);
        });//canvas and else need to be created after ImageView

//        TableRow r = (TableRow) tblPoints.getChildAt(1);
//        TextView a = (TextView) r.getChildAt(0);
//        a.setText("aaaa");



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
            COS_SIN_TABLE.Test();
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
                   PointsCords = new double[NUMBER_OF_POINTS][2];
                   LidarPoint p = CreateLidarPoint(0,1);
                    //drawing the center
                    DisplayPaint.setColor(Color.CYAN);
                    DisplayCanvas.drawCircle(DisplayCanvas.getWidth()/2f,DisplayCanvas.getHeight()/2f,4*getResources().getDisplayMetrics().density,DisplayPaint);
                    DisplayPaint.setColor(Color.BLACK);
                    ivDisplay.setImageBitmap(DisplayBitmap);

                    if(!isTableShowed) ShowTable();

                    //disabling the start button
                    btnStart.setClickable(false);
                    btnStart.setAlpha(0.5f);
                    ButtonMaxAlpha = 0.5f;
                }
        });

        //TODO: on touch listener below
        ivDisplay.setOnTouchListener((view, motionEvent) -> {
            //checks whether one of the points was pressed if yes then selects it, if none was touched then hides the table




            if (!canDisplayBeTouched || PointsCords == null) return false;
            //disables clickability for a short while so no bugs will happen gets reenabled when table animation finishes
            canDisplayBeTouched = false;
            final float density = getResources().getDisplayMetrics().density;
            for (double[] cords : PointsCords)
            {
                if (cords[0]==0||cords[1]==0) break;//no point will have a 0 cord so it's an end if it sees it so it doesn't loop unneeded
                //creating a hitbox
                if (cords[0] >= motionEvent.getX()-10*density && cords[0] <= motionEvent.getX()+10*density)
                {
                    if(!isTableShowed) ShowTable();
                    //TODO: create function that highlits selected point in gui table and display
                    return false;
                }
            }
            if(isTableShowed) HideTable();
            else ShowTable();
            return false;
        });
    }
}