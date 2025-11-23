package com.flowercat.rfmouse.mouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.os.Build;
import com.flowercat.rfmouse.R;
import android.util.Log;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.BitmapFactory;
import java.util.List;
import android.graphics.Rect;
import java.util.ArrayList;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.flowercat.rfmouse.service.FlowerMouseService;


public class MouseManager2 {

    private static final String TAG = "MouseManager2";
    private static MouseManager2 instance;
    private Context context;
    private WindowManager windowManager;
    private FullScreenLayer fullScreenLayer;
    public MouseView mouseView;
    private WindowManager.LayoutParams layoutParams;

	//屏幕宽高
    private int screenWidth, screenHeight;
	//鼠标宽度基准
	private int mouseWidthBase = 0;
	//鼠标宽高
    private int mouseWidth, mouseHeight;
	//当前鼠标位置
    private float mouseX = 0, mouseY = 0;
	//鼠标指针图片
    private Bitmap mouseBitmap;
	//鼠标滚动图片
	private Bitmap scrollBitmap;
	// 鼠标图片缩放比例
	private float mouseAspectRatio = 1.0f; // 默认值为 1.0f
	// 滚动图片缩放比例
	private float scrollAspectRatio = 1.0f; // 默认值为 1.0f
	//状态栏高度
    private int statusBarHeight = 0;
	//高亮节点
	public List<Rect> clickableBounds = new ArrayList<>();
	
    //控制截屏
    private boolean allowScreenshot = true;
	private boolean isMouseShowed = false;
	private boolean keepScreenOn = false;
	
	private boolean fix_x = true;
	private boolean fix_y = true;
	
	// 新增变量：控制自动隐藏
    private boolean autoHideEnabled = false;
    private long autoHideDelayMillis = 3000; // 默认3秒
	

	// 鼠标移动消息
    private static final int MSG_MOVE = 1;
	//鼠标移动间隔
    private static int MOVE_INTERVAL = 16; // 长按移动间隔，毫秒
	//每秒移动的像素数
    private float pixelsPerSecond = 0;
	//上次更新的时间
    private long lastUpdateTime;
	//当前长按移动方向
	private int currentDir = -1;
	
	// 鼠标单次移动距离
    private  int SINGLE_MOVE_STEP = 5;
	
	// 当前模式：鼠标或滚动（拖动的话复用鼠标模式)
	private MouseMode currentMode = MouseMode.POINTER;
	
	//拖动时额外绘制一个蓝色框
	public boolean showDragHighLight = false;
	
    // Bitmap缓存优化
    private Bitmap scaledMouseBitmap;
    private Bitmap scaledScrollBitmap;
    private int cachedMouseWidth = -1;
    private int cachedMouseHeight = -1;

    // 绘制优化
    private boolean isDrawing = false;
    private final Object drawLock = new Object();

    // 性能监控
    private long lastDrawTime = 0;
    private static final long MIN_DRAW_INTERVAL = 5; // ~60fps

    
	
	public enum MouseMode {
		POINTER, // 鼠标指针模式
		SCROLL   // 滚动模式
	}
	
	//隐藏鼠标handler
    private Handler hideHandler;
	
	//鼠标隐藏Runnable
    private final Runnable hideMouseRunnable = new Runnable() {
        @Override
        public void run() {
            // 在UI线程执行隐藏操作
            Log.d(TAG, "因无操作，自动隐藏鼠标。");
            hideMouse();
        }
    };
	
	// moveHandler 用于处理连续移动
    private Handler moveHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_MOVE) {
				//首先更新位置
				try {
					movePointer(currentDir);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// 继续发送延迟消息，形成循环
				try {
					// 再次发送延时消息，实现连续移动
					moveHandler.sendEmptyMessageDelayed(MSG_MOVE, MOVE_INTERVAL);
				} catch (Exception e) {
					// 捕获发送消息时的异常，并停止循环
					e.printStackTrace();
					stopMovePointer();
				}
            }
        }
    };
	
	
	//单例模式
    public static synchronized MouseManager2 getInstance(Context context) {
        if (instance == null) {
            instance = new MouseManager2(context);
        }
        return instance;
    }

	
	//初始化MouseManager方法
    private MouseManager2(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        // 获取屏幕尺寸
        this.screenWidth = windowManager.getDefaultDisplay().getWidth();
        this.screenHeight = windowManager.getDefaultDisplay().getHeight();
		
		//以屏幕小的那边为基准
		if(screenHeight > screenWidth){
			mouseWidthBase = screenWidth;
		} else {
			//小于等于
			mouseWidthBase = screenHeight;
		}
		
        // 获取状态栏高度
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }

        initializeMouseView();
		
		//设置鼠标移动速度
		setMouseSpeed(30);
		//设置单次移动距离
		setSingleMouseSpeed(10);
    }
	
	
	//初始化鼠标视图
    private void initializeMouseView() {
        // 创建全屏层
        fullScreenLayer = new FullScreenLayer(context);

        // 创建鼠标视图
        mouseView = new MouseView(context);

        // 设置窗口参数
        layoutParams = new WindowManager.LayoutParams();

        // 适配不同Android版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY |
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

		//设置悬浮窗flag
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
			WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
			WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|
			WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

		// 根据截屏设置调整标志
        if (!allowScreenshot) {
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        }

		//保持屏幕开启
		if(keepScreenOn){
			layoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		}

		//左上角为坐标
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        // 初始位置在屏幕中央
        mouseX = screenWidth / 2;
        mouseY = screenHeight / 2;

        // 默认鼠标大小
        setMouseSize(MouseSize.DEFAULT);
        // 默认鼠标图片
        setMouseImage(null);
        
        // 启用硬件加速
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mouseView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        

        // 将鼠标视图添加到全屏层
        fullScreenLayer.addView(mouseView);

		//fullScreenLayer.setBackgroundColor(Color.GREEN);
		// 设置全屏层的布局参数
        RelativeLayout.LayoutParams mouseViewParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT);
			
	
        mouseView.setLayoutParams(mouseViewParams);

		
		hideMouse();
    }

	
	/**
     * 设置鼠标移动速度。
     */
    public void setMouseSpeed(int progress) {
        float baseSpeed = mouseWidthBase * 4.0f; 
        this.pixelsPerSecond = (progress / 100.0f) * baseSpeed;
        Log.d(TAG, "速度设置为 " + progress + "%, 即 " + pixelsPerSecond + " 像素/秒");
    }

	
	/**
     * 设置鼠标单次的移动距离。
     */
    public void setSingleMouseSpeed(int progress) {
        float baseSpeed = mouseWidthBase * 0.3f; 
        SINGLE_MOVE_STEP =(int)((progress / 100.0f) * baseSpeed);
        Log.d(TAG, "单次移动速度设置为 " + progress + "%, 即 " + pixelsPerSecond + " 像素/秒");
    }
	
	//设置鼠标大小
    public void setMouseSize(MouseSize size) {
        switch (size) {
            case SMALL:
                mouseWidth = mouseWidthBase / 18;
                break;
            case MEDIUM:
                mouseWidth = mouseWidthBase / 12;
                break;
            case LARGE:
                mouseWidth = mouseWidthBase / 8;
                break;
            case DEFAULT:
            default:
                mouseWidth = mouseWidthBase / 12;
                break;
        }
        //mouseHeight = mouseWidth;
        updateMouseViewSize();
    }
	
	public void setMouseSizeProgress(int progress) {
		if (progress <= 0 || progress > 100) {
			// 对于无效的进度值，返回或抛出异常，而不是直接返回
			// 建议你根据实际需求决定是否抛出异常或者简单记录日志
			Log.e(TAG, "Invalid progress value: " + progress + ". Must be between 1 and 100.");
			return; 
		}

		// 使用浮点数计算，确保结果不被截断
		float progressRatio = (float) progress / 100.0f;
		mouseWidth = (int) (mouseWidthBase * 0.25f * progressRatio); // 0.25f 是你原代码中的 1/4

		// 确保宽度不为0，至少给一个最小值
		if (mouseWidth <= 0) {
			mouseWidth = 1; // 设置一个最小宽度以避免绘制错误
		}

		mouseHeight = mouseWidth;
		updateMouseViewSize();
	}

	//设置自己的尺寸
    public void setCustomSize(int width, int height) {
        mouseWidth = width;
        mouseHeight = height;
        updateMouseViewSize();
    }
	
	public int getMouseSizeProgress() {
		// 使用浮点数进行计算，避免整数除法截断
		return (int) (((float) mouseWidth / (float) mouseWidthBase / 0.25f) * 100);
	}
	
	//更新间隔
	public void setMoveInterval(int ms){
		MOVE_INTERVAL = ms;
	}
	
	
	/**
	 * 切换鼠标的显示模式。
	 * @param mode 要切换到的模式，可以是MouseMode.POINTER或MouseMode.SCROLL。
	 */
	public void setMouseMode(MouseMode mode) {
		if (this.currentMode != mode) {
			this.currentMode = mode;
			// 切换后，需要重新计算宽高并重绘
			updateMouseViewSize();
			mouseView.invalidate();
		}
	}
	
    /**
     * 启用鼠标自动隐藏功能。
     * @param delayMillis 鼠标无操作后自动隐藏的延迟时间（毫秒）。必须为正数。
     */
    public void enableAutoHide(long delayMillis) {
        if (delayMillis <= 0) {
            Log.e(TAG, "自动隐藏延迟时间无效：" + delayMillis + " ms。延迟必须为正数。");
            return;
        }
		
		if(!autoHideEnabled){
        	this.autoHideEnabled = true;
        	this.autoHideDelayMillis = delayMillis;
        	Log.d(TAG, "已启用自动隐藏，延迟时间：" + delayMillis + " ms");
        	resetAutoHideTimer(); // 立即开始计时
		}
    }

    /**
     * 禁用鼠标自动隐藏功能并取消任何待处理的隐藏任务。
     */
    public void disableAutoHide() {
        this.autoHideEnabled = false;
        if (hideHandler != null) {
            hideHandler.removeCallbacks(hideMouseRunnable);
        }
        Log.d(TAG, "已禁用自动隐藏。");
    }

    /**
     * 重置自动隐藏计时器。应在所有鼠标活动（移动、点击、大小改变等）时调用。
     */
    public void resetAutoHideTimer() {
        if (autoHideEnabled) {
           
		//记得在这里显示鼠标。
		showMouse();
		
        // 延迟初始化 Handler
        if (hideHandler == null) {
            hideHandler = new Handler(Looper.getMainLooper());
        }
        // 移除之前的所有隐藏任务，防止重复隐藏
        hideHandler.removeCallbacks(hideMouseRunnable);
        // 发布一个新的隐藏任务
        hideHandler.postDelayed(hideMouseRunnable, autoHideDelayMillis);
		}
    }
	
	
	// 新增方法：设置是否允许截屏
    public void setAllowScreenshot(boolean allow) {
        if (this.allowScreenshot != allow) {
            this.allowScreenshot = allow;

            // 更新窗口标志
            if (allow) {
                layoutParams.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
            } else {
                layoutParams.flags |= WindowManager.LayoutParams.FLAG_SECURE;
            }

            // 更新窗口
            if (fullScreenLayer != null && fullScreenLayer.getParent() != null) {
                try {
                    windowManager.updateViewLayout(fullScreenLayer, layoutParams);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 新增方法：获取当前截屏设置
    public boolean isScreenshotAllowed() {
        return allowScreenshot;
    }
	
	
	// 是否保持屏幕开启？
    public void setScreenOn(boolean on) {
        if (this.keepScreenOn != on) {
            this.keepScreenOn = on;

            // 更新窗口标志
            if (on) {
				layoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            } else {
                layoutParams.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            }

            // 更新窗口
            if (fullScreenLayer != null && fullScreenLayer.getParent() != null) {
                try {
                    windowManager.updateViewLayout(fullScreenLayer, layoutParams);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 当前是否保持屏幕开启。
    public boolean isScreenOn() {
        return keepScreenOn;
    }
	
	
	// 当前是否修正x坐标
    public void setFixX(boolean fix) {
        fix_x = fix;
    }
	
	// 当前是否修正y坐标
    public void setFixY(boolean fix) {
        fix_y = fix;
    }
	
	//当横屏的时候，需要重新测量宽与高
	public void notifyScreenOrientationChange(){
		// 获取屏幕尺寸
        this.screenWidth = windowManager.getDefaultDisplay().getWidth();
        this.screenHeight = windowManager.getDefaultDisplay().getHeight();
		//鼠标回正
		mouseX = screenWidth / 2;
        mouseY = screenHeight / 2;
		mouseView.invalidate();
	}

	
	//显示鼠标
    public void showMouse() {
        try {
            if (fullScreenLayer.getParent() == null) {
                windowManager.addView(fullScreenLayer, layoutParams);
            }
			isMouseShowed = true;
            mouseView.setVisibility(View.VISIBLE);
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // 视图已存在，忽略
        }
    }

	//隐藏鼠标
    public void hideMouse2() {
        try {
			isMouseShowed = false;
            mouseView.setVisibility(View.INVISIBLE);
        } catch (IllegalArgumentException e) {
            // 视图不存在，忽略
        }
    }
	
	// 建议的移除方式
	public void  hideMouse() {
		try {
			hideMouse2(); // 先隐藏鼠标
			if (fullScreenLayer.getParent() != null) {
				windowManager.removeView(fullScreenLayer);
			}
		} catch (IllegalArgumentException e) {
			// 视图不存在，忽略
		}
	}
	

	//当前鼠标是否已显示？
	public boolean isMouseShowed(){
		return isMouseShowed;
	}
	
	//开始移动鼠标
	public void startMovePointer(int dir){
		
		currentDir = dir;
		// 启动长按检测
		lastUpdateTime = System.currentTimeMillis();
		
		try {
            moveHandler.sendEmptyMessageDelayed(MSG_MOVE, 0); //开始匀速移动
        } catch (Exception e) {
            // 记录异常，并停止循环
            e.printStackTrace();
            stopMovePointer();
        }
		
	}
	
	
	//停止移动鼠标
	public void stopMovePointer(){
		currentDir = -1;
		try {
            // 长按检测
			moveHandler.removeMessages(MSG_MOVE); //结束移动
			Log.e("kkk", "cancel loop");
        } catch (Exception e) {
            e.printStackTrace();
			Log.e("kkk", "cancel loop fail");
        } 
	
	}
	
	public void moveStep(int dir){
		switch (dir) {
			case 0:
				updateMousePosition(0, -SINGLE_MOVE_STEP);
				break;
			case 1:
				updateMousePosition(0, SINGLE_MOVE_STEP);
				break;
			case 2:
				updateMousePosition(-SINGLE_MOVE_STEP, 0);
				break;
			case 3:
				updateMousePosition(SINGLE_MOVE_STEP, 0);
				break;
		}
	}
	
	
	//鼠标移动功能
	private void movePointer(int dir) {

		long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastUpdateTime;

        if (deltaTime > 0 && pixelsPerSecond > 0) {
            float deltaPixels = pixelsPerSecond * (deltaTime / 1000.0f);

            switch (dir) {
                case 0:
                    updateMousePosition(0, -deltaPixels);
                    break;
                case 1:
                    updateMousePosition(0, deltaPixels);
                    break;
                case 2:
                    updateMousePosition(-deltaPixels, 0);
                    break;
                case 3:
                    updateMousePosition(deltaPixels, 0);
                    break;
            }
            //mouseManager.resetAutoHideTimer();
        }
        lastUpdateTime = currentTime;

	}
	
	
	//更新鼠标位置
    public void updateMousePosition(float deltaX, float deltaY) {
        mouseX += deltaX;
        mouseY += deltaY;

       
		// 允许鼠标部分移出屏幕，实现更好的边缘访问
		mouseX = Math.max(0, Math.min(screenWidth, mouseX));
		mouseY = Math.max(0, Math.min(screenHeight, mouseY));

		// 只在位置有显著变化时重绘，优化性能
		if (Math.abs(deltaX) > 0.5f || Math.abs(deltaY) > 0.5f) {
			mouseView.invalidate();
		}
		
        // 重绘鼠标
        //mouseView.invalidate();
		
		// 拖动模式下移动。
		if(FlowerMouseService.getInstance() != null){
			if(FlowerMouseService.getInstance().currentMode == 3){
				FlowerMouseService.getInstance().actionManager.mouseDragFunction("move",getMouseX(),getMouseY());
			}
		}
    }

	

	// 修改后的 updateMouseViewSize 方法
    private void updateMouseViewSize() {
        try {
            float currentAspectRatio;
            if (currentMode == MouseMode.POINTER) {
                currentAspectRatio = this.mouseAspectRatio;
            } else {
                currentAspectRatio = this.scrollAspectRatio;
            }

            // 根据宽度和当前宽高比计算新的高度
            this.mouseHeight = (int) (this.mouseWidth / currentAspectRatio);

            // 清除缓存，因为尺寸改变了
            cachedMouseWidth = -1;
            cachedMouseHeight = -1;

            // 重绘鼠标
            mouseView.invalidate();
          
        } catch (Exception e) {
            Log.e(TAG, "更新鼠标视图尺寸失败", e);
        }
    }

	
    // 修改后的 setMouseImage 方法
    public void setMouseImage(Bitmap bitmap) {
        try {
            // 回收旧的自定义Bitmap
            if (this.mouseBitmap != null && this.mouseBitmap != bitmap) {
                if (!this.mouseBitmap.isRecycled()) {
                    this.mouseBitmap.recycle();
                }
            }

            this.mouseBitmap = bitmap;
            if (mouseBitmap != null && mouseBitmap.getHeight() != 0) {
                this.mouseAspectRatio = (float) mouseBitmap.getWidth() / mouseBitmap.getHeight();
            } else {
                this.mouseAspectRatio = 1.0f;
            }

            // 清除缓存，触发重新创建
            cachedMouseWidth = -1;
            cachedMouseHeight = -1;
            updateMouseViewSize();
        } catch (Exception e) {
            Log.e(TAG, "设置鼠标图片失败", e);
        }
    }
	
	
	
	/**
     * 设置鼠标滚动模式下的图片
     */
    public void setScrollImage(Bitmap bitmap) {
        try {
            // 回收旧的滚动Bitmap
            if (this.scrollBitmap != null && this.scrollBitmap != bitmap) {
                if (!this.scrollBitmap.isRecycled()) {
                    this.scrollBitmap.recycle();
                }
            }

            this.scrollBitmap = bitmap;
            if (scrollBitmap != null && scrollBitmap.getHeight() != 0) {
                this.scrollAspectRatio = (float) scrollBitmap.getWidth() / scrollBitmap.getHeight();
            } else {
                this.scrollAspectRatio = 1.0f;
            }

            // 清除缓存，触发重新创建
            cachedMouseWidth = -1;
            cachedMouseHeight = -1;

            // 更新视图，如果当前模式是滚动
            if (currentMode == MouseMode.SCROLL) {
                updateMouseViewSize();
                mouseView.invalidate();
            }
        } catch (Exception e) {
            Log.e(TAG, "设置滚动图片失败", e);
        }
    }
	
	
	
	// 新增方法：获取当前缩放后的Bitmap（带缓存）
    private Bitmap getCurrentScaledBitmap() {
        try {
            Bitmap sourceBitmap;
            if (currentMode == MouseMode.SCROLL) {
                sourceBitmap = (scrollBitmap != null) ? scrollBitmap : mouseView.defaultScrollBitmap;
            } else {
                sourceBitmap = (mouseBitmap != null) ? mouseBitmap : mouseView.defaultMouseBitmap;
            }

            if (sourceBitmap == null || sourceBitmap.isRecycled()) {
                return null;
            }

            // 如果尺寸没变且缓存存在，直接返回缓存
            if (cachedMouseWidth == mouseWidth &&
                cachedMouseHeight == mouseHeight &&
                getCurrentScaledBitmapCache() != null &&
                !getCurrentScaledBitmapCache().isRecycled()) {
                return getCurrentScaledBitmapCache();
            }

            // 创建新的缩放Bitmap
            return createScaledBitmap(sourceBitmap);
        } catch (Exception e) {
            Log.e(TAG, "获取当前缩放Bitmap失败", e);
            return null;
        }
    }

    private Bitmap getCurrentScaledBitmapCache() {
        return (currentMode == MouseMode.SCROLL) ? scaledScrollBitmap : scaledMouseBitmap;
    }

    private void setCurrentScaledBitmapCache(Bitmap bitmap) {
        if (currentMode == MouseMode.SCROLL) {
            scaledScrollBitmap = bitmap;
        } else {
            scaledMouseBitmap = bitmap;
        }
    }

    private Bitmap createScaledBitmap(Bitmap source) {
        if (source == null || source.isRecycled() || mouseWidth <= 0 || mouseHeight <= 0) {
            return null;
        }

        try {
            // 回收旧的Bitmap
            recycleCurrentScaledBitmap();

            Bitmap scaled = Bitmap.createScaledBitmap(source, mouseWidth, mouseHeight, true);
            setCurrentScaledBitmapCache(scaled);
            cachedMouseWidth = mouseWidth;
            cachedMouseHeight = mouseHeight;

            return scaled;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "内存不足，无法创建缩放Bitmap", e);
            // 尝试清理内存后重试
            System.gc();
            try {
                Bitmap scaled = Bitmap.createScaledBitmap(source, mouseWidth, mouseHeight, true);
                setCurrentScaledBitmapCache(scaled);
                cachedMouseWidth = mouseWidth;
                cachedMouseHeight = mouseHeight;
                return scaled;
            } catch (OutOfMemoryError e2) {
                Log.e(TAG, "重试创建缩放Bitmap仍然内存不足", e2);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "创建缩放Bitmap失败", e);
            return null;
        }
    }

    private void recycleCurrentScaledBitmap() {
        try {
            Bitmap cached = getCurrentScaledBitmapCache();
            if (cached != null && !cached.isRecycled()) {
                cached.recycle();
                setCurrentScaledBitmapCache(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "回收缩放Bitmap失败", e);
        }
    }
	
	
	
	
    public enum MouseSize {
        SMALL, MEDIUM, LARGE, DEFAULT
		}

    public int getMouseWidth() {
        return mouseWidth;
    }

    public int getMouseHeight() {
        return mouseHeight;
    }
	
	public int getMouseWidthProgress() {
        return mouseWidth;
    }

    public int getMouseHeightProgress() {
        return mouseHeight;
    }
	
	

    // 获取坐标的方法，适配横屏与竖屏
    public int getMouseX() {
		if(fix_x){
        	return checkAndAdaptMousePositionX((int) mouseX);
		} else {
			return (int)mouseX;
		}
    }

    public int getMouseY() {
		if(fix_y){
        	return checkAndAdaptMousePositionY((int) mouseY) ;
		} else {
			return (int)mouseY;
		}
    }


	public int checkAndAdaptMousePositionY(int Ypos) {
		int[] location = new int[2];
		mouseView.getLocationOnScreen(location);
		int yCoordinateOnScreen = location[1];

		// 如果视图的y坐标接近状态栏高度，则说明悬浮窗未延伸到状态栏
		if (Math.abs(yCoordinateOnScreen - statusBarHeight) < 3) { // 增加一个小的容错值
			// 悬浮窗未延伸到状态栏，计算点击坐标时需要加上状态栏高度
			// 你的逻辑: 鼠标y坐标 = (悬浮窗内的y坐标) + statusBarHeight
			return Ypos + statusBarHeight;
		} else {
			// 悬浮窗延伸到了状态栏，计算点击坐标时不需要额外加上状态栏高度
			// 你的逻辑: 鼠标y坐标 = (悬浮窗内的y坐标)
			return Ypos;
		}
	}
	
	
	public int checkAndAdaptMousePositionX(int Xpos) {
		int[] location = new int[2];
		mouseView.getLocationOnScreen(location);
		int xCoordinateOnScreen = location[0];

		// 如果视图的x坐标接近状态栏高度，则说明悬浮窗未延伸到状态栏
		if (Math.abs(xCoordinateOnScreen - statusBarHeight) < 3) { // 增加一个小的容错值
			// 悬浮窗未延伸到状态栏，计算点击坐标时需要加上状态栏高度
			// 你的逻辑: 鼠标x坐标 = (悬浮窗内的x坐标) + statusBarHeight
			return Xpos + statusBarHeight;
		} else {
			// 悬浮窗延伸到了状态栏，计算点击坐标时不需要额外加上状态栏高度
			// 你的逻辑: 鼠标x坐标 = (悬浮窗内的x坐标)
			return Xpos;
		}
	}
	

    // 自定义全屏层
    private class FullScreenLayer extends RelativeLayout {
        public FullScreenLayer(Context context) {
            super(context);
        }
    }


	
	
	// 自定义鼠标视图
    private class MouseView extends View {
        private Paint paint;
        private Bitmap defaultMouseBitmap;
        private Bitmap defaultScrollBitmap;
        private Paint green_paint, red_paint,blue_paint;
        private List<Rect> boundsList = new ArrayList<>();
        private Rect currentRect = null;
        // 手势点击与滚动debug
        public boolean isDrawCircle = false;
        public boolean isDrawLine = false;
        private int startX = 0;
        private int startY = 0;
        private int endX = 0;
        private int endY = 0;

        public MouseView(Context context) {
            super(context);
            initializePaints();
            loadDefaultBitmaps();
            setWillNotDraw(false); // 允许绘制
        }

        private void initializePaints() {
            try {
                paint = new Paint();
                green_paint = new Paint();
                red_paint = new Paint();
				blue_paint = new Paint();
				
                green_paint.setColor(Color.GREEN);
                green_paint.setStyle(Paint.Style.STROKE);
                green_paint.setStrokeWidth(5);
                green_paint.setAntiAlias(true);

                red_paint.setColor(Color.RED);
                red_paint.setStyle(Paint.Style.STROKE);
                red_paint.setStrokeWidth(5);
                red_paint.setAntiAlias(true);
				
				blue_paint.setColor(Color.BLUE);
                blue_paint.setStyle(Paint.Style.STROKE);
                blue_paint.setStrokeWidth(5);
                blue_paint.setAntiAlias(true);
	
            } catch (Exception e) {
                Log.e(TAG, "初始化画笔失败", e);
            }
        }

        private void loadDefaultBitmaps() {
            try {
                defaultMouseBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mouse_pointer);
                defaultScrollBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mouse_scroll);
            } catch (Exception e) {
                Log.e(TAG, "加载默认鼠标图片失败", e);
            }
        }

        public void setClickableBounds(List<Rect> newBounds) {
            try {
                this.boundsList = (newBounds != null) ? new ArrayList<>(newBounds) : new ArrayList<>();
                invalidate();
            } catch (Exception e) {
                Log.e(TAG, "设置可点击边界失败", e);
            }
        }

        public void setCurrentBounds(Rect current) {
            try {
                this.currentRect = current;
                invalidate();
            } catch (Exception e) {
                Log.e(TAG, "设置当前边界失败", e);
            }
        }

        // 手势分发点击区域高亮
        public void setHighLightPos(int x, int y) {
            try {
                this.startX = x;
                this.startY = y;
                this.isDrawCircle = true;
                invalidate();
            } catch (Exception e) {
                Log.e(TAG, "设置高亮位置失败", e);
            }
        }

        // 手势分发滑动区域高亮
        public void setHighLightLinePos(int x, int y, int endX, int endY) {
            try {
                this.startX = x;
                this.startY = y;
                this.endX = endX;
                this.endY = endY;
                this.isDrawLine = true;
                invalidate();
            } catch (Exception e) {
                Log.e(TAG, "设置高亮线条失败", e);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // 绘制频率控制，避免过度绘制
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastDrawTime < MIN_DRAW_INTERVAL) {
                return;
            }
            lastDrawTime = currentTime;

            synchronized (drawLock) {
                if (isDrawing) return;
                isDrawing = true;
            }

            try {
                super.onDraw(canvas);

				if(showDragHighLight){
					int radius = (int) (mouseWidth / 2.0f);
                    canvas.drawCircle(mouseX, mouseY, radius, blue_paint);
				}
				
                // 绘制debug信息
                drawDebugInfo(canvas);

                // 优化后的Bitmap绘制
                Bitmap currentScaledBitmap = getCurrentScaledBitmap();
                if (currentScaledBitmap != null && !currentScaledBitmap.isRecycled()) {
                    canvas.drawBitmap(currentScaledBitmap, mouseX, mouseY, paint);
                } else {
                    // 备用绘制方案
                    drawFallbackCursor(canvas);
                }
            } catch (Exception e) {
                Log.e(TAG, "绘制鼠标失败", e);
                // 即使绘制失败也尝试绘制备用光标
                try {
                    drawFallbackCursor(canvas);
                } catch (Exception e2) {
                    Log.e(TAG, "绘制备用光标也失败", e2);
                }
            } finally {
                synchronized (drawLock) {
                    isDrawing = false;
                }
            }
        }

        private void drawDebugInfo(Canvas canvas) {
            try {
                if (boundsList != null) {
                    for (Rect rect : boundsList) {
                        canvas.drawRect(rect, green_paint);
                    }
                }

                if (currentRect != null) {
                    canvas.drawRect(currentRect, red_paint);
                }

                if (isDrawCircle) {
                    int radius = (int) (mouseWidth / 2.0f);
                    canvas.drawCircle(mouseX, mouseY, radius, red_paint);
                }

                if (isDrawLine) {
                    canvas.drawLine(startX, startY, endX, endY, green_paint);
                }
            } catch (Exception e) {
                Log.e(TAG, "绘制调试信息失败", e);
            }
        }

        private void drawFallbackCursor(Canvas canvas) {
            try {
                paint.setColor(0xFF000000);
                paint.setStyle(Paint.Style.FILL);
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(mouseX, mouseY);
                path.lineTo(mouseX - mouseWidth * 0.4f, mouseY + mouseHeight);
                path.lineTo(mouseX + mouseWidth * 0.4f, mouseY + mouseHeight);
                path.close();
                canvas.drawPath(path, paint);
            } catch (Exception e) {
                Log.e(TAG, "绘制备用光标失败", e);
            }
        }
    }
	
	
	//高亮可以点击的控件
	public void highlightClickables(List<AccessibilityNodeInfo> allNodes) {
		
		if(allNodes == null){
			return;
		}
		clickableBounds.clear();
		
		for (AccessibilityNodeInfo node : allNodes) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            // 有些情况下，bounds.top会包含状态栏高度，需要根据实际情况调整
            // 简单的判断方式是如果悬浮窗的 Y 坐标接近0，则说明它覆盖了状态栏
            int[] overlayLocation = new int[2];
            mouseView.getLocationOnScreen(overlayLocation);
			
            if (overlayLocation[1] > 0) { // 悬浮窗没有覆盖状态栏
                bounds.top -= statusBarHeight;
                bounds.bottom -= statusBarHeight;
            }
			
			if (overlayLocation[0] > 0) { // 悬浮窗没有覆盖状态栏
                bounds.left -= statusBarHeight;
                bounds.right -= statusBarHeight;
            }

             // 确保边框在屏幕内
             bounds.left = Math.max(0, bounds.left);
             bounds.top = Math.max(0, bounds.top);
             bounds.right = Math.min(screenWidth, bounds.right);
             bounds.bottom = Math.min(screenHeight, bounds.bottom);

             if (bounds.width() > 0 && bounds.height() > 0) {
                clickableBounds.add(bounds);
             }
        }
		
        // 更新自定义 View 的绘制数据并触发重绘
        mouseView.setClickableBounds(clickableBounds);

	}
	
	//清空高亮
	public void removehighlight(){
		mouseView.setClickableBounds(null);
		mouseView.setCurrentBounds(null);
		mouseView.isDrawCircle = false;
		mouseView.isDrawLine = false;
	}
	
	
	//高亮当前点击的控件
	public void highlightCurrent(AccessibilityNodeInfo current){
		if(current == null){
			return;
		}
		Rect bounds = new Rect();
		current.getBoundsInScreen(bounds);

		// 有些情况下，bounds.top会包含状态栏高度，需要根据实际情况调整
		// 简单的判断方式是如果悬浮窗的 Y 坐标接近0，则说明它覆盖了状态栏
		int[] overlayLocation = new int[2];
		mouseView.getLocationOnScreen(overlayLocation);

		if (overlayLocation[1] > 0) { // 悬浮窗没有覆盖状态栏
			bounds.top -= statusBarHeight;
			bounds.bottom -= statusBarHeight;
		}

		if (overlayLocation[0] > 0) { // 悬浮窗没有覆盖状态栏
			bounds.left -= statusBarHeight;
			bounds.right -= statusBarHeight;
		}

		// 确保边框在屏幕内
		bounds.left = Math.max(0, bounds.left);
		bounds.top = Math.max(0, bounds.top);
		bounds.right = Math.min(screenWidth, bounds.right);
		bounds.bottom = Math.min(screenHeight, bounds.bottom);

		if (bounds.width() > 0 && bounds.height() > 0) {
			mouseView.setCurrentBounds(bounds);
		}
	
		
	}
	
	public void highlightCurrentGesture(int x,int y){
		mouseView.setHighLightPos(x,y);
	}
	
	public void highlightLineGesture(int x,int y,int endX,int endY){
		mouseView.setHighLightLinePos(x,y,endX,endY);
	}
	

	

    /**
     * 释放所有资源，防止内存泄漏
     */
    public void release() {
        try {
            Log.d(TAG, "开始释放MouseManager2资源");

            // 停止所有移动
            stopMovePointer();

            // 禁用自动隐藏
            disableAutoHide();

            // 隐藏并移除视图
            hideMouse();

            // 回收所有Bitmap资源
            recycleAllBitmaps();

            // 清空列表
            if (clickableBounds != null) {
                clickableBounds.clear();
            }

            // 清空Handler消息
            if (moveHandler != null) {
                moveHandler.removeCallbacksAndMessages(null);
            }

            if (hideHandler != null) {
                hideHandler.removeCallbacksAndMessages(null);
            }

            Log.d(TAG, "MouseManager2资源释放完成");
        } catch (Exception e) {
            Log.e(TAG, "释放资源失败", e);
        }
    }

    private void recycleAllBitmaps() {
        try {
            // 回收自定义Bitmaps
            safeRecycleBitmap(mouseBitmap);
            mouseBitmap = null;

            safeRecycleBitmap(scrollBitmap);
            scrollBitmap = null;

            // 回收缩放Bitmaps
            safeRecycleBitmap(scaledMouseBitmap);
            scaledMouseBitmap = null;

            safeRecycleBitmap(scaledScrollBitmap);
            scaledScrollBitmap = null;

            // 重置缓存尺寸
            cachedMouseWidth = -1;
            cachedMouseHeight = -1;
        } catch (Exception e) {
            Log.e(TAG, "回收所有Bitmap失败", e);
        }
    }

    private void safeRecycleBitmap(Bitmap bitmap) {
        try {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "安全回收Bitmap失败", e);
        }
    }
	
	
	
	
	
	
}



