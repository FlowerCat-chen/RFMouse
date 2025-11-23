package com.flowercat.rfmouse.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.util.SPUtil;
import com.flowercat.rfmouse.compati.CompatibilityKeySetting;
import com.flowercat.rfmouse.compati.MIUIUtils;
import android.content.Context;

public class WizardActivity extends Activity {

    private static final int STEP_AGREEMENT = 0;
    private static final int STEP_PERMISSIONS = 1;
    private static final int STEP_IMPORT = 2;
    private static final int STEP_COMPATIBILITY = 3;
    private static final int STEP_MOUSE_TEST = 4;

	private static final int REQUEST_PERM = 1000;
    private static final int REQUEST_IMPORT = 1001;
    private static final int REQUEST_COMPATIBILITY = 1002;
    private static final int REQUEST_MOUSE_TEST = 1003;

    private int currentStep = STEP_AGREEMENT;
    private LinearLayout contentLayout;
    private Button btnPrevious, btnNext;
    private TextView stepTitle;

    // 状态变量
    private boolean agreementAccepted = false;
    private boolean permissionsGranted = false;
    private boolean configImported = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.wizard_mouse);

        agreementAccepted = SPUtil.getBoolean(SPUtil.KEY_AGREEMENT_ACCEPTED, false);

        // 如果协议已接受，直接跳过协议步骤
        if (agreementAccepted) {
            currentStep = STEP_PERMISSIONS;
        }

        initializeViews();
        setupListeners();
        showStep(currentStep);

		//提醒用户
		MIUIUtils.checkAndPromptDeveloperOptions(this);
    }

    private void initializeViews() {
        contentLayout = (LinearLayout) findViewById(R.id.content_layout);
        btnPrevious = (Button) findViewById(R.id.btn_previous);
        btnNext = (Button) findViewById(R.id.btn_next);
        stepTitle = (TextView) findViewById(R.id.step_title);
    }

    private void setupListeners() {
        btnPrevious.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentStep > getFirstAvailableStep()) {
						currentStep--;
						showStep(currentStep);
					}
				}
			});

        btnNext.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					handleNextButton();
				}
			});
    }

    /**
     * 获取第一个可用的步骤（如果协议已接受则跳过协议步骤）
     */
    private int getFirstAvailableStep() {
        return agreementAccepted ? STEP_PERMISSIONS : STEP_AGREEMENT;
    }

    private void handleNextButton() {
        switch (currentStep) {
            case STEP_AGREEMENT:
                if (agreementAccepted) {
                    currentStep++;
                    showStep(currentStep);
                } else {
                    Toast.makeText(WizardActivity.this, 
                                   "请先接受用户协议", Toast.LENGTH_SHORT).show();
                }
                break;

            case STEP_PERMISSIONS:
                // 直接进入下一步，不等待权限回调
                currentStep++;
                showStep(currentStep);
                break;

            case STEP_IMPORT:
                // 直接进入下一步，不等待导入回调
                currentStep++;
                showStep(currentStep);
                break;

            case STEP_COMPATIBILITY:
                // 启动按键录入Activity并自动进入下一步
                Intent keyIntent = new Intent(WizardActivity.this, 
											  CompatibilityKeySetting.class);
				keyIntent.putExtra("guide_mode","引导模式");
				startActivityForResult(keyIntent, REQUEST_COMPATIBILITY);

				// 不等待回调，直接进入下一步
				currentStep++;
				showStep(currentStep);
                break;

            case STEP_MOUSE_TEST:
                // 启动鼠标测试Activity
                Intent mouseIntent = new Intent(WizardActivity.this, 
                                                MouseTestActivity.class);
				mouseIntent.putExtra("guide_mode","引导模式");
                startActivityForResult(mouseIntent, REQUEST_MOUSE_TEST);

                // 显示完成对话框，不等待回调
                showCompletionDialog();
                break;
        }
    }

    private void showStep(int step) {
        contentLayout.removeAllViews();

        // 如果协议已接受且当前要显示协议步骤，则跳过直接显示权限步骤
        if (agreementAccepted && step == STEP_AGREEMENT) {
            step = STEP_PERMISSIONS;
            currentStep = step;
        }

        switch (step) {
            case STEP_AGREEMENT:
                showAgreementStep();
                break;
            case STEP_PERMISSIONS:
                showPermissionsStep();
                break;
            case STEP_IMPORT:
                showImportStep();
                break;
            case STEP_COMPATIBILITY:
                showCompatibilityStep();
                break;
            case STEP_MOUSE_TEST:
                showMouseTestStep();
                break;
        }

        updateNavigation();
    }

    private void showAgreementStep() {
        stepTitle.setText("用户协议");

        View agreementView = getLayoutInflater().inflate(
            R.layout.step_agreement, contentLayout, false);
        contentLayout.addView(agreementView);

        final TextView agreementText = (TextView) agreementView.findViewById(
            R.id.agreement_text);
        final Button acceptButton = (Button) agreementView.findViewById(
            R.id.btn_accept);

        agreementText.setText("欢迎使用咱的应用汪！\n\n" +
                              "请仔细阅读以下用户协议：\n\n" +
                              "1. 本应用尊重并保护你的个人隐私权\n" +
                              "2. 你同意不利用本应用进行任何违法活动\n" +
                              "3. 所有的数据处理均在本地，咱也没有服务器\n" +
                              "4. 最好不要将本应用用于商业用途（不稳定）\n\n" +
                              "点击'接受'按钮表示您同意以上协议。");

        acceptButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					agreementAccepted = true;
					// 保存到 SharedPreferences
					SPUtil.putBoolean(SPUtil.KEY_AGREEMENT_ACCEPTED, true);
					acceptButton.setEnabled(false);
					acceptButton.setText("已接受");
					Toast.makeText(WizardActivity.this, 
								   "协议已接受", Toast.LENGTH_SHORT).show();

					// 协议接受后，自动进入下一步
					currentStep = STEP_PERMISSIONS;
					showStep(currentStep);
				}
			});
    }

    private void showPermissionsStep() {
        stepTitle.setText("权限申请");

        View permissionsView = getLayoutInflater().inflate(
            R.layout.step_permissions, contentLayout, false);
        contentLayout.addView(permissionsView);

        final Button grantButton = (Button) permissionsView.findViewById(
            R.id.btn_grant_permissions);
        final TextView permissionsText = (TextView) permissionsView.findViewById(
            R.id.permissions_text);

		final Button btn_skip_permissions =  (Button) permissionsView.findViewById(
            R.id.btn_skip_permissions);
        permissionsText.setText("应用需要以下权限：\n\n" +
                                "• 存储权限 - 用于保存配置文件和数据\n" +
                                "• 辅助服务 - 用于拦截系统按键\n" +
                                "• 悬浮窗 - 用于显示鼠标\n\n" +
                                "点击下方按钮授予权限");

		if(checkOverlayPermission() && isAccessibilityServiceEnabled() && checkReadWritePermission()){
			permissionsGranted = true;
			grantButton.setEnabled(false);
			grantButton.setText("权限已有");
			Toast.makeText(WizardActivity.this, 
						   "权限已有", Toast.LENGTH_SHORT).show();
		}

		btn_skip_permissions.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					permissionsGranted = true;
					grantButton.setEnabled(false);
					grantButton.setText("用户选择稍后用adb授权");
					Toast.makeText(WizardActivity.this, 
								   "用户选择稍后用adb授权", Toast.LENGTH_SHORT).show();
				}
			});					

        grantButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 权限授予过程
					Intent perm = new Intent(WizardActivity.this, 
											 PermissionRequestActivity.class);
					perm.putExtra("guide_mode","引导模式");
					startActivityForResult(perm, REQUEST_PERM);
				}
			});
    }

    private void showImportStep() {
        stepTitle.setText("导入配置");

        View importView = getLayoutInflater().inflate(
            R.layout.step_import, contentLayout, false);
        contentLayout.addView(importView);

        Button importYes = (Button) importView.findViewById(R.id.btn_import_yes);
        Button importNo = (Button) importView.findViewById(R.id.btn_import_no);

        importYes.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 启动导入Activity
					Intent importIntent = new Intent(WizardActivity.this, 
													 ConfigManagerActivity.class);
					importIntent.putExtra("guide_mode","引导模式");
					startActivityForResult(importIntent, REQUEST_IMPORT);

					// 不等待回调，用户可以选择继续操作
					Toast.makeText(WizardActivity.this, 
								   "导入配置已启动，您可以继续下一步", Toast.LENGTH_SHORT).show();
				}
			});

        importNo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					configImported = false;
					currentStep++;
					showStep(currentStep);
				}
			});
    }

    private void showCompatibilityStep() {
        stepTitle.setText("快速按键录入");

        View compatView = getLayoutInflater().inflate(
            R.layout.step_compatibility, contentLayout, false);
        contentLayout.addView(compatView);

        TextView compatText = (TextView) compatView.findViewById(R.id.compat_text);
        compatText.setText("点击'开始设置'录入鼠标相关按键。录入按键需要权限，请确保授权完成。");
    }

    private void showMouseTestStep() {
        stepTitle.setText("鼠标测试");

        View mouseView = getLayoutInflater().inflate(
            R.layout.step_mouse_test, contentLayout, false);
        contentLayout.addView(mouseView);

        TextView mouseText = (TextView) mouseView.findViewById(R.id.mouse_test_text);
        mouseText.setText("点击'开始测试'进行鼠标功能测试。鼠标测试需要权限，请确保授权完成。");
    }

    private void updateNavigation() {
        // 更新按钮状态
        btnPrevious.setEnabled(currentStep > getFirstAvailableStep());

        switch (currentStep) {
            case STEP_AGREEMENT:
                btnNext.setText("下一步");
                break;
            case STEP_PERMISSIONS:
                btnNext.setText("下一步");
                break;
            case STEP_IMPORT:
                btnNext.setText("下一步");
                break;
            case STEP_COMPATIBILITY:
                btnNext.setText("开始设置");
                break;
            case STEP_MOUSE_TEST:
                btnNext.setText("开始测试");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
				case REQUEST_PERM:
                    permissionsGranted = true;
                    Toast.makeText(this, "权限申请完成", Toast.LENGTH_SHORT).show();
                    break;

                case REQUEST_IMPORT:
                    configImported = true;
                    Toast.makeText(this, "配置导入完成", Toast.LENGTH_SHORT).show();
                    break;

                case REQUEST_COMPATIBILITY:
                    Toast.makeText(this, "按键录入设置完成", Toast.LENGTH_SHORT).show();
                    break;

                case REQUEST_MOUSE_TEST:
                    Toast.makeText(this, "鼠标测试完成", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    /**
     * 显示完成对话框
     */
    private void showCompletionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("向导完成")
            .setMessage("初次使用向导已完成！如果鼠标无法正常点击，请前往鼠标设置，并将模式改为 节点点击 或者 触摸注入。")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });

        // 创建对话框并设置按钮样式
        AlertDialog dialog = builder.show();
        Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveBtn != null) {
            LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
            positiveParams.leftMargin = dp2px(this, 30);
            positiveBtn.setLayoutParams(positiveParams);
            positiveBtn.setBackgroundResource(R.drawable.button_background_selector);
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("退出向导")
            .setMessage("确定要退出初次使用向导吗？")
            .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            })
            .setNegativeButton("取消", null);
		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dialog = builder.show();

		// 2. 获取"确认"按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
		}

		// 3. 获取"取消"按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
			negativeBtn.setLayoutParams(negativeParams);
			negativeBtn.setBackgroundResource(R.drawable.button_background_selector);
		}
    }

	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}

	// 检查悬浮窗权限
    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // 低于 Android M 的系统默认有权限
    }

	//检查辅助服务是否打开？
    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // 忽略
        }

        if (accessibilityEnabled == 1) {
            String service = getPackageName() + "/" + FlowerMouseService.class.getName();
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null && settingValue.contains(service)) {
                return true;
            }
        }
        return false;
    }


    // 检查读写权限
    private boolean checkReadWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // 低于 Android M 的系统默认有权限
    }
}
