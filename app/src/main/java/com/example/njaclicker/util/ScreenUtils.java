
package com.example.njaclicker.util;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Display;
import android.view.WindowManager;

public final class ScreenUtils {

    private ScreenUtils() {
        // Utility class. No instances required.
    }

    // --------------------------------------------------
    // SCREEN SIZE
    // --------------------------------------------------

    public static int getScreenWidth(
            AccessibilityService service
    ) {

        Point size = getScreenSize(service);

        return size.x;
    }

    public static int getScreenHeight(
            AccessibilityService service
    ) {

        Point size = getScreenSize(service);

        return size.y;
    }

    public static Point getScreenSize(
            AccessibilityService service
    ) {

        WindowManager windowManager =
                (WindowManager) service.getSystemService(
                        AccessibilityService.WINDOW_SERVICE
                );

        Display display = windowManager.getDefaultDisplay();

        Point size = new Point();

        display.getRealSize(size);

        return size;
    }

    // --------------------------------------------------
    // DENSITY CONVERSION
    // --------------------------------------------------

    public static int dpToPx(
            AccessibilityService service,
            float dp
    ) {

        float density =
                service.getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(dp * density);
    }

    public static float pxToDp(
            AccessibilityService service,
            float px
    ) {

        float density =
                service.getResources()
                        .getDisplayMetrics()
                        .density;

        return px / density;
    }

    // --------------------------------------------------
    // COORDINATE VALIDATION
    // --------------------------------------------------

    public static boolean isValidCoordinate(
            AccessibilityService service,
            float x,
            float y
    ) {

        Point size = getScreenSize(service);

        return x >= 0 &&
                y >= 0 &&
                x < size.x &&
                y < size.y;
    }

    // --------------------------------------------------
    // KEEP COORDINATES INSIDE SCREEN
    // --------------------------------------------------

    public static float clampX(
            AccessibilityService service,
            float x
    ) {

        int width = getScreenWidth(service);

        return Math.max(
                0,
                Math.min(x, width - 1)
        );
    }

    public static float clampY(
            AccessibilityService service,
            float y
    ) {

        int height = getScreenHeight(service);

        return Math.max(
                0,
                Math.min(y, height - 1)
        );
    }

    // --------------------------------------------------
    // RECTANGLE HELPERS
    // --------------------------------------------------

    public static boolean isInsideScreen(
            AccessibilityService service,
            Rect rect
    ) {

        if (rect == null) {
            return false;
        }

        int width = getScreenWidth(service);
        int height = getScreenHeight(service);

        return rect.left >= 0 &&
                rect.top >= 0 &&
                rect.right <= width &&
                rect.bottom <= height;
    }

    // --------------------------------------------------
    // COORDINATE NORMALIZATION
    // --------------------------------------------------

    public static float normalizeX(
            AccessibilityService service,
            float x
    ) {

        int width = getScreenWidth(service);

        return x / width;
    }

    public static float normalizeY(
            AccessibilityService service,
            float y
    ) {

        int height = getScreenHeight(service);

        return y / height;
    }

    public static float denormalizeX(
            AccessibilityService service,
            float normalizedX
    ) {

        return normalizedX * getScreenWidth(service);
    }

    public static float denormalizeY(
            AccessibilityService service,
            float normalizedY
    ) {

        return normalizedY * getScreenHeight(service);
    }
}
