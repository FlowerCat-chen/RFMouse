package com.flowercat.rfmouse;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;
import com.flowercat.rfmouse.ui.AboutActivity;
import com.flowercat.rfmouse.ui.AdvanceSettingsActivity;
import com.flowercat.rfmouse.ui.BaseActivity;
import com.flowercat.rfmouse.ui.FunctionExecutor;
import com.flowercat.rfmouse.ui.HelloScreen;
import com.flowercat.rfmouse.ui.KeyConfigActivity;
import com.flowercat.rfmouse.ui.MouseSettingsActivity;
import com.flowercat.rfmouse.ui.PermissionRequestActivity;
import com.flowercat.rfmouse.ui.TestActivity;
import com.flowercat.rfmouse.util.RootPermissionGranter;
import com.flowercat.rfmouse.util.RootShellManager;
import com.flowercat.rfmouse.util.SPUtil;
import java.util.List;
import com.flowercat.rfmouse.util.RootPermissionGranter;
import android.view.View;
import com.flowercat.rfmouse.ui.AppListActivity;
import com.flowercat.rfmouse.ui.ResolutionSettingsActivity;
import com.flowercat.rfmouse.ui.ToolsActivity;
import android.os.Build;
import com.flowercat.rfmouse.util.DeviceAdminUtil;
import android.net.Uri;
import com.flowercat.rfmouse.ui.MouseOverSettingActivity;
import com.flowercat.rfmouse.compati.CompatibilityTest;
import com.flowercat.rfmouse.compati.CompatibilityKeySetting;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.donate.DonateActivity;
import com.flowercat.rfmouse.adb.ShellTestActivity;
import com.flowercat.rfmouse.compati.MIUIUtils;
import com.flowercat.rfmouse.ui.InjectSettingActivity;
import com.flowercat.rfmouse.compati.AssetsCopyUtil;
import com.flowercat.rfmouse.util.SPHelper;
import com.flowercat.rfmouse.ui.ConfigManagerActivity;
import com.flowercat.rfmouse.ui.WizardActivity;
import com.flowercat.rfmouse.ui.CompTotalActivity;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;

public class MouseMainActivity extends BaseActivity {


	String mode;//鼠标点击模式
	boolean isAdded = false;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注意：这里不再需要 setContentView，因为 BaseActivity 已经设置了
        // 如果你的子类需要额外的视图除了 ListView，你可以在 onCreate 后添加
        // 但对于只需要 ListView 的情况，这样写很简洁
		//如果是第一次运行显示欢迎界面
		
		//如果是第一次运行显示欢迎界面
		if(SPUtil.isFirstRun()){
			//跳转欢迎页面
			Intent hello = new Intent (this,HelloScreen.class);
			startActivity(hello);  
			
		}
		
    }

    @Override
    protected void initializeSections() {
		
	
		addSection("权限申请", PermissionRequestActivity.class, Color.BLACK, Color.parseColor("#FFEF9A9A"));

		addSection("兼容性",CompTotalActivity.class, Color.BLACK, Color.parseColor("#99e195")); // 深灰文字+浅蓝灰背景，柔和且独特

		addSection("捐赠", DonateActivity.class, Color.BLACK, Color.parseColor("#8483da"));

		addSection("按键映射", KeyConfigActivity.class, Color.BLACK, Color.parseColor("#FF80DEEA"));

		addSection("鼠标设置", MouseSettingsActivity.class, Color.BLACK, Color.parseColor("#FFEAF2B2")); 
		
		addSection("高级功能", AdvanceSettingsActivity.class, Color.BLACK, Color.parseColor("#FFD1C4E9")); 

		addSection("小工具", ToolsActivity.class, Color.BLACK, Color.parseColor("#fca295")); 

		addSection("关于", AboutActivity.class, Color.BLACK, Color.parseColor("#d6b364"));
		
		
		addSection("卸载本应用", new FunctionExecutor() {
				@Override
				public void execute() {
					//取消激活
					DeviceAdminUtil.cancelDeviceAdmin();
					
					if(FlowerMouseService.getInstance() != null){
						FlowerMouseService.getInstance().spaceMenu = true;
						FlowerMouseService.getInstance().aggressivelyStopSelf();
					}
					
					uninstallSelf();
				}
			},Color.BLACK, Color.parseColor("#ff4300"));
		
		
    }




    public void hideBottomUIMenu() {
        int flags;
        int curApiVersion = android.os.Build.VERSION.SDK_INT;
        // This work only for android 4.4+
        if(curApiVersion >= Build.VERSION_CODES.KITKAT){

            // This work only for android 4.4+
            // hide navigation bar permanently in android activity
            // touch the screen, the navigation bar will not show

            flags = View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_IMMERSIVE
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }else{
            // touch the screen, the navigation bar will show
            flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }

        // must be executed in main thread :)
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		//super.onWindowFocusChanged(hasFocus);
		hideBottomUIMenu();
	}

	
	//卸载自己
	private void uninstallSelf() {
		Intent intent = new Intent(Intent.ACTION_DELETE);
		intent.setData(Uri.parse("package:" + getPackageName())); // 获取当前应用的包名
		startActivity(intent); // 启动卸载意图
	}
	
	
	
	@Override
	protected void onNewIntent(Intent intent) {
		
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().currentMode = 0;
			FlowerMouseService.getInstance().updateKeyListeners(null);
			FlowerMouseService.getInstance().mouseManager.hideMouse();
			FlowerMouseService.getInstance().showTipToast("按键模式");
			FlowerMouseService.getInstance().spaceMenu = true;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().spaceMenu = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().spaceMenu = false;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().currentMode = 0;
			FlowerMouseService.getInstance().updateKeyListeners(null);
			FlowerMouseService.getInstance().mouseManager.hideMouse();
			FlowerMouseService.getInstance().showTipToast("按键模式");
			FlowerMouseService.getInstance().spaceMenu = true;
		}
		
		//鼠标点击模式设置获取
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mode= SPUtil.getString(SPUtil.KEY_CLICK_MODE,"gesture");
		} else {
			mode= SPUtil.getString(SPUtil.KEY_CLICK_MODE,"node");
		}
		
		// 使用 addSectionToTop 辅助方法来添加列表项和对应的动作
		if(mode.equals("mgr") && !isAdded){
			addSectionToTop("触摸注入设置", InjectSettingActivity.class,Color.BLACK, Color.parseColor("#bb6fae")); 
			isAdded = true;
		}
	
		if(checkOverlayPermission() && isAccessibilityServiceEnabled() && checkReadWritePermission()){
			setAdviseText("必要权限已授予～");
		} else {
				if(mode.equals("mgr")){
					blinkItem(1);
				} else {
					blinkItem(0);
				}
		}
	}

	
	
	// 检查悬浮窗权限
    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // 低于 Android M 的系统默认有权限
    }
	
	//检查辅助服务是否打开？
    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // 忽略
        }

        if (accessibilityEnabled == 1) {
            String service = getPackageName() + "/" + FlowerMouseService.class.getName();
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null && settingValue.contains(service)) {
                return true;
            }
        }
        return false;
    }
	

    // 检查读写权限
    private boolean checkReadWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // 低于 Android M 的系统默认有权限
    }
	
	
	
	
}


