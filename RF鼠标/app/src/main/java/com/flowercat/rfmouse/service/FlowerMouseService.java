package com.flowercat.rfmouse.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import com.flowercat.rfmouse.MouseMainActivity;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.floatview.RecordOverlayManager;
import com.flowercat.rfmouse.floatview.ScreenshotOverlayManager;
import com.flowercat.rfmouse.key.KeyAction;
import com.flowercat.rfmouse.key.KeyEventPolicy;
import com.flowercat.rfmouse.key.KeyPressManager;
import com.flowercat.rfmouse.key.OnKeyEventListener;
import com.flowercat.rfmouse.ui.CallPhoneActivity;
import com.flowercat.rfmouse.ui.VolumeBoostActivity;
import com.flowercat.rfmouse.util.DeviceAdminUtil;
import com.flowercat.rfmouse.util.LoudnessEnhancerUtil;
import com.flowercat.rfmouse.util.NetworkUtils;
import com.flowercat.rfmouse.util.RootShellManager;
import com.flowercat.rfmouse.util.SPUtil;
import com.flowercat.rfmouse.util.ScreenCaptureHelper;
import com.flowercat.rfmouse.util.SystemOverlayHelper;
import com.flowercat.rfmouse.util.TtsManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.concurrent.atomic.AtomicBoolean;
import com.flowercat.rfmouse.mouse.MouseManager2;
import com.flowercat.rfmouse.mouse.MouseActionManager;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import com.flowercat.rfmouse.util.BitmapManager;


//辅助服务点击核心类
public class FlowerMouseService extends AccessibilityService {

    private static final String TAG = "FlowerMouseService";
	private static FlowerMouseService mMouseService;//单例模式
	public KeyPressManager keyPressManager;//按键管理
	public MouseManager2 mouseManager;//鼠标管理
	public MouseActionManager actionManager;//鼠标动作管理
	private TtsManager ttsManager;//语音转文本，用不着了
	
	//定义一个全局布尔变量来控制是否显示Toast
	public static boolean shouldShowToast = false;
	
	//当前鼠标处于何种模式？点击，滚动或者按键
	public int currentMode = 0;
	
	//是否在录制按键模式？
	public boolean isInKeyRecord = false;
	
	// 按键录制监听器
	public KeyRecordListener keyRecord;
	
	//对话框与Toast工具类
	public SystemOverlayHelper overlayHelper;
	
	//截屏窗口
	public ScreenshotOverlayManager screenshotOverlayManager;
	
	//截屏/录屏工具类
	public ScreenCaptureHelper mScreenCapture;
	
	//录屏窗口
	public RecordOverlayManager recordOverlayManager;

	//空出所有按键。
	public static boolean spaceMenu = false;
	
	//允许点击一次系统按键
	public boolean allowkeyOnce = false;
	
	private final AtomicBoolean isImeShowing = new AtomicBoolean(false);
	private final AtomicBoolean isEnterSetting = new AtomicBoolean(false);
	
	//音量提高类
	public static LoudnessEnhancerUtil enhancerUtil;
	
	//系统home键，电源，recent
	private HomeListenReceiver homeListenReceiver;
	//其它广播
	private CommonReceiver commonreceiver;
	
	//按钮单击与双击判断
	private int clickNum = 0;
    private Handler homeHandler = new Handler();
	
	//当前应用包名
	public String packageName = "unknown";
	
	//空出输入
	public static boolean space_input_bool;
	
	// Define a state variable outside of the function that gets called repeatedly.
	// For example, as a member variable of your class.
	private String lastPackageName = null;
	
	private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    private static final String SYSTEM_DIALOG_REASON_LOCK = "lock";
    private static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";
	


	//单例模式
	public static FlowerMouseService getInstance() {
        if (mMouseService == null) {
            //Toast.makeText(getApplicationContext(), "辅助服务未开启", Toast.LENGTH_SHORT).show();
        }
        return mMouseService;
    }
	
	//设置按键录制监听
	public void recordKeyPress(KeyRecordListener record){
		if(record == null){
			return;
		}
		this.keyRecord = record;
		isInKeyRecord = true;
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		mMouseService = this;
		
		//初始化按键管理
        keyPressManager = new KeyPressManager(this);
		//设置长按判定
		keyPressManager.setLongClickJudge(SPUtil.getInt(SPUtil.KEY_LONG_CLICK_JUDGE,300));
		// 初始化截屏助手
        mScreenCapture = ScreenCaptureHelper.getInstance(this);
		
	}
	
	
	//服务开启
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
		mMouseService = this;
		
	
        mouseManager = MouseManager2.getInstance(this);//初始化鼠标管理
		mouseManager.hideMouse();//隐藏鼠标
		mouseManager.setAllowScreenshot(true);//设置为允许截屏
		mouseManager.hideMouse();
		
		loadMouseData();
		
		actionManager = MouseActionManager.getInstance(this,mouseManager);
		//smoothMover = SmoothMouseMover.getInstance(mouseManager,screenWidth,screenHeight);//鼠标移动管理类
		
		keyPressManager.resetState();//重置按键监听
		updateKeyListeners(null);//设置按键监听，从存储的列表中
		
		ttsManager = TtsManager.getInstance(this);//获取tts管理类实例
		//对话框与自定Toast
		overlayHelper = SystemOverlayHelper.getInstance(this);
		
		//原A3+按键映射内容。
		
		screenshotOverlayManager = new ScreenshotOverlayManager(this);
		recordOverlayManager = new RecordOverlayManager(this);
		recordOverlayManager.setRecordingStateCallback(new RecordOverlayManager.RecordingStateCallback() {
				@Override
				public void onRecordingStarted() {
					// Your logic to start screen recording
					//Log.d(TAG, "Screen recording has started.");
					mScreenCapture.startRecording("/sdcard/record.mp4", new ScreenCaptureHelper.RecordingCallback(){

							@Override
							public void onRecordingStart() {
								Toast.makeText(FlowerMouseService.this, "录屏中…", Toast.LENGTH_SHORT).show();
							}

							@Override
							public void onRecordingStop(String videoPath) {
								Toast.makeText(FlowerMouseService.this, "视频保存在" + videoPath, Toast.LENGTH_SHORT).show();
							}

							@Override
							public void onRecordingError(String error) {
								Toast.makeText(FlowerMouseService.this, "录屏错误！" + error, Toast.LENGTH_SHORT).show();
							}


						});
				}

				@Override
				public void onRecordingStopped() {
					mScreenCapture.stopRecording();
					// Your logic to stop screen recording
					//Log.d(TAG, "Screen recording has stopped.");
					recordOverlayManager.hideRecordOverlay();
				}
			});

		//音量提高初始化
		enhancerUtil = LoudnessEnhancerUtil.getInstance();
		enhancerUtil.init();
		
		//当前音量提高百分比不为零。
		if(SPUtil.getInt(SPUtil.KEY_UP_PERCENT,0) != 0){
			//恢复上次设置的音量
			if(FlowerMouseService.getInstance() != null && FlowerMouseService.getInstance().enhancerUtil != null){
				FlowerMouseService.getInstance().enhancerUtil.setEnhanceGain(SPUtil.getInt(SPUtil.KEY_UP_PERCENT,0), 100);
			}
		}

		//是否允许使用Home锁屏？
		if(SPUtil.getBoolean(SPUtil.KEY_HOME_KEY_LOCK, true)){
			registerHomeListenReceiver();
		}
		
		//锁屏与亮屏监听
		registerCommonReceiver();
		
		//开启鼠标
		if(SPUtil.getBoolean(SPUtil.KEY_MOUSE_ON_BOOT, false)){
			currentMode = 1;
			restoreAllKeyListeners();
			mouseManager.showMouse();
			mouseManager.setMouseMode(MouseManager2.MouseMode.POINTER);
			showTipToast("鼠标模式");
		}
		
		//鼠标点击模式设置。
		String mode ;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mode= SPUtil.getString(SPUtil.KEY_CLICK_MODE,"gesture");
		} else {
			mode= SPUtil.getString(SPUtil.KEY_CLICK_MODE,"node");
		}

		if(mode.equals("gesture")){
			actionManager.setClickMode(2);
		} else if(mode.equals("node")){
			actionManager.setClickMode(1);
		}
		
		//保持屏幕开启?
		mouseManager.setScreenOn(SPUtil.getBoolean(SPUtil.KEY_KEEP_ON, false));
		
		//修正x?
		mouseManager.setFixX(SPUtil.getBoolean(SPUtil.KEY_FIX_X, true));
		
		//修正y?
		mouseManager.setFixY(SPUtil.getBoolean(SPUtil.KEY_FIX_Y, true));
		//输入法弹出时空出按键?
		space_input_bool = SPUtil.getBoolean(SPUtil.KEY_SPACE_INPUT, true);
		actionManager.inDebugMode = SPUtil.getBoolean(SPUtil.KEY_CLICK_DEBUG, false);
	
		
		int gest_short= SPUtil.getInt(SPUtil.KEY_GESTURE_SHORT, 50);
		int gest_long = SPUtil.getInt(SPUtil.KEY_GESTURE_LONG, 2);
		int gest_scroll_time = SPUtil.getInt(SPUtil.KEY_GESTURE_SCROLL, 300);
		int gest_scroll_ud = SPUtil.getInt(SPUtil.KEY_GESTURE_UD_DIS, 25);
		int gest_scroll_lr = SPUtil.getInt(SPUtil.KEY_GESTURE_LR_DIS, 25);
		
		
		actionManager.setShortClickTime(gest_short);
		actionManager.setLongClickTime(gest_long);
		actionManager.setScrollTime(gest_scroll_time);
		actionManager.setScrollDisUD(gest_scroll_ud);
		actionManager.setScrollDisLR(gest_scroll_lr);

		
		//showMyToast();
		Toast.makeText(FlowerMouseService.this,"鼠标服务开启。",Toast.LENGTH_SHORT).show();
    }


	
	private void loadMouseData() {
        // 从SharedPreferences加载数据并更新UI
        int presetMouseSize = SPUtil.getInt(SPUtil.KEY_PRESET_MOUSE_SIZE, 2);
        int customMouseSize = SPUtil.getInt(SPUtil.KEY_USER_MOUSE_SIZE, 50); // 默认50
        int mouseStep = SPUtil.getInt(SPUtil.KEY_MOUSE_STEP, 10);
        int mouseSpeed = SPUtil.getInt(SPUtil.KEY_MOUSE_SPEED, 40);
        int mouseInterval = SPUtil.getInt(SPUtil.KEY_MOUSE_TIME, 16);
        int mouseHideTime = SPUtil.getInt(SPUtil.KEY_MOUSE_HIDE_TIME, 30);
		boolean mouseHideEnabled = SPUtil.getBoolean(SPUtil.KEY_MOUSE_HIDE_ENABLE, true);
        //String imageUriString = SPUtil.getString(SPUtil.KEY_MOUSE_IMAGE_URI, null);
        //String scrollImageUriString = SPUtil.getString(SPUtil.KEY_MOUSE_SCROLL_IMAGE_URI, null);

		
        // 更新大小预设按钮和自定义尺寸
        if (presetMouseSize == 1) {
			mouseManager.setMouseSize(MouseManager2.MouseSize.SMALL);
        } else if (presetMouseSize == 2) {
			mouseManager.setMouseSize(MouseManager2.MouseSize.MEDIUM);
        } else if (presetMouseSize == 3) {
			mouseManager.setMouseSize(MouseManager2.MouseSize.LARGE);
        } else {
            // 自定义尺寸
			mouseManager.setMouseSizeProgress(customMouseSize);
        }

        // 加载自定义图片
        Bitmap tmp = BitmapManager.getBitmap(this,"mouse");
		if(tmp != null){
			mouseManager.setMouseImage(tmp);
		}

		// 加载自定义图片
        Bitmap tmp2 = BitmapManager.getBitmap(this,"mouse_scroll");
		if(tmp2 != null){
			mouseManager.setScrollImage(tmp2);
		}
		mouseManager.setSingleMouseSpeed(mouseStep);
		mouseManager.setMouseSpeed(mouseSpeed);
		mouseManager.setMoveInterval(mouseInterval);
		//如果鼠标设置为自动隐藏
		if(mouseHideEnabled){
			mouseManager.disableAutoHide();
			mouseManager.enableAutoHide(mouseHideTime * 1000);
		}
		mouseManager.hideMouse();
		
		
    }
	
	
	
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
		//Log.e(TAG, "event:" + String.valueOf(event.getEventType()));
		
        try {
			//当窗口改变，窗口内容改变，或者控件被滑动
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
			||event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
			|| event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
				//隐藏绘制的绿框
				actionManager.clearHighlight();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling accessibility event", e);
        }
		
		
		
		if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
			return;
		}
		
		packageName = event.getPackageName() != null ? 
			event.getPackageName().toString() : 
			"Unknown";
			
		
			
		if(Build.VERSION.SDK_INT < 21 || !space_input_bool){
			return;
		}
		
		try {
            // 获取当前所有窗口的信息
            boolean currentImeStatus = false;
            for (AccessibilityWindowInfo window : getWindows()) {
                // 判断窗口类型是否为输入法窗口
                if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    currentImeStatus = true;
                    Log.d(TAG, "Found input method window. IME is currently showing.");
                    break;
                }
            }

            // 使用 synchronized 块或 AtomicBoolean 确保线程安全地更新状态
            // 只有当当前状态与上次记录的状态不一致时，才执行回调
            if (currentImeStatus != isImeShowing.get()) {
                isImeShowing.set(currentImeStatus); // 更新状态

                if (currentImeStatus) {
                    Log.d(TAG, "IME state changed: Opened.");
                    showTipToast("输入法弹出");
					spaceMenu = true;
					//记得把系统按键空出来。
					
                } else {
                    Log.d(TAG, "IME state changed: Closed.");
                    showTipToast("输入法关闭");
					spaceMenu = false;
					//记得把系统按键空出来。
                }
					
            } else {
                // 状态没有改变，不执行任何操作，避免重复回调
                Log.d(TAG, "IME status is unchanged. Ignoring event.");
            }
        } catch (Exception e) {
            // 完善的错误处理
            Log.e(TAG, "Error processing accessibility event", e);
        }
		
		
    }
	
	
	//当屏幕方向发生改变的时候
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// 检查当前屏幕方向
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// 屏幕方向变为横屏，处理悬浮窗逻辑
			Toast.makeText(FlowerMouseService.this, "横屏", Toast.LENGTH_SHORT).show();
			// 在这里调用悬浮窗更新方法
			// 例如：updateGestureNavViewForLandscape();
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			// 屏幕方向变为竖屏，处理悬浮窗逻辑
			Toast.makeText(FlowerMouseService.this, "竖屏", Toast.LENGTH_SHORT).show();
			// 例如：updateGestureNavViewForPortrait();
		}
		mouseManager.notifyScreenOrientationChange();
	}
	
	

	
	
	
	/***
	开始按键处理
	***/

	// 重写 onKeyEvent 来处理按键
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
		//允许一次系统按键
		if(allowkeyOnce){
			allowkeyOnce = false;
			return false;
		}

		if(spaceMenu){
			return false;
		}
		
		//如果当前在按键录制模式并且回调不为空
		if(isInKeyRecord && keyRecord != null){
			keyRecord.onKeyPress(event.getKeyCode());
			isInKeyRecord = false;
			keyRecord = null;
			return true;
		}
		
		//Toast.makeText(this,String.valueOf(event.getKeyCode()),Toast.LENGTH_LONG).show();
        return keyPressManager.handleKeyEvent(event);//处理按钮
    }
	
	
	
	// 更新按键监听
	public void updateKeyListeners(List<KeyAction> keyActionList) {
		
		// 设置按键之前先清除所有按键监听
		keyPressManager.cleanUp();
		
		//如果传过来的列表为空
		if (keyActionList == null) {
			keyActionList = SPUtil.loadData(); //加载上次存储的数据
		}
		
		//当前处于按键模式
		if(currentMode == 0){
			temporarilyRemoveMouseFunction(keyActionList);//那么，更新按键监听的时候，我们禁用鼠标移动功能。
			return;
		}

		for (final KeyAction action : keyActionList) {
			// 由按键名称获取键值
			int keyCode = getKeyCodeFromName(action.getKeyName());
			if (keyCode == -1) {
				continue; // Skip if the key name is not recognized
			}

			// Determine the policy for short and long presses
			KeyEventPolicy shortPressPolicy = getPolicyFromAction(action.getShortPressAction());
			KeyEventPolicy longPressPolicy = getPolicyFromAction(action.getLongPressAction());

			// We only set a listener if there's a non-default action
			if (shortPressPolicy != KeyEventPolicy.DEFAULT || longPressPolicy != KeyEventPolicy.DEFAULT) {
				keyPressManager.setOnKeyEventListener(keyCode, shortPressPolicy, longPressPolicy, new OnKeyEventListener() {

						@Override
						public void onKeyDown(int keyCode) {
						}
						

						@Override
						public void onKeyUp(int keycode) {
							if(action.getShortPressAction().contains("上移") || action.getShortPressAction().contains("下移") || action.getShortPressAction().contains("左移") || action.getShortPressAction().contains("右移")){
								//当按键抬起时，记得取消鼠标的移动
								// 停止长按检测
								//moveHandler.removeMessages(MSG_MOVE);
								mouseManager.stopMovePointer();
							}
						}

						@Override
						public void onUnrecognizedEvent(KeyEvent event) {
						
							// 启动长按检测
							//moveHandler.removeMessages(MSG_MOVE); //开始加速移动
							mouseManager.stopMovePointer();
						}

						@Override
						public void onSingleClick(int keyCode) {
							performAction(0,action.getShortPressAction());
						}

						@Override
						public void onLongClick(int keyCode) {
							performAction(1,action.getLongPressAction());
						}
					});
			}
		}
	}
	
	/*
	// 暂时移除鼠标移动相关的功能，
	public void temporarilyRemoveMouseFunction(List<KeyAction> keyActionList) {
	
		// 创建一个临时的 KeyAction 列表
		List<KeyAction> tempActionList = new ArrayList<>();
		for (KeyAction action : keyActionList) {
			
			//将鼠标移动的功能设置为默认
			if (action.getShortPressAction().contains("上移")
				|| action.getShortPressAction().contains("下移")
				|| action.getShortPressAction().contains("左移")
				|| action.getShortPressAction().contains("右移")){
				action.setShortPressAction("默认");
			}
			
			if (action.getLongPressAction().contains("上移")
				|| action.getLongPressAction().contains("下移")
				|| action.getLongPressAction().contains("左移")
				|| action.getLongPressAction().contains("右移")){
				action.setLongPressAction("默认");
			}
			
			if (action.getShortPressAction().contains("短按")
				|| action.getShortPressAction().contains("长按")){
				action.setShortPressAction("默认");
			}
			
			
			if (action.getLongPressAction().contains("短按")
				|| action.getLongPressAction().contains("长按")){
				action.setLongPressAction("默认");
			}
			
			
			tempActionList.add(action);
		}

		// 清除所有监听
		keyPressManager.cleanUp();

		// 设置其余的按键监听
		for (final KeyAction action : tempActionList) {
			int keyCode = getKeyCodeFromName(action.getKeyName());
			if (keyCode != -1) {
				KeyEventPolicy shortPressPolicy = getPolicyFromAction(action.getShortPressAction());
				KeyEventPolicy longPressPolicy = getPolicyFromAction(action.getLongPressAction());

				keyPressManager.setOnKeyEventListener(keyCode, shortPressPolicy, longPressPolicy, new OnKeyEventListener() {
						// 这里的监听器逻辑与 updateKeyListeners 中的类似
						@Override
						public void onSingleClick(int keyCode) {
							performAction(0, action.getShortPressAction());
						}

						@Override
						public void onLongClick(int keyCode) {
							performAction(1, action.getLongPressAction());
						}

						@Override
						public void onUnrecognizedEvent(KeyEvent event) {
						}

						@Override
						public void onKeyUp(int keycode) {
							if(action.getShortPressAction().contains("上移") || action.getShortPressAction().contains("下移") || action.getShortPressAction().contains("左移") || action.getShortPressAction().contains("右移")){
								//当按键抬起时，记得取消鼠标的快速移动
								moveHandler.removeMessages(MSG_MOVE);
							}
						}
					});
			}
		}
	}
	
*/



	// 暂时移除鼠标移动相关的功能，
	public void temporarilyRemoveMouseFunction(List<KeyAction> keyActionList) {

		// 创建一个临时的 KeyAction 列表
		List<KeyAction> tempActionList = new ArrayList<>();
		for (KeyAction action : keyActionList) {

			// 对 KeyAction 对象进行深拷贝，以确保不修改原始列表
			KeyAction tempAction = new KeyAction(action.getKeyName()); // 假设 KeyAction 有一个构造函数可以复制属性
			tempAction.setShortPressAction(action.getShortPressAction());
			tempAction.setLongPressAction(action.getLongPressAction());
			
			// 将鼠标移动的功能设置为默认
			if (tempAction.getShortPressAction().contains("上移")
				|| tempAction.getShortPressAction().contains("下移")
				|| tempAction.getShortPressAction().contains("左移")
				|| tempAction.getShortPressAction().contains("右移")){
				tempAction.setShortPressAction("默认");
			}

			if (tempAction.getLongPressAction().contains("上移")
				|| tempAction.getLongPressAction().contains("下移")
				|| tempAction.getLongPressAction().contains("左移")
				|| tempAction.getLongPressAction().contains("右移")){
				tempAction.setLongPressAction("默认");
			}

			if (tempAction.getShortPressAction().contains("短按")
				|| tempAction.getShortPressAction().contains("长按")){
				tempAction.setShortPressAction("默认");
			}


			if (tempAction.getLongPressAction().contains("短按")
				|| tempAction.getLongPressAction().contains("长按")){
				tempAction.setLongPressAction("默认");
			}
			
			if (tempAction.getLongPressAction().contains("点击菜单")
				|| tempAction.getLongPressAction().contains("点击菜单")){
				tempAction.setLongPressAction("默认");
			}
			

			tempActionList.add(tempAction);
		}

		// 清除所有监听
		keyPressManager.cleanUp();

		// 设置其余的按键监听
		for (final KeyAction action : tempActionList) {
			int keyCode = getKeyCodeFromName(action.getKeyName());
			if (keyCode != -1) {
				KeyEventPolicy shortPressPolicy = getPolicyFromAction(action.getShortPressAction());
				KeyEventPolicy longPressPolicy = getPolicyFromAction(action.getLongPressAction());

				keyPressManager.setOnKeyEventListener(keyCode, shortPressPolicy, longPressPolicy, new OnKeyEventListener() {

						@Override
						public void onKeyDown(int keyCode) {
						}

						// 这里的监听器逻辑与 updateKeyListeners 中的类似
						@Override
						public void onSingleClick(int keyCode) {
							performAction(0, action.getShortPressAction());
						}

						@Override
						public void onLongClick(int keyCode) {
							performAction(1, action.getLongPressAction());
						}

						@Override
						public void onUnrecognizedEvent(KeyEvent event) {
							// 停止长按检测
							//moveHandler.removeMessages(MSG_MOVE);
							mouseManager.stopMovePointer();
						}

						@Override
						public void onKeyUp(int keycode) {
							if(action.getShortPressAction().contains("上移") || action.getShortPressAction().contains("下移") || action.getShortPressAction().contains("左移") || action.getShortPressAction().contains("右移")){
								// 当按键抬起时，记得取消鼠标的快速移动
								// 停止长按检测
								//moveHandler.removeMessages(MSG_MOVE);
								mouseManager.stopMovePointer();
			
							}
						}
					});
			}
		}
	}


	// 恢复所有按键监听
	public void restoreAllKeyListeners() {
		// 恢复所有按键监听，只需再次调用 updateKeyListeners 方法
		// 这个方法会重新加载所有按键配置并设置监听
		updateKeyListeners(null); // 调用此方法时传入 null，它会自动从 SPUtil 加载数据
	}
	
	// Helper method to convert an action string to a KeyEventPolicy
	private KeyEventPolicy getPolicyFromAction(String action) {
		if ("默认".equals(action)) {
			return KeyEventPolicy.DEFAULT;
		}
		// "默认" and other actions will result in a DEFAULT policy
		return KeyEventPolicy.CONSUME;
	}

// Helper method to execute the action based on its name


	/**
	 * Helper method to show a toast message based on a global flag.
	 *
	 * @param message The message to display.
	 */
	private void showToast(String message) {
		if (shouldShowToast) {
			Toast.makeText(FlowerMouseService.this, message, Toast.LENGTH_SHORT).show();
		}
	}

	
// Helper method to execute the action based on its name
	public void performAction(int islong, String action) {
		// This is where you'll implement the logic for each action
		// You'll need a switch or if-else block here
		switch (action) {
			
			case "取消鼠标移动":
				showTipToast("取消移动被调用");
				//smoothMover.stopMoving();
				break;
				
			case "鼠标上移/上滑":
				// Code for the back button action
				mouseManager.resetAutoHideTimer();
				if(currentMode == 1){
					//鼠标上移
					//mouseManager.updateMousePosition(0,-MOVE_STEP);
					mouseManager.moveStep(0);
					//showToast("鼠标上移/上滑");
				} else if(currentMode == 2) {
					//鼠标上滑
					actionManager.scrollAtMousePosition(MouseActionManager.SCROLL_UP);
					//showToast("鼠标上移/上滑");
				}
				break;
			case "鼠标下移/下滑":
				// Code for the back button action
				mouseManager.resetAutoHideTimer();
				if(currentMode == 1){
					//mouseManager.updateMousePosition(0,MOVE_STEP);
					mouseManager.moveStep(1);
					//showToast("鼠标下移/下滑");
				} else if(currentMode == 2){
					actionManager.scrollAtMousePosition(MouseActionManager.SCROLL_DOWN);
					//showToast("鼠标下移/下滑");
				}
				break;
			case "鼠标左移/左滑(*)":
				// Code for the back button action
				mouseManager.resetAutoHideTimer();
				if(currentMode == 1){
					//mouseManager.updateMousePosition(-MOVE_STEP,0);
					mouseManager.moveStep(2);
					//showToast("鼠标左移/左滑(*)");
				} else if(currentMode == 2){
					actionManager.scrollAtMousePosition(MouseActionManager.SCROLL_LEFT);
					//showToast("鼠标左移/左滑(*)");
				}
				break;
			case "鼠标右移/右滑(*)":
				// Code for the back button action
				mouseManager.resetAutoHideTimer();
				if(currentMode == 1){
					//mouseManager.updateMousePosition(MOVE_STEP,0);
					mouseManager.moveStep(3);
					//showToast("鼠标右移/右滑(*)");
				} else if(currentMode == 2){
					actionManager.scrollAtMousePosition(MouseActionManager.SCROLL_RIGHT);
					//showToast("鼠标右移/右滑(*)");
				}
				break;
			case "鼠标加速上移":
				if(currentMode == 0){
					return;
				}
				mouseManager.resetAutoHideTimer();
				mouseManager.startMovePointer(0);
				//showToast("鼠标加速上移");
				break;
			case "鼠标加速下移":
				// Code for the back button action
				if(currentMode == 0){
					return;
				}
				mouseManager.resetAutoHideTimer();
				mouseManager.startMovePointer(1);
				//showToast("鼠标加速下移");
				break;
			case "鼠标加速左移":
				// Code for the back button action
				if(currentMode == 0){
					return;
				}
				mouseManager.resetAutoHideTimer();
				mouseManager.startMovePointer(2);
				//showToast("鼠标加速左移");
				break;
			case "鼠标加速右移":
				// Code for the back button action
				if(currentMode == 0){
					return;
				}
				mouseManager.resetAutoHideTimer();
				mouseManager.startMovePointer(3);
				//showToast("鼠标加速右移");
				break;
			case "鼠标短按":
				if(currentMode == 0){
					return;
				}
				// Code for the back button action
				actionManager.clickAtMousePosition(false);
				mouseManager.resetAutoHideTimer();
				//showToast("鼠标短按");
				break;
			case "鼠标长按":
				if(currentMode == 0){
					return;
				}
				// Code for the back button action
				actionManager.clickAtMousePosition(true);
				mouseManager.resetAutoHideTimer();
				//showToast("鼠标长按");
				break;
			case "模式切换":
				mouseManager.resetAutoHideTimer();
				// Code for the back button action
				if (currentMode == 0){
					currentMode = 1;
					restoreAllKeyListeners();
					mouseManager.showMouse();
					mouseManager.setMouseMode(MouseManager2.MouseMode.POINTER);
					showTipToast("鼠标模式");
				} else if (currentMode == 1) {
					currentMode = 2;
					mouseManager.setMouseMode(MouseManager2.MouseMode.SCROLL);
					showTipToast("滚动模式");
				} else if (currentMode == 2){
					currentMode = 0;
					updateKeyListeners(null);
					mouseManager.hideMouse();
					showTipToast("按键模式");
				}
				//showToast("模式切换");
				break;
				
			case "鼠标处上滑":
				// Code for the back button action
				actionManager.scrollAtMousePosition(MouseActionManager.SCROLL_UP);
				showToast("鼠标处上滑");
				break;
				
			case "鼠标处下滑":
				// Code for the back button action
				actionManager.scrollAtMousePosition(MouseActionManager.SCROLL_DOWN);
				showToast("鼠标处下滑");
				break;
				
			case "鼠标处左滑(*)":
				// Code for the back button action
				actionManager.scrollAtMousePosition(MouseActionManager.SCROLL_LEFT);
				showToast("鼠标处左滑");
				break;
				
			case "鼠标处右滑(*)":
				// Code for the back button action
				actionManager.scrollAtMousePosition(MouseActionManager.SCROLL_RIGHT);
				showToast("鼠标处右滑");
				break;
				
			case "进入/退出调试模式":
				if(actionManager.inDebugMode){
					actionManager.inDebugMode = false;
					mouseManager.removehighlight();
				} else {
					actionManager.inDebugMode = true;
				}
				break;
				
			case "紧急禁用鼠标":
				//紧急关闭！
				stopSelf();
				android.os.Process.killProcess(android.os.Process.myPid());
				for(int i = 0; i<4; i++){
					System.exit(0);
				}
				break;
				
			case "系统按键一次":
				// Code for the back button action
				allowkeyOnce = true;
				showToast("系统按键一次");
				break;
				
			case "允许/禁止截屏":
				if(mouseManager.isScreenshotAllowed()){
					mouseManager.setAllowScreenshot(false);
				} else {
					mouseManager.setAllowScreenshot(true);
				}
				break;
				
			case "点击菜单":
				showFunctionListDialog();
				break;
				
			case "显示/隐藏鼠标":
				//显示隐藏鼠标只有在鼠标或者滚动模式下才有效。
				if(currentMode == 0){
					return;
				}
				if(mouseManager.isMouseShowed()){
					mouseManager.hideMouse();
				} else {
					mouseManager.showMouse();
				}
				break;
				
				
		//原来A3+按键映射的功能		
			case "音量增强界面":
				// Code for the back button action
				Intent boost = new Intent(FlowerMouseService.this, VolumeBoostActivity.class);
				boost.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

				PendingIntent pendingIntent1 =
					PendingIntent.getActivity(FlowerMouseService.this, 0, boost, 0);
				try {
					pendingIntent1.send();
				} catch (PendingIntent.CanceledException e) {
					startActivity(boost);
					e.printStackTrace();
				}	
				showToast("音量增强界面");
				break;

			case "本应用主页":
				// Code for the back button action
				Intent mainIntent = new Intent(FlowerMouseService.this,MouseMainActivity.class);
				mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(mainIntent);
				showToast("RF鼠标已启动");
				break;
			case "注入退格":
				// Code for the back button action
				RootShellManager.getInstance().injectKeyCode(KeyEvent.KEYCODE_DEL);
				showToast("注入退格");
				break;
			case "注入*":
				// Code for the back button action
				RootShellManager.getInstance().injectKeyCode(KeyEvent.KEYCODE_STAR);
				showToast("注入*");
				break;
			case "拨号界面":
				// Code for the back button action
				Intent intentr = new Intent(FlowerMouseService.this, CallPhoneActivity.class);
				intentr.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

				PendingIntent pendingIntent =
					PendingIntent.getActivity(FlowerMouseService.this, 0, intentr, 0);
				try {
					pendingIntent.send();
				} catch (PendingIntent.CanceledException e) {
					startActivity(intentr);
					e.printStackTrace();
				}	

				showToast("拨号界面");
				break;
			case "返回":
				// Code for the back button action
				performGlobalAction(GLOBAL_ACTION_BACK);
				showToast("返回");
				break;
			case "主页":
				// Code for the home button action
				performGlobalAction(GLOBAL_ACTION_HOME);
				showToast("主页");
				break;
			case "最近任务":
				// Code for the recent apps button action
				performGlobalAction(GLOBAL_ACTION_RECENTS);
				showToast("最近任务");
				break;
			case "电源框":
				// Code to open the power dialog
				performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
				showToast("电源框");
				break;
			case "展开通知栏":
				// Code to expand the notification shade
				performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
				showToast("展开通知栏");
				break;
			case "截屏":
				// Code to take a screenshot

				// 判断是否 ≥ Android 9.0（API 28）
				if (Build.VERSION.SDK_INT >= 28) {
					// 当 API ≥ 28 时执行的代码
					performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
				} else {
					
					if (Build.VERSION.SDK_INT <21){
						showTipToast("该功能在安卓五以下无法使用。");
						return;//安卓5及以下没有截屏。
					}
					// 当 API < 28 时的兼容处理
					mScreenCapture.captureScreenshot(new ScreenCaptureHelper.CaptureCallback() {
							@Override
							public void onCaptureSuccess(Bitmap bitmap) {
								if(bitmap!=null){
									screenshotOverlayManager.showScreenshotOverlay(bitmap);
									showToast("截屏");
								} else {
									showToast("截屏失败");
								}
							}

							@Override
							public void onCaptureError(String error) {
								showToast("截屏失败：" + error);
							}
						});
				}

				break;
			case "锁屏":
				// Code to lock the screen
				// Note: This requires the BIND_DEVICE_ADMIN permission
				// Or you can use a different method if not a device admin
				DeviceAdminUtil.lockScreen();
				showToast("锁屏");
				break;
			case "音量加":
				// Code to increase volume
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
				showToast("音量加");
				break;
			case "音量减":
				// Code to decrease volume
				AudioManager audioManagerDec = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				audioManagerDec.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
				showToast("音量减");
				break;
			case "一键静音":
				// Code to toggle silent mode
				AudioManager audioManagerMute = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				audioManagerMute.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
				showToast("一键静音");
				break;
			case "打开系统设置":
				// Code to open system settings
				Intent settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
				settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(settingsIntent);
				showToast("打开系统设置");
				break;
			case "飞行模式":
				// Code to toggle airplane mode
				// This is a simplified example. Toggling airplane mode directly from an app is complex and may require root access or specific permissions on some Android versions.
				NetworkUtils.setAirplaneModeEnabled(true);
				showToast("飞行模式");
				break;
			case "打开流量":
				// Code to open mobile data settings
				// Note: Toggling mobile data directly requires system permissions that are generally not available to regular apps. Opening the settings is a more common approach.
				NetworkUtils.setMobileDataEnabled(true);
				showToast("打开流量");
				break;
			case "关闭流量":
				// Same as above, directs to settings
				NetworkUtils.setMobileDataEnabled(false);
				showToast("关闭流量");
				break;
			case "切换流量":
				// Same as above, directs to settings
				NetworkUtils.switchMobileData();
				showToast("切换流量 ");
				break;
			case "打开wifi":
				// Code to open Wi-Fi settings
				NetworkUtils.setWifiEnabled(true);
				showToast("打开wifi");
				break;
			case "关闭wifi":
				// Same as above, directs to settings
				NetworkUtils.setWifiEnabled(false);
				showToast("关闭wifi");
				break;
			case "切换wifi":
				// Same as above, directs to settings
				NetworkUtils.switchWiFi();
				showToast("切换wifi");
				break;
			case "热点界面":
				// Code to open Wi-Fi hotspot settings
				Intent hotspotIntent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
				hotspotIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(hotspotIntent);
				//NetworkUtils.setHotspotEnabled(true);
				showToast("热点界面");
				break;
			case "显示录屏界面":
				if (Build.VERSION.SDK_INT < 21){
					showTipToast("该功能在安卓五以下无法使用");
					return;//安卓5及以下没有截屏。
				}
				recordOverlayManager.showRecordOverlay();
				showToast("录屏界面");
				break;
			case "打开键盘灯":
				try {
					RootShellManager.getInstance().setBacklight(true);
				} catch (InterruptedException e) {} catch (SecurityException e) {} catch (IOException e) {}
				showToast("键盘灯开启");
				break;
			case "关闭键盘灯":
				try {
					RootShellManager.getInstance().setBacklight(false);
				} catch (InterruptedException e) {} catch (SecurityException e) {} catch (IOException e) {}
				showToast("键盘灯关闭");
				break;
				
		}
	}
	
	
	
	
	
	
	
	
    @Override
    public void onInterrupt() {
		keyPressManager.resetState();//重置状态
		if (TtsManager.getInstance(this) != null) {
			TtsManager.getInstance(this).shutdown();
		}
		if(homeListenReceiver != null){
			unregisterReceiver(homeListenReceiver);
		}

		if(commonreceiver != null){
			unregisterReceiver(commonreceiver);
		}
    }
	
	
	@Override
    public void onTaskRemoved(android.content.Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        keyPressManager.cleanUp();//清理
		if (TtsManager.getInstance(this) != null) {
			TtsManager.getInstance(this).shutdown();
		}
		if(homeListenReceiver != null){
			unregisterReceiver(homeListenReceiver);
		}

		if(commonreceiver != null){
			unregisterReceiver(commonreceiver);
		}
	}
	
	
	@Override
    public void onDestroy() {
        super.onDestroy();
        keyPressManager.cleanUp();//清理
		if (TtsManager.getInstance(this) != null) {
			TtsManager.getInstance(this).shutdown();
		}
		if(homeListenReceiver != null){
			unregisterReceiver(homeListenReceiver);
		}

		if(commonreceiver != null){
			unregisterReceiver(commonreceiver);
		}
		
	}
	

	

	/**
	 * 根据传入的按键中文名称返回对应的 Keycode。
	 * 如果名称无法识别，返回 -1。
	 * @param keyName 要查询的按键中文名称
	 * @return 对应的 Keycode 或 -1
	 */
	public int getKeyCodeFromName(String keyName) {
		if (keyName == null || keyName.isEmpty()) {
			return -1; // 输入为空，返回无效键值
		}

		// 检查是否是用户添加的键值
		if (keyName.startsWith("▶")) {
			keyName = keyName.replace("▶", "");
		}

		switch (keyName) {
				// 电话/通用功能键
			case "拨号键":
				return KeyEvent.KEYCODE_CALL;
			case "挂机键":
				return KeyEvent.KEYCODE_ENDCALL;
			case "主页键":
				return KeyEvent.KEYCODE_HOME;
			case "菜单键":
				return KeyEvent.KEYCODE_MENU;
			case "返回键":
				return KeyEvent.KEYCODE_BACK;
			case "搜索键":
				return KeyEvent.KEYCODE_SEARCH;
			case "拍照键":
				return KeyEvent.KEYCODE_CAMERA;
			case "拍照对焦键":
				return KeyEvent.KEYCODE_FOCUS;
			case "电源键":
				return KeyEvent.KEYCODE_POWER;
			case "通知键":
				return KeyEvent.KEYCODE_NOTIFICATION;
			case "话筒静音键":
				return KeyEvent.KEYCODE_MUTE;
			case "扬声器静音键":
				return KeyEvent.KEYCODE_VOLUME_MUTE;
			case "音量增加键":
				return KeyEvent.KEYCODE_VOLUME_UP;
			case "音量减小键":
				return KeyEvent.KEYCODE_VOLUME_DOWN;
			case "应用切换键":
				return KeyEvent.KEYCODE_APP_SWITCH;
			case "设置键":
				return KeyEvent.KEYCODE_SETTINGS;
			case "耳机挂断键":
				return KeyEvent.KEYCODE_HEADSETHOOK;

				// 导航/编辑键
			case "回车键":
				return KeyEvent.KEYCODE_ENTER;
			case "ESC键":
				return KeyEvent.KEYCODE_ESCAPE;
			case "导航键-确定":
				return KeyEvent.KEYCODE_DPAD_CENTER;
			case "导航键-向上":
				return KeyEvent.KEYCODE_DPAD_UP;
			case "导航键-向下":
				return KeyEvent.KEYCODE_DPAD_DOWN;
			case "导航键-向左":
				return KeyEvent.KEYCODE_DPAD_LEFT;
			case "导航键-向右":
				return KeyEvent.KEYCODE_DPAD_RIGHT;
			case "光标移动到开始":
				return KeyEvent.KEYCODE_MOVE_HOME;
			case "光标移动到末尾":
				return KeyEvent.KEYCODE_MOVE_END;
			case "向上翻页":
				return KeyEvent.KEYCODE_PAGE_UP;
			case "向下翻页":
				return KeyEvent.KEYCODE_PAGE_DOWN;
			case "退格键":
				return KeyEvent.KEYCODE_DEL;
			case "删除键":
				return KeyEvent.KEYCODE_FORWARD_DEL;
			case "插入键":
				return KeyEvent.KEYCODE_INSERT;
			case "Tab键":
				return KeyEvent.KEYCODE_TAB;

				// 数字/字母键
			case "按键'0'":
				return KeyEvent.KEYCODE_0;
			case "按键'1'":
				return KeyEvent.KEYCODE_1;
			case "按键'2'":
				return KeyEvent.KEYCODE_2;
			case "按键'3'":
				return KeyEvent.KEYCODE_3;
			case "按键'4'":
				return KeyEvent.KEYCODE_4;
			case "按键'5'":
				return KeyEvent.KEYCODE_5;
			case "按键'6'":
				return KeyEvent.KEYCODE_6;
			case "按键'7'":
				return KeyEvent.KEYCODE_7;
			case "按键'8'":
				return KeyEvent.KEYCODE_8;
			case "按键'9'":
				return KeyEvent.KEYCODE_9;
			case "按键'A'":
				return KeyEvent.KEYCODE_A;
			case "按键'B'":
				return KeyEvent.KEYCODE_B;
			case "按键'C'":
				return KeyEvent.KEYCODE_C;
			case "按键'D'":
				return KeyEvent.KEYCODE_D;
			case "按键'E'":
				return KeyEvent.KEYCODE_E;
			case "按键'F'":
				return KeyEvent.KEYCODE_F;
			case "按键'G'":
				return KeyEvent.KEYCODE_G;
			case "按键'H'":
				return KeyEvent.KEYCODE_H;
			case "按键'I'":
				return KeyEvent.KEYCODE_I;
			case "按键'J'":
				return KeyEvent.KEYCODE_J;
			case "按键'K'":
				return KeyEvent.KEYCODE_K;
			case "按键'L'":
				return KeyEvent.KEYCODE_L;
			case "按键'M'":
				return KeyEvent.KEYCODE_M;
			case "按键'N'":
				return KeyEvent.KEYCODE_N;
			case "按键'O'":
				return KeyEvent.KEYCODE_O;
			case "按键'P'":
				return KeyEvent.KEYCODE_P;
			case "按键'Q'":
				return KeyEvent.KEYCODE_Q;
			case "按键'R'":
				return KeyEvent.KEYCODE_R;
			case "按键'S'":
				return KeyEvent.KEYCODE_S;
			case "按键'T'":
				return KeyEvent.KEYCODE_T;
			case "按键'U'":
				return KeyEvent.KEYCODE_U;
			case "按键'V'":
				return KeyEvent.KEYCODE_V;
			case "按键'W'":
				return KeyEvent.KEYCODE_W;
			case "按键'X'":
				return KeyEvent.KEYCODE_X;
			case "按键'Y'":
				return KeyEvent.KEYCODE_Y;
			case "按键'Z'":
				return KeyEvent.KEYCODE_Z;

				// 符号键
			case "按键'+'":
				return KeyEvent.KEYCODE_PLUS;
			case "按键'-'":
				return KeyEvent.KEYCODE_MINUS;
			case "按键'*'":
				return KeyEvent.KEYCODE_STAR;
			case "按键'/'":
				return KeyEvent.KEYCODE_SLASH;
			case "按键'='":
				return KeyEvent.KEYCODE_EQUALS;
			case "按键'@'":
				return KeyEvent.KEYCODE_AT;
			case "按键'#'":
				return KeyEvent.KEYCODE_POUND;
			case "按键'' (单引号)":
				return KeyEvent.KEYCODE_APOSTROPHE;
			case "按键'\\' (反斜杠)":
				return KeyEvent.KEYCODE_BACKSLASH;
			case "按键','":
				return KeyEvent.KEYCODE_COMMA;
			case "按键'.'":
				return KeyEvent.KEYCODE_PERIOD;
			case "按键'['":
				return KeyEvent.KEYCODE_LEFT_BRACKET;
			case "按键']'":
				return KeyEvent.KEYCODE_RIGHT_BRACKET;
			case "按键';'":
				return KeyEvent.KEYCODE_SEMICOLON;
			case "按键'`'":
				return KeyEvent.KEYCODE_GRAVE;
			case "空格键":
				return KeyEvent.KEYCODE_SPACE;

				// 修饰键
			case "左Alt键":
				return KeyEvent.KEYCODE_ALT_LEFT;
			case "右Alt键":
				return KeyEvent.KEYCODE_ALT_RIGHT;
			case "左Control键":
				return KeyEvent.KEYCODE_CTRL_LEFT;
			case "右Control键":
				return KeyEvent.KEYCODE_CTRL_RIGHT;
			case "左Shift键":
				return KeyEvent.KEYCODE_SHIFT_LEFT;
			case "右Shift键":
				return KeyEvent.KEYCODE_SHIFT_RIGHT;

				// 多媒体键
			case "多媒体键-播放":
				return KeyEvent.KEYCODE_MEDIA_PLAY;
			case "多媒体键-停止":
				return KeyEvent.KEYCODE_MEDIA_STOP;
			case "多媒体键-暂停":
				return KeyEvent.KEYCODE_MEDIA_PAUSE;
			case "多媒体键-播放/暂停":
				return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
			case "多媒体键-快进":
				return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
			case "多媒体键-快退":
				return KeyEvent.KEYCODE_MEDIA_REWIND;
			case "多媒体键-下一首":
				return KeyEvent.KEYCODE_MEDIA_NEXT;
			case "多媒体键-上一首":
				return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
			case "多媒体键-关闭":
				return KeyEvent.KEYCODE_MEDIA_CLOSE;
			case "多媒体键-弹出":
				return KeyEvent.KEYCODE_MEDIA_EJECT;
			case "多媒体键-录音":
				return KeyEvent.KEYCODE_MEDIA_RECORD;

				// 游戏手柄键
			case "游戏手柄按钮-A":
				return KeyEvent.KEYCODE_BUTTON_A;
			case "游戏手柄按钮-B":
				return KeyEvent.KEYCODE_BUTTON_B;
			case "游戏手柄按钮-X":
				return KeyEvent.KEYCODE_BUTTON_X;
			case "游戏手柄按钮-Y":
				return KeyEvent.KEYCODE_BUTTON_Y;
				// ... (根据需要添加更多游戏手柄键)
			default:
				try {
					return Integer.parseInt(keyName);
				} catch (NumberFormatException e) {
					return -1; // 格式不正确，返回无效键值
				}
		}
	}
	
		
		
	//记录用户选中的操作
	String diaaction ="";
	
	private void showFunctionListDialog() {
		
		spaceMenu = true;//系统按键空出来

		String[] items = new String[]{"系统按键一次","紧急禁用鼠标","鼠标短按", "鼠标长按","鼠标处上滑", "鼠标处下滑", "鼠标处左滑(*)", "鼠标处右滑(*)"};

		overlayHelper.showListDialog(true,items, new SystemOverlayHelper.OnListItemClickListener() {

				@Override
				public void onDismiss() {
					spaceMenu = false;//恢复系统按键监听
					//我们在这里执行功能,因为首饰可能会被其他的触摸所取消。
					if(!diaaction.isEmpty()){
						performAction(0,diaaction);
						diaaction = "";
					}
				}

				@Override
				public void onListItemClick(int position, String text) {
					// 处理点击事件
					Toast.makeText(FlowerMouseService.this, "您选择了: " + text, Toast.LENGTH_SHORT).show();
					diaaction = text;
				}
			});
	}
	
	
	
	//显示提示文本
	private void showTipToast(String Tip) {
		// 无图标
		//overlayHelper.showCustomToast("这是一个自定义Toast！", Toast.LENGTH_SHORT, null);

		// 有图标
		// 确保你的drawable目录里有相应的图标文件，比如 R.drawable.ic_success
		Drawable successIcon = getResources().getDrawable(R.drawable.ic_launcher);
		overlayHelper.showCustomToast(Tip, Toast.LENGTH_LONG, successIcon);
	}



	//注册系统按键监听
    public void registerHomeListenReceiver() {

	    homeListenReceiver = new HomeListenReceiver();
	    IntentFilter intentFilter = new IntentFilter();
		//home,recent
	    intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

	    System.out.println("I'm coming, myBroadCastReceiver注册了!");
		intentFilter.setPriority(Integer.MAX_VALUE);
	    registerReceiver(homeListenReceiver, intentFilter);	
	}

	//注销桌面按键监听
	public void unregisterHomeListenReceiver() {
		if(homeListenReceiver != null){
			unregisterReceiver(homeListenReceiver);
		}
	}

	//注册home键监听器
	private class HomeListenReceiver extends BroadcastReceiver{
		@Override
	   	public void onReceive(Context context, Intent intent) {
			//你自己先把 reasons == homekey 和 长按homekey 排除，剩下的做下面的处理
			String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
			if (intent.getAction().equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)){
				System.out.println("Intent.ACTION_CLOSE_SYSTEM_DIALOGS : " + intent.getStringExtra("reason"));
				if (intent.getExtras()!=null && intent.getExtras().getBoolean("myReason")){
					homeListenReceiver.abortBroadcast();
				}else if (reason != null){

					if (reason.equalsIgnoreCase("globalactions")){
						//屏蔽电源长按键的方法：
						System.out.println("电源  键被长按");
					} else if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason) || SYSTEM_DIALOG_REASON_ASSIST.equals(reason)) {

						handleHomeClick();
					} else if (SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
						// 长按Home键 或者 activity切换键
						//Toast.makeText(KeyAccessibilityService.this,"home  键被长按",Toast.LENGTH_LONG).show();

					} else if (SYSTEM_DIALOG_REASON_LOCK.equals(reason)) {
						// 锁屏

					}

				}
			}
		}
	}


	//处理挂机，也就是home键点击
	public void handleHomeClick(){
		clickNum++;

		homeHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (clickNum == 1) {
						// 短按Home键
						//Toast.makeText(KeyAccessibilityService.this,"home 键被按",Toast.LENGTH_LONG).show();
							DeviceAdminUtil.lockScreen();
					}else if(clickNum==2){
						Log.d("btn listener:", "btn is doubleClicked!");
						FlowerMouseService.getInstance().performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
						// 短按Home键
						//Toast.makeText(KeyAccessibilityService.this,"home 键被双按",Toast.LENGTH_LONG).show();
					}
					//防止handler引起的内存泄漏
					homeHandler.removeCallbacksAndMessages(null);
					clickNum = 0;
				}
			},300);
	}
	
	
	

	//普通广播
	private void registerCommonReceiver(){
		commonreceiver=new CommonReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		filter.addAction(Intent.ACTION_BATTERY_LOW);
		filter.addAction(Intent.ACTION_BATTERY_OKAY);
		//屏幕
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);

		registerReceiver(commonreceiver, filter);
	}

	//普通广播监听器
	private class CommonReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			switch (action) {
				case Intent.ACTION_SCREEN_ON:
					
					break;

				case Intent.ACTION_SCREEN_OFF:
					//不在鼠标模式，则不执行
					//息屏
					if(SPUtil.getBoolean(SPUtil.KEY_LOCK_BACK, true) && currentMode != 0){
						mouseManager.notifyScreenOrientationChange();//调用屏幕改变函数回正
						showToast("鼠标回到中心");
					}
					
			        break;

			}
		}

	}



	
	
}


