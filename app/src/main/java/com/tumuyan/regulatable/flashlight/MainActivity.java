package com.tumuyan.regulatable.flashlight;

import android.content.Context;
import android.database.DataSetObserver;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Objects;

import android.hardware.camera2.CameraCharacteristics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FlashlightTest";

    private ArrayList<String> mFlashCameraIdList;
    private ArrayList<String> mNoFlashCameraIdList;
    private ArrayList<String> mFlashCameraInfoList;
    private ArrayList<Integer> mFlashStrengthMaxList;
    private String cameraId;

    private CameraManager mCameraManager;

    private SeekBar seekBar;
    private TextView tvProgress;
    private Spinner cameraSelector;

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

        mFlashCameraIdList = new ArrayList<>();
        mNoFlashCameraIdList = new ArrayList<>();
        mFlashCameraInfoList = new ArrayList<>();
        mFlashStrengthMaxList = new ArrayList<>();
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        seekBar = findViewById(R.id.seekBar);
        tvProgress = findViewById(R.id.tvProgress);
        cameraSelector = findViewById(R.id.cameraSelector);
        checkFlashlightAvailability();


    }

    private void checkFlashlightAvailability() {
        try {

            String[] cameraIdList = mCameraManager.getCameraIdList();

            int index = 0;
            int bestCameraIndex = -1;
            int bestCameraScore = 0;

            for (String id : cameraIdList) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                if (hasFlash != null && hasFlash) {
                    Log.d(TAG, "Camera ID: " + id + " has flash.");
                    mFlashCameraIdList.add(id);

                    CameraCharacteristics pc = mCameraManager.getCameraCharacteristics(id);

                    int maxLevel = 0;
                    if (pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) != null) {
                        maxLevel = pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                    }
                    int defaultLevel = 0;
                    if (pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) != null) {
                        defaultLevel = pc.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL);
                    }
                    int score = defaultLevel << 1 + maxLevel;
                    if (score > bestCameraScore) {
                        bestCameraIndex = index;
                        bestCameraScore = score;
                    }

                    String info = "Camera " + id +
                            ": Default=" + defaultLevel +
                            ", Max=" + maxLevel;
                    mFlashCameraInfoList.add(info);
                    mFlashStrengthMaxList.add(maxLevel);
                    Log.i("Camera info", info);
//                    buffer.append(info).append('\n');

                    index++;
                } else {
                    Log.d(TAG, "Camera ID: " + id + " does not have flash.");
                    mNoFlashCameraIdList.add(id);
                }
            }


            TextView tv = findViewById(R.id.flashInfo);
            tv.setText(R.string.info);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mFlashCameraInfoList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cameraSelector.setAdapter(adapter);
            cameraSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    changeCameraId(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });


            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    changeLightLevel(progress, true, true);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {

                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    super.onTorchModeChanged(cameraId, enabled);
                    if (!enabled)
                        changeLightLevel(0, false, true, cameraId);
                }
            };
            Handler handler = new Handler(getMainLooper());
            mCameraManager.registerTorchCallback(torchCallback, handler);
            tvProgress.setOnClickListener(v -> changeLightLevel());

            if (bestCameraIndex >= 0)
                cameraSelector.setSelection(bestCameraIndex);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void changeCameraId(int position) {

        if (position < 0 && position >= mFlashCameraIdList.size())
            return;

        try {
            String id = mFlashCameraIdList.get(position);
            maxLightLevel = mFlashStrengthMaxList.get(position);
            if (null != cameraId)
                mCameraManager.setTorchMode(cameraId, false);
            cameraId = id;

            seekBar.setMax(maxLightLevel);
            seekBar.setProgress(0);
            if (maxLightLevel < 1)
                tvProgress.setText(R.string.not_support);
            else if (maxLightLevel == 1)
                tvProgress.setText(R.string.may_not_support);
            else
                tvProgress.setText(R.string.off);


        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }


    }

    private int lightLevel = 0;
    private int maxLightLevel = 0;

    private void changeLightLevel() {
        int v = (lightLevel + 1) % (maxLightLevel + 1);
        changeLightLevel(v, true, true, cameraId);
    }

    private void changeLightLevel(int v, boolean updateManager, boolean updateUI) {
        changeLightLevel(v, updateManager, updateUI, cameraId);
    }

    private void changeLightLevel(int v, boolean updateManager, boolean updateUI, String id) {
        if (null == id || !Objects.equals(id, cameraId))
            return;

        if (v == lightLevel)
            return;
        lightLevel = v;

        if (updateManager) {
            try {
                if (v > 0) {
                    mCameraManager.turnOnTorchWithStrengthLevel(cameraId, v);
                } else {
                    mCameraManager.setTorchMode(cameraId, false);
                }
            } catch (CameraAccessException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        if (updateUI) {
            if (v > 0)
                tvProgress.setText("" + v);
            else if (maxLightLevel < 1)
                tvProgress.setText(R.string.not_support);
            else if (maxLightLevel == 1)
                tvProgress.setText(R.string.may_not_support);
            else
                tvProgress.setText(R.string.off);

            seekBar.setProgress(v);
        }

    }
/*
    @Override
    protected void onResume(){
        super.onResume();
        try {
            mCameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    && mCameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.FLASH_INFO_FIRING); // 检查闪光灯是否正在工作


           if( mCameraManager.getTorchMode(cameraId))
               return;
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        seekBar.setProgress(0);
    }*/

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