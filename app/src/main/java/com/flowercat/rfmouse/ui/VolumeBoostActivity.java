package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout;
import android.view.KeyEvent;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.util.SPUtil;
import com.flowercat.rfmouse.service.FlowerMouseService;

public class VolumeBoostActivity extends Activity {

    private static final String PREFS_NAME = "VolumeBoosterPrefs";
    private static final String PREF_MAX_BOOST = "maxBoostPercentage";
    private static final int DEFAULT_MAX_BOOST = 100;

    // 使用字符串常量代替类中可能不存在的字段
    private static final String ACTION_VOLUME_CHANGED_STREAM = "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";

    private SeekBar systemVolumeSeekBar;
    private SeekBar enhancePercentageSeekBar;
    private Button stopButton;
    private RelativeLayout stop_boost,rels1,rels2;
    private TextView currentSystemVolumeTextView;
    private TextView enhancePercentageTextView;

    private AudioManager audioManager;
  
    private SharedPreferences sharedPreferences;

    private int maxBoostPercentage = DEFAULT_MAX_BOOST;
	
	private int boostPercent = 0;

    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 检查广播动作是否是音量改变
            String action = intent.getAction();
            if (ACTION_VOLUME_CHANGED_STREAM.equals(action)) {
                // 检查改变的是否是媒体音量
                int streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType == AudioManager.STREAM_MUSIC) {
                    // 获取当前媒体音量并更新UI
                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    systemVolumeSeekBar.setProgress(currentVolume);
                    currentSystemVolumeTextView.setText("系统音量: " + currentVolume + "/" + maxVolume);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.vol_up);

        // 初始化UI组件
        systemVolumeSeekBar = findViewById(R.id.system_volume_seekbar);
        enhancePercentageSeekBar = findViewById(R.id.enhance_percentage_seekbar);
        stopButton = findViewById(R.id.stop_button);
        stop_boost = findViewById(R.id.stop_boost);
		
		rels1 = findViewById(R.id.rels1);
		rels2 = findViewById(R.id.rels2);
		
        currentSystemVolumeTextView = findViewById(R.id.current_system_volume_text);
        enhancePercentageTextView = findViewById(R.id.enhance_percentage_text);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
 
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 加载最大提升百分比限制
        maxBoostPercentage = sharedPreferences.getInt(PREF_MAX_BOOST, DEFAULT_MAX_BOOST);

        // 初始化系统音量UI
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        systemVolumeSeekBar.setMax(maxVolume);
        systemVolumeSeekBar.setProgress(currentVolume);
        currentSystemVolumeTextView.setText("系统音量: " + currentVolume + "/" + maxVolume);

        systemVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					//if (fromUser) {
						audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
						currentSystemVolumeTextView.setText("系统音量: " + progress + "/" + maxVolume);
					//}
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
			});

        // 设置音量提升百分比拖动条
        enhancePercentageSeekBar.setMax(maxBoostPercentage);
        enhancePercentageTextView.setText("增强百分比: 0%");
		
		enhancePercentageSeekBar.setProgress(SPUtil.getInt(SPUtil.KEY_UP_PERCENT,0));
		enhancePercentageTextView.setText("增强百分比: " + SPUtil.getInt(SPUtil.KEY_UP_PERCENT,0) + "%");
		
		if(FlowerMouseService.getInstance() != null && FlowerMouseService.getInstance().enhancerUtil != null){
			FlowerMouseService.getInstance().enhancerUtil.setEnhanceGain(SPUtil.getInt(SPUtil.KEY_UP_PERCENT,0), maxBoostPercentage);
		}
		
        enhancePercentageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					//if (fromUser) {
					if(FlowerMouseService.getInstance() != null && FlowerMouseService.getInstance().enhancerUtil != null){
						FlowerMouseService.getInstance().enhancerUtil.setEnhanceGain(progress, maxBoostPercentage);
					}
						boostPercent = progress;
						enhancePercentageTextView.setText("增强百分比: " + progress + "%");
						SPUtil.putInt(SPUtil.KEY_UP_PERCENT,progress);
					//}
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
			});

        // 一键停止按钮
        stopButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(FlowerMouseService.getInstance() != null && FlowerMouseService.getInstance().enhancerUtil != null){
						FlowerMouseService.getInstance().enhancerUtil.stopEnhance();
					}
					enhancePercentageSeekBar.setProgress(0);
					boostPercent  = 0;
					SPUtil.putInt(SPUtil.KEY_UP_PERCENT,0);
					Toast.makeText(VolumeBoostActivity.this, "音量增强已停止", Toast.LENGTH_SHORT).show();
				}
			});

		// 一键停止按钮
        stop_boost.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					 stopButton.performClick();
				}
			});
      
			
		//让事件继续向下传递。
		rels1.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								systemVolumeSeekBar.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
		rels2.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 检查按键是否是方向键，并且是按下事件
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						switch (keyCode) {
							case KeyEvent.KEYCODE_DPAD_RIGHT:
							case KeyEvent.KEYCODE_DPAD_LEFT:
								// 将事件传递给子视图（SeekBar）
								enhancePercentageSeekBar.onKeyDown(keyCode, event);
								// 返回 true 表示我们已经处理了这个事件
								return true;
						}
					}
					// 返回 false 表示事件未被处理，让它继续向下传递
					return false;
				}
			});
			
			 // 注册广播接收器，当 Activity 可见时开始监听音量变化
        IntentFilter filter = new IntentFilter(ACTION_VOLUME_CHANGED_STREAM);
        registerReceiver(volumeReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
		
		int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		systemVolumeSeekBar.setProgress(currentVolume);
		currentSystemVolumeTextView.setText("系统音量: " + currentVolume + "/" + maxVolume);
	
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 在 Activity 不可见时取消注册，避免不必要的资源消耗
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 在 Activity 销毁时释放所有资源
		if(FlowerMouseService.getInstance() != null && FlowerMouseService.getInstance().enhancerUtil != null ){
			//FlowerMouseService.getInstance().enhancerUtil.release();
		}
       
		if(volumeReceiver != null){
			unregisterReceiver(volumeReceiver);
		}
    }
}
