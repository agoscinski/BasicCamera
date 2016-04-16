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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.LinkedList;

/**
 * This view is responsible for drawing the results of the image processing task
 *
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

public final class ResultView extends View {

    private static final String TAG = ResultView.class.getSimpleName();
    private static final int VIEW_OPACITY = 160;

    private final Paint mPaint;
    private Bitmap mResultBitmap;
    private Point mScreenResolution;


    // This constructor is used when the class is built from an XML resource.
    public ResultView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAlpha(VIEW_OPACITY);

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point theScreenResolution = new Point();
        display.getSize(theScreenResolution);
        mScreenResolution = theScreenResolution;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Log.i(TAG, "draw Picture begin on " + System.nanoTime());
        if (mResultBitmap == null) {
            return; // not ready yet, early draw before done configuring
        }
        canvas.drawBitmap(mResultBitmap, 0, 0, mPaint);
        Log.i(TAG, "draw Picture end on " + System.nanoTime());
    }

    protected void drawResult(Bitmap resultBitmap) {
        this.mResultBitmap = Bitmap.createScaledBitmap(resultBitmap, mScreenResolution.x, mScreenResolution.y, false);
        invalidate();
    }

}
