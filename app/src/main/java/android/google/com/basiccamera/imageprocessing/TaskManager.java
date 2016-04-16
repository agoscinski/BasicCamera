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

package android.google.com.basiccamera.imageprocessing;

import android.google.com.basiccamera.R;
import android.google.com.basiccamera.camera.CameraManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * This class manages the an image processing task and handles all messages correspondent to the task
 *
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

public final class TaskManager extends HandlerThread {

    private static final String TAG = TaskManager.class.getSimpleName();

    private CameraManager mCameraManager;
    private Handler mMainActivityHandler;
    private Handler mHandler;
    private ImageTask mTask;
    private CountDownLatch mInitLatch;
    public boolean dataReady;

    public TaskManager(CameraManager cameraManager, Handler mainActivityHandler) {
        super(TAG);
        mCameraManager = cameraManager;
        mInitLatch = new CountDownLatch(1);
        mMainActivityHandler = mainActivityHandler;
        dataReady = false;
    }

    public void startTask(){
        mTask = new CannyEdgeDetector(this);
        try {
            mInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mTask.running = true;
        mTask.start();
    }

    public void quitTask() {
        if (mTask != null) {
            mTask.running = false;
            try {
                // waits till the task finishes.
                mTask.join(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mTask = null;
    }

    @Override
    protected void onLooperPrepared() {
        Log.v(TAG, "onLooperPrepared has been invoked.");
        mHandler = new Handler(getLooper(), new Handler.Callback() {
            public boolean handleMessage(Message message) {
                byte[] data;
                switch (message.what) {
                    case R.id.picture_taken:
                        Log.v(TAG, "Picture received");
                        Point pictureResolution = new Point();
                        pictureResolution.x = message.arg1;
                        pictureResolution.y = message.arg2;
                        data = (byte[]) message.obj;
                        synchronized (mTask) {
                            mTask.setImageResolution(pictureResolution);
                            mTask.setImage(data);
                            dataReady = true;
                            mTask.notify();
                        }
                        return true;
                    case R.id.preview_captured:
                        Point previewResolution = new Point();
                        previewResolution.x = message.arg1;
                        previewResolution.y = message.arg2;
                        data = (byte[]) message.obj;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, previewResolution.x,
                                previewResolution.y, null);
                        yuvImage.compressToJpeg(new Rect(0, 0, previewResolution.x, previewResolution.y), 50, out);
                        data = out.toByteArray();
                        synchronized (mTask) {
                            mTask.setImageResolution(previewResolution);
                            mTask.setImage(data);
                            dataReady = true;
                            mTask.notify();
                        }
                        return true;
                }
                return false;
            }
        });
        mInitLatch.countDown();
        Log.v(TAG, "Handler has been initialized.");
    }

    protected void drawResult(Bitmap resultBitmap) {
        try {
            mInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg = mMainActivityHandler.obtainMessage(R.id.draw_result, resultBitmap);
        msg.sendToTarget();
    }

    /*
     * It requests a picture from the camera by sending a request for a picture to the
     * CameraManager thread. This takes around 1000 ms.
     */
    protected void requestPicture() {
        try {
            mInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mCameraManager.sendTask(R.id.take_picture, R.id.picture_taken, 0, mHandler);
        try {
            synchronized (mTask) {
                while (!dataReady) {
                    mTask.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        dataReady = false;
    }

    /*
     * It requests a preview frame from the camera by sending a request for a preview frame to the
     * CameraManager thread. This takes around 150 ms.
     */
    protected void requestPreviewFrame() {
        try {
            mInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mCameraManager.sendTask(R.id.capture_preview, R.id.preview_captured, 0, mHandler);
        try {
            synchronized (mTask) {
                while (!dataReady) {
                    mTask.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        dataReady = false;
    }
}
