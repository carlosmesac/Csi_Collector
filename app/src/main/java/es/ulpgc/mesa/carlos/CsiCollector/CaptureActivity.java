package es.ulpgc.mesa.carlos.CsiCollector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.ulpgc.mesa.carlos.CsiCollector.services.CSIDataInterface;
import es.ulpgc.mesa.carlos.CsiCollector.services.CSIFileGenerator;
import es.ulpgc.mesa.carlos.CsiCollector.services.DataCollector;
import es.ulpgc.mesa.carlos.CsiCollector.services.ESP32CSISerial;

public class CaptureActivity extends AppCompatActivity implements CSIDataInterface {
    private EditText positionText;
    private TextView csiString, csiCounter;
    private Button changePositionButton, clearPositionButton, endCaptureButton;
    private ArrayList<String> macList;
    private MaterialButtonToggleGroup toggleButton;
    private String orientation = "Front";
    private String position = "Undefined";
    private DataCollector dataCollector;
    private DataCollector dataCollectorFull;
    private Integer count = 0;
    private String TAG = "CAPTURE_ACTIVITY";
    private ESP32CSISerial csiSerial = new ESP32CSISerial();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        initLayout();
        macList = getIntent().getStringArrayListExtra("macList");
        String captureName = getIntent().getStringExtra("captureName");

        Log.d(TAG, "onCreate: " + macList);
        initCSICapture(captureName);

        csiSerial.setup(this, "test");
        csiSerial.onCreate(this);

        toggleButton.check(R.id.bt_north);
        toggleButton.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                switch (checkedId) {
                    case R.id.bt_north:
                        orientation = "Front";
                        break;
                    case R.id.bt_east:
                        orientation = "Right";
                        break;
                    case R.id.bt_west:
                        orientation = "Left";
                        break;
                    case R.id.bt_south:
                        orientation = "Back";
                        break;
                    default:
                        break;
                }
            }
        });

        changePositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!positionText.getText().toString().isEmpty()) {
                    position = positionText.getText().toString();
                    String toastText = "Current position set to " + position;
                    Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                }
            }
        });

        clearPositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                position = "Undefined";
                positionText.getText().clear();

            }
        });


        endCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CaptureActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                //this will always start your activity as a new task
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        csiSerial.onResume(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        csiSerial.onPause(this);
    }

    private void initCSICapture(String captureID) {
        dataCollectorFull = new CSIFileGenerator("Whole" + captureID, ".csv");
        dataCollectorFull.setup(this);
        dataCollectorFull.handle("type,id,mac,rssi,rate,sig_mode,mcs,bandwidth,smoothing,not_sounding,aggregation,stbc,fec_coding,sgi,noise_floor,ampdu_cnt,channel,secondary_channel,local_timestamp,ant,sig_len,rx_state,len,first_word,data\n");
        dataCollector = new CSIFileGenerator(captureID, ".csv");
        dataCollector.setup(this);
        dataCollector.handle("type,id,Mac,RSSI,TimeStamp,CSI_Amplitude,CSI_Phase,Position,Orientation\n");


    }

    private void initLayout() {
        positionText = findViewById(R.id.positionText);
        toggleButton = findViewById(R.id.toggleButton);
        changePositionButton = findViewById(R.id.changePositionButton);
        clearPositionButton = findViewById(R.id.clearPositionButton);

        csiCounter = findViewById(R.id.tv_csiCounter);
        csiString = findViewById(R.id.tv_csiString);

        endCaptureButton = findViewById(R.id.endCaptureButton);
    }


    @Override
    public void addCsi(String csi_string) {
        csiCounter.setText(count.toString());
        String[] arr = csi_string.split(",");
        if (arr.length > 0) {
            if (macList.contains(arr[2]) && arr[4].equals("11")) {
                if (!position.equals("Undefined")) {
                    count++;
                    csiString.setText(Arrays.toString(arr));
                    String id = arr[1];
                    String mac = arr[2];
                    String type = arr[0];
                    String rssi = arr[3];
                    String csi = arr[arr.length - 1];
                    dataCollector.handle(updateString(
                            type, id, mac, rssi, csi, position, orientation
                    ));
                }
            }
        }
    }

    private String updateString(String type, String id, String mac, String rssi, String csi, String position, String orientation) {
        if (!csi.isEmpty()) {
            String amp = String.valueOf(parseCSI(csi, false));
            String amplitude = amp.replace(",", "");
            String pha = String.valueOf(parseCSI(csi, true));
            String phase = pha.replace(",", "");
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n", type, id, mac, rssi, System.currentTimeMillis(), amplitude, phase, position, orientation);

        } else {
            return String.format("%s,%s,%s,%s,%s,-,-,%s,%s\n", type, id, mac, rssi, System.currentTimeMillis(), position, orientation);
        }


    }

    private ArrayList<Float> parseCSI(String CSI, boolean returnPhases) {
        ArrayList<Integer> csiInt = new ArrayList<>();
        ArrayList<Float> imaginary = new ArrayList<>();
        ArrayList<Float> real = new ArrayList<>();
        ArrayList<Float> amplitudes = new ArrayList<>();
        ArrayList<Float> phases = new ArrayList<>();

        String[] csiRaw = CSI.substring(1, CSI.length() - 1).split(" ");
        for (String i : csiRaw) {
            csiInt.add(Integer.parseInt(i));
        }

        for (int i = 0; i < csiInt.size(); i++) {
            if (i % 2 == 0) {
                imaginary.add((float) csiInt.get(i));
            } else {
                real.add((float) csiInt.get(i));
            }
        }

        for (int i = 0; i < csiInt.size() / 2; i++) {
            float amp = (float) (Math.pow(imaginary.get(i), 2) + Math.pow(real.get(i), 2));
            amplitudes.add((float) Math.sqrt(amp));
            phases.add((float) Math.atan2(imaginary.get(i), real.get(i)));
        }

        return returnPhases ? phases : amplitudes;
    }

}