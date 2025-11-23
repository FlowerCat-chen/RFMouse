package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.mouse.MouseManager2;
import com.flowercat.rfmouse.util.BitmapManager;
import com.flowercat.rfmouse.util.SPUtil;
import java.io.FileNotFoundException;
import java.io.InputStream;
import android.content.Context;
import android.widget.RadioButton;
import android.os.Build;
import android.graphics.Color;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.mouse.MouseActionManager;

public class MouseSettingsActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_SCROLL_IMAGE_REQUEST = 2; // 添加滚动图标选择请求码

    private ImageView previewImageView;
    private ImageView previewImageView_scroll;
    private RadioGroup sizeRadioGroup;
 
    private Button selectImageButton;
    private Button resetImageButton;
    private Button selectScrollImageButton; // 滚动图标选择按钮
    private Button resetScrollImageButton;  // 滚动图标重置按钮
	private Button btn_set_default;
	private SeekBar customSizeSeekBar;
	
    private TextView tvCustomSizeValue; // 新增
    private SeekBar seekBarStep;
    private TextView tvStepValue; // 新增
    private SeekBar seekBarSpeed;
    private TextView tvSpeedValue; // 新增
    private SeekBar seekBarInterval;
    private TextView tvIntervalValue; // 新增
    private SeekBar seekBarHideTime;
    private TextView tvHideTimeValue; // 新增
	
	private TextView tv_gesture_short_time; // 新增
	private TextView tv_gesture_long_time; // 新增
	private TextView tv_scroll_updown_dis; // 新增
	private TextView tv_scroll_leftright_dis; // 新增

	
    private MouseManager2 mouseManager;
    private Bitmap selectedBitmap;
    private Bitmap selectedScrollBitmap; // 选中的滚动图标
	private RelativeLayout sett1,sett2,sett3,sett4,sett5,sett6;
	private Switch auto_hide_enabled;
	private RelativeLayout sett11,sett12,sett13,sett14,sett16,sett17;
	private Switch lock_auto_back,keep_screen_on,fix_x,fix_y,click_debug,space_input;
	
	//设置鼠标工作模式相关
	private RadioGroup radioGroupOptions;
    private RadioButton radioButtonGesture;
    private RadioButton radioButtonNodeClick;
	private RadioButton radioButtonInject;
    private Button buttonShowLimitations;
	private Button exit_setting;
	private Button btn_lay_setting;
	
	private LinearLayout gesture_2;
	private View gesture_1;
	
	//手势点击时
	private SeekBar gesture_short_time;
	private SeekBar gesture_long_time;
	private SeekBar scroll_updown_dis;
	private SeekBar scroll_leftright_dis;
	
	private RelativeLayout sett7,sett8,sett9,sett10;
	
	//手势滚动时间
	private RelativeLayout scroll_dur;
	private SeekBar scroll_time;
	private TextView tv_scroll_time;
	//触摸注入设置按钮。
	private Button btn_mgrsetting;
	
	//重置
	private Button btn_gesture_default,btn_advance_default;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.mouse_settings);

        mouseManager = MouseManager2.getInstance(this);

        // 绑定控件
        previewImageView = (ImageView) findViewById(R.id.iv_preview_mouse);
        previewImageView_scroll = (ImageView) findViewById(R.id.iv_preview_mouse_scroll);
        sizeRadioGroup = (RadioGroup) findViewById(R.id.rg_mouse_size);
		

        customSizeSeekBar = (SeekBar) findViewById(R.id.sb_custom_size);
        tvCustomSizeValue = (TextView) findViewById(R.id.tv_custom_size_value); // 绑定
        seekBarStep = (SeekBar) findViewById(R.id.seek_bar_step);
        tvStepValue = (TextView) findViewById(R.id.tv_step_value); // 绑定
        seekBarSpeed = (SeekBar) findViewById(R.id.seek_bar_speed);
        tvSpeedValue = (TextView) findViewById(R.id.tv_speed_value); // 绑定
        seekBarInterval = (SeekBar) findViewById(R.id.seek_bar_interval);
        tvIntervalValue = (TextView) findViewById(R.id.tv_interval_value); // 绑定
        seekBarHideTime = (SeekBar) findViewById(R.id.seek_bar_hide_time);
        tvHideTimeValue = (TextView) findViewById(R.id.tv_hide_time_value); // 绑定
		
        selectImageButton = (Button) findViewById(R.id.btn_select_image);
        resetImageButton = (Button) findViewById(R.id.btn_reset_image);
        selectScrollImageButton = (Button) findViewById(R.id.btn_select_image_scroll);
        resetScrollImageButton = (Button) findViewById(R.id.btn_reset_image_scroll);
		btn_set_default =(Button) findViewById(R.id.btn_set_default);
		
        auto_hide_enabled = findViewById(R.id.auto_hide_enabled);
		exit_setting = findViewById(R.id.exit_mouse_setting);
		
		// 初始化控件
        radioGroupOptions = (RadioGroup) findViewById(R.id.rg_options);
        radioButtonGesture = (RadioButton) findViewById(R.id.rb_gesture);
        radioButtonNodeClick = (RadioButton) findViewById(R.id.rb_node_click);
		radioButtonInject = (RadioButton) findViewById(R.id.rb_mgr_click);
        buttonShowLimitations = (Button) findViewById(R.id.btn_show_limitations);
		
		gesture_1 = findViewById(R.id.gesture_1);
		gesture_2 = findViewById(R.id.gesture_2);
		
		btn_mgrsetting = findViewById(R.id.btn_mgrsetting);
		
		btn_gesture_default = findViewById(R.id.btn_gesture_default);
		btn_advance_default = findViewById(R.id.btn_advance_default);
		
        // 根据安卓版本初始化选中项
        initSelectionBasedOnVersion();
		

		//设置按键监听
		sett1 = findViewById(R.id.sett1);
		sett2 = findViewById(R.id.sett2);
		sett3 = findViewById(R.id.sett3);
		sett4 = findViewById(R.id.sett4);
		sett5 = findViewById(R.id.sett5);
		sett6 = findViewById(R.id.sett6);
		
		sett7 = findViewById(R.id.sett7);
		sett8 = findViewById(R.id.sett8);
		sett9 = findViewById(R.id.sett9);
		sett10 = findViewById(R.id.sett10);
		
		sett11 = findViewById(R.id.sett11);
		sett12 = findViewById(R.id.sett12);
		sett13 = findViewById(R.id.sett13);
		sett14 = findViewById(R.id.sett14);
		//sett15 = findViewById(R.id.sett15);
		sett16 = findViewById(R.id.sett16);
		sett17 = findViewById(R.id.sett17);
		//sett18 = findViewById(R.id.sett18);
		scroll_dur = findViewById(R.id.scroll_dur);
		
		lock_auto_back = findViewById(R.id.lock_auto_back);
		keep_screen_on = findViewById(R.id.keep_screen_on);
		fix_x = findViewById(R.id.fix_x);
		fix_y = findViewById(R.id.fix_y);
		//lay_on_setting = findViewById(R.id.lay_on_setting);
		click_debug = findViewById(R.id.click_debug);
		space_input = findViewById(R.id.space_input);
		//disable_input = findViewById(R.id.disable_input);
		//disable_input.setEnabled(false);
		//sett18.setEnabled(false);

		gesture_short_time = findViewById(R.id.gesture_short_time);
		gesture_long_time = findViewById(R.id.gesture_long_time);
		scroll_time = findViewById(R.id.scroll_time);
		scroll_updown_dis = findViewById(R.id.scroll_updown_dis);
		scroll_leftright_dis = findViewById(R.id.scroll_leftright_dis);
		
		tv_gesture_short_time = findViewById(R.id.tv_gesture_short_time);
		tv_gesture_long_time = findViewById(R.id.tv_gesture_long_time);
		tv_scroll_updown_dis = findViewById(R.id.tv_scroll_updown_dis);
		tv_scroll_leftright_dis = findViewById(R.id.tv_scroll_leftright_dis);
		tv_scroll_time = findViewById(R.id.tv_scroll_time);
		
		btn_lay_setting = findViewById(R.id.btn_lay_setting);
		
        // 加载SharedPreferences保存的数据
        loadViewData();

        // 初始化预览
        updatePreview();

        // 设置监听器
        sizeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					if (checkedId == R.id.rb_small) {
						mouseManager.setMouseSize(MouseManager2.MouseSize.SMALL);
						SPUtil.putInt(SPUtil.KEY_PRESET_MOUSE_SIZE, 1);
					} else if (checkedId == R.id.rb_medium) {
						mouseManager.setMouseSize(MouseManager2.MouseSize.MEDIUM);
						SPUtil.putInt(SPUtil.KEY_PRESET_MOUSE_SIZE, 2);
					} else if (checkedId == R.id.rb_large) {
						mouseManager.setMouseSize(MouseManager2.MouseSize.LARGE);
						SPUtil.putInt(SPUtil.KEY_PRESET_MOUSE_SIZE, 3);
					}
					customSizeSeekBar.setProgress(mouseManager.getMouseSizeProgress());
					tvCustomSizeValue.setText(String.valueOf(mouseManager.getMouseSizeProgress()));
					SPUtil.putInt(SPUtil.KEY_USER_MOUSE_SIZE, mouseManager.getMouseSizeProgress());
					updatePreview();
				}
			});

        customSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						
						mouseManager.setMouseSizeProgress(progress);
						
						// 保存自定义尺寸
						SPUtil.putInt(SPUtil.KEY_USER_MOUSE_SIZE, progress);
					    tvCustomSizeValue.setText(String.valueOf(progress));
						updatePreview();
						
						// 确保预设按钮不被选中
						
						SPUtil.putInt(SPUtil.KEY_PRESET_MOUSE_SIZE, 0); // 0 表示自定义尺寸
						
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					sizeRadioGroup.clearCheck();
				}
			});

        selectImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openImageChooser(PICK_IMAGE_REQUEST);
				}
			});

        resetImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectedBitmap = null;
					mouseManager.setMouseImage(null);
					// 清除保存的自定义图片路径
					//SPUtil.putString(SPUtil.KEY_MOUSE_IMAGE_URI, null);
					BitmapManager.deleteBitmap(MouseSettingsActivity.this,"mouse");
					updatePreview();
				}
			});

        selectScrollImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openImageChooser(PICK_SCROLL_IMAGE_REQUEST);
				}
			});

        resetScrollImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectedScrollBitmap = null;
					mouseManager.setScrollImage(null);
					// 清除保存的自定义滚动图片路径
					//SPUtil.putString(SPUtil.KEY_MOUSE_SCROLL_IMAGE_URI, null);
					BitmapManager.deleteBitmap(MouseSettingsActivity.this,"mouse_scroll");
					updatePreview();
				}
			});

        // 鼠标移动参数SeekBar监听器
        seekBarStep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						// 保存单次移动距离
						tvStepValue.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_MOUSE_STEP, progress);
						// 这里需要调用 MouseManager2 的相应方法来更新设置
						mouseManager.setSingleMouseSpeed(progress);
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tvSpeedValue.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_MOUSE_SPEED, progress);
						mouseManager.setMouseSpeed(progress);
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});

        seekBarInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tvIntervalValue.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_MOUSE_TIME, progress);
						mouseManager.setMoveInterval(progress);
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});

        seekBarHideTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tvHideTimeValue.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_MOUSE_HIDE_TIME, progress);
						mouseManager.disableAutoHide();
						mouseManager.enableAutoHide(progress *1000);
						mouseManager.hideMouse();
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});
			
			
		//是否开启自动隐藏
        auto_hide_enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_MOUSE_HIDE_ENABLE, isChecked);
					if(isChecked){
						mouseManager.disableAutoHide();
						mouseManager.enableAutoHide(SPUtil.getInt(SPUtil.KEY_MOUSE_HIDE_TIME, 30) *1000);
						seekBarHideTime.setEnabled(true);
					} else {
						mouseManager.disableAutoHide();
						seekBarHideTime.setEnabled(false);
						//mouseManager.showMouse();
					}
					
					mouseManager.hideMouse();
					if(FlowerMouseService.getInstance() != null){
						FlowerMouseService.getInstance().currentMode = 0;
						FlowerMouseService.getInstance().updateKeyListeners(null);
						FlowerMouseService.getInstance().showTipToast("按键模式");
					}
					
					
				}
			});
			
		btn_set_default.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showRestoreConfirmationDialog(0,"确定恢复默认吗?");
				}
			});
			
		btn_gesture_default.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showRestoreConfirmationDialog(1,"确定将手势恢复默认吗?");
				}
			});
			
		btn_advance_default.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showRestoreConfirmationDialog(2,"确定将高级选项恢复默认吗?");
				}
			});
			
		exit_setting.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
					overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
				}
			});
			
			
		btn_mgrsetting.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent startIntent = new Intent(getApplicationContext(),
													InjectSettingActivity.class);
					startActivity(startIntent);
					
				}
			});
			
			
			
		
			
			/* 手势分发*/
			
		gesture_short_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tv_gesture_short_time.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_GESTURE_SHORT, progress);
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().actionManager.setShortClickTime(progress);
						}
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});
			
		gesture_long_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tv_gesture_long_time.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_GESTURE_LONG, progress);
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().actionManager.setLongClickTime(progress);
						}
						
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});
			
			
		scroll_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tv_scroll_time.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_GESTURE_SCROLL, progress);
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().actionManager.setScrollTime(progress);
						}
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});
		
		scroll_updown_dis.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tv_scroll_updown_dis.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_GESTURE_UD_DIS, progress);
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().actionManager.setScrollDisUD(progress);
						}
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});
		
		scroll_leftright_dis.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tv_scroll_leftright_dis.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_GESTURE_LR_DIS, progress);
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().actionManager.setScrollDisLR(progress);
						}
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});
			
		//锁屏后鼠标回正
        lock_auto_back.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_LOCK_BACK, isChecked);
				}
			});
			
		//保持屏幕开启
        keep_screen_on.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_KEEP_ON, isChecked);
					mouseManager.setScreenOn(isChecked);
				}
			});
			
		//x修正
        fix_x.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_FIX_X, isChecked);
					mouseManager.setFixX(isChecked);
				}
			});
			
		//y修正
        fix_y.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_FIX_Y, isChecked);
					mouseManager.setFixY(isChecked);
				}
			});
			
			/*
		//在设置上显示
        lay_on_setting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_LAYON_SETTING, isChecked);

				}
			});
			
			*/
			
		//debug
        click_debug.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_CLICK_DEBUG, isChecked);
					if(FlowerMouseService.getInstance() !=null){
						FlowerMouseService.getInstance().actionManager.inDebugMode = isChecked;
					}
				}
			});
			
		//空出输入
        space_input.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_SPACE_INPUT, isChecked);
					if(FlowerMouseService.getInstance() !=null){
						FlowerMouseService.getInstance().space_input_bool = isChecked;
					}
				}
			});
			
			
		//禁止触摸
		/*
        disable_input.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_DISABLE_INPUT, isChecked);

				}
			});
		*/
			
			
		//让事件继续向下传递。
		sett1.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								customSizeSeekBar.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
		//让事件继续向下传递。
		sett2.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								seekBarStep.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
		
			
		//让事件继续向下传递。
		sett3.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								seekBarSpeed.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
		//让事件继续向下传递。
		sett4.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								seekBarInterval.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
		//让事件继续向下传递。
		sett5.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								seekBarHideTime.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});

		sett6.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(auto_hide_enabled.isChecked()){
						auto_hide_enabled.setChecked(false);
					} else {
						auto_hide_enabled.setChecked(true);
					}
				}
			});
			
			
		//让事件继续向下传递。
		sett7.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								gesture_short_time.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
		//让事件继续向下传递。
		sett8.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								gesture_long_time.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
			
		//让事件继续向下传递。
		sett9.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								scroll_updown_dis.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
			
		//让事件继续向下传递。
		sett10.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								scroll_leftright_dis.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
			
		sett11.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(lock_auto_back.isChecked()){
						lock_auto_back.setChecked(false);
					} else {
						lock_auto_back.setChecked(true);
					}
				}
			});
			
			
		sett12.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(keep_screen_on.isChecked()){
						keep_screen_on.setChecked(false);
					} else {
						keep_screen_on.setChecked(true);
					}
				}
			});
		
		sett13.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(fix_x.isChecked()){
						fix_x.setChecked(false);
					} else {
						fix_x.setChecked(true);
					}
				}
			});
			
		sett14.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(fix_y.isChecked()){
						fix_y.setChecked(false);
					} else {
						fix_y.setChecked(true);
					}
				}
			});
		
		/*
		sett15.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(lay_on_setting.isChecked()){
						lay_on_setting.setChecked(false);
					} else {
						lay_on_setting.setChecked(true);
					}
				}
			});
		*/
		sett16.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(click_debug.isChecked()){
						click_debug.setChecked(false);
					} else {
						click_debug.setChecked(true);
					}
				}
			});
		
		sett17.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(space_input.isChecked()){
						space_input.setChecked(false);
					} else {
						space_input.setChecked(true);
					}
				}
			});
		
		/*
		sett18.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(disable_input.isChecked()){
						disable_input.setChecked(false);
					} else {
						disable_input.setChecked(true);
					}
				}
			});
		
		*/
		//让事件继续向下传递。
		scroll_dur.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								scroll_time.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
			
			// 模式选择相关
		// 设置RadioGroup的选中监听
        radioGroupOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					switch(checkedId){
						case R.id.rb_gesture:
							//保险起见再做一次判断
							if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
								radioButtonGesture.setChecked(false);
								return;
							}
							SPUtil.putString(SPUtil.KEY_CLICK_MODE,"gesture");
							gesture_1.setVisibility(View.VISIBLE);
							gesture_2.setVisibility(View.VISIBLE);
							btn_mgrsetting.setVisibility(View.GONE);
							if(FlowerMouseService.getInstance() != null){
								FlowerMouseService.getInstance().actionManager.setClickMode(2);
								FlowerMouseService.getInstance().mode = "gesture";
							}
							break;
						case R.id.rb_node_click:
							SPUtil.putString(SPUtil.KEY_CLICK_MODE,"node");
							gesture_1.setVisibility(View.GONE);
							gesture_2.setVisibility(View.GONE);
							btn_mgrsetting.setVisibility(View.GONE);
							if(FlowerMouseService.getInstance() != null){
								FlowerMouseService.getInstance().actionManager.setClickMode(1);
								FlowerMouseService.getInstance().mode = "node";
							}
							break;	
							
						case R.id.rb_mgr_click:
							SPUtil.putString(SPUtil.KEY_CLICK_MODE,"mgr");
							gesture_1.setVisibility(View.GONE);
							gesture_2.setVisibility(View.GONE);
							btn_mgrsetting.setVisibility(View.VISIBLE);
							if(FlowerMouseService.getInstance() != null){
								FlowerMouseService.getInstance().actionManager.setClickMode(3);
								FlowerMouseService.getInstance().mode = "mgr";
							}
							break;	
							
							
					}
				}
			});


        buttonShowLimitations.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(MouseSettingsActivity.this, "无法点击系统级的UI元素或无法获取UI节点信息的区域。",Toast.LENGTH_LONG).show();
				}
			});
 
		btn_lay_setting.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent over = new Intent (MouseSettingsActivity.this,MouseOverSettingActivity.class);
					startActivity(over);  
				}
			});
		
			
    }

    private void loadViewData() {
        // 从SharedPreferences加载数据并更新UI
        int presetMouseSize = SPUtil.getInt(SPUtil.KEY_PRESET_MOUSE_SIZE, 2);
        int customMouseSize = SPUtil.getInt(SPUtil.KEY_USER_MOUSE_SIZE, 50); // 默认50
        int mouseStep = SPUtil.getInt(SPUtil.KEY_MOUSE_STEP, 10);
        int mouseSpeed = SPUtil.getInt(SPUtil.KEY_MOUSE_SPEED, 40);
        int mouseInterval = SPUtil.getInt(SPUtil.KEY_MOUSE_TIME, 16);
        int mouseHideTime = SPUtil.getInt(SPUtil.KEY_MOUSE_HIDE_TIME, 30);
        //String imageUriString = SPUtil.getString(SPUtil.KEY_MOUSE_IMAGE_URI, null);
        //String scrollImageUriString = SPUtil.getString(SPUtil.KEY_MOUSE_SCROLL_IMAGE_URI, null);
		boolean mouseHideEnabled = SPUtil.getBoolean(SPUtil.KEY_MOUSE_HIDE_ENABLE, true);
		
		int gest_short= SPUtil.getInt(SPUtil.KEY_GESTURE_SHORT, 50);
		int gest_long = SPUtil.getInt(SPUtil.KEY_GESTURE_LONG, 2);
		int gest_scroll_time = SPUtil.getInt(SPUtil.KEY_GESTURE_SCROLL, 300);
		int gest_scroll_ud = SPUtil.getInt(SPUtil.KEY_GESTURE_UD_DIS, 25);
		int gest_scroll_lr = SPUtil.getInt(SPUtil.KEY_GESTURE_LR_DIS, 25);
		
		btn_mgrsetting.setVisibility(View.GONE);
		
		String mode ;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mode= SPUtil.getString(SPUtil.KEY_CLICK_MODE,"gesture");
		} else {
			mode= SPUtil.getString(SPUtil.KEY_CLICK_MODE,"node");
		}
		
		if(mode.equals("gesture")){
			radioButtonGesture.setChecked(true);
			gesture_1.setVisibility(View.VISIBLE);
			gesture_2.setVisibility(View.VISIBLE);
		} else if(mode.equals("node")){
			radioButtonNodeClick.setChecked(true);
			gesture_1.setVisibility(View.GONE);
			gesture_2.setVisibility(View.GONE);
		} else if(mode.equals("mgr")){
			radioButtonInject.setChecked(true);
			gesture_1.setVisibility(View.GONE);
			gesture_2.setVisibility(View.GONE);
			btn_mgrsetting.setVisibility(View.VISIBLE);
		}
		
        // 更新大小预设按钮和自定义尺寸
        if (presetMouseSize == 1) {
            sizeRadioGroup.check(R.id.rb_small);
			mouseManager.setMouseSize(MouseManager2.MouseSize.SMALL);
        } else if (presetMouseSize == 2) {
			mouseManager.setMouseSize(MouseManager2.MouseSize.MEDIUM);
            sizeRadioGroup.check(R.id.rb_medium);
        } else if (presetMouseSize == 3) {
			mouseManager.setMouseSize(MouseManager2.MouseSize.LARGE);
            sizeRadioGroup.check(R.id.rb_large);
        } else {
            // 自定义尺寸
            sizeRadioGroup.clearCheck();
            customSizeSeekBar.setProgress(customMouseSize);
			mouseManager.setMouseSizeProgress(customMouseSize);
        }

        // 更新SeekBar值
        // 更新 TextView
        tvCustomSizeValue.setText(String.valueOf(customMouseSize));
        seekBarStep.setProgress(mouseStep);
        tvStepValue.setText(String.valueOf(mouseStep));
        seekBarSpeed.setProgress(mouseSpeed);
        tvSpeedValue.setText(String.valueOf(mouseSpeed));
        seekBarInterval.setProgress(mouseInterval);
        tvIntervalValue.setText(String.valueOf(mouseInterval));
        seekBarHideTime.setProgress(mouseHideTime);
        tvHideTimeValue.setText(String.valueOf(mouseHideTime));
		auto_hide_enabled.setChecked(mouseHideEnabled);
		
		//手势
		tv_gesture_short_time.setText(String.valueOf(gest_short));
        gesture_short_time.setProgress(gest_short);
		
		tv_gesture_long_time.setText(String.valueOf(gest_long));
        gesture_long_time.setProgress(gest_long);
		
		tv_scroll_time.setText(String.valueOf(gest_scroll_time));
        scroll_time.setProgress(gest_scroll_time);
		
		
		tv_scroll_updown_dis.setText(String.valueOf(gest_scroll_ud));
        scroll_updown_dis.setProgress(gest_scroll_ud);
		
		
		tv_scroll_leftright_dis.setText(String.valueOf(gest_scroll_lr));
        scroll_leftright_dis.setProgress(gest_scroll_lr);
		
		
		
        // 加载自定义图片
        Bitmap tmp = BitmapManager.getBitmap(this,"mouse");
		if(tmp != null){
			selectedBitmap = tmp;
			mouseManager.setMouseImage(selectedBitmap);
		}
                
		// 加载自定义图片
        Bitmap tmp2 = BitmapManager.getBitmap(this,"mouse_scroll");
		if(tmp2 != null){
			selectedScrollBitmap = tmp2;
			mouseManager.setScrollImage(selectedScrollBitmap);
		}
	
		mouseManager.setSingleMouseSpeed(mouseStep);
		mouseManager.setMouseSpeed(mouseSpeed);
		if(mouseHideEnabled){
			//先关闭一下
			mouseManager.disableAutoHide();
			mouseManager.enableAutoHide(mouseHideTime *1000);
			seekBarHideTime.setEnabled(true);
		} else {
			seekBarHideTime.setEnabled(false);
		}
		
		mouseManager.hideMouse();
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().currentMode = 0;
			FlowerMouseService.getInstance().updateKeyListeners(null);
			FlowerMouseService.getInstance().showTipToast("按键模式");
		}
		
		lock_auto_back.setChecked(SPUtil.getBoolean(SPUtil.KEY_LOCK_BACK, true));
		keep_screen_on.setChecked(SPUtil.getBoolean(SPUtil.KEY_KEEP_ON, false));
		fix_x.setChecked(SPUtil.getBoolean(SPUtil.KEY_FIX_X, true));
		fix_y.setChecked(SPUtil.getBoolean(SPUtil.KEY_FIX_Y, true));
		//lay_on_setting.setChecked(SPUtil.getBoolean(SPUtil.KEY_LAYON_SETTING, false));
		click_debug.setChecked(SPUtil.getBoolean(SPUtil.KEY_CLICK_DEBUG, false));
		space_input.setChecked(SPUtil.getBoolean(SPUtil.KEY_SPACE_INPUT, true));
		//disable_input.setChecked(SPUtil.getBoolean(SPUtil.KEY_DISABLE_INPUT, false));
		
		//强制进入按键模式
		if(FlowerMouseService.getInstance() != null){
			MouseActionManager actionManager = FlowerMouseService.getInstance().actionManager;
			actionManager.setShortClickTime(gest_short);
			actionManager.setLongClickTime(gest_long);
			actionManager.setScrollTime(gest_scroll_time);
			actionManager.setScrollDisUD(gest_scroll_ud);
			actionManager.setScrollDisLR(gest_scroll_lr);

		}
		
		
    }

    private void openImageChooser(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(intent, "选择图片"), requestCode);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "没有找到文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (requestCode == PICK_IMAGE_REQUEST) {
                    selectedBitmap = BitmapFactory.decodeStream(inputStream);
                    mouseManager.setMouseImage(selectedBitmap);
                    // 保存图片URI
                    //SPUtil.putString(SPUtil.KEY_MOUSE_IMAGE_URI, imageUri.toString());
					BitmapManager.putBitmap(this,"mouse",selectedBitmap);
                } else if (requestCode == PICK_SCROLL_IMAGE_REQUEST) {
                    selectedScrollBitmap = BitmapFactory.decodeStream(inputStream);
                   mouseManager.setScrollImage(selectedScrollBitmap);
                    // 保存图片URI
                   // SPUtil.putString(SPUtil.KEY_MOUSE_SCROLL_IMAGE_URI, imageUri.toString());
					BitmapManager.putBitmap(this,"mouse_scroll",selectedScrollBitmap);
                }
                updatePreview();
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "文件未找到", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

	
	
	private void updatePreview() {
		// 获取 ImageView 实例，确保它们与你的 XML 布局 ID 匹配
		ImageView previewImageView = findViewById(R.id.iv_preview_mouse);
		ImageView previewImageView_scroll = findViewById(R.id.iv_preview_mouse_scroll);

		// 更新普通鼠标预览
		if (selectedBitmap != null) {
			previewImageView.setImageBitmap(selectedBitmap);
		} else {
			previewImageView.setImageResource(R.drawable.mouse_pointer); // 使用默认图片
		}

		// 更新滚动鼠标预览
		if (selectedScrollBitmap != null) {
			previewImageView_scroll.setImageBitmap(selectedScrollBitmap);
		} else {
			previewImageView_scroll.setImageResource(R.drawable.mouse_scroll); // 使用默认图片
		}

		// 获取当前鼠标尺寸
		int mouseWidth = mouseManager.getMouseWidth();
		int mouseHeight = mouseManager.getMouseHeight();

		// 为两个 ImageView 设置相同的 LinearLayout.LayoutParams
		// 注意：这里需要使用 LinearLayout.LayoutParams，而不是 FrameLayout.LayoutParams
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mouseWidth, mouseHeight);
		previewImageView.setLayoutParams(params);

		// 为滚动鼠标预览设置同样的尺寸和边距
		// 边距可以在 XML 中设置，也可以在这里设置
		LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(mouseWidth, mouseHeight);
		// 这里设置左边距，以在两个图片之间创建间隔
		scrollParams.setMargins(30, 0, 0, 0); 
		previewImageView_scroll.setLayoutParams(scrollParams);

		//mouseManager.showMouse();
	}
	
	
	
	//显示恢复默认对话框
    private void showRestoreConfirmationDialog(final int id ,String content) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("确定恢复默认？")
			.setMessage(content)
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(id){
						case 0:
							setCommonSettinsDefault();
							break;
						case 1:
							setGestureSettingDefault();
							break;
						case 2:
							setAdvanceSettingDefault();
							break;
							
					}
					
				}
			})
			.setNegativeButton("取消", null);

		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dialog = builder.show();

		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
			negativeBtn.setLayoutParams(negativeParams);
			negativeBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//negativeBtn.setPadding(30, 10, 30, 10); // 可选：同确认按钮，保持样式一致
		}

    }
	
	
	public void setCommonSettinsDefault(){
		// 从SharedPreferences加载数据并更新UI
		int presetMouseSize = 2;
		int customMouseSize = 50;
		int mouseStep = 10;
		int mouseSpeed = 40;
		int mouseInterval = 16;
		int mouseHideTime = 30;
		//String imageUriString = SPUtil.getString(SPUtil.KEY_MOUSE_IMAGE_URI, null);
		//String scrollImageUriString = SPUtil.getString(SPUtil.KEY_MOUSE_SCROLL_IMAGE_URI, null);
		boolean mouseHideEnabled = true;

		mouseManager.setMouseSize(MouseManager2.MouseSize.MEDIUM);
		sizeRadioGroup.check(R.id.rb_medium);
		customSizeSeekBar.setProgress(0);


		SPUtil.putInt(SPUtil.KEY_PRESET_MOUSE_SIZE, 2);
		SPUtil.putInt(SPUtil.KEY_USER_MOUSE_SIZE, 50); // 默认50
		SPUtil.putInt(SPUtil.KEY_MOUSE_STEP, 10);
		SPUtil.putInt(SPUtil.KEY_MOUSE_SPEED, 40);
		SPUtil.putInt(SPUtil.KEY_MOUSE_TIME, 16);
		SPUtil.putInt(SPUtil.KEY_MOUSE_HIDE_TIME, 30);
		SPUtil.putBoolean(SPUtil.KEY_MOUSE_HIDE_ENABLE, true);

		// 更新SeekBar值
		// 更新 TextView
		tvCustomSizeValue.setText(String.valueOf(customMouseSize));
		seekBarStep.setProgress(mouseStep);
		tvStepValue.setText(String.valueOf(mouseStep));
		seekBarSpeed.setProgress(mouseSpeed);
		tvSpeedValue.setText(String.valueOf(mouseSpeed));
		seekBarInterval.setProgress(mouseInterval);
		tvIntervalValue.setText(String.valueOf(mouseInterval));
		seekBarHideTime.setProgress(mouseHideTime);
		tvHideTimeValue.setText(String.valueOf(mouseHideTime));
		auto_hide_enabled.setChecked(mouseHideEnabled);

		selectedBitmap = null;
		mouseManager.setMouseImage(null);
		// 清除保存的自定义图片路径
		//SPUtil.putString(SPUtil.KEY_MOUSE_IMAGE_URI, null);
		BitmapManager.deleteBitmap(MouseSettingsActivity.this,"mouse");

		selectedScrollBitmap = null;
		mouseManager.setScrollImage(null);
		// 清除保存的自定义滚动图片路径
		//SPUtil.putString(SPUtil.KEY_MOUSE_SCROLL_IMAGE_URI, null);
		BitmapManager.deleteBitmap(MouseSettingsActivity.this,"mouse_scroll");


		mouseManager.setSingleMouseSpeed(mouseStep);
		mouseManager.setMouseSpeed(mouseSpeed);
		if(mouseHideEnabled){
			//先关闭一下
			mouseManager.disableAutoHide();
			mouseManager.enableAutoHide(mouseHideTime *1000);
			seekBarHideTime.setEnabled(true);
		} else {
			seekBarHideTime.setEnabled(false);
		}

		updatePreview();

	}
	
	//手势分发设置为默认。
	
	public void setGestureSettingDefault(){
		int gest_short= 50;
		int gest_long = 2;
		int gest_scroll_time = 300;
		int gest_scroll_ud = 25;
		int gest_scroll_lr = 25;
		
		
		SPUtil.putInt(SPUtil.KEY_GESTURE_SHORT, 50);
		SPUtil.putInt(SPUtil.KEY_GESTURE_LONG, 2);
		SPUtil.putInt(SPUtil.KEY_GESTURE_SCROLL, 300);
		SPUtil.putInt(SPUtil.KEY_GESTURE_UD_DIS, 25);
		SPUtil.putInt(SPUtil.KEY_GESTURE_LR_DIS, 25);
		
		
		tv_gesture_short_time.setText(String.valueOf(gest_short));
        gesture_short_time.setProgress(gest_short);

		tv_gesture_long_time.setText(String.valueOf(gest_long));
        gesture_long_time.setProgress(gest_long);

		tv_scroll_time.setText(String.valueOf(gest_scroll_time));
        scroll_time.setProgress(gest_scroll_time);


		tv_scroll_updown_dis.setText(String.valueOf(gest_scroll_ud));
        scroll_updown_dis.setProgress(gest_scroll_ud);


		tv_scroll_leftright_dis.setText(String.valueOf(gest_scroll_lr));
        scroll_leftright_dis.setProgress(gest_scroll_lr);
		
		
		if(FlowerMouseService.getInstance() != null){
			MouseActionManager actionManager = FlowerMouseService.getInstance().actionManager;
			actionManager.setShortClickTime(gest_short);
			actionManager.setLongClickTime(gest_long);
			actionManager.setScrollTime(gest_scroll_time);
			actionManager.setScrollDisUD(gest_scroll_ud);
			actionManager.setScrollDisLR(gest_scroll_lr);

		}
	}
	
	//将高级设置重置为默认
	public void setAdvanceSettingDefault(){
		SPUtil.putBoolean(SPUtil.KEY_LOCK_BACK, true);
		SPUtil.putBoolean(SPUtil.KEY_KEEP_ON, false);
		mouseManager.setScreenOn(false);
		SPUtil.putBoolean(SPUtil.KEY_FIX_X, true);
		mouseManager.setFixX(true);
		SPUtil.putBoolean(SPUtil.KEY_FIX_Y, true);
		mouseManager.setFixY(true);
		SPUtil.putBoolean(SPUtil.KEY_CLICK_DEBUG, false);
		if(FlowerMouseService.getInstance() !=null){
			FlowerMouseService.getInstance().actionManager.inDebugMode = false;
		}
		SPUtil.putBoolean(SPUtil.KEY_SPACE_INPUT, true);
		if(FlowerMouseService.getInstance() !=null){
			FlowerMouseService.getInstance().space_input_bool = true;
		}
		
		lock_auto_back.setChecked(SPUtil.getBoolean(SPUtil.KEY_LOCK_BACK, true));
		keep_screen_on.setChecked(SPUtil.getBoolean(SPUtil.KEY_KEEP_ON, false));
		fix_x.setChecked(SPUtil.getBoolean(SPUtil.KEY_FIX_X, true));
		fix_y.setChecked(SPUtil.getBoolean(SPUtil.KEY_FIX_Y, true));
		//lay_on_setting.setChecked(SPUtil.getBoolean(SPUtil.KEY_LAYON_SETTING, false));
		click_debug.setChecked(SPUtil.getBoolean(SPUtil.KEY_CLICK_DEBUG, false));
		space_input.setChecked(SPUtil.getBoolean(SPUtil.KEY_SPACE_INPUT, true));
		//disable_input.setChecked(SPUtil.getBoolean(SPUtil.KEY_DISABLE_INPUT, false));
		
		
	}
	
	
	

	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}
	
	/**
     * 根据安卓版本初始化选项。
     * Android 7.0 (API 24) 或以上选择手势分发，否则选择节点点击。
     */
    private void initSelectionBasedOnVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            radioButtonGesture.setChecked(true);
			
        } else {
            radioButtonNodeClick.setChecked(true);
			radioButtonGesture.setEnabled(false);
			radioButtonGesture.setBackgroundColor(Color.GRAY);
	
        }
    }
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	
}
