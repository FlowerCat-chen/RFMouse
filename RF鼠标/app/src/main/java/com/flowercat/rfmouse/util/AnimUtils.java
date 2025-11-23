package com.flowercat.rfmouse.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import java.lang.reflect.Field;
import android.app.Activity;
import android.os.RemoteException;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

public class AnimUtils {

    private static final String TAG = "AnimationUtils";

    // 检查系统是否禁用了动画
    public static boolean areAnimationsEnabled(Context context) {
        try {
            ContentResolver resolver = context.getContentResolver();
            float transitionAnimationScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE);
            float animatorDurationScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE);

            return transitionAnimationScale != 0 && animatorDurationScale != 0;
        } catch (Settings.SettingNotFoundException e) {
            // 默认返回 true，即假设动画未被禁用
            return true;
        }
    }

    // 使用反射强制启用所有动画
    public static void forceEnableAllAnimators() {
        try {
            // 使用反射获取 Animator 类的 private 字段 "sDurationScale"
            Class<?> animatorClass = Class.forName("android.animation.Animator");
            Field sDurationScaleField = animatorClass.getDeclaredField("sDurationScale");
            sDurationScaleField.setAccessible(true);

            // 强制设置动画持续时间比例为1.0，即使系统禁用了动画
            sDurationScaleField.setFloat(null, 1.0f);
			
        } catch (Exception e) {
            Log.e(TAG, "Failed to force enable all animators", e);
		
        }

    }

	
	
	public static void animateView(final View view,final AnimatorListenerAdapter callback) {
		// 1. 设置控件的初始位置
		view.setTranslationY(view.getHeight());
		view.setAlpha(0f);
		
		//取消view的隐藏状态
		if(view.getVisibility() ==  View.GONE){
			view.setVisibility(View.VISIBLE);
		}

		// 2. 创建动画
		AnimatorSet animatorSet = new AnimatorSet();

		// 移动动画
		ObjectAnimator moveUp = ObjectAnimator.ofFloat(view, "translationY", view.getHeight(), 0f);

		// 翻转动画
		ObjectAnimator rotate = ObjectAnimator.ofFloat(view, "rotationY", 90f, 0f);

		// 渐显动画
		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

		// 将这些动画添加到集合中
		animatorSet.playTogether(moveUp, rotate, fadeIn);

		// 动画持续时间
		animatorSet.setDuration(1000);

		//设置监听器
		animatorSet.addListener(callback);
		
		// 开始动画
		animatorSet.start();
	}
	
	
	
	public static void overridePendingTransition(Activity mActivity,int enterAnim, int exitAnim) {
            mActivity.overridePendingTransition(enterAnim, exitAnim);
    }
	
	
	
	
}
