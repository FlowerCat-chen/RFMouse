package com.flowercat.rfmouse.ui;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import com.flowercat.rfmouse.util.DeviceAdminUtil;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.adb.ShellTestActivity;

public class TestActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);     
		hideInfo();
		setAdviseText("测试界面");
    }
    
	
	@Override
	protected void initializeSections() {
		// 使用 addSection 辅助方法来添加列表项和对应的动作
		
		addSection("鼠标测试",MouseTestActivity.class, Color.BLACK, Color.parseColor("#4c9b0f"));
		
        addSection("锁屏测试", new FunctionExecutor() {
				@Override
				public void execute() {
					DeviceAdminUtil.lockScreen();			
				}
			});

		addSection("拨打电话", new FunctionExecutor() {
				@Override
				public void execute() {
					Intent intent = new Intent(TestActivity.this, CallPhoneActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			}, Color.BLACK, Color.parseColor("#FFD1C4E9"));
			
		// 使用 addSection 辅助方法来添加列表项和对应的动作
        addSection("截屏测试", new FunctionExecutor() {
				@Override
				public void execute() {
					Intent intent = new Intent(TestActivity.this, ScreenShotTestActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			}, Color.BLACK, Color.parseColor("#FF88C6F3"));
			
	
		addSection("音量增加测试",VolumeBoostActivity.class, Color.BLACK, Color.parseColor("#F24846F7"));
			
		addSection("Shell测试",ShellTestActivity.class, Color.BLACK, Color.parseColor("#bee48c"));
		
		
		addSection("返回主页", new FunctionExecutor() {
				@Override
				public void execute() {
					finish();
					overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
				}
			}, Color.BLACK, Color.parseColor("#cc6e62"));
		
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	
}
