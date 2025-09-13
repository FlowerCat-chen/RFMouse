package com.flowercat.rfmouse.floatview;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.graphics.Color;

public class ScreenshotOverlayManager {

    private static final String TAG = "ScreenshotOverlayManager";
    private final AccessibilityService service;
    private final WindowManager windowManager;
    private View screenshotView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int DISPLAY_DURATION_MS = 2500; // 2.5 seconds
	private  boolean isWriting = false;
	
    public ScreenshotOverlayManager(AccessibilityService service) {
        this.service = service;
        this.windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Shows a fullscreen overlay with the given bitmap and a border, then hides it after a delay.
     * The bitmap will be saved as a PNG.
     *
     * @param bitmap The bitmap to display and save.
     */
    public void showScreenshotOverlay(Bitmap bitmap) {
		
		if(isWriting){
			return;
		}
		
        if (screenshotView != null && screenshotView.getParent() != null) {
            // Remove any existing view before showing a new one
            windowManager.removeView(screenshotView);
        }

     	isWriting = true;

        // Create the fullscreen overlay view
        screenshotView = createScreenshotView(bitmap);

        try {
            windowManager.addView(screenshotView, (WindowManager.LayoutParams) screenshotView.getLayoutParams());

            // Post a runnable to hide the view after a delay
            handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (screenshotView != null && screenshotView.getParent() != null) {
							windowManager.removeView(screenshotView);
							isWriting = false;
						}
					}
				}, DISPLAY_DURATION_MS);
			
        } catch (WindowManager.BadTokenException e) {
            Log.e(TAG, "Failed to add screenshot view: " + e.getMessage());
            Toast.makeText(service, "创建截图悬浮窗失败", Toast.LENGTH_SHORT).show();
        }
		
		// Save the bitmap to a file
        Uri savedUri = saveBitmapToFile(bitmap);
        if (savedUri == null) {
            Toast.makeText(service, "保存截图失败", Toast.LENGTH_SHORT).show();
            return;
        }
		
    }

    /**
     * Creates the fullscreen view with an ImageView and a border.
     */
    private View createScreenshotView(Bitmap bitmap) {
        LinearLayout container = new LinearLayout(service);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setBackgroundColor(0x80000000); // Semi-transparent black background

        ImageView imageView = new ImageView(service);
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(10, 10, 10, 10); // Border padding
        imageView.setBackgroundColor(Color.YELLOW); // White border color
        container.addView(imageView);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
			WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY :
			WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;
        container.setLayoutParams(params);
        return container;
    }

    /**
     * Saves the given bitmap to a PNG file in the Pictures directory.
     * @return The Uri of the saved file, or null if saving failed.
     */
    private Uri saveBitmapToFile(Bitmap bitmap) {
        // You'll need to request WRITE_EXTERNAL_STORAGE permission for this
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + directory.getAbsolutePath());
            return null;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Screenshot_" + timeStamp + ".png";
        File file = new File(directory, fileName);

        FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.flush();
			Log.d(TAG, "Bitmap saved to: " + file.getAbsolutePath());
			return Uri.fromFile(file);
		} catch (IOException e) {
			Log.e(TAG, "Failed to save bitmap to file: " + e.getMessage());
			return null;
		} finally {
			if (out != null) {
				try {
					out.close(); // 手动关闭输出流
				} catch (IOException e) {
					Log.e(TAG, "Failed to close FileOutputStream: " + e.getMessage());
				}
			}
		}
		
    }
}
