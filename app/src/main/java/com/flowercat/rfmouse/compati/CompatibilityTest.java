package com.flowercat.rfmouse.compati;

// MainActivity.java

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.util.SPUtil;
import android.content.Context;

public class CompatibilityTest extends Activity implements TestFragmentListener {

    private FragmentManager fragmentManager;
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        setContentView(R.layout.compatibility_test);
		
	
		
        fragmentManager = getFragmentManager();
        if (savedInstanceState == null) {
            // 启动第一个测试
            showTestOne();
        }
    }
	
	
	

    private void showTestOne() {
        TestOneFragment fragment = new TestOneFragment();
        fragmentManager.beginTransaction()
			.replace(R.id.fragment_container, fragment)
			.commit();
    }

    private void showTestTwo() {
        TestTwoFragment fragment = new TestTwoFragment();
        fragmentManager.beginTransaction()
			.replace(R.id.fragment_container, fragment)
			.commit();
    }

    private void showTestThree() {
        TestThreeFragment fragment = new TestThreeFragment();
        fragmentManager.beginTransaction()
			.replace(R.id.fragment_container, fragment)
			.commit();
    }

    @Override
    public void onTestComplete(int nextTestId) {
        if (nextTestId == 2) {
            showTestTwo();
        } else if (nextTestId == 3) {
            showTestThree();
		}else if (nextTestId == 114514) {
			//出错了，尝试切换点击模式
			
        } else if (nextTestId == -1) {
            // 所有测试完成或用户退出
           finish();
        }
    }
	
	

	
	
	
}
