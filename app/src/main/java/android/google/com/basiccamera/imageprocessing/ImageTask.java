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

import android.graphics.Point;
import android.util.Log;


/**
 * This class should be extended and used for image processing task
 *
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

public abstract class ImageTask extends Thread {

    private static final String TAG = ImageTask.class.getSimpleName();

    protected boolean running;
    private byte[] mImage;
    private Point mImageResolution;

    @Override
    public void run() {
        Log.i(TAG,"ImageTask started to run");
        runTask();
        running = false;
        Log.i(TAG,"ImageTask stopped to run");

    }

    /** Should be used for the image processing task */
    abstract protected void runTask();

    protected byte[] getImage() { return mImage; }

    protected Point getImageResolution() {return mImageResolution; }

    protected void setImage(byte[] image) { this.mImage = image; }

    protected void setImageResolution(Point point) { mImageResolution = point; }
}
