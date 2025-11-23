package shellService.scrcpy;
import android.os.Build;
import android.view.InputEvent;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.view.InputDevice;
import android.view.Display;
import android.view.MotionEvent;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Message;

public class InputUtil {
  
    public static final int INJECT_MODE_ASYNC = InputManager.INJECT_INPUT_EVENT_MODE_ASYNC;
    public static final int INJECT_MODE_WAIT_FOR_RESULT = InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT;
    public static final int INJECT_MODE_WAIT_FOR_FINISH = InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH;
    public static final int DEFAULT_DISPLAY_ID = Display.DEFAULT_DISPLAY; // 发送到主屏幕 (ID=0)

	
	
	//判断是否能够注入事件
	public static boolean supportsInputEvents(int displayId) {
        // main display or any display on Android >= 10
        return displayId == 0 || Build.VERSION.SDK_INT >= AndroidVersions.API_29_ANDROID_10;
    }
	
	
	//注入事件，带返回值
    public static boolean injectEvent(InputEvent inputEvent, int displayId, int injectMode) {
        if (!supportsInputEvents(displayId)) {
            throw new AssertionError("Could not inject input event if !supportsInputEvents()");
        }

        if (displayId != 0 && !InputManager.setDisplayId(inputEvent, displayId)) {
            return false;
        }

        return ServiceManager.getInputManager().injectInputEvent(inputEvent, injectMode);
    }
	
	
	//注入事件，不等待返回结果。
	public static void injectMotionEvent(int inputSource, int action, long when, float x, float y, float pressure) {
        final float DEFAULT_SIZE = 1.0f;
        final int DEFAULT_META_STATE = 0;
        final float DEFAULT_PRECISION_X = 1.0f;
        final float DEFAULT_PRECISION_Y = 1.0f;
        final int DEFAULT_DEVICE_ID = 0;
        final int DEFAULT_EDGE_FLAGS = 0;
        MotionEvent event = MotionEvent.obtain(when, when, action, x, y, pressure, DEFAULT_SIZE,
											   DEFAULT_META_STATE, DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y, DEFAULT_DEVICE_ID,
											   DEFAULT_EDGE_FLAGS);
        event.setSource(inputSource);
        
        try {
			ServiceManager.getInputManager().injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
			
        }catch (Exception e) {
            System.err.println(e.toString());
            return;
        }
    }
	
	
	//注入事件，等待返回结果。
    public static void injectMotionEventB(int inputSource, int action, long when, float x, float y, float pressure) {
        final float DEFAULT_SIZE = 1.0f;
        final int DEFAULT_META_STATE = 0;
        final float DEFAULT_PRECISION_X = 1.0f;
        final float DEFAULT_PRECISION_Y = 1.0f;
        final int DEFAULT_DEVICE_ID = 0;
        final int DEFAULT_EDGE_FLAGS = 0;
        MotionEvent event = MotionEvent.obtain(when, when, action, x, y, pressure, DEFAULT_SIZE,
											   DEFAULT_META_STATE, DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y, DEFAULT_DEVICE_ID,
											   DEFAULT_EDGE_FLAGS);
        event.setSource(inputSource);
 
        try {
			ServiceManager.getInputManager().injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
			
        }catch (Exception e) {
            System.err.println(e.toString());
            return;
        }
    }
	
	//简易线性插值
    public static final float lerp(float a, float b, float alpha) {
        return (b - a) * alpha + a;
    }
	
	//发送点击事件，不等待结果。
	public static void sendTap(int inputSource, float x, float y) {
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, MotionEvent.ACTION_DOWN, now, x, y, 1.0f);
        injectMotionEvent(inputSource, MotionEvent.ACTION_UP, now, x, y, 0.0f);
    }

	//发送点击事件，等待结果。
    public static void sendTapB(int inputSource, float x, float y) {
        long now = SystemClock.uptimeMillis();
        injectMotionEventB(inputSource, MotionEvent.ACTION_DOWN, now, x, y, 1.0f);
        injectMotionEventB(inputSource, MotionEvent.ACTION_UP, now, x, y, 0.0f);
    }
	

	//发送滑动事件
    public static void sendSwipe(int inputSource, float x1, float y1, float x2, float y2) {
        final int NUM_STEPS = 11;
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, MotionEvent.ACTION_DOWN, now, x1, y1, 1.0f);
        for (int i = 1; i < NUM_STEPS; i++) {
            float alpha = (float) i / NUM_STEPS;
            injectMotionEvent(inputSource, MotionEvent.ACTION_MOVE, now, lerp(x1, x2, alpha),
							  lerp(y1, y2, alpha), 1.0f);
        }
        injectMotionEvent(inputSource, MotionEvent.ACTION_UP, now, x1, y1, 0.0f);
    }

	//发送滑动事件，不抬起。
    public static void sendSwipeUnUP(int inputSource, float x1, float y1, float x2, float y2,long now) {
    	final int NUM_STEPS = 11;
        injectMotionEvent(inputSource, MotionEvent.ACTION_DOWN, now, x1, y1, 1.0f);
        for (int i = 1; i < NUM_STEPS; i++) {
            float alpha = (float) i / NUM_STEPS;
            injectMotionEvent(inputSource, MotionEvent.ACTION_MOVE, now, lerp(x1, x2, alpha),
							  lerp(y1, y2, alpha), 1.0f);
        }
    }
	
	//发送滑动抬起事件。
    public static void sendSwipeUP(int inputSource, float x1, float y1, float x2, float y2,long now) {
        injectMotionEvent(inputSource, MotionEvent.ACTION_UP, now, x1, y1, 0.0f);
    }

	//发送移动事件
    public static void sendMove(int inputSource, float dx, float dy) {
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, MotionEvent.ACTION_MOVE, now, dx, dy, 0.0f);
    }

	
	//简易线程池
	private static final java.util.concurrent.ScheduledExecutorService scheduler = 
    java.util.concurrent.Executors.newScheduledThreadPool(2);
	
	// ==================== 按键注入方法 ====================
	
	// 快速按键输入（异步模式，不等待完成）
	public static void quickKeyPress(int keyCode,long durationMs) {
		long now = SystemClock.uptimeMillis();
		
		final KeyEvent downEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0, 
										  KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
		final KeyEvent upEvent = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, 0, 
										KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);

		// 异步注入，不等待完成
		injectEvent(downEvent, DEFAULT_DISPLAY_ID, INJECT_MODE_ASYNC);
		
		if (durationMs > 0) {
			// 延迟释放
			scheduler.schedule(new Runnable() {
					@Override
					public void run() {
						injectEvent(upEvent, DEFAULT_DISPLAY_ID, INJECT_MODE_ASYNC);
					}
				}, durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
		} else {
			// 立即释放
			injectEvent(upEvent, DEFAULT_DISPLAY_ID, INJECT_MODE_ASYNC);
		}
	}
	

	// 只按下按键（异步模式）
	public static void quickKeyDown(int keyCode) {
		long now = SystemClock.uptimeMillis();
		KeyEvent event = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0, 
									  KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
		injectEvent(event, DEFAULT_DISPLAY_ID, INJECT_MODE_ASYNC);
	}

	// 只释放按键（异步模式）
	public static void quickKeyUp(int keyCode) {
		long now = SystemClock.uptimeMillis();
		KeyEvent event = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, 0, 
									  KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
		injectEvent(event, DEFAULT_DISPLAY_ID, INJECT_MODE_ASYNC);
	}
	
	
	
	
	
	// ==================== 虚拟鼠标专用方法 ====================
	


	/**
	 * 虚拟鼠标点击（异步，不等待）
	 * @param x 点击的X坐标
	 * @param y 点击的Y坐标
	 * @param durationMs 点击持续时间（毫秒），0表示快速点击
	 */
	public static void virtualMouseClick(final float x, final float y, long durationMs) {
		final long now = SystemClock.uptimeMillis();
		injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_DOWN, now, x, y, 1.0f);

		if (durationMs > 0) {
			// 延迟释放
			scheduler.schedule(new Runnable() {
					@Override
					public void run() {
						injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_UP, 
										  SystemClock.uptimeMillis(), x, y, 0.0f);
					}
				}, durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
		} else {
			// 立即释放
			injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_UP, now, x, y, 0.0f);
		}
	}


	
	/**
	 * 虚拟鼠标平滑滚动（异步）
	 * @param startX 起始X坐标
	 * @param startY 起始Y坐标
	 * @param endX 结束X坐标
	 * @param endY 结束Y坐标
	 * @param durationMs 滚动持续时间（毫秒）
	 * @param steps 滚动步数，越多越平滑
	 */
	public static void virtualMouseSmoothScroll(final float startX, final float startY, 
												final float endX, final float endY, 
												final long durationMs, final int steps) {
		final long startTime = SystemClock.uptimeMillis();
		final long stepInterval = durationMs / steps;

		// 按下开始
		injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_DOWN, startTime, startX, startY, 1.0f);

		// 平滑移动
		for (int i = 1; i <= steps; i++) {
			final int step = i;
			scheduler.schedule(new Runnable() {
					@Override
					public void run() {
						float alpha = (float) step / steps;
						float currentX = lerp(startX, endX, alpha);
						float currentY = lerp(startY, endY, alpha);
						injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_MOVE, 
										  SystemClock.uptimeMillis(), currentX, currentY, 1.0f);
					}
				}, step * stepInterval, java.util.concurrent.TimeUnit.MILLISECONDS);
		}

		// 最终释放
		scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_UP, 
									  SystemClock.uptimeMillis(), endX, endY, 0.0f);
				}
			}, durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	
	
	
	
	
	

	
	/**
	 * 虚拟鼠标拖拽操作（异步）
	 * @param startX 拖拽起始X坐标
	 * @param startY 拖拽起始Y坐标
	 * @param endX 拖拽结束X坐标
	 * @param endY 拖拽结束Y坐标
	 * @param holdDurationMs 长按时间（毫秒）
	 * @param dragDurationMs 拖拽移动时间（毫秒）
	 */
	public static void virtualMouseDrag(final float startX, final float startY, 
										final float endX, final float endY, 
										final long holdDurationMs, final long dragDurationMs) {
		final long startTime = SystemClock.uptimeMillis();

		// 第一步：长按
		injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_DOWN, startTime, startX, startY, 1.0f);

		// 第二步：等待长按时间后开始拖拽移动
		scheduler.schedule(new Runnable() {
				@Override
				public void run() {
					virtualMouseSmoothScroll(startX, startY, endX, endY, dragDurationMs, 20);
				}
			}, holdDurationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	
	/**
	 * 简化的虚拟鼠标点击（快速点击）
	 * @param x 点击的X坐标
	 * @param y 点击的Y坐标
	 */
	public static void virtualMouseClick(float x, float y) {
		virtualMouseClick(x, y, 0);
	}

	/**
	 * 简化的平滑滚动（使用默认步数）
	 * @param startX 起始X坐标
	 * @param startY 起始Y坐标
	 * @param endX 结束X坐标
	 * @param endY 结束Y坐标
	 * @param durationMs 滚动持续时间（毫秒）
	 */
	public static void virtualMouseSmoothScroll(float startX, float startY, float endX, float endY, long durationMs) {
		virtualMouseSmoothScroll(startX, startY, endX, endY, durationMs, 20);
	}

	/**
	 * 简化的拖拽操作（使用默认时间）
	 * @param startX 拖拽起始X坐标
	 * @param startY 拖拽起始Y坐标
	 * @param endX 拖拽结束X坐标
	 * @param endY 拖拽结束Y坐标
	 */
	public static void virtualMouseDrag(float startX, float startY, float endX, float endY) {
		virtualMouseDrag(startX, startY, endX, endY, 500, 1000);
	}
	
	//以下是封装的拖动方法
	
	//只按下，不抬起
	public static void virtualMouseDown(final float x, final float y) {
		final long now = SystemClock.uptimeMillis();
		injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_DOWN, now, x, y, 1.0f);
	}

	//只抬起
	public static void virtualMouseUp(final float x, final float y) {
		final long now = SystemClock.uptimeMillis();
		injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_UP, now, x, y, 0.0f);
	}

	//鼠标移动
	public static void virtualMouseMove(final float x, final float y) {
		injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, MotionEvent.ACTION_MOVE, 
		SystemClock.uptimeMillis(), x, y, 1.0f);
	}
	

	
}
