package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.mouse.MouseManager2;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.CheckBox;
import android.widget.RadioButton;

// 虚拟鼠标测试Activity
public class MouseTestActivity extends Activity {
    private MouseManager2 mouseManager; // 鼠标管理器
    private TextView resultText; // 结果显示文本框
    private View touchPad; // 触摸板视图
    private Button shortPressBtn; // 短按测试按钮
    private Button longPressBtn; // 长按测试按钮

    private float startX, startY; // 触摸起始坐标
    private long pressStartTime; // 按下开始时间
    private boolean isLongPress = false; // 是否为长按标志
    private Handler longPressHandler = new Handler(); // 长按检测处理器

    // 长按检测runnable
    private Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            isLongPress = true;
            showToast("长按事件触发");
            updateResultText("检测到长按操作");
        }
    };

    private boolean inGuideMode = false; // 是否为引导模式

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.mouse_test);

        if(getIntent() != null){
            inGuideMode = getIntent().hasExtra("guide_mode");
        }

        mouseManager = MouseManager2.getInstance(this); // 初始化鼠标管理器

        // 初始化UI组件
        resultText = (TextView) findViewById(R.id.resultText);
        touchPad = findViewById(R.id.touchPad);
        shortPressBtn = (Button) findViewById(R.id.shortPressBtn);
        longPressBtn = (Button) findViewById(R.id.longPressBtn);

        // 设置短按测试按钮点击监听
        shortPressBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showToast("短按测试按钮被点击");
					updateResultText("短按测试按钮被点击");
				}
			});

        // 设置长按测试按钮长按监听
        longPressBtn.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					showToast("长按测试按钮被长按");
					updateResultText("长按测试按钮被长按");
					return true;
				}
			});

        // 初始化开关控件并设置监听
        initSwitchControls();

        // 初始化文本输入框并设置监听
        initEditTextControls();

        // 初始化单选按钮组并设置监听
        initRadioGroupControls();

        // 初始化复选框并设置监听
        initCheckBoxControls();

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
									updateResultText(String.format("滑动操作:\n起始: (%.1f, %.1f)\n结束: (%.1f, %.1f)\n距离: (%.1f, %.1f)\n持续时间: %dms", 
																   startX, startY, endX, endY, moveX, moveY, pressDuration));
								} else {
									// 短按操作
									showToast("短按操作");
									updateResultText(String.format("短按操作:\n位置: (%.1f, %.1f)\n持续时间: %dms", 
																   startX, startY, pressDuration));
								}
							}
							break;
					}
					return true;
				}
			});
    }

    // 初始化开关控件并设置监听
    private void initSwitchControls() {
        Switch switch1 = (Switch) findViewById(R.id.switch1);
        Switch switch2 = (Switch) findViewById(R.id.switch2);

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					String status = isChecked ? "开启" : "关闭";
					updateResultText("开关1 " + status);
					showToast("开关1 " + status);
				}
			});

        switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					String status = isChecked ? "开启" : "关闭";
					updateResultText("开关2 " + status);
					showToast("开关2 " + status);
				}
			});
    }

    // 初始化文本输入框并设置监听
    private void initEditTextControls() {
        EditText editText1 = (EditText) findViewById(R.id.editText1);

        editText1.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// 文本改变前的回调
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// 文本改变时的回调
				}

				@Override
				public void afterTextChanged(Editable s) {
					// 文本改变后的回调
					if (s.length() > 0) {
						updateResultText("文本输入: " + s.toString());
					}
				}
			});
    }

    // 初始化单选按钮组并设置监听
    private void initRadioGroupControls() {
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					String selectedText = "";
					if (checkedId == R.id.radioButton1) {
						selectedText = "选项一";
					} else if (checkedId == R.id.radioButton2) {
						selectedText = "选项二";
					} else if (checkedId == R.id.radioButton3) {
						selectedText = "选项三";
					}
					updateResultText("单选按钮选择: " + selectedText);
					showToast("选择了: " + selectedText);
				}
			});
    }

    // 初始化复选框并设置监听
    private void initCheckBoxControls() {
        CheckBox checkBox1 = (CheckBox) findViewById(R.id.checkBox1);
        CheckBox checkBox2 = (CheckBox) findViewById(R.id.checkBox2);
        CheckBox checkBox3 = (CheckBox) findViewById(R.id.checkBox3);

        CompoundButton.OnCheckedChangeListener checkBoxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String status = isChecked ? "选中" : "取消选中";
                String checkBoxName = "";

                if (buttonView.getId() == R.id.checkBox1) {
                    checkBoxName = "选择项一";
                } else if (buttonView.getId() == R.id.checkBox2) {
                    checkBoxName = "选择项二";
                } else if (buttonView.getId() == R.id.checkBox3) {
                    checkBoxName = "选择项三";
                }

                updateResultText(checkBoxName + " " + status);
                showToast(checkBoxName + " " + status);
            }
        };

        checkBox1.setOnCheckedChangeListener(checkBoxListener);
        checkBox2.setOnCheckedChangeListener(checkBoxListener);
        checkBox3.setOnCheckedChangeListener(checkBoxListener);
    }

    // 更新结果文本内容
    private void updateResultText(String text) {
        resultText.setText(text);
    }

    // 显示Toast消息
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
        //overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	
}
