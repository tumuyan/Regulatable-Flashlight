package com.tumuyan.regulatable.flashlight;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;

import java.util.ArrayList;

import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FlashlightTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int TORCH_DURATION_MS = 1000;
    private static final int TORCH_TIMEOUT_MS = 3000;
    private static final int NUM_REGISTERS = 10;

    private ArrayList<String> mFlashCameraIdList;
    private ArrayList<String> mNoFlashCameraIdList;
    private String cameraId;

    private CameraManager mCameraManager;

    private SeekBar seekBar;
    private TextView tvProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mFlashCameraIdList = new ArrayList<String>();
        mNoFlashCameraIdList = new ArrayList<String>();
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        seekBar = findViewById(R.id.seekBar);
        tvProgress = findViewById(R.id.tvProgress);
        checkFlashlightAvailability();


    }

    private void checkFlashlightAvailability() {
        try {

            String[] cameraIdList = mCameraManager.getCameraIdList();
            mFlashCameraIdList.clear();
            mNoFlashCameraIdList.clear();
            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                if (hasFlash != null && hasFlash) {
                    Log.d(TAG, "Camera ID: " + cameraId + " has flash.");
                    mFlashCameraIdList.add(cameraId);
                } else {
                    Log.d(TAG, "Camera ID: " + cameraId + " does not have flash.");
                    mNoFlashCameraIdList.add(cameraId);
                }
            }

            for (String id : mFlashCameraIdList) {
                CameraCharacteristics pc = mCameraManager.getCameraCharacteristics(id);
                Log.i("CameraId " + id, "Flash Default level" + pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL)
                        + ", Max=" + pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)
                );
            }

            if (mFlashCameraIdList.size() > 0) {
                String id = mFlashCameraIdList.get(0);
                int maxLevel = 0;
//                int defaultLevel = 0;
                int minLevel = 0;
                CameraCharacteristics pc = mCameraManager.getCameraCharacteristics(id);
//                if (pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) != null) {
//                    defaultLevel = pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL);
//                }
                if (pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) != null) {
                    maxLevel = pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                }
                cameraId = id;
                seekBar.setMax(maxLevel);
                seekBar.setMin(minLevel);
//                seekBar.setProgress(defaultLevel);
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        changeLightLevel(progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void changeLightLevel(int v) {

        try {
            //   mCameraManager.setTorchMode(cameraId, true);
            if (v > 0) {
                mCameraManager.turnOnTorchWithStrengthLevel(cameraId, v);
                tvProgress.setText(""+v);
            }
            else {
                mCameraManager.setTorchMode(cameraId, false);
                tvProgress.setText("Off");
            }
        } catch (CameraAccessException e) {
            // throw new RuntimeException(e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

//    @Override
//    protected void onDestroy() {
//        try {
//            mCameraManager.setTorchMode(cameraId, false);
//        } catch (CameraAccessException e) {
//            throw new RuntimeException(e);
//        }
//        super.onDestroy();
//    }

}