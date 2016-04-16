package android.google.com.basiccamera.camera;

import android.content.Context;
import android.google.com.basiccamera.R;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @original_author dswitkin@google.com (Daniel Switkin)
 * @original_author Sean Owen
 * @altered_by alexander.goscinski@posteo.de (Alexander Goscinski)
 */

public final class CameraManager extends HandlerThread {

    private static final String TAG = CameraManager.class.getSimpleName();
    // The ID of the camera our system uses
    private static final int DEFAULT_CAMERA_ID = 0;

    private final CameraConfigurationManager mConfigManager;
    private final Context mContext;
    private PreviewCallback mPreviewCallback;
    private PictureCallback mPictureCallback;
    private Camera mCamera;
    private Handler mHandler;
    private CountDownLatch mInitLatch;

    // Tells us if a preview is currently active
    public boolean previewing;
    // Tells us if the camera has already been opened
    public boolean isOpen;

    public CameraManager(Context context) {
        super(TAG);
        this.mContext = context;
        this.mConfigManager = new CameraConfigurationManager(context);
        this.mPreviewCallback = new PreviewCallback(mConfigManager);
        this.mPictureCallback = new PictureCallback(mConfigManager);
        this.mInitLatch = new CountDownLatch(1);
    }

    @Override
    protected void onLooperPrepared() {
        Log.v(TAG, "onLooperPrepared has been invoked.");
        mHandler = new Handler(getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case R.id.init_camera:
                        SurfaceHolder holderInit = (SurfaceHolder) msg.obj;
                        initCamera(holderInit);
                        return true;
                    case R.id.open_camera:
                        SurfaceHolder holderOpen = (SurfaceHolder) msg.obj;
                        try {
                            openDriver(holderOpen);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    case R.id.start_preview:
                        startPreview();
                        return true;
                    case R.id.stop_preview:
                        stopPreview();
                        return true;
                    case R.id.take_picture:
                        int msgPicId = msg.arg1;
                        Handler pictureTakenHandler = (Handler) msg.obj;
                        requestTakenPicture(pictureTakenHandler, msgPicId);
                        return true;
                    case R.id.capture_preview:
                        int msgPrevId = msg.arg1;
                        Handler previewCapturedHandler = (Handler) msg.obj;
                        requestPreviewFrame(previewCapturedHandler, msgPrevId);
                        return true;
                }
                return false;
            }
        });
        mInitLatch.countDown();
        Log.v(TAG, "Handler has been initialized");

    }

    public void sendTask(int what, int arg1, int arg2, Object obj) {
        try {
            mInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg = mHandler.obtainMessage(what, arg1, arg2, obj);
        msg.sendToTarget();
    }

    public void sendTask(int what, Object obj) {
        try {
            mInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg = mHandler.obtainMessage(what, obj);
        msg.sendToTarget();
    }

    public void sendTask(int what) {
        try {
            mInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Message msg = mHandler.obtainMessage(what);
        msg.sendToTarget();
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     * Shoud be only invoked by the handler mHandler
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IllegalStateException Indicates the given surface is not valid.
     * @throws IOException           Indicates the camera driver failed to open.
     */
    private void initCamera(SurfaceHolder holder) {
        if (holder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (isOpen) {
            Log.w(TAG, "initCamera() while already open. Check for early SurfaceView callback?");
            return;
        }
        try {
            openDriver(holder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            startPreview();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // COPY from zxing app. There it is said:
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     * It also is invoked if camera parameters have to be updated.
     * Shoud be only invoked by the handler mHandler
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    private void openDriver(SurfaceHolder holder) throws IOException {
        if (mCamera == null) {
            mCamera = Camera.open(DEFAULT_CAMERA_ID);
            if (mCamera == null) {
                throw new IOException();
            }
        }

        mCamera.setPreviewDisplay(holder);
        mConfigManager.initFromCameraParameters(mCamera);
        isOpen = true;
    }

    /**
     * Is used for destroying the camera as equivalent to initCamera()
     */
    public void destroyCamera() {
        stopPreview();
        closeDriver();              // release the camera immediately on pause event
    }

    /**
     * Requests the camera hardware to begin drawing preview frames to the screen.
     * this function is needed if the PreviewSurface is changed and needs to invoke
     * a restart the preview.
     * Shoud be only invoked by the handler mHandler.
     */
    private void startPreview() {
        Camera theCamera = mCamera;
        if (theCamera != null && !previewing) {
            theCamera.startPreview();
            previewing = true;
        }
    }

    /*
     * Shoud be only invoked by the handler mHandler.
     */
    private void stopPreview() {
        if (mCamera != null && previewing) {
            mCamera.stopPreview();
            mPreviewCallback.setHandler(null, 0);
            mPictureCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    /**
     * Releases the camera driver if still in use.
     */
    private void closeDriver() {
        if (mCamera != null) {
            mCamera.release();
        }
        isOpen = false;
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the preview frame to.
     * @param what The what field of the message to be sent with the preview frame.
     */
    public void requestPreviewFrame(Handler handler, int what) {
        if (mCamera != null && previewing) {
            mPreviewCallback.setHandler(handler, what);
            mCamera.setOneShotPreviewCallback(mPreviewCallback);
        }
    }

    /**
     * A single picture taken by the camera will be returned to the handler supplied. The data will
     * arrive as byte[] in the message.obj field, with width and height encoded as message.arg1 and
     * message.arg2,respectively.
     *
     * @param handler The handler to send the picture to.
     * @param what The what field of the message to be sent with the picture.
     */
    public void requestTakenPicture(Handler handler, int what) {
        if (mCamera != null && previewing) {
            mPictureCallback.setHandler(handler, what);
            mCamera.takePicture(null, null, null, mPictureCallback);
        }
    }

}
