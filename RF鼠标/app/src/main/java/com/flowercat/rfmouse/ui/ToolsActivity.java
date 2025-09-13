package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import com.flowercat.rfmouse.R;

public class ToolsActivity extends BaseActivity {

	@Override
	protected void initializeSections() {
		
		addSection("分辨率调整",ResolutionSettingsActivity.class, Color.BLACK, Color.parseColor("#d2e559"));
		
			
		addSection("权限授予器", AppListActivity.class, Color.BLACK, Color.parseColor("#59aee5"));
			
			
		addSection("导航栏屏蔽/开启", HideSystemUIActivity.class, Color.BLACK, Color.parseColor("#2cc098"));
	}
	
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setAdviseText("小工具");
        hideInfo();
    }
    

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	
	
}
