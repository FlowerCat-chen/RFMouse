package com.flowercat.rfmouse.ui;

import android.graphics.Color;
import android.os.Bundle;
import com.flowercat.rfmouse.compati.CompatibilityKeySetting;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.util.AppLauncherDialog;

public class CompTotalActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setAdviseText("兼容性设置界面");
        hideInfo();
    }
    
	
	@Override
    protected void initializeSections() {
		// 使用 addSection 辅助方法来添加列表项和对应的动作
		
		addSection("初次使用向导",WizardActivity.class, Color.BLACK, Color.parseColor("#FFD1C4E9")); // 浅紫灰，柔和独特
		
		addSection("导入/出鼠标配置",ConfigManagerActivity.class, Color.BLACK, Color.parseColor("#FFE0E9F0")); // 浅蓝灰，与向导项区分
		
		addSection("快速按键录入", CompatibilityKeySetting.class, Color.BLACK, Color.parseColor("#FFEAF2B2")); // 浅黄绿，清新不刺眼
		
		addSection("功能测试", TestActivity.class, Color.BLACK, Color.parseColor("#FFC8E6C9")); // 浅青绿，与原有测试项配色区分
		
		addSection("问题反馈", new FunctionExecutor() {
				@Override
				public void execute() {
					AppLauncherDialog.showAppLauncher(CompTotalActivity.this);
				}
			},Color.BLACK, Color.parseColor("#ff4300"));
	}
	
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
}
