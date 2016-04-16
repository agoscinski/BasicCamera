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

import android.app.Activity;
import android.google.com.basiccamera.camera.CameraManager;
import android.google.com.basiccamera.imageprocessing.TaskManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

/**
 * The main activity which initialize everything and handles the UI
 *
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

public final class UIActivity extends Activity {

    private static final String TAG = UIActivity.class.getSimpleName();
    private static final boolean INIT_OPENCV = true;

    private SurfaceView mPreview;
    private ResultView mResultView;
    private CameraManager mCameraManager;
    private TaskManager mTaskManager;
    private Handler mMainHandler;
    private Button mCaptureButton;
    private Button mCapturePreviewButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mResultView = (ResultView) findViewById(R.id.result_view);
        mCaptureButton = (Button) findViewById(R.id.button_capture);
        mCaptureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCameraManager.sendTask(R.id.take_picture,
                                R.id.picture_taken, 0, mMainHandler);
                    }
                }
        );
        mCapturePreviewButton = (Button) findViewById(R.id.button_capture_preview);
        mCapturePreviewButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCameraManager.sendTask(R.id.capture_preview,
                                R.id.preview_captured, 0, mMainHandler);
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraManager = new CameraManager(this);
        mPreview = new CameraPreview(this, mCameraManager);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        mMainHandler = new UIHandler(this);

        if (!mCameraManager.isAlive()) {
            mCameraManager.start();
        } else {
            Log.w(TAG, "CameraManager has been already started. Check race conditions with CameraPreview ");
        }
        mCameraManager.sendTask(R.id.init_camera, mPreview.getHolder());

        mTaskManager = new TaskManager(mCameraManager, mMainHandler);
        mTaskManager.start();
    }

    protected void onFinishedSurfaceChanged() {
        if (INIT_OPENCV){
            // Load OpenCV
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }

    // OpenCV Callback, giving a signal when the OpenCV library has been successful initialized
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mTaskManager.startTask();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    protected void onPause() {
        super.onPause();
        mTaskManager.quitTask();
        mTaskManager.quit();
        mCameraManager.quit();
        mCameraManager.destroyCamera();
        mCameraManager = null;
        mTaskManager = null;
        mMainHandler = null;
        mPreview = null;
        mResultView = null;
    }

    protected void onDestroy(){
        super.onDestroy();
    }

    protected void drawResult(Bitmap resultBitmap) {
        mResultView.drawResult(resultBitmap);
    }
}

