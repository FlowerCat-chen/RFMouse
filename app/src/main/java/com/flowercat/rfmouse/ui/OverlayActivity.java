package com.flowercat.rfmouse.ui;


import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.flowercat.rfmouse.R;

public class OverlayActivity extends Activity {
	private TextView tv_adv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.tip_overlay);
		tv_adv = findViewById(R.id.tv_adv);
		if(getIntent().hasExtra("adv_text")){
			String adv = getIntent().getStringExtra("adv_text");
			tv_adv.setText(adv);
		}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 用户触碰屏幕时关闭 Activity
        finish();
        return super.onTouchEvent(event);
    }
}
