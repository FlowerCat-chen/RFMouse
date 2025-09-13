
package com.flowercat.rfmouse.mouse;

/**被弃用的类,但是不用手动判断悬浮窗是否延伸到导航栏。**/

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;
import android.os.Build;
import com.flowercat.rfmouse.R;
import android.graphics.Color;

public class MouseManager {

    private static final String TAG = "MouseManager";
    private static MouseManager instance;
    private Context context;
    private WindowManager windowManager;
    private ImageView mouseView;
    private RelativeLayout mouseContainer;
    private WindowManager.LayoutParams layoutParams;

    private int screenWidth, screenHeight;
    private int mouseWidth, mouseHeight;
    private float mouseX = 0, mouseY = 0;
    private Bitmap mouseBitmap;
	private int statusBarHeight = 0;

    private MouseManager(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.screenWidth = windowManager.getDefaultDisplay().getWidth();
        this.screenHeight = windowManager.getDefaultDisplay().getHeight();
        initializeMouseView();
		//获取状态栏高度。
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
		}
    }

    public static synchronized MouseManager getInstance(Context context) {
        if (instance == null) {
            instance = new MouseManager(context);
        }
        return instance;
    }

    private void initializeMouseView() {
        mouseContainer = new RelativeLayout(context);
        mouseView = new ImageView(context);
        mouseContainer.addView(mouseView);

        layoutParams = new WindowManager.LayoutParams();
		
		// 适配不同Android版本
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		} else {
			layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
		}
		
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
			WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
			WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
			WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

		mouseX = screenWidth / 2;
		mouseY = screenHeight /2;
		
		layoutParams.x = (int)mouseX;
		layoutParams.y = (int)mouseY;
		//mouseContainer.setBackgroundColor(Color.GREEN);
        // 默认鼠标大小
        setMouseSize(MouseSize.DEFAULT);
        // 默认鼠标图片
        setMouseImage(null);
		mouseContainer.setBackgroundColor(Color.GREEN);
    }

    public void showMouse() {
        try {
            if (mouseContainer.getParent() == null) {
                windowManager.addView(mouseContainer, layoutParams);
            }
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
			// 视图已存在，忽略
        }
    }

    public void hideMouse() {
        try {
            if (mouseContainer.getParent() != null) {
                windowManager.removeView(mouseContainer);
            }
        } catch (IllegalArgumentException e) {
            // 视图不存在，忽略
        }
    }

    public void updateMousePosition(float deltaX, float deltaY) {
        mouseX += deltaX;
        mouseY += deltaY;

        // 处理边界，确保鼠标不移出屏幕
        if (mouseX < 0) mouseX = 0;
        if (mouseX > screenWidth - mouseWidth) mouseX = screenWidth - mouseWidth;
        if (mouseY < 0) mouseY = 0;
        if (mouseY > screenHeight - mouseHeight) mouseY = screenHeight - mouseHeight;

        layoutParams.x = (int) mouseX;
        layoutParams.y = (int) mouseY;

        try {
            if (mouseContainer.getParent() != null) {
                windowManager.updateViewLayout(mouseContainer, layoutParams);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void setMouseSize(MouseSize size) {
        switch (size) {
            case SMALL:
                mouseWidth = screenWidth / 18;
                break;
            case MEDIUM:
                mouseWidth = screenWidth / 12;
                break;
            case LARGE:
                mouseWidth = screenWidth / 8;
                break;
            case DEFAULT:
            default:
                mouseWidth = screenWidth / 12;
                break;
        }
        mouseHeight = mouseWidth;
        updateMouseViewSize();
    }

    public void setCustomSize(int width, int height) {
        mouseWidth = width;
        mouseHeight = height;
        updateMouseViewSize();
    }

    private void updateMouseViewSize() {
        if (mouseBitmap != null) {
            // 根据新的宽高，重新缩放位图
            //Bitmap scaledBitmap = Bitmap.createScaledBitmap(mouseBitmap, mouseWidth, mouseHeight, true);
            //mouseView.setImageBitmap(scaledBitmap);
			mouseView.setImageBitmap(mouseBitmap);
        } else {
            // 如果没有自定义图片，设置默认图标
            mouseView.setImageResource(R.drawable.mouse_pointer);
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mouseWidth, mouseHeight);
        mouseView.setLayoutParams(params);

        // 更新布局参数
        layoutParams.width = mouseWidth;
        layoutParams.height = mouseHeight;

        try {
            if (mouseContainer.getParent() != null) {
				windowManager.updateViewLayout(mouseContainer, layoutParams);
            }
        } catch (IllegalArgumentException e) {
			// 视图未添加，忽略
        }
    }

	//FrameLayout
	
    public void setMouseImage(Bitmap bitmap) {
        if (bitmap != null) {
            this.mouseBitmap = bitmap;
        } else {
            this.mouseBitmap = null;
        }
        updateMouseViewSize();
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
		
	
	// 获取坐标的方法
	public int getMouseX() {
		return layoutParams.x;
	}

	public int getMouseY() {
		return layoutParams.y;
	}
	
}



