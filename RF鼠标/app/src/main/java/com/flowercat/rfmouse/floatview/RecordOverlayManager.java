package com.flowercat.rfmouse.floatview;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;

public class RecordOverlayManager {

    private static final String TAG = "RecordOverlayManager";
    private final AccessibilityService service;
    private final WindowManager windowManager;
    private View recordView;
    private boolean isRecording = false;
    private int sizePx = 50; // Default size in pixels

    public interface RecordingStateCallback {
        void onRecordingStarted();
        void onRecordingStopped();
    }

    private RecordingStateCallback callback;

    public RecordOverlayManager(AccessibilityService service) {
        this.service = service;
        this.windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        sizePx = (int) (sizePx * metrics.density);
    }

    /**
     * Sets the callback for recording state changes.
     */
    public void setRecordingStateCallback(RecordingStateCallback callback) {
        this.callback = callback;
    }

    /**
     * Sets the size of the record overlay in pixels.
     * @param sizePx The width and height in pixels.
     */
    public void setOverlaySize(int sizePx) {
        this.sizePx = sizePx;
    }

    /**
     * Creates and displays the record overlay view.
     */
    public void showRecordOverlay() {
        if (recordView == null) {
            recordView = createRecordView();
        }
        if (recordView.getParent() == null) {
            try {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) recordView.getLayoutParams();
                // 设置初始位置
                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(metrics);
                params.x = metrics.widthPixels - sizePx;
                params.y = (int) (metrics.heightPixels * 0.75);
                windowManager.addView(recordView, params);
            } catch (WindowManager.BadTokenException e) {
                Toast.makeText(service, "创建录屏悬浮窗失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Hides and removes the record overlay view.
     */
    public void hideRecordOverlay() {
        if (recordView != null && recordView.getParent() != null) {
            windowManager.removeView(recordView);
            recordView = null;
        }
    }

    private View createRecordView() {
        View view = new View(service);
        updateViewShape(view); // Initial shape is a circle

        view.setOnTouchListener(new View.OnTouchListener() {
				private float initialX, initialY; // 触摸点的初始坐标
				private int initialViewX, initialViewY; // 视图的初始位置

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							// 记录触摸点的初始绝对坐标
							initialX = event.getRawX();
							initialY = event.getRawY();

							// 记录视图当前的初始位置
							WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();
							initialViewX = params.x;
							initialViewY = params.y;
							return true;

						case MotionEvent.ACTION_MOVE:
							// 计算触摸点的偏移量
							float deltaX = event.getRawX() - initialX;
							float deltaY = event.getRawY() - initialY;

							// 更新视图的位置
							WindowManager.LayoutParams moveParams = (WindowManager.LayoutParams) v.getLayoutParams();
							moveParams.x = (int) (initialViewX + deltaX);
							moveParams.y = (int) (initialViewY + deltaY);
							windowManager.updateViewLayout(v, moveParams);
							return true;

						case MotionEvent.ACTION_UP:
							// Check for a click (no significant movement)
							float upDeltaX = event.getRawX() - initialX;
							float upDeltaY = event.getRawY() - initialY;
							if (Math.abs(upDeltaX) < 10 && Math.abs(upDeltaY) < 10) {
								toggleRecordingState();
							}
							return true;
					}
					return false;
				}
			});

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            sizePx,
            sizePx,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY :
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START; // 必须设置Gravity为TOP|START，这样x和y才有效
        view.setLayoutParams(params);
        return view;
    }

    private void toggleRecordingState() {
        isRecording = !isRecording;
        updateViewShape(recordView);
        if (isRecording) {
            //Toast.makeText(service, "开始录屏", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onRecordingStarted();
            }
        } else {
            //Toast.makeText(service, "停止录屏", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onRecordingStopped();
            }
        }
    }

    private void updateViewShape(View view) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.RED);
        if (isRecording) {
            drawable.setCornerRadius(10); // Square with rounded corners
        } else {
            drawable.setCornerRadius(sizePx / 2f); // Circle
        }
        view.setBackground(drawable);
    }
}
