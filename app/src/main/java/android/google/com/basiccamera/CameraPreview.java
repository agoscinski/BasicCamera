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

package android.google.com.basiccamera;

import android.google.com.basiccamera.camera.CameraManager;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * This surface is used for the camera preview
 *
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

public final class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraPreview.class.getSimpleName();

    private CameraManager mCameraManager;
    private UIActivity mActivity;
    private SurfaceHolder mHolder;

    public CameraPreview(UIActivity activity, CameraManager cameraManager) {
        super(activity);
        mActivity = activity;
        mCameraManager = cameraManager;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * Because surfaceChanged is also called with the creation of the surface, we do not have to
     * explicitly start the preview here. But for reasons of stability we still do it.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "Surface was created.");
        if (!mCameraManager.isAlive()) {
            mCameraManager.start();
            Log.w(TAG, "CameraManager not started yet. Too early or late callback?");

        }
        // request to execute initCamera
        mCameraManager.sendTask(R.id.init_camera, mHolder);
    }

    /** Is executed right after creation and when the surface is changed */
     @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG,"Surface was changed.");
        Message msg;

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            Log.w(TAG, "Surface does not exist. Too early or late callback?");
            return;
        }
        mCameraManager.sendTask(R.id.stop_preview);

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        // request to execute openDriver
        mCameraManager.sendTask(R.id.open_camera, mHolder);
        // request to execute startPreview
        mCameraManager.sendTask(R.id.start_preview);
        mActivity.onFinishedSurfaceChanged();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraManager = null;
        mHolder.removeCallback(this);
        mHolder = null;
    }
}
