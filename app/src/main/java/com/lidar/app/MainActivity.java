package com.lidar.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.lang.Math;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {

    private static final double DEG = 3.6;//will maybe make it selectable not now
    private static final int NUMBER_OF_POINTS = (int) (360 / DEG);//must be a whole number
    private static final double ANGLE_INCREASE_RAD = Math.toRadians(DEG);
    private static final CosSin COS_SIN_TABLE = new CosSin();
    //no idea how to do it better in java

    private static final String TAG = "LIDAR_APP";
    private static final int REQUEST_EN_BT = 1;
    private static final int ERROR_READ = 0;//for bt handler to update message
    BluetoothDevice Lidar = null;
    UUID LidarUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//default uuid

    private Button btnStart = null;
    private Button btnBTCnct = null;
    private ImageView ivDisplay = null;
    private TableLayout tblPoints = null;
    private TableLayout tblBtn = null;
    private ScrollView tblScroll = null;
    private Canvas DisplayCanvas = null;
    private Paint DisplayPaint = null;
    private Bitmap DisplayBitmap = null;
    private double[] LastSelectedPointCords = null;
    private Integer LastSelectedPointID = null; //not Point id but table child id
    private boolean isTableShowed = true;
    private float ButtonMaxAlpha = 1f;
    private double[][] PointsCords = null;
    private boolean canDisplayBeTouched = true;
    private Handler handler = null;

    @TestOnly
    private void PopulatePointsWithRandomShit()
    {
        for (int i = 0; i < NUMBER_OF_POINTS; ++i)
        {
            CreateLidarPoint(i, ThreadLocalRandom.current().nextInt(400,501));
        }
    };
    private static final class CosSin {
        public CosSin() {
            for (double[] row : m_table) {
                row[0] = Math.round(Math.cos(angleRad) * 10000) / 10000d;
                row[1] = Math.round(-Math.sin(angleRad) * 10000) / 10000d;
                angleRad += ANGLE_INCREASE_RAD;
            }
        }

        public double Get(int x, int y) {
            return m_table[x][y];
        }

        @TestOnly
        public void Test() {
            angleRad = 0;
            double deg = 0;

            for (double[] row : m_table) {
                System.out.println("deg: " + deg + ", rad: " + angleRad + ", cos: " + row[0] + ", sin: " + row[1]);

                angleRad += ANGLE_INCREASE_RAD;
                deg += DEG;
            }
        }

        private final double[][] m_table = new double[NUMBER_OF_POINTS][2];
        private double angleRad = 0;
    }

    private record LidarPoint(int id, double x, double y, double distance) {}

    //Creates lidar point, sets proper cords and adds it to the table and displays
    private void CreateLidarPoint(int id, double distance) {
        //rotating point around the center
        double x = ivDisplay.getWidth() / 2f + COS_SIN_TABLE.Get(id, 1) * -distance;
        double y = ivDisplay.getHeight() / 2f + COS_SIN_TABLE.Get(id, 0) * -distance;

        //adding cords to the their table and display
        PointsCords[id][0] = x;
        PointsCords[id][1] = y;
        DisplayCanvas.drawCircle((float) x, (float) y, 4 * getResources().getDisplayMetrics().density, DisplayPaint);
        ivDisplay.setImageBitmap(DisplayBitmap);
        ivDisplay.setImageBitmap(DisplayBitmap);

        //adding points to gui table
        LidarPoint point = new LidarPoint(id, x, y, distance);
        addTableElement(point);
    }

    private void ConnectPoints()
    {
        Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(4 * getResources().getDisplayMetrics().density);

        for(int id = 0; id<NUMBER_OF_POINTS; ++id)
        {
            if (id + 1 == NUMBER_OF_POINTS) DisplayCanvas.drawLine((float)PointsCords[id][0], (float)PointsCords[id][1],(float)PointsCords[0][0],(float)PointsCords[0][1],paint);
            else DisplayCanvas.drawLine((float)PointsCords[id][0], (float)PointsCords[id][1],(float)PointsCords[id+1][0],(float)PointsCords[id+1][1],paint);
            //drawing the point on top of the line
            DisplayCanvas.drawCircle((float)PointsCords[id][0],(float)PointsCords[id][1],4 * getResources().getDisplayMetrics().density, DisplayPaint);
        }
    }

    private enum ScanningState {
        STARTED,
        NO_DEVICE,
        BLUETOOTH_UNSUPPORTED,
        ERROR
    }

    //TODO: check bluetooth connecting
    //if no connected device or some shit happens returns false;
    private ScanningState BeginScanning() {
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager.getAdapter();
        BluetoothSocket socket = null;

        //if the device doesn't support bluetooth
        if (adapter == null) return ScanningState.BLUETOOTH_UNSUPPORTED;

        //if bluetooth is disabled
        if (!adapter.isEnabled()) return ScanningState.NO_DEVICE;

        //if no device found
        Set<BluetoothDevice> pairedDevices = null;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            pairedDevices = adapter.getBondedDevices();
        }
        if(pairedDevices == null || pairedDevices.size() == 0) return ScanningState.NO_DEVICE;

        //everything working
        if(pairedDevices.size() > 0)
        {

            for (BluetoothDevice device : pairedDevices)
            {
                if (device.getName().equals("Lidar"))
                {
                    try
                    {
                        socket = device.createRfcommSocketToServiceRecord(UUID.randomUUID());
                        adapter.cancelDiscovery();
                    }catch(IOException e)
                    {
                        Log.e("LIDAR_TAG", "Socket creation failed", e);
                    }

                    try
                    {
                        socket.connect();
                    }catch(IOException e)
                    {
                        try
                        {
                            socket.close();
                        }catch(IOException closeE)
                        {
                            Log.e("LIDAR_TAG", "Could not close the client socket", closeE);
                        }
                        return ScanningState.ERROR;
                    }
                }
            }
            return ScanningState.STARTED;
        }

        //default return (shouldn't happen)
        return ScanningState.ERROR;
    }

    private void ShowTable()
    {
        AlphaAnimation animation = new AlphaAnimation(ButtonMaxAlpha,0f);
        animation.setDuration(300);
        animation.setFillAfter(true);
        tblBtn.startAnimation(animation);
        tblScroll.setVisibility(TableLayout.VISIBLE);
        tblScroll.animate().translationY(0).setListener(new AnimatorListenerAdapter() {
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
        tblScroll.animate().translationY(tblScroll.getHeight()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                tblScroll.setVisibility(TableLayout.GONE);
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

        txt_id.setText(String.valueOf(point.id));
        txt_id.setTextColor(Color.WHITE);
        txt_id.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);
        txt_id.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        txt_id.setPadding(paddingInDP, paddingInDP, paddingInDP, paddingInDP);
        newRow.addView(txt_id);

        txt_distance.setText(String.format(Locale.UK,"%.0fmm",point.distance));
        txt_distance.setTextColor(Color.WHITE);
        txt_distance.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);
        txt_distance.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        txt_distance.setPadding(paddingInDP, paddingInDP, paddingInDP, paddingInDP);
        newRow.addView(txt_distance);

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
        btnBTCnct = findViewById(R.id.btnBTCnct);
        ivDisplay = findViewById(R.id.ivDisplay);
        tblPoints = findViewById(R.id.tblPoints);
        tblBtn = findViewById(R.id.tblBtn);
        tblScroll = findViewById(R.id.tblScroll);

        tblScroll.post(() -> {
            tblScroll.animate().translationY(tblScroll.getHeight());
            tblScroll.setVisibility(LinearLayout.GONE);
            tblBtn.setVisibility(TableLayout.VISIBLE);
            isTableShowed = false;
        });//needs to be like that bc the first time it shows up the height is measured as 0 cuz its "gone"

        ivDisplay.post(() -> {
            DisplayBitmap = Bitmap.createBitmap(ivDisplay.getWidth(), ivDisplay.getHeight(),Bitmap.Config.ARGB_8888);
            DisplayCanvas = new Canvas(DisplayBitmap);
            DisplayPaint = new Paint();
            DisplayPaint.setColor(Color.BLACK);
            DisplayPaint.setStyle(Paint.Style.FILL);
        });//canvas and else must be created after ImageView


        //setting up dialog box builder for errors
        AlertDialog.Builder dlgConnectionError_builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        dlgConnectionError_builder.setMessage("Bluetooth connection error.\nMake sure bluetooth is enabled and the device is connected.");
        dlgConnectionError_builder.setTitle("Connection Error");
        dlgConnectionError_builder.setNeutralButton("OK",null);

        AlertDialog.Builder dlgUnexpectedError_builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        dlgUnexpectedError_builder.setMessage("An Unexpected error occurred.\nThis shouldn't have happened.");
        dlgUnexpectedError_builder.setTitle("Unexpected Error");
        dlgUnexpectedError_builder.setNeutralButton("OK",null);

        AlertDialog.Builder dlgBluetoothUnsupportedError_builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        dlgUnexpectedError_builder.setMessage("This device doesn't support bluetooth");
        dlgUnexpectedError_builder.setTitle("Bluetooth Error");
        dlgUnexpectedError_builder.setNeutralButton("OK",null);

        btnStart.setOnClickListener((view) -> {
                //checking if scanning has began
                ScanningState scanningState = ScanningState.STARTED;//BeginScanning(); //<-TODO
                if (scanningState != ScanningState.STARTED)
                {
                    AlertDialog err_dlg = null;
                    switch (scanningState) {
                        case ERROR -> err_dlg = dlgUnexpectedError_builder.create();
                        case NO_DEVICE -> err_dlg = dlgConnectionError_builder.create();
                        case BLUETOOTH_UNSUPPORTED -> err_dlg = dlgBluetoothUnsupportedError_builder.create();
                        default -> {}
                    }
                    err_dlg.show();

                    //centering the ok button
                    LinearLayout LayoutOfParent = (LinearLayout) err_dlg.getButton(AlertDialog.BUTTON_NEUTRAL).getParent();
                    LayoutOfParent.setGravity(Gravity.CENTER_HORIZONTAL);
                    LayoutOfParent.getChildAt(1).setVisibility(View.GONE);
                }
                else
                {
                    //clearing the canvas
                    DisplayPaint.setColor(Color.WHITE);
                    DisplayCanvas.drawPaint(DisplayPaint);

                    //drawing the center
                    DisplayPaint.setColor(Color.YELLOW);
                    DisplayCanvas.drawCircle(DisplayCanvas.getWidth()/2f,DisplayCanvas.getHeight()/2f,4*getResources().getDisplayMetrics().density,DisplayPaint);
                    DisplayPaint.setColor(Color.BLACK);
                    ivDisplay.setImageBitmap(DisplayBitmap);


                   PointsCords = new double[NUMBER_OF_POINTS][2];

                   PopulatePointsWithRandomShit();
                   ConnectPoints();

                    //disabling the start button
                    btnStart.setClickable(false);
                    btnStart.setAlpha(0.5f);
                    ButtonMaxAlpha = 0.5f;

                    //this'll need to happen when everything is measured and drawn
                    btnStart.setClickable(true);
                    btnStart.setAlpha(1f);
                    ButtonMaxAlpha = 1f;



                    if(!isTableShowed) ShowTable();


                }
        });

        ivDisplay.setOnTouchListener((view, motionEvent) -> {
            //checks whether one of the points was pressed if yes then selects it, if none was touched then hides the table
            ivDisplay.performClick();
            if (!canDisplayBeTouched || PointsCords == null) return false;
            //disables clickability for a short while so no bugs will happen gets re-enabled when table animation finishes
            canDisplayBeTouched = false;
            final float density = getResources().getDisplayMetrics().density;
            int pointID = 0;

            for (double[] cords : PointsCords)
            {
                pointID += 2;
                if (cords[0]==0||cords[1]==0) break;//no point will have a 0 cord so it's an end if it sees it so it doesn't loop unneeded
                    //"creating" a hitbox
                else if (cords[0] >= motionEvent.getX()-7 * density && cords[0] <= motionEvent.getX()+7 * density
                && cords[1] >= motionEvent.getY() - 7 * density && cords[1] <= motionEvent.getY()+7 * density)
                {
                    if(!isTableShowed) ShowTable();
                    else canDisplayBeTouched = true;

                    if (cords == LastSelectedPointCords) return false; //no point doing something that's already been done
                    else
                    {
                        if(LastSelectedPointCords != null && LastSelectedPointID != null)
                        {
                            //returning the colour of point when it's unselected
                            DisplayCanvas.drawCircle((float) LastSelectedPointCords[0], (float) LastSelectedPointCords[1], 4 * getResources().getDisplayMetrics().density, DisplayPaint);
                            ivDisplay.setImageBitmap(DisplayBitmap);
                            TableRow row = (TableRow) tblPoints.getChildAt(LastSelectedPointID);
                            row.setBackgroundColor(Color.parseColor("#323232"));

                        }
                        LastSelectedPointCords = cords;
                        LastSelectedPointID = pointID + 1;
                    }

                    //highlighting selected point on Display
                    DisplayPaint.setColor(Color.CYAN);
                    DisplayCanvas.drawCircle((float) cords[0], (float) cords[1], 4 * getResources().getDisplayMetrics().density, DisplayPaint);
                    ivDisplay.setImageBitmap(DisplayBitmap);
                    DisplayPaint.setColor(Color.BLACK);

                    //highlighting selected point in table
                    TableRow row = (TableRow) tblPoints.getChildAt(pointID+1);
                    row.setBackgroundColor(Color.parseColor("#424242"));
                    //getting -6 row so it is somewhat in the middle
                    final int topRow = pointID < 5 ? row.getTop() : tblPoints.getChildAt(pointID-5).getTop();

                    //scrolling to selected point
                    tblScroll.post(() -> tblScroll.smoothScrollTo(0,topRow));

                    return false;
                }
                else
                {
                    if (LastSelectedPointCords != null)
                    {
                        //returning the colour of point when it's unselected
                        DisplayCanvas.drawCircle((float) LastSelectedPointCords[0], (float) LastSelectedPointCords[1], 4 * getResources().getDisplayMetrics().density, DisplayPaint);
                        ivDisplay.setImageBitmap(DisplayBitmap);
                        System.out.println(pointID+1);
                        TableRow row = (TableRow) tblPoints.getChildAt(LastSelectedPointID);
                        row.setBackgroundColor(Color.parseColor("#323232"));
                        LastSelectedPointCords = null;
                        LastSelectedPointID = null;
                    }
                }
            }
            if(isTableShowed) HideTable();
            else ShowTable();
            return false;
        });
    }
}