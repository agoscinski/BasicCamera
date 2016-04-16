/**
 * Copyright (C) 2016 Alexander Goscinski
 *
 * Licensed under the BSD 3-Clause License:
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.google.com.basiccamera.camera;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 *
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

@SuppressWarnings("deprecation")

public final class PictureCallback implements Camera.PictureCallback {

    private static final String TAG = PictureCallback.class.getSimpleName();

    private final CameraConfigurationManager mCameraConfigManager;
    // the handler to send back the picture
    private Handler mPictureHandler;
    // the what field of the message
    private int mPictureMessage;

    public PictureCallback(CameraConfigurationManager cameraConfigManager) {
        this.mCameraConfigManager = cameraConfigManager;
    }

    public void setHandler(Handler handler, int message) {
        this.mPictureHandler = handler;
        this.mPictureMessage = message;
    }

    /**
     * This method is called within the CameraManager thread.
     * @param data the byte array of the picture encoded in RBG
     * @param camera the camera object
     */
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.v(TAG,"onPictureTaken() method was called");
        camera.startPreview();
        Point cameraResolution = mCameraConfigManager.getPictureResolution();
        Handler thePictureHandler = mPictureHandler;
        if (cameraResolution != null && thePictureHandler != null) {
            Message message = thePictureHandler.obtainMessage(mPictureMessage, cameraResolution.x,
                    cameraResolution.y, data);
            message.sendToTarget();
            mPictureHandler = null;
        } else {
            Log.d(TAG, "Got preview callback, but no handler or resolution available");
        }
    }
}