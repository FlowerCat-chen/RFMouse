package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.mouse.MouseManager2;
import android.widget.ScrollView;

// 在你的Activity中初始化并设置监听器
public class MouseTestActivity extends Activity {
    private MouseManager2 mouseManager;//鼠标管理
    private TextView statusText;
    private TextView resultText;
    private View touchPad;
    private Button shortPressBtn;
    private Button longPressBtn;

    private float startX, startY;
    private long pressStartTime;
    private boolean isLongPress = false;
    private Handler longPressHandler = new Handler();

    // 长按检测runnable
    private Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            isLongPress = true;
            showToast("长按事件触发");
            resultText.setText("检测到长按操作");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.mouse_test);
        mouseManager = MouseManager2.getInstance(this);//初始化鼠标管理

        // 初始化UI组件
        statusText = (TextView) findViewById(R.id.statusText);
        resultText = (TextView) findViewById(R.id.resultText);
        touchPad = findViewById(R.id.touchPad);
        shortPressBtn = (Button) findViewById(R.id.shortPressBtn);
        longPressBtn = (Button) findViewById(R.id.longPressBtn);

        // 设置按钮点击监听
        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showToast("按钮1被点击");
					resultText.setText("按钮1被点击 - 短按操作");
				}
			});

        Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showToast("按钮2被点击");
					resultText.setText("按钮2被点击 - 短按操作");
				}
			});

        // 短按测试按钮
        shortPressBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showToast("短按测试按钮被点击");
					resultText.setText("短按测试按钮被点击");
				}
			});

        // 长按测试按钮
        longPressBtn.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					showToast("长按测试按钮被长按");
					resultText.setText("长按测试按钮被长按");
					return true;
				}
			});

        // 设置触摸板监听 - 阻止父ScrollView滚动
        touchPad.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// 获取父ScrollView
					ViewGroup parent = (ViewGroup) v.getParent();
					while (parent != null && !(parent instanceof ScrollView)) {
						parent = (ViewGroup) parent.getParent();
					}

					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							// 阻止父ScrollView滚动
							if (parent != null) {
								parent.requestDisallowInterceptTouchEvent(true);
							}

							startX = event.getX();
							startY = event.getY();
							pressStartTime = System.currentTimeMillis();
							isLongPress = false;
							longPressHandler.postDelayed(longPressRunnable, 500); // 500ms后检测长按
							statusText.setText("状态: 按下");
							break;

						case MotionEvent.ACTION_MOVE:
							// 继续阻止父ScrollView滚动
							if (parent != null) {
								parent.requestDisallowInterceptTouchEvent(true);
							}

							float currentX = event.getX();
							float currentY = event.getY();

							// 计算移动距离
							float deltaX = currentX - startX;
							float deltaY = currentY - startY;

							// 更新状态
							statusText.setText(String.format("状态: 滑动中 (ΔX: %.1f, ΔY: %.1f)", deltaX, deltaY));

							// 如果移动距离超过阈值，取消长按检测
							if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
								longPressHandler.removeCallbacks(longPressRunnable);
							}
							break;

						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_CANCEL:
							// 允许父ScrollView再次滚动
							if (parent != null) {
								parent.requestDisallowInterceptTouchEvent(false);
							}

							long pressDuration = System.currentTimeMillis() - pressStartTime;
							longPressHandler.removeCallbacks(longPressRunnable);

							if (!isLongPress) {
								// 短按或滑动结束
								float endX = event.getX();
								float endY = event.getY();

								float moveX = endX - startX;
								float moveY = endY - startY;

								if (Math.abs(moveX) > 20 || Math.abs(moveY) > 20) {
									// 滑动操作
									showToast("滑动操作 detected");
									resultText.setText(String.format("滑动操作:\n起始: (%.1f, %.1f)\n结束: (%.1f, %.1f)\n距离: (%.1f, %.1f)\n持续时间: %dms", 
																	 startX, startY, endX, endY, moveX, moveY, pressDuration));
								} else {
									// 短按操作
									showToast("短按操作");
									resultText.setText(String.format("短按操作:\n位置: (%.1f, %.1f)\n持续时间: %dms", 
																	 startX, startY, pressDuration));
								}
							}

							statusText.setText("状态: 抬起");
							break;
					}
					return true;
				}
			});
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
}
