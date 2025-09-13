package com.flowercat.rfmouse.ui;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;
import com.flowercat.rfmouse.ui.AdvanceSettingsActivity;
import com.flowercat.rfmouse.util.SPUtil;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.service.FlowerMouseService;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.KeyEvent;

public class AdvanceSettingsActivity extends Activity {

   
    
    private Switch switch1, switch2, switch3,switch4, switch5;
	private RelativeLayout l1,l2,l3,l4,l5;
	
	private SeekBar seek_bar_judge;
	private RelativeLayout long_click_judge;
	private TextView tv_long_cluck_judge;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.advance_settings);

        // 初始化Switch控件
        switch1 = findViewById(R.id.switch_1);
        switch2 = findViewById(R.id.switch_2);
      	switch3 = findViewById(R.id.switch_3);
        switch4 = findViewById(R.id.switch_4);
        switch5 = findViewById(R.id.switch_5);
		
		seek_bar_judge = findViewById(R.id.seek_bar_judge);
		long_click_judge = findViewById(R.id.long_click_judge);
		tv_long_cluck_judge = findViewById(R.id.tv_long_click_judge);
		
		l1 = findViewById(R.id.l1);
		l2 = findViewById(R.id.l2);
		l3 = findViewById(R.id.l3);
		l4 = findViewById(R.id.l4);
		l5 = findViewById(R.id.l5);

        // 从SharedPreferences加载保存的状态
        loadSavedStates();

        // 设置Switch状态改变监听器
        setupSwitchListeners();
    }

    private void loadSavedStates() {
        // 使用默认值false（未选中）如果没有找到保存的值
        switch1.setChecked(SPUtil.getBoolean(SPUtil.KEY_KEYBOARD_LIGHT, true));
        switch2.setChecked(SPUtil.getBoolean(SPUtil.KEY_WIFI_ON_BOOT, false));
		switch3.setChecked(SPUtil.getBoolean(SPUtil.KEY_MOUSE_ON_BOOT, false));
        switch4.setChecked(SPUtil.getBoolean(SPUtil.KEY_KEY_MAPPING_HINT, false));
        switch5.setChecked(SPUtil.getBoolean(SPUtil.KEY_HOME_KEY_LOCK, true));
		seek_bar_judge.setProgress(SPUtil.getInt(SPUtil.KEY_LONG_CLICK_JUDGE,300));
		tv_long_cluck_judge.setText(String.valueOf(SPUtil.getInt(SPUtil.KEY_LONG_CLICK_JUDGE,300)));
    }

    private void setupSwitchListeners() {
		
		l1.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(switch1.isChecked()){
						switch1.setChecked(false);
					} else {
						switch1.setChecked(true);
					}
					
				}
		});
		
		l2.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(switch2.isChecked()){
						switch2.setChecked(false);
					} else {
						switch2.setChecked(true);
					}

				}
			});
			
		l3.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(switch3.isChecked()){
						switch3.setChecked(false);
					} else {
						switch3.setChecked(true);
					}

				}
			});
		
			
		l4.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(switch4.isChecked()){
						switch4.setChecked(false);
					} else {
						switch4.setChecked(true);
					}

				}
			});
			
		l5.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(switch5.isChecked()){
						switch5.setChecked(false);
					} else {
						switch5.setChecked(true);
					}

				}
			});
			
		//让事件继续向下传递。
		long_click_judge.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								seek_bar_judge.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
			
        // 为每个Switch设置监听器
		//屏蔽键盘灯
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_KEYBOARD_LIGHT, isChecked);
				}
			});

		//开启wifi
        switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_WIFI_ON_BOOT, isChecked);
				}
			});

		//开启鼠标
        switch3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_MOUSE_ON_BOOT, isChecked);
				}
			});
			
		//按下按键时弹窗
        switch4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_KEY_MAPPING_HINT, isChecked);
					
					if(FlowerMouseService.getInstance() != null){
						//设置是否应该显示按键映射时的通知？
						FlowerMouseService.getInstance().shouldShowToast = isChecked;
					}
				}
			});
			
		//使用home锁屏
        switch5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SPUtil.putBoolean(SPUtil.KEY_HOME_KEY_LOCK, isChecked);
					if(isChecked){
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().registerHomeListenReceiver();
						}
					} else {
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().unregisterHomeListenReceiver();
						}
					}
				}
			});
			
			
		seek_bar_judge.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						tv_long_cluck_judge.setText(String.valueOf(progress));
						SPUtil.putInt(SPUtil.KEY_LONG_CLICK_JUDGE, progress);
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().keyPressManager.setLongClickJudge(progress);
						}
					}
				}
				@Override public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			});
		
			
    }

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	
}
