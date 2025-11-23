package com.flowercat.rfmouse.floatview;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.util.Log;

public class RecordOverlayManager {

    private static final String TAG = "RecordOverlayManager";
    private final AccessibilityService service;
    private final WindowManager windowManager;
    private TextView recordView;
    private boolean isRecording = false;
    private int sizePx = 50; // Default size in pixels
    private DisplayMetrics displayMetrics;
	public int initialViewX, initialViewY;
	

    // Animation and timing variables
    private Handler handler;
    private Runnable recordTimeUpdater;
    private long recordingStartTime = 0;
    private static int SNAP_ANIMATION_DURATION = 300;
    private static int SNAP_THRESHOLD = 50; // pixels from edge to trigger snap
    private Interpolator interpolator = new AccelerateDecelerateInterpolator();

    // Edge snap state
    private boolean isSnappedToEdge = false;
    private int snappedEdge = -1; // 0: left, 1: right, -1: not snapped
    private static final int SNAP_LEFT = 0;
    private static final int SNAP_RIGHT = 1;

    public interface RecordingStateCallback {
        void onRecordingStarted();
        void onRecordingStopped();
    }

    private RecordingStateCallback callback;

    public RecordOverlayManager(AccessibilityService service) {
        this.service = service;
        this.windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        this.displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        sizePx = (int) (sizePx * displayMetrics.density);
		SNAP_THRESHOLD = (int) (SNAP_THRESHOLD * displayMetrics.density);
        this.handler = new Handler();
        initRecordTimeUpdater();
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
        try {
            if (recordView == null) {
                recordView = createRecordView();
            }
            if (recordView.getParent() == null) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) recordView.getLayoutParams();
                // Set initial position - start snapped to right edge
                params.x = displayMetrics.widthPixels - (sizePx / 3); // Show only 1/3
                params.y = (int) (displayMetrics.heightPixels * 0.75);
                isSnappedToEdge = true;
                snappedEdge = SNAP_RIGHT;
                windowManager.addView(recordView, params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing record overlay", e);
            showToast("创建录屏悬浮窗失败");
        }
    }

    /**
     * Hides and removes the record overlay view.
     */
    public void hideRecordOverlay() {
        try {
            if (recordView != null && recordView.getParent() != null) {
                windowManager.removeView(recordView);
            }
            recordView = null;
            stopRecordTimeUpdate();
            isSnappedToEdge = false;
            snappedEdge = -1;
        } catch (Exception e) {
            Log.e(TAG, "Error hiding record overlay", e);
        }
    }

    private TextView createRecordView() {
        TextView view = new TextView(service);
        view.setTextColor(Color.WHITE);
        view.setTextSize(8);
        view.setGravity(Gravity.CENTER);
        updateViewAppearance(view);

        view.setOnTouchListener(new View.OnTouchListener() {
				private float initialX, initialY;
				private long touchDownTime;
				private boolean isDragging = false;
				private boolean hasMovedFromSnapPosition = false;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					try {
						switch (event.getAction()) {
							case MotionEvent.ACTION_DOWN:
								initialX = event.getRawX();
								initialY = event.getRawY();
								WindowManager.LayoutParams downParams = (WindowManager.LayoutParams) v.getLayoutParams();
								initialViewX = downParams.x;
								initialViewY = downParams.y;
								touchDownTime = System.currentTimeMillis();
								isDragging = false;
								hasMovedFromSnapPosition = false;

								// If currently snapped, bring the view fully into screen when starting to drag
								if (isSnappedToEdge) {
									bringViewToScreen(v, downParams);
								}
								return true;

							case MotionEvent.ACTION_MOVE:
								float deltaX = event.getRawX() - initialX;
								float deltaY = event.getRawY() - initialY;

								// Check if user is actually dragging (not just clicking)
								if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
									isDragging = true;
									hasMovedFromSnapPosition = true;
								}

								WindowManager.LayoutParams moveParams = (WindowManager.LayoutParams) v.getLayoutParams();
								moveParams.x = (int) (initialViewX + deltaX);
								moveParams.y = (int) (initialViewY + deltaY);

								// Keep view within screen bounds while dragging
								moveParams.x = Math.max(-sizePx * 2 / 3, Math.min(moveParams.x, displayMetrics.widthPixels - sizePx / 3));
								moveParams.y = Math.max(0, Math.min(moveParams.y, displayMetrics.heightPixels - sizePx));

								windowManager.updateViewLayout(v, moveParams);
								return true;

							case MotionEvent.ACTION_UP:
								if (isDragging) {
									// Only snap to edge if user has moved significantly from snap position
									if (hasMovedFromSnapPosition && shouldSnapToEdge(v)) {
										snapToEdge(v);
									}
								} else {
									// Check for click (no significant movement and short duration)
									long touchDuration = System.currentTimeMillis() - touchDownTime;
									if (touchDuration < 300) {
										toggleRecordingState();
									}
								}
								return true;
						}
					} catch (Exception e) {
						Log.e(TAG, "Error handling touch event", e);
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
        params.gravity = Gravity.TOP | Gravity.START;
        view.setLayoutParams(params);
        return view;
    }

    /**
     * Brings the view fully into screen when starting drag from snapped position
     */
    private void bringViewToScreen(View view, WindowManager.LayoutParams params) {
        try {
            int targetX;
            if (snappedEdge == SNAP_LEFT) {
                // If snapped to left, bring fully to left edge
                targetX = 0;
            } else {
                // If snapped to right, bring fully to right edge minus full width
                targetX = displayMetrics.widthPixels - sizePx;
            }

            params.x = targetX;
            windowManager.updateViewLayout(view, params);
            initialViewX = targetX; // Update initial position for drag calculations

            // Reset snap state since we're now dragging
            isSnappedToEdge = false;
            snappedEdge = -1;
        } catch (Exception e) {
            Log.e(TAG, "Error bringing view to screen", e);
        }
    }

    /**
     * Determines if the view should snap to edge based on its current position
     */
    private boolean shouldSnapToEdge(View view) {
        try {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
            int currentX = params.x;
            int screenWidth = displayMetrics.widthPixels;

            // Don't snap if view is near the center of screen
            int centerThreshold = screenWidth / 4;
            if (currentX > centerThreshold && currentX < screenWidth - centerThreshold - sizePx) {
                return false;
            }

            // Snap if close to edges
            return currentX < SNAP_THRESHOLD || currentX > screenWidth - sizePx - SNAP_THRESHOLD;
        } catch (Exception e) {
            Log.e(TAG, "Error in shouldSnapToEdge", e);
        }
        return false;
    }

    /**
     * Auto-snap the view to the nearest screen edge with 2/3 outside
     */
    private void snapToEdge(final View view) {
        try {
            final WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
            final int currentX = params.x;
            final int currentY = params.y;
            final int screenWidth = displayMetrics.widthPixels;
            final int screenHeight = displayMetrics.heightPixels;

            // Calculate target position - 2/3 outside, 1/3 inside
            final int targetX;
            if (currentX < screenWidth / 2) {
                // Snap to left edge: 2/3 outside, 1/3 inside
                targetX = -sizePx * 2 / 3;
                snappedEdge = SNAP_LEFT;
            } else {
                // Snap to right edge: 2/3 outside, 1/3 inside  
                targetX = screenWidth - sizePx / 3;
                snappedEdge = SNAP_RIGHT;
            }

            // Ensure Y position stays within screen bounds
            final int targetY = Math.max(0, Math.min(currentY, screenHeight - sizePx));

            // Animate to target position
            animateViewToPosition(view, currentX, currentY, targetX, targetY);
            isSnappedToEdge = true;

        } catch (Exception e) {
            Log.e(TAG, "Error in snapToEdge", e);
        }
    }

    /**
     * Animate view to target position with smooth animation
     */
    private void animateViewToPosition(final View view, final int startX, final int startY, 
									   final int targetX, final int targetY) {
        final long startTime = System.currentTimeMillis();

        final Runnable animationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = Math.min(1.0f, (float) elapsed / SNAP_ANIMATION_DURATION);
                    float interpolatedProgress = interpolator.getInterpolation(progress);

                    int currentX = (int) (startX + (targetX - startX) * interpolatedProgress);
                    int currentY = (int) (startY + (targetY - startY) * interpolatedProgress);

                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
                    params.x = currentX;
                    params.y = currentY;
                    windowManager.updateViewLayout(view, params);

                    if (progress < 1.0f) {
                        handler.postDelayed(this, 16); // ~60fps
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during view animation", e);
                }
            }
        };

        handler.post(animationRunnable);
    }

    private void toggleRecordingState() {
        try {
            isRecording = !isRecording;
            updateViewAppearance(recordView);

            if (isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling recording state", e);
            showToast("切换录屏状态失败");
        }
    }

    private void startRecording() {
        recordingStartTime = SystemClock.elapsedRealtime();
        startRecordTimeUpdate();
        if (callback != null) {
            callback.onRecordingStarted();
        }
    }

    private void stopRecording() {
        stopRecordTimeUpdate();
        recordingStartTime = 0;
        if (callback != null) {
            callback.onRecordingStopped();
        }
    }

    private void updateViewAppearance(TextView view) {
        try {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(isRecording ? Color.RED : Color.parseColor("#FF4444"));

            // Add white stroke
            drawable.setStroke(6, Color.WHITE);

            if (isRecording) {
                drawable.setCornerRadius(10); // Square with rounded corners
                // Show recording time
                view.setText("00:00");
            } else {
                drawable.setCornerRadius(sizePx / 2f); // Circle
                view.setText(""); // Clear text when not recording
            }
			
			//安卓4.0适配
			if (Build.VERSION.SDK_INT >= 16) {
            	view.setBackground(drawable);
			} else {
				view.setBackgroundDrawable(drawable);
			}
			
        } catch (Exception e) {
            Log.e(TAG, "Error updating view appearance", e);
        }
    }

    private void initRecordTimeUpdater() {
        recordTimeUpdater = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRecording && recordView != null) {
                        long elapsedMillis = SystemClock.elapsedRealtime() - recordingStartTime;
                        long seconds = elapsedMillis / 1000;
                        long minutes = seconds / 60;
                        seconds = seconds % 60;

                        String timeText = String.format("%02d:%02d", minutes, seconds);
                        recordView.setText(timeText);

                        // Schedule next update
                        handler.postDelayed(this, 1000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating record time", e);
                }
            }
        };
    }

    private void startRecordTimeUpdate() {
        handler.removeCallbacks(recordTimeUpdater);
        handler.post(recordTimeUpdater);
    }

    private void stopRecordTimeUpdate() {
        handler.removeCallbacks(recordTimeUpdater);
    }

    private void showToast(final String message) {
        try {
            handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
					}
				});
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }

    /**
     * Clean up resources when no longer needed
     */
    public void destroy() {
        try {
            hideRecordOverlay();
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during destroy", e);
        }
    }
}
