package es.ulpgc.mesa.carlos.CsiCollector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private TextInputLayout macLayout, captureNameLayout;
    private TextView tv_captureName;
    private EditText macText, captureNameText;
    private Button addMacButton, setCaptureNameButton, startCaptureButton;
    private ArrayList<String> macList;
    private ListView macListView;
    private String captureName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();


        initTextFilters();

        macList = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.simple_list_item_1, macList);

        macListView.setAdapter(adapter);


        setCaptureNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!captureNameText.getText().toString().isEmpty()) {
                    captureName = captureNameText.getText().toString();
                    captureNameLayout.setError(null);
                    tv_captureName.setText(getString(R.string.project_name) + " " + captureName);
                } else {
                    captureNameLayout.setError("Select a name to save the capture");
                }
            }
        });


        addMacButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String newMac = macText.getText().toString();
                if (newMac.length() < 17) {
                    macLayout.setError("MAC Address incomplete");
                } else if (macList.contains(newMac)) {
                    macLayout.setError("MAC Address already exists");
                } else {
                    adapter.add(newMac);
                    macText.getText().clear();
                    macLayout.setError(null);
                    adapter.notifyDataSetChanged();

                }
            }
        });

        startCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!captureName.isEmpty() && !macList.isEmpty()) {
                    Intent i = new Intent(MainActivity.this, CaptureActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra("captureName", captureName);
                    i.putExtra("macList", macList);
                    startActivity(i);
                }
                if (captureName.isEmpty()) {
                    captureNameLayout.setError("Select a name for this capture");
                } else {
                    captureNameLayout.setError(null);
                }

                if (macList.size() == 0) {
                    macLayout.setError("MAC list can't be empty");
                }else{
                    macLayout.setError(null);
                }

                if (captureName.isEmpty() && macList.isEmpty()) {
                    macLayout.setError("MAC list can't be empty");
                    captureNameLayout.setError("Select a name for this capture");

                }
                ;
            }
        });
    }

    private void initTextFilters() {
        // Set the input filter to limit the input length and allow only valid characters
        macText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(17),
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        StringBuilder filteredStringBuilder = new StringBuilder();
                        for (int i = start; i < end; i++) {
                            char c = source.charAt(i);
                            if (Character.isLetterOrDigit(c) || c == ':') {
                                filteredStringBuilder.append(c);
                            }
                        }
                        return filteredStringBuilder.toString();
                    }
                }});

// Add a TextWatcher to listen for text changes
        macText.addTextChangedListener(new TextWatcher() {
            private boolean mFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mFormatting) {
                    mFormatting = true;

                    // Insert colons as needed after every two characters
                    int cursorPosition = macText.getSelectionEnd();
                    String text = s.toString().replaceAll(":", "");
                    StringBuilder formattedStringBuilder = new StringBuilder();
                    for (int i = 0; i < text.length(); i += 2) {
                        if (i != 0) {
                            formattedStringBuilder.append(":");
                        }
                        formattedStringBuilder.append(text.substring(i, Math.min(i + 2, text.length())));
                    }

                    macText.setText(formattedStringBuilder.toString());
                    macText.setSelection(Math.min(cursorPosition + (formattedStringBuilder.length() - s.length()), formattedStringBuilder.length()));
                    mFormatting = false;
                }
            }
        });

    }

    private void initLayout() {
        macLayout = findViewById(R.id.addMacInputLayout);
        macText = findViewById(R.id.addMacText);
        addMacButton = findViewById(R.id.addMacButton);
        macListView = findViewById(R.id.macListView);

        tv_captureName = findViewById(R.id.tv_currentName);

        captureNameLayout = findViewById(R.id.captureInputLayout);
        captureNameText = findViewById(R.id.captureText);
        setCaptureNameButton = findViewById(R.id.setCaptureNameButton);

        startCaptureButton = findViewById(R.id.startCaptureButton);

    }
}