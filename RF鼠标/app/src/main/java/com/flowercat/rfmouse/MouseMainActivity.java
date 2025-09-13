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


public class MouseMainActivity extends BaseActivity {


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
        // 使用 addSection 辅助方法来添加列表项和对应的动作
        addSection("权限申请", PermissionRequestActivity.class,Color.BLACK, Color.parseColor("#FFEF9A9A")); 

		addSection("按键设置", KeyConfigActivity.class, Color.BLACK, Color.parseColor("#FF80DEEA"));

		addSection("鼠标设置", MouseSettingsActivity.class, Color.BLACK, Color.parseColor("#FFEAF2B2")); 

		addSection("测试", TestActivity.class, Color.BLACK, Color.parseColor("#FF88C6F3")); 

		addSection("高级功能", AdvanceSettingsActivity.class, Color.BLACK, Color.parseColor("#FFD1C4E9")); 

        addSection("关于", AboutActivity.class, Color.BLACK, Color.parseColor("#d6b364")); 
		
		addSection("小工具", ToolsActivity.class, Color.BLACK, Color.parseColor("#fca295")); 
		
    }

    // MainActivity 自己的特殊方法
    private void performMySpecificAction() {
        Toast.makeText(this, "这是 MyListActivity 特有的操作！", Toast.LENGTH_SHORT).show();
    }

	
	// 在 Activity 类内部调用此方法
	private void showToastOnMainThread(final String toastContent) {
		// 切换到主线程
		runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 主线程中弹出 Toast
					Toast.makeText(MouseMainActivity.this, toastContent, Toast.LENGTH_SHORT).show();
				}
			});
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

	
	
	
}


