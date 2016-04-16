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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * This is a implementation of a Canny edge detector
 *
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

public final class CannyEdgeDetector extends ImageTask {

    private TaskManager mTaskManager;

    private static final String TAG = CannyEdgeDetector.class.getSimpleName();

    public CannyEdgeDetector(TaskManager taskManager) {
        mTaskManager = taskManager;
    }

    protected void runTask() {
        Log.i(TAG, "Starting heavy image processing task");
        while(running) {
            Long begin, end;
            //begin = System.currentTimeMillis();

            // requests picture and blocks till it receives one
            mTaskManager.requestPreviewFrame();
            //end = System.currentTimeMillis();
            //Log.i(TAG, "Process took " + String.valueOf(end - begin) + " ms");
            byte[] image = getImage();
            if (image == null) {
                Log.w(TAG, "Received null as picture");
            }

            // do Canny edge detection
            Mat img = new Mat();
            Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
            Utils.bitmapToMat(bmp, img);
            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);
            Imgproc.blur(img, img, new Size(3, 3));
            Imgproc.Canny(img, img, 20, 100);
            Utils.matToBitmap(img, bmp);

            mTaskManager.drawResult(bmp);
        }
        return;
    }


}
