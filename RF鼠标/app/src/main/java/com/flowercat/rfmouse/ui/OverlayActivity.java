package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import com.flowercat.rfmouse.R;


public class OverlayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.tip_overlay);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 用户触碰屏幕时关闭 Activity
        finish();
        return super.onTouchEvent(event);
    }
}
