package com.flowercat.rfmouse.compati;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import com.flowercat.rfmouse.R;

public class MIUIUtils {

    private static final String TAG = "MIUIUtils";

    /**
     * 判断当前系统是否为MIUI
     * @return true: MIUI系统 false: 非MIUI系统
     */
    public static boolean isMIUI() {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method getMethod = systemPropertiesClass.getDeclaredMethod("get", String.class);
            String miuiVersion = (String) getMethod.invoke(systemPropertiesClass, "ro.miui.ui.version.name");
            return miuiVersion != null && !miuiVersion.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "判断MIUI系统时发生异常", e);
            return false;
        }
    }

    /**
     * 检查并提示用户进入开发者选项
     * @param context 上下文对象
     */
    public static void checkAndPromptDeveloperOptions(final Context context) {
        if (!isMIUI()) {
            Log.i(TAG, "当前不是MIUI系统，无需提示");
            return;
        }

        try {
            // 确保在主线程中显示对话框
            if (context instanceof android.app.Activity) {
                showDialog(context);
            } else {
                // 如果不是Activity上下文，可能需要其他处理方式
                Log.w(TAG, "上下文不是Activity，可能无法显示对话框");
            }
        } catch (Exception e) {
            Log.e(TAG, "显示对话框时发生异常", e);
        }
    }

    /**
     * 显示提示对话框
     */
    private static void showDialog(final Context context) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("MIUI?");
            builder.setMessage("MIUI会限制adb授权，以及触摸注入。请开启开发者选项中的USB调试(安全设置，在USB调试项的下面一点)。点击确认将跳转到开发者选项设置页面。");

            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						openDeveloperOptions(context);
						dialog.dismiss();
					}
				});

            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

            builder.setCancelable(false);

            AlertDialog dialog = builder.show();

            // 设置按钮样式（仅在API level 11及以上支持）
            if (Build.VERSION.SDK_INT >= 11) {
                setButtonStyles(context, dialog);
            }

        } catch (Exception e) {
            Log.e(TAG, "创建或显示对话框时发生异常", e);
        }
    }

    /**
     * 设置按钮样式（Android 3.0+）
     */
    private static void setButtonStyles(Context context, AlertDialog dialog) {
        try {
            // 获取"确认"按钮并设置样式
            Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positiveBtn != null) {
                LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
                positiveParams.leftMargin = dp2px(context, 30);
                positiveBtn.setLayoutParams(positiveParams);

                // 检查资源是否存在，避免ResourceNotFoundException
                try {
                    positiveBtn.setBackgroundResource(R.drawable.button_background_selector);
                } catch (Exception e) {
                    Log.w(TAG, "按钮背景资源未找到，使用默认样式");
                }
            }

            // 获取"取消"按钮并设置样式
            Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            if (negativeBtn != null) {
                LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
                negativeParams.rightMargin = dp2px(context, 10);
                negativeBtn.setLayoutParams(negativeParams);

                try {
                    negativeBtn.setBackgroundResource(R.drawable.button_background_selector);
                } catch (Exception e) {
                    Log.w(TAG, "按钮背景资源未找到，使用默认样式");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "设置按钮样式时发生异常", e);
        }
    }

    // 工具方法：dp转px
    private static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 打开开发者选项设置页面（兼容Android 4.0及以下版本）
     * @param context 上下文对象
     */
    private static void openDeveloperOptions(Context context) {
        try {
            Intent intent;

            // 根据Android版本使用不同的Intent
            if (Build.VERSION.SDK_INT >= 17) {
                // Android 4.2+ 使用标准的开发者选项Intent
                intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            } else {
                // Android 4.0及以下版本使用通用的设置Intent
                // 或者尝试使用已知的Action
                intent = new Intent();

                // 尝试几种可能的开发者选项入口
                String[] possibleActions = {
                    "android.settings.APPLICATION_DEVELOPMENT_SETTINGS", // 标准Action（可能不存在）
                    "android.settings.DEVELOPMENT_SETTINGS",             // 一些设备的自定义Action
                };

                boolean intentSet = false;
                for (String action : possibleActions) {
                    try {
                        intent.setAction(action);
                        if (intent.resolveActivity(context.getPackageManager()) != null) {
                            intentSet = true;
                            break;
                        }
                    } catch (Exception e) {
                        // 忽略异常，继续尝试下一个
                    }
                }

                // 如果所有特定Action都失败，使用通用设置
                if (!intentSet) {
                    intent = new Intent(Settings.ACTION_SETTINGS);
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 检查是否有可以处理该Intent的Activity
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                // 备用方案：尝试打开常规设置页面
                openFallbackSettings(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "打开开发者选项时发生异常", e);
            openFallbackSettings(context);
        }
    }

    /**
     * 打开备用设置页面
     */
    private static void openFallbackSettings(Context context) {
        try {
            Intent fallbackIntent = new Intent(Settings.ACTION_SETTINGS);
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (fallbackIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(fallbackIntent);
                Log.i(TAG, "使用备用设置页面");
            } else {
                Log.e(TAG, "无法打开任何设置页面");
            }
        } catch (Exception fallbackException) {
            Log.e(TAG, "打开备用设置页面时发生异常", fallbackException);
        }
    }

    /**
     * 获取MIUI版本信息
     * @return MIUI版本号，如果不是MIUI返回null
     */
    public static String getMIUIVersion() {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method getMethod = systemPropertiesClass.getDeclaredMethod("get", String.class);
            String miuiVersion = (String) getMethod.invoke(systemPropertiesClass, "ro.miui.ui.version.name");
            return miuiVersion;
        } catch (Exception e) {
            Log.e(TAG, "获取MIUI版本时发生异常", e);
            return null;
        }
    }
}
