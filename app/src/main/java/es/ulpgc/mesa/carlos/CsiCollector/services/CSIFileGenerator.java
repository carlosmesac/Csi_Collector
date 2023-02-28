package es.ulpgc.mesa.carlos.CsiCollector.services;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class CSIFileGenerator extends DataCollector {
    private String LOG_TAG = "FileDataCollectorService";
    private FileOutputStream localBackup = null;
    public String filePrefix;
    public String fileType;

    public CSIFileGenerator() {
        this.filePrefix = "backup";
        this.fileType = "csv";
    }

    public CSIFileGenerator(String filePrefix, String fileType) {
        this.filePrefix = filePrefix;
        this.fileType = fileType;
    }

    public void setup(Context context) {
        if (!Objects.equals(filePrefix, "backup")) {
            try {
                File test = new File(context.getExternalFilesDir(null), filePrefix + System.currentTimeMillis() + "." + fileType);
                localBackup = new FileOutputStream(test, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.w(LOG_TAG, "FileOutputStream exception: - " + e.toString());
            }
        }

    }

    public void handle(String csi) {
        try {
            if (localBackup != null) {
                localBackup.write(csi.getBytes());
                localBackup.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
