package com.flowercat.rfmouse.util;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class TtsManager {

    private static final String TAG = "TtsManager";
    private static TtsManager sInstance;

    private Context mContext;
    private TextToSpeech mTextToSpeech;
    private boolean mIsInitialized = false;

    // 单例模式，使用双重检查锁定
    public static TtsManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (TtsManager.class) {
                if (sInstance == null) {
                    sInstance = new TtsManager(context);
                }
            }
        }
        return sInstance;
    }

    private TtsManager(Context context) {
        // 使用 ApplicationContext 防止内存泄漏
        mContext = context;
        initializeTts();
    }

    // 初始化 TextToSpeech 引擎
    private void initializeTts() {
        mTextToSpeech = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
				@Override
				public void onInit(int status) {
					if (status == TextToSpeech.SUCCESS) {
						// 设置默认语言为中文，可以根据需要更改
						int result = mTextToSpeech.setLanguage(Locale.CHINA);

						if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
							Log.e(TAG, "Language not supported or data missing.");
						} else {
							mIsInitialized = true;
							Log.i(TAG, "TTS initialized successfully.");
							// 设置朗读监听器，用于 Android 4.4 (KitKat) 或更高版本
							setTtsListener();
						}
					} else {
						Log.e(TAG, "TTS initialization failed.");
					}
				}
			});
    }

    // 设置朗读监听器
    private void setTtsListener() {
        if (mTextToSpeech != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            mTextToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
					@Override
					public void onStart(String utteranceId) {
						Log.d(TAG, "TTS onStart: " + utteranceId);
						// 朗读开始，可以添加你的逻辑，如更新 UI
					}

					@Override
					public void onDone(String utteranceId) {
						Log.d(TAG, "TTS onDone: " + utteranceId);
						// 朗读完成，可以添加你的逻辑
					}

					@Override
					public void onError(String utteranceId) {
						Log.e(TAG, "TTS onError: " + utteranceId);
						// 朗读出错
					}
				});
        }
    }

    /**
     * 朗读文本
     * @param text 要朗读的文本
     * @param pitch 语调，范围 0.5 到 2.0，默认为 1.0
     * @param rate 语速，范围 0.5 到 2.0，默认为 1.0
     * @return true 如果朗读成功，否则返回 false
     */
    public boolean speak(String text, float pitch, float rate) {
        if (mTextToSpeech == null || !mIsInitialized) {
            Log.e(TAG, "TTS engine not initialized.");
            return false;
        }

        // 检查系统是否支持 TTS
        if (mTextToSpeech.isLanguageAvailable(mTextToSpeech.getLanguage()) != TextToSpeech.LANG_AVAILABLE) {
            Log.e(TAG, "Language is not available for TTS.");
            return false;
        }

        // 调整语调和语速
        mTextToSpeech.setPitch(pitch);
        mTextToSpeech.setSpeechRate(rate);

        int result;
        String utteranceId = "utterance_" + System.currentTimeMillis();

        // 适配不同的 Android 版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0 (Lollipop) 或更高版本
            result = mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        } else {
            // Android 4.4 (KitKat) 或更低版本
            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            result = mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }

        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Failed to speak text: " + text);
            return false;
        }

        Log.d(TAG, "Successfully started speaking.");
        return true;
    }

    /**
     * 停止当前朗读
     */
    public void stop() {
        if (mTextToSpeech != null && mTextToSpeech.isSpeaking()) {
            mTextToSpeech.stop();
            Log.d(TAG, "TTS speaking stopped.");
        }
    }

    /**
     * 检查 TTS 是否正在朗读
     * @return true 如果正在朗读，否则返回 false
     */
    public boolean isSpeaking() {
        return mTextToSpeech != null && mTextToSpeech.isSpeaking();
    }

    /**
     * 释放 TTS 资源
     * 在 Activity 或 Fragment 的 onStop/onDestroy 中调用
     */
    public void shutdown() {
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
            mTextToSpeech = null;
            mIsInitialized = false;
            sInstance = null; // 清除单例实例
            Log.d(TAG, "TTS resources released.");
        }
    }
}
