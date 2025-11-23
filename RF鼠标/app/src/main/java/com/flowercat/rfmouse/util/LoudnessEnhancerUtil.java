package com.flowercat.rfmouse.util;

import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.util.Log;

public class LoudnessEnhancerUtil {

    private static final String TAG = "LoudnessEnhancerUtil";

    // 使用 0 作为 session ID，尝试作用于全局音频混音器
    private static final int GLOBAL_AUDIO_SESSION_ID = 0;
    // Equalizer 的 ID，可以是任意非 0 的整数
    private static final int EQUALIZER_ID = 87654325;

    private LoudnessEnhancer mLoudnessEnhancer;
    private Equalizer mEqualizer;
    private short mEqualizerBands;
    private short mEqualizerMaxLevel;

    private boolean isEnhancerEnabled = false;
    private int currentGainInPercentage = 0;
	
	// 单例实例
    private static LoudnessEnhancerUtil instance;
	
    public static synchronized LoudnessEnhancerUtil getInstance() {
        if (instance == null) {
            instance = new LoudnessEnhancerUtil();
        }
        return instance;
    }
	
	
    /**
     * 构造函数。初始化
     */
    public void init() {
        // 尝试创建 LoudnessEnhancer
        try {
            Log.d(TAG, "Trying to create LoudnessEnhancer with session ID 0.");
            mLoudnessEnhancer = new LoudnessEnhancer(GLOBAL_AUDIO_SESSION_ID);
            // 默认禁用，等用户设置增益后再启用
            mLoudnessEnhancer.setEnabled(false);
            Log.d(TAG, "LoudnessEnhancer created successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create LoudnessEnhancer, falling back to Equalizer.", e);
            mLoudnessEnhancer = null;
            // 尝试创建 Equalizer
            try {
                mEqualizer = new Equalizer(EQUALIZER_ID, GLOBAL_AUDIO_SESSION_ID);
                mEqualizer.setEnabled(false);
                mEqualizerBands = mEqualizer.getNumberOfBands();
                mEqualizerMaxLevel = mEqualizer.getBandLevelRange()[1];
                Log.d(TAG, "Equalizer created successfully with " + mEqualizerBands + " bands.");
            } catch (Exception e2) {
                Log.e(TAG, "Failed to create Equalizer. No enhancement available.", e2);
                mEqualizer = null;
            }
        }
    }

    /**
     * 启用并设置音量增强效果。
     * @param gainInPercentage 提升的百分比（0-100）。
     * @param maxPercentageLimit 限制的最大百分比。
     */
    public void setEnhanceGain(int gainInPercentage, int maxPercentageLimit) {
        // 确保增益百分比不超过限制
        if (gainInPercentage > maxPercentageLimit) {
            gainInPercentage = maxPercentageLimit;
        }

        currentGainInPercentage = gainInPercentage;

        // 如果增益为0，则停止增强效果
        if (currentGainInPercentage <= 0) {
            stopEnhance();
            return;
        }

        if (mLoudnessEnhancer != null) {
            // 将百分比转换为 LoudnessEnhancer 所需的增益值 (mB)
            // 这里的常数 750 来自你的反编译代码，可以根据效果调整
            int gainInMilliBel = (currentGainInPercentage * 3000) / 100;
            try {
                if (!mLoudnessEnhancer.getEnabled()) {
                    mLoudnessEnhancer.setEnabled(true);
                }
                mLoudnessEnhancer.setTargetGain(gainInMilliBel);
                isEnhancerEnabled = true;
                Log.d(TAG, "LoudnessEnhancer set to gain: " + gainInMilliBel + " mB");
            } catch (Exception e) {
                Log.e(TAG, "Failed to set LoudnessEnhancer gain.", e);
                // 发生异常，释放资源
                release();
            }
        } else if (mEqualizer != null) {
            // 使用 Equalizer 来模拟音量提升
            // 这里的 1500 和 750 同样来自你的反编译代码
            int gainLevel = ((currentGainInPercentage * 3000) / 100);

            // 确保增益值在 Equalizer 的有效范围内
            short equalizerLevel = (short) (((gainLevel * mEqualizerMaxLevel) + 750) / 1500);
            if (equalizerLevel > mEqualizerMaxLevel) {
                equalizerLevel = mEqualizerMaxLevel;
            }

            try {
                if (!mEqualizer.getEnabled()) {
                    mEqualizer.setEnabled(true);
                }
                // 将所有频段都设置为这个增益值
                for (short i = 0; i < mEqualizerBands; i++) {
                    mEqualizer.setBandLevel(i, equalizerLevel);
                }
                isEnhancerEnabled = true;
                Log.d(TAG, "Equalizer set to gain: " + equalizerLevel + " mB for all bands.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to set Equalizer gain.", e);
                release();
            }
        } else {
            Log.w(TAG, "No audio effect is available to enhance volume.");
        }
    }

    /**
     * 停止音量增强效果。
     */
    public void stopEnhance() {
        if (mLoudnessEnhancer != null) {
            try {
                mLoudnessEnhancer.setEnabled(false);
                isEnhancerEnabled = false;
                Log.d(TAG, "LoudnessEnhancer stopped.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop LoudnessEnhancer.", e);
            }
        }
        if (mEqualizer != null) {
            try {
                mEqualizer.setEnabled(false);
                isEnhancerEnabled = false;
                Log.d(TAG, "Equalizer stopped.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop Equalizer.", e);
            }
        }
        currentGainInPercentage = 0;
    }

    /**
     * 释放所有资源。
     */
    public void release() {
        if (mLoudnessEnhancer != null) {
            try {
                mLoudnessEnhancer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing LoudnessEnhancer.", e);
            } finally {
                mLoudnessEnhancer = null;
            }
        }
        if (mEqualizer != null) {
            try {
                mEqualizer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing Equalizer.", e);
            } finally {
                mEqualizer = null;
            }
        }
        isEnhancerEnabled = false;
        currentGainInPercentage = 0;
        Log.d(TAG, "All audio effect resources released.");
    }

    /**
     * 检查音量增强效果是否启用。
     * @return 如果启用则返回 true，否则返回 false。
     */
    public boolean isEnhancerEnabled() {
        return isEnhancerEnabled;
    }
}
