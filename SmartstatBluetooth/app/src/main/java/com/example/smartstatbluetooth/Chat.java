package com.example.smartstatbluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.aflak.bluetooth.Bluetooth;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback{
    private String name;
    private Bluetooth b;
    private EditText message;
    private Button send;
    private TextView text;
    private ScrollView scrollView;
    private boolean registered = false;

    private static Button start, setup;
    private static SeekBar scan_seek, num_seek, eq_seek;
    private static TextView scanRate_text, numCycles_text, eqTime_text;
    private static TextView scanRate_text2, numCycles_text2, eqTime_text2;

    int min_num = 1, max_num = 30, current_num = 1;
    int min_eq = 0, max_eq = 60, current_eq = 1;

    public int scanRate = 0, numCycles = current_num, eqTime = current_eq;

    private LineChart chart;
    private Thread thread;
    private boolean plotData = true;

    public List<String> dataList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView)findViewById(R.id.text);
        message = (EditText)findViewById(R.id.message);
        send = (Button)findViewById(R.id.send);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        chart = findViewById(R.id.linechart1);

        start = (Button) findViewById(R.id.start_btn);
        setup = (Button) findViewById(R.id.setup_btn);

        start.setEnabled(false);
        setup.setEnabled(false);

        scan_seek = findViewById(R.id.scan_seek);
        num_seek = findViewById(R.id.num_seek);
        eq_seek = findViewById(R.id.eq_seek);

        num_seek.setMax(max_num - min_num);
        num_seek.setProgress(current_num - min_num);

        eq_seek.setMax(max_eq - min_eq);
        eq_seek.setProgress(current_eq - min_eq);

        scanRate_text = findViewById(R.id.scanRate_text);
        numCycles_text = findViewById(R.id.numCycles_text);
        eqTime_text = findViewById(R.id.eqTime_text);

        scanRate_text2 = findViewById(R.id.textView2);
        numCycles_text2 = findViewById(R.id.textView3);
        eqTime_text2 = findViewById(R.id.textView4);

        scanRate_text.setText(""+0);
        numCycles_text.setText(String.format("%d", current_num));
        eqTime_text.setText(String.format("%d", current_eq));

        scan_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                double j = ((double)i / 1000.0) * 2;
                scanRate = i;
                scanRate_text.setText(String.format("%.2f V/s",j));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        num_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                current_num = i + min_num;
                numCycles = current_num;
                numCycles_text.setText(String.format("%d",current_num));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        eq_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                current_eq = i + min_eq;
                eqTime = current_eq;
                eqTime_text.setText(String.format("%d s",current_eq));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        text.setMovementMethod(new ScrollingMovementMethod());
        send.setEnabled(false);

        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");
        name = b.getPairedDevices().get(pos).getName();

        Display("Connecting...");
        b.connectToDevice(b.getPairedDevices().get(pos));

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = message.getText().toString();
                message.setText("");
                b.send(msg);
                Display("You: "+msg);
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered=true;

        chart.getDescription().setEnabled(true);
        chart.getDescription().setText("Plot of Voltage vs Time");

        YAxis leftaxis = chart.getAxisLeft();
        leftaxis.setAxisMaximum(6f);
        leftaxis.setAxisMinimum(0f);

        YAxis rightaxis =chart.getAxisRight();
        rightaxis.setEnabled(false);

        XAxis xAxis = chart.getXAxis();
//        xAxis.setAxisMinimum(0f);
//        xAxis.setAxisMaximum(1000f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        chart.setDrawBorders(true);

        LineData data = new LineData();
        chart.setData(data);
        chart.invalidate();
        feedMultiple();
        chart.setVisibility(View.INVISIBLE);
    }

    private void addEntry(float value){
        LineData data = chart.getData();

        if(data != null){
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null){
                set = createSet(0);
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), value), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(150);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());
        }

    }

    private LineDataSet createSet(int i) {

        LineDataSet set = new LineDataSet(null, "Voltage");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);

        set.setColor(Color.CYAN);
        set.setCircleColor(Color.CYAN);

        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    private void feedMultiple() {

        if (thread != null){
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true){
                    plotData = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    @Override
    public void onDestroy() {
        thread.interrupt();
        super.onDestroy();
        if(registered) {
            unregisterReceiver(mReceiver);
            registered=false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (thread != null) {
            thread.interrupt();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.close:
                b.removeCommunicationCallback();
                b.disconnect();
                Intent intent = new Intent(this, Select.class);
                startActivity(intent);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void Display(final String s){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.append(s + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Display("Connected to "+device.getName()+" - "+device.getAddress());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                send.setEnabled(true);
                start.setEnabled(true);
                setup.setEnabled(true);
            }
        });
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Display("Disconnected!");
        Display("Connecting again...");
        b.connectToDevice(device);
    }

    @Override
    public void onMessage(String message) {
        int result = 0;
        if(message.length() < 5){
            result = Integer.parseInt(message);
            double value = (double) ((result*5)/1024.0);
            value = Math.round(value * 100.0) / 100.0;
            addEntry((float)value);
            Display(name+"xx: "+value);
            dataList.add(String.format("%.2f", value));
        }
        else{
//            Log.d("io", message + " " + String.format("%d, %d", message.length(), result));
            Display(name+": "+message);
            dataList.add(message);
            chart.fitScreen();
        }
    }

    @Override
    public void onError(String message) {
        Display("Error: "+message);
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
        Display("Error: "+message);
        Display("Trying again in 3 sec.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.connectToDevice(device);
                    }
                }, 2000);
            }
        });
    }

    public void generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "SmartstatData");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String GetStringValueFromList(List<String> data) {
        String s = ">> ";
        for (String mydata : data) {
            s += mydata.toString() + "\n";
        }
        return s;
    }

    public void onStartClick(View view){
        b.send("1");
        text.setVisibility(View.VISIBLE);
        message.setVisibility(View.VISIBLE);
        send.setVisibility(View.VISIBLE);
//        scrollView.setVisibility(View.VISIBLE);
        scan_seek.setVisibility(View.VISIBLE);
        num_seek.setVisibility(View.VISIBLE);
        eq_seek.setVisibility(View.VISIBLE);
        scanRate_text.setVisibility(View.VISIBLE);
        numCycles_text.setVisibility(View.VISIBLE);
        eqTime_text.setVisibility(View.VISIBLE);
        scanRate_text2.setVisibility(View.VISIBLE);
        numCycles_text2.setVisibility(View.VISIBLE);
        eqTime_text2.setVisibility(View.VISIBLE);
        chart.setVisibility(View.INVISIBLE);
//        chart.clear();
    }

    public void onSetupClick(View view) throws InterruptedException {
        b.send(String.format("%d", scanRate));
        TimeUnit.SECONDS.sleep(2);
//        Display("You: " + scanRate);
        b.send(String.format("%d", numCycles));
        TimeUnit.SECONDS.sleep(2);
//        Display("You: " + numCycles);
        b.send(String.format("%d", eqTime));
//        Display("You: " + eqTime);

        text.setVisibility(View.INVISIBLE);
        message.setVisibility(View.INVISIBLE);
        send.setVisibility(View.INVISIBLE);
//        scrollView.setVisibility(View.INVISIBLE);
        scan_seek.setVisibility(View.INVISIBLE);
        num_seek.setVisibility(View.INVISIBLE);
        eq_seek.setVisibility(View.INVISIBLE);
        scanRate_text.setVisibility(View.INVISIBLE);
        numCycles_text.setVisibility(View.INVISIBLE);
        eqTime_text.setVisibility(View.INVISIBLE);
        scanRate_text2.setVisibility(View.INVISIBLE);
        numCycles_text2.setVisibility(View.INVISIBLE);
        eqTime_text2.setVisibility(View.INVISIBLE);

        chart.setVisibility(View.VISIBLE);
    }

    public void onSaveClick(View view){
        generateNoteOnSD(this, "SS_" + System.currentTimeMillis()+".txt", GetStringValueFromList(dataList));
        if (chart.saveToGallery("SS_" + System.currentTimeMillis(), 70))
            Toast.makeText(getApplicationContext(), "Saving SUCCESSFUL!",
                    Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), "Saving FAILED!", Toast.LENGTH_SHORT)
                    .show();

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(Chat.this, Select.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }
    };
}
