/**
 * Copyright (C) 2016 Alexander Goscinski
 * <p/>
 * Licensed under the BSD 3-Clause License:
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * https://opensource.org/licenses/BSD-3-Clause
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.google.com.basiccamera;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class handles all the messaging requesting interaction with the UI and receives messages
 * back invoked by the UI
 *
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

public final class UIHandler extends Handler {

    private static final String TAG = UIHandler.class.getSimpleName();

    private final UIActivity mActivity;

    public UIHandler(UIActivity activity) {
        this.mActivity = activity;
    }

    @Override
    public void handleMessage(Message message) {
        byte[] data;
        switch (message.what) {
            case R.id.picture_taken:
                data = (byte[]) message.obj;
                if (data == null) {
                    Log.d(TAG, "Picture is null.");
                }
                break;
            case R.id.preview_captured:
                data = (byte[]) message.obj;
                if (data == null) {
                    Log.d(TAG, "Preview is null.");
                }
                break;
            case R.id.draw_result:
                Bitmap resultBitmap = (Bitmap) message.obj;
                mActivity.drawResult(resultBitmap);
                break;
        }
    }


}
