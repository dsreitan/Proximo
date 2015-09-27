package dagfinn.proximo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

import dagfinn.proximo.util.SystemUiHider;

public class Main extends Activity {

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final boolean TOGGLE_ON_CLICK = false;
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
    private SystemUiHider mSystemUiHider;

    private BeaconManager beaconManager;
    private Region region;

    class BeaconTracker {
        String name;
        String uuid;
        int time;

        public BeaconTracker(String name, String uuid, int time) {
            this.name = name;
            this.uuid = uuid;
            this.time = time;
        }
    }

    private BeaconTracker dog;
    private BeaconTracker shoe;

    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }

    PieChart mChart;

    protected PieData generatePieData() {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();

        xVals.add("Dog");
        xVals.add("Shoe");

        int totalTime = dog.time + shoe.time;
        totalTime = (totalTime != 0) ? totalTime : 1;

        for (String s : xVals) {
            if (s.equals(dog.name))
                entries.add(new Entry((float) dog.time / totalTime * 100, 0));
            if (s.equals(shoe.name))
                entries.add(new Entry((float) shoe.time / totalTime * 100, 1));
        }


        PieDataSet ds1 = new PieDataSet(entries, "");
        ds1.setColors(ColorTemplate.PASTEL_COLORS);
        ds1.setSliceSpace(2f);
        ds1.setValueTextColor(Color.WHITE);
        ds1.setValueTextSize(12f);

        PieData d = new PieData(xVals, ds1);
        d.setValueTypeface(Typeface.DEFAULT);

        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconManager = new BeaconManager(getApplicationContext());
        region = new Region("All devices", null, null, null);

        dog = new BeaconTracker("Dog", "d0d3fa86-ca76-45ec-9bd9-6af4300e20b0", 0);
        shoe = new BeaconTracker("Shoe", "d0d3fa86-ca76-45ec-9bd9-6af469288852", 0);

        //RANGING
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                int i = 0;
                for (Beacon b : list) {
                    i++;
                    Log.d(i + "B", b.getName() + ", " + b.getProximityUUID() + ", " + b.getMajor() + ", " + b.getMinor() + ", Distance: " + calculateAccuracy(b.getMeasuredPower(), b.getRssi()));

                    if (dog.uuid.equals(b.getProximityUUID()))
                        dog.time += 2;
                    if (shoe.uuid.equals(b.getProximityUUID()))
                        shoe.time += 2;
                }
                Log.d("DOG", "" + dog.time);
                Log.d("SHOE", "" + shoe.time);
                mChart.invalidate();
                mChart.setCenterText("" + i);
                mChart.setData(generatePieData());
            }
        });

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        // MONITOR
        /*
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> list) {
                for (Beacon b : list){
                    Log.d("BEACON", b.getName() + ", " + b.getProximityUUID() + ", " + b.getMajor() + ", " + b.getMinor());
                }
                Toast.makeText(getApplicationContext(), "hey", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onExitedRegion(Region region) {

            }
        });
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startMonitoring(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        */


        /**** PIE CHART STUFF ****/
        mChart = (PieChart) findViewById(R.id.pieChart1);
        Typeface tf = Typeface.SANS_SERIF;
        mChart.setCenterTextTypeface(tf);
        mChart.setCenterTextSize(22f);

        // radius of the center hole in percent of maximum radius
        mChart.setHoleRadius(20f);
        mChart.setTransparentCircleRadius(25f);
        mChart.setDescription("");
        Legend l = mChart.getLegend();
        l.setEnabled(false);        mChart.setData(generatePieData());


        /**** REST OF THE DOCUMENT IS JUST FULLSCREEN STUFF ****/
        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
