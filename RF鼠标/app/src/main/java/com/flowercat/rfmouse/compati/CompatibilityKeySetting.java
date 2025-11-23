package com.flowercat.rfmouse.compati;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.key.KeyAction;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.service.KeyRecordListener;
import com.flowercat.rfmouse.util.KeyCodeUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.flowercat.rfmouse.util.SPUtil;

public class CompatibilityKeySetting extends Activity {

    private static final int TOTAL_STEPS = 6;
    private static final int COUNTDOWN_TIME = 10; // 10秒倒计时

    private int currentStep = 0;
    private Map<Integer, Integer> keyMap = new HashMap<Integer, Integer>();
    private Map<Integer, Boolean> stepStatus = new HashMap<Integer, Boolean>();

    private TextView titleText;
    private TextView instructionText;
    private TextView keyDisplay;
    private TextView keyNameText;
    private TextView countdownText;
    private ProgressBar countdownProgress;
    private Button prevButton;
    private Button nextButton;
    private Button exitButton;

    private TextView[] stepIndicators = new TextView[TOTAL_STEPS];

    private int countdownSeconds = COUNTDOWN_TIME;
    private boolean isCounting = false;
    private Handler countdownHandler;

    private String[] stepInstructions = {
        "请录入鼠标切换键",
        "请录入确认键",
        "请录入上方向键",
        "请录入下方向键", 
        "请录入左方向键",
        "请录入右方向键"
    };

    private String[] keyNames = {
        "鼠标切换键", "确认键", "上方向键", "下方向键", "左方向键", "右方向键"
    };

	private boolean inGuideMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        setContentView(R.layout.compatibility_key_setting);
		
		if(getIntent() != null){
			inGuideMode = getIntent().hasExtra("guide_mode");
		}
		
        initViews();
        setupCountdownHandler();
        startStep(0);
    }

    private void initViews() {
        titleText = (TextView) findViewById(R.id.titleText);
        instructionText = (TextView) findViewById(R.id.instructionText);
        keyDisplay = (TextView) findViewById(R.id.keyDisplay);
        keyNameText = (TextView) findViewById(R.id.keyNameText);
        countdownText = (TextView) findViewById(R.id.countdownText);
        countdownProgress = (ProgressBar) findViewById(R.id.countdownProgress);
        prevButton = (Button) findViewById(R.id.prevButton);
        nextButton = (Button) findViewById(R.id.nextButton);
        exitButton = (Button) findViewById(R.id.exitButton);

        stepIndicators[0] = (TextView) findViewById(R.id.step1);
        stepIndicators[1] = (TextView) findViewById(R.id.step2);
        stepIndicators[2] = (TextView) findViewById(R.id.step3);
        stepIndicators[3] = (TextView) findViewById(R.id.step4);
        stepIndicators[4] = (TextView) findViewById(R.id.step5);
        stepIndicators[5] = (TextView) findViewById(R.id.step6);

        prevButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					previousStep();
				}
			});

        nextButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					nextStep();
				}
			});

        exitButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showExitConfirmDialog();
				}
			});
    }

    private void setupCountdownHandler() {
        countdownHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (isCounting) {
                    countdownSeconds--;
                    updateCountdownDisplay();

                    if (countdownSeconds <= 0) {
                        showTimeoutDialog();
                    } else {
                        countdownHandler.sendEmptyMessageDelayed(0, 1000);
                    }
                }
            }
        };
    }

    private void startStep(int step) {
        currentStep = step;
        updateStepIndicator();
        updateInstruction();
        resetCountdown();
        startCountdown();
        startKeyListening();
    }

    private void updateStepIndicator() {
        for (int i = 0; i < TOTAL_STEPS; i++) {
            TextView indicator = stepIndicators[i];
            if (i < currentStep) {
                // 已完成步骤
                indicator.setTextColor(0xFFFFFFFF);
                indicator.setBackgroundResource(R.drawable.step_indicator_active);
            } else if (i == currentStep) {
                // 当前步骤
                indicator.setTextColor(0xFFFFFFFF);
                indicator.setBackgroundResource(R.drawable.step_indicator_active);
            } else {
                // 未完成步骤
                indicator.setTextColor(0xFF666666);
                indicator.setBackgroundResource(R.drawable.step_indicator_inactive);
            }
        }
    }

    private void updateInstruction() {
        instructionText.setText(stepInstructions[currentStep]);
        keyNameText.setText("等待按键...");
        keyDisplay.setText("?");

        // 更新按钮状态
        prevButton.setEnabled(currentStep > 0);
        if (currentStep == TOTAL_STEPS - 1) {
            nextButton.setText("完成");
        } else {
            nextButton.setText("下一个");
        }
    }

    private void resetCountdown() {
        countdownSeconds = COUNTDOWN_TIME;
        updateCountdownDisplay();
    }

    private void startCountdown() {
        isCounting = true;
        countdownHandler.sendEmptyMessage(0);
    }

    private void stopCountdown() {
        isCounting = false;
        countdownHandler.removeMessages(0);
    }

    private void updateCountdownDisplay() {
        countdownText.setText(countdownSeconds + "秒");
        int progress = (countdownSeconds * 100) / COUNTDOWN_TIME;
        countdownProgress.setProgress(progress);
    }

    private void startKeyListening() {
        if (FlowerMouseService.getInstance() != null) {
            FlowerMouseService.getInstance().recordKeyPress(new KeyRecordListener() {
					@Override
					public void onKeyPress(int keycode) {
						handleKeyPress(keycode);
					}
				});
        }
    }
	
	
	
	private void stopKeyListening() {
        if (FlowerMouseService.getInstance() != null) {
            FlowerMouseService.getInstance().stopRecordKeyPress();
        }
    }
	
	
	

    private void handleKeyPress(int keycode) {
        stopCountdown();

        keyMap.put(currentStep, keycode);
        stepStatus.put(currentStep, true);

        String keyStr = KeyCodeUtil.getKeyNameFromCode(keycode);
        keyDisplay.setText(keyStr);
        keyNameText.setText("录入: " + keyStr);

        // 自动进入下一步（延迟1秒让用户看到反馈）
        new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (currentStep < TOTAL_STEPS - 1) {
						nextStep();
					} else {
						showResultSummary();
					}
				}
			}, 1000);
    }

    private void previousStep() {
        if (currentStep > 0) {
            stopCountdown();
            startStep(currentStep - 1);
        }
    }

    private void nextStep() {
        if (currentStep < TOTAL_STEPS - 1) {
            stopCountdown();
            startStep(currentStep + 1);
        } else {
            showResultSummary();
        }
    }

	
	/*
    private void showTimeoutDialog() {
        stopCountdown();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示")
			.setMessage("在10秒内没有检测到按键输入，是否遇到问题？")
			.setPositiveButton("重试", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					resetCountdown();
					startCountdown();
					startKeyListening();
				}
			})
			.setNegativeButton("跳过", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					stepStatus.put(currentStep, false);
					if (currentStep < TOTAL_STEPS - 1) {
						nextStep();
					} else {
						showResultSummary();
					}
				}
			})
			.setCancelable(false)
			.show();
    }
	*/
	
	
	private void showTimeoutDialog() {
		
		if (FlowerMouseService.getInstance() != null) {
            FlowerMouseService.getInstance().spaceMenu = true;
        }
		
		stopCountdown();
		
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("按键录入")
        .setMessage("在10秒内没有检测到按键输入，是否遇到问题？")
        .setPositiveButton("重试", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					resetCountdown();
					startCountdown();
					startKeyListening();
					
					if (FlowerMouseService.getInstance() != null) {
						FlowerMouseService.getInstance().spaceMenu = false;
					}
				}
			})
			
        .setNegativeButton("跳过", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					if (FlowerMouseService.getInstance() != null) {
						FlowerMouseService.getInstance().spaceMenu = false;
					}
					
					stepStatus.put(currentStep, false);
					if (currentStep < TOTAL_STEPS - 1) {
						nextStep();
					} else {
						showResultSummary();
					}
				}
			})
		.setCancelable(false);

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
	
	
	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}
	
	
    private void showResultSummary() {
        stopCountdown();

        StringBuilder successBuilder = new StringBuilder("成功设置的按键：\n");
        StringBuilder failedBuilder = new StringBuilder("未能设置的按键：\n");

        for (int i = 0; i < TOTAL_STEPS; i++) {
            Boolean status = stepStatus.get(i);
            if (status != null && status) {
                Integer keycode = keyMap.get(i);
                if (keycode != null) {
                    successBuilder.append(keyNames[i]).append(": ")
						.append(KeyEvent.keyCodeToString(keycode)).append("\n");
                }
            } else {
                failedBuilder.append(keyNames[i]).append("\n");
            }
        }

        String message = successBuilder.toString() + "\n" + failedBuilder.toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设置完成");
		builder.setMessage(message);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					saveKeyMappingResults(stepStatus,keyMap,keyNames);
					stopKeyListening();
					setResult(RESULT_OK);
					finish();
				}
			});
		builder.setCancelable(false);
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

    private void showExitConfirmDialog() {
		
		stopCountdown();
		if (FlowerMouseService.getInstance() != null) {
            FlowerMouseService.getInstance().spaceMenu = true;
        }
		
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认退出")
			.setMessage("是否确定退出设置？未保存的所有进度将丢失。")
			.setPositiveButton("退出", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					stopKeyListening();
					setResult(RESULT_OK);
					finish();
				}
			})
			.setNegativeButton("取消", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					stopKeyListening();
					resetCountdown();
					startCountdown();
					startKeyListening();

					if (FlowerMouseService.getInstance() != null) {
						FlowerMouseService.getInstance().spaceMenu = false;
					}
				}
			});

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
		stopKeyListening();
        stopCountdown();
		if(countdownHandler != null){
			countdownHandler = null;
		}
		
		
    }

	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		showExitConfirmDialog();
	}
	

	/**
	 * 保存按键录入结果，不干扰其他已保存的按键设置
	 */
	/**
	 * 保存按键录入结果，对于录入成功的按键，如果已存在则覆盖其动作为占位符，如果不存在则新增
	 */
	public static void saveKeyMappingResults(Map<Integer, Boolean> stepStatus, Map<Integer, Integer> keyMap, String[] keyNames) {
		// 加载现有的按键设置
		List<KeyAction> existingKeyActions = SPUtil.loadData();

		// 创建现有按键名称的映射，方便查找
		Map<String, KeyAction> keyActionMap = new HashMap<>();
		for (KeyAction action : existingKeyActions) {
			keyActionMap.put(action.getKeyName(), action);
		}

		// 处理新录入的按键
		for (int i = 0; i < stepStatus.size(); i++) {
			Boolean status = stepStatus.get(i);
			if (status != null && status) { // 只处理录入成功的按键
				Integer keycode = keyMap.get(i);
				if (keycode != null) {
					String keyName = KeyCodeUtil.getKeyNameFromCode(keycode);

					// 如果这个按键名称已经存在，则覆盖其短按和长按动作为占位符
					if (keyActionMap.containsKey(keyName)) {
						KeyAction existingAction = keyActionMap.get(keyName);
						
						if(keyNames[i].equals("鼠标切换键")){
							existingAction.setShortPressAction("默认");
							existingAction.setLongPressAction("模式切换");
						}
						
						if(keyNames[i].equals("确认键")){
							existingAction.setShortPressAction("鼠标短按");
							existingAction.setLongPressAction("点击菜单");
						}
						
						if(keyNames[i].equals("上方向键")){
							existingAction.setShortPressAction("鼠标上移/上滑");
							existingAction.setLongPressAction("鼠标加速上移");
						}
						
						if(keyNames[i].equals("下方向键")){
							existingAction.setShortPressAction("鼠标下移/下滑");
							existingAction.setLongPressAction("鼠标加速下移");
						}
						
						if(keyNames[i].equals("左方向键")){
							existingAction.setShortPressAction("鼠标左移/左滑(*)");
							existingAction.setLongPressAction("鼠标加速左移");
						}
						
						if(keyNames[i].equals("右方向键")){
							existingAction.setShortPressAction("鼠标右移/右滑(*)");
							existingAction.setLongPressAction("鼠标加速右移");
						}
						
					} else {
						// 如果不存在，则添加新的KeyAction
						KeyAction newAction = new KeyAction(keyName);
						
						if(keyNames[i].equals("鼠标切换键")){
							newAction.setShortPressAction("默认");
							newAction.setLongPressAction("模式切换");
						}

						if(keyNames[i].equals("确认键")){
							newAction.setShortPressAction("鼠标短按");
							newAction.setLongPressAction("点击菜单");
						}

						if(keyNames[i].equals("上方向键")){
							newAction.setShortPressAction("鼠标上移/上滑");
							newAction.setLongPressAction("鼠标加速上移");
						}

						if(keyNames[i].equals("下方向键")){
							newAction.setShortPressAction("鼠标下移/下滑");
							newAction.setLongPressAction("鼠标加速下移");
						}

						if(keyNames[i].equals("左方向键")){
							newAction.setShortPressAction("鼠标左移/左滑(*)");
							newAction.setLongPressAction("鼠标加速左移");
						}

						if(keyNames[i].equals("右方向键")){
							newAction.setShortPressAction("鼠标右移/右滑(*)");
							newAction.setLongPressAction("鼠标加速右移");
						}
						existingKeyActions.add(newAction);
						keyActionMap.put(keyName, newAction);
					}
				}
			}
		}

		// 保存更新后的列表
		SPUtil.saveData(existingKeyActions);
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().updateKeyListeners(existingKeyActions);
		}
	}

 
}
