/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.google.com.basiccamera.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Utility methods for configuring the Android camera.
 *
 * original author
 * @author Sean Owen
 * altered by
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

@SuppressWarnings("deprecation")
public class CameraConfigurationManager {

    private static final String TAG = CameraConfigurationManager.class.getSimpleName();

    private final Context mContext;
    private Point mScreenResolution;
    private Point mPreviewResolution;
    private Point mPictureResolution;

    public CameraConfigurationManager(Context context) {
        this.mContext = context;
    }

    /**
     * Sets preview and picture size, and focus mode
     * @param camera
     */
    public void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point theScreenResolution = new Point();
        display.getSize(theScreenResolution);
        mScreenResolution = theScreenResolution;
        Log.i(TAG, "Screen resolution: " + mScreenResolution);
        mPreviewResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters,
                mScreenResolution);
        parameters.setPreviewSize(mPreviewResolution.x, mPreviewResolution.y);
        Log.i(TAG, "Preview resolution: " + mPreviewResolution);
        mPictureResolution = CameraConfigurationUtils.findBestPictureSizeValue(parameters,
                mScreenResolution);
        parameters.setPictureSize(mPictureResolution.x, mPictureResolution.y);
        Log.i(TAG, "Picture resolution: " + mPictureResolution);

        CameraConfigurationUtils.setFocus(parameters);

        camera.setParameters(parameters);
    }


    public Point getScreenResolution() { return mScreenResolution; }

    public Point getPreviewResolution() { return mPreviewResolution; }

    public Point getPictureResolution() {
        return mPictureResolution;
    }


    public void saveCameraInfo(Camera.Parameters parameters) {

        String cameraInformation = parameters.flatten();

        mContext.getFilesDir();
        File cameraInfoFile = new File(mContext.getFilesDir(), "CameraSpecs.txt");

        try {
            FileOutputStream outputStream = new FileOutputStream(cameraInfoFile);
            outputStream.write(cameraInformation.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Camera information saved in " + cameraInfoFile.getAbsolutePath());
        CharSequence text = "Camera information saved in " + cameraInfoFile.getAbsolutePath();
        Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
        toast.show();
    }


}
