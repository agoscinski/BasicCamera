/*
 * Copyright (C) 2014 ZXing authors
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

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for configuring the Android camera.
 *
 * original author
 * @author Sean Owen
 * altered by
 * @author alexander.goscinski@posteo.de (Alexander Goscinski)
 */

/** This application is designed to use the old camera interface which is deprecated */
@SuppressWarnings("deprecation")
public final class CameraConfigurationUtils {

    private static final String TAG = "CameraConfUtils";
    private static final int MIN_SIZE_PIXELS = 480 * 320; // normal screen
    private static final double MAX_ASPECT_DISTORTION = 0.15;

    private CameraConfigurationUtils() {}

    /**
     * Sets the camera focus mode to infinity
     * @param parameters parameters of a camera
     */
    protected static void setFocus(Camera.Parameters parameters) {

        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        String focusMode = findSettableValue("focus mode", supportedFocusModes,
                parameters.FOCUS_MODE_INFINITY);

        if (focusMode != null) {
            parameters.setFocusMode(focusMode);
        }
        Log.i(TAG, "Focus mode already set to " + focusMode);
    }

    protected static Point findBestPreviewSizeValue(Camera.Parameters parameters,
                                                    Point screenResolution) {
        List<Camera.Size> rawSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size defaultPreview = parameters.getPreviewSize();
        return findBestSizeValue(screenResolution, rawSupportedPreviewSizes, defaultPreview);
    }

    public static Point findBestPictureSizeValue(Camera.Parameters parameters,
                                                    Point screenResolution) {
        List<Camera.Size> rawSupportedPictureSizes = parameters.getSupportedPictureSizes();
        Camera.Size defaultPicture = parameters.getPictureSize();
        return findBestSizeValue(screenResolution, rawSupportedPictureSizes, defaultPicture);
    }

    /**
     * Finds the most optimal size. The optimal size is when possible the same as
     * the camera resolution, if not is is it the best size between the camera solution and
     * MIN_SIZE_PIXELS
     * @param screenResolution
     * @param rawSupportedSizes
     *@param defaultCameraSize @return optimal preview size
     */
    private static Point findBestSizeValue(Point screenResolution,
                                             List<Camera.Size> rawSupportedSizes,
                                             Camera.Size defaultCameraSize) {

        if (rawSupportedSizes == null) {
            Log.w(TAG, "Device returned no supported sizes; using default");
            if (defaultCameraSize == null) {
                throw new IllegalStateException("Parameters contained no size!");
            }
            return new Point(defaultCameraSize.width, defaultCameraSize.height);
        }

        // Sort by size, descending
        List<Camera.Size> supportedSizes = new ArrayList<>(rawSupportedSizes);
        Collections.sort(supportedSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels > aPixels) {
                    return -1;
                }
                if (bPixels < aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder sizesString = new StringBuilder();
            for (Camera.Size supportedSize : supportedSizes) {
                sizesString.append(supportedSize.width).append('x')
                        .append(supportedSize.height).append(' ');
            }
            Log.i(TAG, "Supported sizes: " + sizesString);
        }

        double screenAspectRatio =  (double) screenResolution.x / (double) screenResolution.y;

        // Remove sizes that are unsuitable
        Iterator<Camera.Size> it = supportedSizes.iterator();
        while (it.hasNext()) {
            Camera.Size supportedSize = it.next();
            int realWidth = supportedSize.width;
            int realHeight = supportedSize.height;
            if (realWidth * realHeight < MIN_SIZE_PIXELS) {
                it.remove();
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.i(TAG, "Found size exactly matching screen size: " + exactPoint);
                return exactPoint;
            }
        }

        // If no exact match, use largest size. This was not a great idea on older devices because
        // of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
        // the CPU is much more powerful.
        if (!supportedSizes.isEmpty()) {
            Camera.Size largestCameraSizes = supportedSizes.get(0);
            Point largestSize = new Point(largestCameraSizes.width, largestCameraSizes.height);
            Log.i(TAG, "Using largest suitable size: " + largestSize);
            return largestSize;
        }

        // If there is nothing at all suitable, return current size
        if (defaultCameraSize == null) {
            throw new IllegalStateException("Parameters contained no size!");
        }
        Point defaultSize = new Point(defaultCameraSize.width, defaultCameraSize.height);
        Log.i(TAG, "No suitable sizes, using default: " + defaultSize);

        return defaultSize;
    }

    /**
     * Finds the best possible value out of the list of supported values
     * @param name name of the desired mode the values are searched for
     * @param supportedValues the supported values
     * @param desiredValues the desired values with descending priority
     * @return the best possible value
     */
    private static String findSettableValue(String name,
                                            Collection<String> supportedValues,
                                            String... desiredValues) {
        Log.i(TAG, "Requesting " + name + " value from among: " + Arrays.toString(desiredValues));
        Log.i(TAG, "Supported " + name + " values: " + supportedValues);
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    Log.i(TAG, "Can set " + name + " to: " + desiredValue);
                    return desiredValue;
                }
            }
        }
        Log.i(TAG, "No supported values match");
        return null;
    }

}