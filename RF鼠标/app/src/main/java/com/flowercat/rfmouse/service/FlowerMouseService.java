package com.flowercat.rfmouse.service;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;
import com.flowercat.rfmouse.MouseMainActivity;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.floatview.RecordOverlayManager;
import com.flowercat.rfmouse.floatview.ScreenshotOverlayManager;
import com.flowercat.rfmouse.key.KeyAction;
import com.flowercat.rfmouse.key.KeyEventPolicy;
import com.flowercat.rfmouse.key.KeyPressManager;
import com.flowercat.rfmouse.key.OnKeyEventListener;
import com.flowercat.rfmouse.mouse.MouseActionManager;
import com.flowercat.rfmouse.mouse.MouseManager2;
import com.flowercat.rfmouse.ui.CallPhoneActivity;
import com.flowercat.rfmouse.ui.MouseOverSettingActivity;
import com.flowercat.rfmouse.ui.VolumeBoostActivity;
import com.flowercat.rfmouse.util.BitmapManager;
import com.flowercat.rfmouse.util.DeviceAdminUtil;
import com.flowercat.rfmouse.util.KeyCodeUtil;
import com.flowercat.rfmouse.util.LoudnessEnhancerUtil;
import com.flowercat.rfmouse.util.NetworkUtils;
import com.flowercat.rfmouse.util.RootShellManager;
import com.flowercat.rfmouse.util.SPUtil;
import com.flowercat.rfmouse.util.ScreenCaptureHelper;
import com.flowercat.rfmouse.util.SystemOverlayHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import android.provider.Settings;
import android.net.Uri;
import com.flowercat.rfmouse.ui.InjectSettingActivity;


//辅助服务点击核心类
public class FlowerMouseService extends AccessibilityService {

    private static final String TAG = "FlowerMouseService";
	private static FlowerMouseService mMouseService;//单例模式
	public KeyPressManager keyPressManager;//按键管理
	public MouseManager2 mouseManager;//鼠标管理
	public MouseActionManager actionManager;//鼠标动作管理

	
	//定义一个全局布尔变量来控制是否显示Toast
	public static boolean shouldShowToast = false;
	
	//当前鼠标处于何种模式？点击，滚动，按键，或者拖动
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
	
	public static boolean isSelfStop = false;
	//鼠标点击模式设置。
	public String mode ;
	
	public boolean isMouseDown = true;
	
	// Define a state variable outside of the function that gets called repeatedly.
	// For example, as a member variable of your class.
	private String lastPackageName = null;
	
	private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    private static final String SYSTEM_DIALOG_REASON_LOCK = "lock";
    private static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";
	
	public static volatile List<MouseOverSettingActivity.AppInfo> mAppList = new ArrayList<>();

	// 广播相关
    private static final String SERVER_STARTED_ACTION = "com.flowercat.rfmouse.SERVER_STARTED";
    private BroadcastReceiver serverStatusReceiver;
    private boolean isReceiverRegistered = false;
	
	
	
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
	
	
	public void stopRecordKeyPress(){
		if(this.keyRecord != null){
			this.keyRecord = null;
		}
		isInKeyRecord = false;
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
		
		//loadMouseData();
		
		actionManager = MouseActionManager.getInstance(this,mouseManager);
		//smoothMover = SmoothMouseMover.getInstance(mouseManager,screenWidth,screenHeight);//鼠标移动管理类
		
		keyPressManager.resetState();//重置按键监听
		updateKeyListeners(null);//设置按键监听，从存储的列表中
		
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
								if(videoPath.isEmpty()){
									Toast.makeText(FlowerMouseService.this, "录制时长小于3秒", Toast.LENGTH_SHORT).show();
									return;
								}
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
		
		
		//锁屏与亮屏监听
		registerCommonReceiver();
		setupServerBroadcastReceiver();
		
		
		updateServiceConfig();
		
		//showMyToast();
		Toast.makeText(FlowerMouseService.this,"鼠标服务开启。",Toast.LENGTH_SHORT).show();
		
		
		if(mode.equals("mgr")){
			//开机启动
			Intent intent1 = new Intent(getApplicationContext(), InjectSettingActivity.class);
			intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent1.putExtra("startMgr","所谓成长，不过是童年渐行渐远的背影");
			startActivity(intent1);
		}
    }


	
	/**
     * 设置服务器启动广播接收器
     */
    private void setupServerBroadcastReceiver() {
        serverStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (SERVER_STARTED_ACTION.equals(action)) {
                    // 服务器启动成功，跳转到那个设置页面。
                    //开机启动
					Intent intent1 = new Intent(getApplicationContext(), InjectSettingActivity.class);
					intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					intent1.putExtra("startMgr","所谓成长，不过是童年渐行渐远的背影");
					intent1.putExtra("startAcce","。");
					startActivity(intent1);
                }
            }
        };

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVER_STARTED_ACTION);
        registerReceiver(serverStatusReceiver, filter);
        isReceiverRegistered = true;
    }
	
	//取消服务器启动广播监听。
	 private void unServerBroadcastReceiver() {
		// 取消注册广播接收器
		if (isReceiverRegistered && serverStatusReceiver != null) {
			try {
				unregisterReceiver(serverStatusReceiver);
				isReceiverRegistered = false;
			} catch (Exception e) {
				// 忽略可能的异常，如接收器未注册
			}
		}
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
	
	
	//更新所有鼠标里的方法
	public void updateServiceConfig() {
		
		loadMouseData();
		
		//当前音量提高百分比不为零。
		if(SPUtil.getInt(SPUtil.KEY_UP_PERCENT,0) != 0){
			//恢复上次设置的音量
			if(FlowerMouseService.getInstance() != null && FlowerMouseService.getInstance().enhancerUtil != null){
				FlowerMouseService.getInstance().enhancerUtil.setEnhanceGain(SPUtil.getInt(SPUtil.KEY_UP_PERCENT,0), 100);
			}
		}

		//是否允许使用Home锁屏？
		if(SPUtil.getBoolean(SPUtil.KEY_HOME_KEY_LOCK, false)){
			registerHomeListenReceiver();
		}
		
		
		//开启鼠标
		if(SPUtil.getBoolean(SPUtil.KEY_MOUSE_ON_BOOT, false)){
			currentMode = 1;
			restoreAllKeyListeners();
			mouseManager.showMouse();
			mouseManager.setMouseMode(MouseManager2.MouseMode.POINTER);
			showTipToast("鼠标模式");
		}


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mode= SPUtil.getString(SPUtil.KEY_CLICK_MODE,"gesture");
		} else {
			mode= SPUtil.getString(SPUtil.KEY_CLICK_MODE,"node");
		}

		if(mode.equals("gesture")){
			actionManager.setClickMode(2);
		} else if(mode.equals("node")){
			actionManager.setClickMode(1);
		} else if(mode.equals("mgr")){
			actionManager.setClickMode(3);
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


		int mgr_short= SPUtil.getInt(SPUtil.KEY_MGR_SHORT, 50);
		int mgr_long = SPUtil.getInt(SPUtil.KEY_MGR_LONG, 2);
		int scroll_time = SPUtil.getInt(SPUtil.KEY_MGR_SCROLL, 300);
		int mgr_scroll_ud = SPUtil.getInt(SPUtil.KEY_MGR_UD_DIS, 25);
		int mgr_scroll_lr = SPUtil.getInt(SPUtil.KEY_MGR_LR_DIS, 25);


		actionManager.setShortClickTimeMgr(mgr_short);
		actionManager.setLongClickTimeMgr(mgr_long);
		actionManager.setScrollTimeMgr(scroll_time);
		actionManager.setScrollDisUDMgr(mgr_scroll_ud);
		actionManager.setScrollDisLRMgr(mgr_scroll_lr);


		//加载应用列表
		SPUtil.loadAppList(this,mAppList);
		
		
	}
	
	
	
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
		
		
		if(isSelfStop){
			return;
		}
		
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
		
			

		//注意过滤掉自己的弹窗
		if (!packageName.equals(lastPackageName) && !packageName.contains(getPackageName())) {
         	lastPackageName = packageName;
			
			if(mAppList != null){

				if(isAppSaved(packageName)){
					switch(getAppMode(packageName)){
						case 0:
							currentMode = 1;
							restoreAllKeyListeners();
							mouseManager.showMouse();
							mouseManager.setMouseMode(MouseManager2.MouseMode.POINTER);
							showTipToast("鼠标模式");
							break;
						case 1:
							currentMode = 2;
							restoreAllKeyListeners();
							mouseManager.showMouse();
							mouseManager.setMouseMode(MouseManager2.MouseMode.SCROLL);
							showTipToast("滚动模式");
							break;
						case 2:
							currentMode = 0;
							updateKeyListeners(null);
							mouseManager.hideMouse();
							showTipToast("按键模式");
							break;

					}
				}
			}
		
		}
			
		
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
                    //showTipToast("输入法弹出");
					spaceMenu = true;
					//记得把系统按键空出来。
					
                } else {
                    Log.d(TAG, "IME state changed: Closed.");
                    //showTipToast("输入法关闭");
					spaceMenu = false;
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
	
	
	public void updateAppList(List<MouseOverSettingActivity.AppInfo> newList) {
		this.mAppList = newList;

		// 可能还需要做一些处理，比如重新评估当前应用的模式
	}
	

	
	/**
     * 获取指定包名对应的模式
     * @param packageName 要查询的包名
     * @return 对应的模式（0, 1, 2），如果未找到则返回-1
     */
    public int getAppMode(String packageName) {
		
		if(mAppList == null || mAppList.isEmpty()){
			return -1;
		}
        for (MouseOverSettingActivity.AppInfo app : mAppList) {
            if (app.getPackageName().equals(packageName)) {
                return app.getMode();
            }
        }
        return -1; // 返回-1表示未找到
    }

	/**
     * 判断某个包名是否已在列表中
     * @param packageName 要检查的包名
     * @return 如果存在返回true，否则返回false
     */
    public boolean isAppSaved(String packageName) {
		if(mAppList == null || mAppList.isEmpty()){
			return false;
		}
		
        for (MouseOverSettingActivity.AppInfo app : mAppList) {
            if (app.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
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
			int keyCode = KeyCodeUtil.getKeyCodeFromName(action.getKeyName());
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
			int keyCode = KeyCodeUtil.getKeyCodeFromName(action.getKeyName());
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
				}else if(currentMode == 3) {
					mouseManager.moveStep(0);
					actionManager.mouseDragFunction("move",mouseManager.getMouseX(),mouseManager.getMouseY());
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
				}else if(currentMode == 3) {
					mouseManager.moveStep(1);
					actionManager.mouseDragFunction("move",mouseManager.getMouseX(),mouseManager.getMouseY());
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
				}else if(currentMode == 3) {
					mouseManager.moveStep(2);
					actionManager.mouseDragFunction("move",mouseManager.getMouseX(),mouseManager.getMouseY());
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
				}else if(currentMode == 3) {
					mouseManager.moveStep(3);
					actionManager.mouseDragFunction("move",mouseManager.getMouseX(),mouseManager.getMouseY());
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
				if(currentMode == 0 || currentMode == 3){
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
				mouseManager.resetAutoHideTimer();
				
				if(currentMode == 3){
					
					if(isMouseDown){
						actionManager.mouseDragFunction("up",mouseManager.getMouseX(),mouseManager.getMouseY());
						showTipToast("拖动结束");
						isMouseDown = false;
					} else {
						actionManager.mouseDragFunction("down",mouseManager.getMouseX(),mouseManager.getMouseY());
						showTipToast("拖动开始");
						isMouseDown = true;
					}
					
					return;
				}
				// Code for the back button action
				actionManager.clickAtMousePosition(true);
				
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
					
					mouseManager.showDragHighLight = false;
					isMouseDown = false;
					actionManager.mouseDragFunction("up",mouseManager.getMouseX(),mouseManager.getMouseY());
					
					showTipToast("鼠标模式");
				} else if (currentMode == 1) {
					currentMode = 2;
					mouseManager.setMouseMode(MouseManager2.MouseMode.SCROLL);
					
					mouseManager.showDragHighLight = false;
					isMouseDown = false;
					actionManager.mouseDragFunction("up",mouseManager.getMouseX(),mouseManager.getMouseY());
				
					showTipToast("滚动模式");
				} else if (currentMode == 2 && mode.equals("mgr")){
					//如果当前在触摸注入模式
					currentMode = 3;
					//拖动模式其实就是鼠标模式的复用
					restoreAllKeyListeners();
					mouseManager.showMouse();
					mouseManager.setMouseMode(MouseManager2.MouseMode.POINTER);
					mouseManager.showDragHighLight = true;
					actionManager.mouseDragFunction("down",mouseManager.getMouseX(),mouseManager.getMouseY());
					isMouseDown = true;
					showTipToast("拖动模式，长按“确认”开始/结束拖动");
				} else if(currentMode == 2 && !mode.equals("mgr")){
					currentMode = 0;
					updateKeyListeners(null);
					mouseManager.hideMouse();
					
					mouseManager.showDragHighLight = false;
					isMouseDown = false;
					actionManager.mouseDragFunction("up",mouseManager.getMouseX(),mouseManager.getMouseY());
					
					showTipToast("按键模式");
				} else if(currentMode == 3){
					currentMode = 0;
					updateKeyListeners(null);
					mouseManager.hideMouse();
					
					mouseManager.showDragHighLight = false;
					isMouseDown = false;
					actionManager.mouseDragFunction("up",mouseManager.getMouseX(),mouseManager.getMouseY());
					
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
				aggressivelyStopSelf();
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
				
				if(currentMode == 3){

					if(isMouseDown){
						actionManager.mouseDragFunction("up",mouseManager.getMouseX(),mouseManager.getMouseY());
						showTipToast("拖动结束");
						isMouseDown = false;
					} else {
						actionManager.mouseDragFunction("down",mouseManager.getMouseX(),mouseManager.getMouseY());
						showTipToast("拖动开始");
						isMouseDown = true;
					}

					return;
				}
				
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
				
			case "切换键盘灯":
				try {
					RootShellManager.getInstance().toggleBacklight();
				} catch (InterruptedException e) {} catch (SecurityException e) {} catch (IOException e) {}
				showToast("键盘灯切换");
				break;
				
		}
	}
	
	
	

    @Override
    public void onInterrupt() {
		ReleaseAll();
    }
	
	
	@Override
    public void onTaskRemoved(android.content.Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        ReleaseAll();
	}
	
	
	@Override
    public void onDestroy() {
        super.onDestroy();
        ReleaseAll();
		
	}
	
	
	
	//全部梭哈…
	public void ReleaseAll() {

		// 1. 清理按键管理器
		if (keyPressManager != null) {
			keyPressManager.cleanUp();
			keyPressManager = null;
		}

		// 2. 清理鼠标管理器
		if (mouseManager != null) {
			mouseManager.hideMouse();
			mouseManager = null;
		}

		// 3. 清理鼠标动作管理器
		if (actionManager != null) {
			actionManager.clearHighlight();
			actionManager = null;
		}
		
		// 5. 清理对话框工具类
		if (overlayHelper != null) {
			overlayHelper = null;
		}

		// 6. 清理截屏窗口管理器
		if (screenshotOverlayManager != null) {
			screenshotOverlayManager = null;
		}

		// 7. 清理录屏窗口管理器
		if (recordOverlayManager != null) {
			recordOverlayManager.hideRecordOverlay();
			recordOverlayManager = null;
		}

		// 8. 清理截屏工具类
		if (mScreenCapture != null) {
			mScreenCapture = null;
		}

		// 9. 清理音量增强器
		if (enhancerUtil != null) {
			enhancerUtil.release();
			enhancerUtil = null;
		}

		// 10. 取消注册广播接收器
		if (homeListenReceiver != null) {
			unregisterReceiver(homeListenReceiver);
			homeListenReceiver = null;
		}

		if (commonreceiver != null) {
			unregisterReceiver(commonreceiver);
			commonreceiver = null;
		}

		// 11. 清理Handler和回调
		if (homeHandler != null) {
			homeHandler.removeCallbacksAndMessages(null);
			homeHandler = null;
		}

		// 12. 清理按键录制监听器
		if (keyRecord != null) {
			keyRecord = null;
		}

		// 13. 清理应用列表
		if (mAppList != null) {
			mAppList.clear();
			mAppList = null;
		}
		
		
		unServerBroadcastReceiver();

		// 14. 重置单例实例
		mMouseService = null;

		Log.d(TAG, "FlowerMouseService destroyed and all resources released");
	}
	
	
	


	public void aggressivelyStopSelf() {
		
		FlowerMouseService.getInstance().currentMode = 0;
		FlowerMouseService.getInstance().updateKeyListeners(null);
		FlowerMouseService.getInstance().mouseManager.hideMouse();

		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			// Android 7.0 (API 24) 及以上版本
			spaceMenu = true;
			disableSelf();
			Log.d(TAG, "辅助服务已调用 disableSelf() 停止。");
			ReleaseAll();
			killAppProcess();
		} else {
			// 在较旧版本上，可以尝试其他方法，但可靠性较低，且可能无法禁用设置中的开关。
			Log.d(TAG, "设备版本低于 N (24)，尝试调用 Service.stopSelf()...");
			spaceMenu = true;
			// 清除所有监听
			keyPressManager.cleanUp();
			isSelfStop = true;
			showTipToast("鼠标已禁用！如要再次使用，请强制停止应用或重启后授权");
			
			//stopSelf();
			// 在某些旧版本上，您可能还需要尝试：
			// Process.killProcess(Process.myPid()); 
			// 但这通常会导致重启。
		}

			// 可选：为了确保在某些情况下活动和任务栈也结束，可以使用 finishAffinity()，但这需要在一个 Activity 中调用。
			// 如果是从服务中调用，则需要发送一个广播或通过其他方式通知 Activity 结束。
			
			
			
			
		}

	
		
	public void killAppProcess()
	{
		//注意：不能先杀掉主进程，否则逻辑代码无法继续执行，需先杀掉相关进程最后杀掉主进程
		ActivityManager mActivityManager = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> mList = mActivityManager.getRunningAppProcesses();
		for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : mList) 
		{
			if (runningAppProcessInfo.pid != android.os.Process.myPid()) 
			{
				android.os.Process.killProcess(runningAppProcessInfo.pid);
			}
		}
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
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
	public void showTipToast(String Tip) {
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
	
	
	private final static int COUNTS = 4;// 点击次数
	private final static long DURATION = 10000;// 规定有效时间
	private long[] mHits = new long[COUNTS];

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
					continuousClick(COUNTS, DURATION);
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


	private void continuousClick(int count, long time) {
		//每次点击时，数组向前移动一位
		System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
		//为数组最后一位赋值
		mHits[mHits.length - 1] = SystemClock.uptimeMillis();
		if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
			mHits = new long[COUNTS];//重新初始化数组
			Toast.makeText(this, "连续锁屏了4次!鼠标禁用！", Toast.LENGTH_LONG).show();
			if(FlowerMouseService.getInstance() != null){
				FlowerMouseService.getInstance().spaceMenu = true;
				FlowerMouseService.getInstance().aggressivelyStopSelf();
			}
		}
	}

	
	
}


