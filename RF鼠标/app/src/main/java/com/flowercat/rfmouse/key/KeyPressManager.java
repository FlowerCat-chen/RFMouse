package com.flowercat.rfmouse.key;

import java.util.Map;
import java.util.HashMap;
import android.os.Handler;
import android.content.Context;
import android.util.Log;
import android.os.Looper;
import android.view.KeyEvent;

public class KeyPressManager {
	
	private static final String TAG = "KeyPressAction";
    
	// 静态成员，用于外部设置回调和监听的按键
    // 使用 Map 来存储按键码和对应的监听器，支持为不同按键设置不同行为
    private static final Map<Integer, KeyEventConfig> sKeyConfigs = new HashMap<>();

    // 按键事件的阈值，可以根据需要调整
    public static int LONG_CLICK_TIME_DELTA = 300;   // 长按时间，单位毫秒

	
	// 新增一个内部类，用于封装回调监听器和对应的事件处理策略
    public static class KeyEventConfig {
        final OnKeyEventListener listener;
        final KeyEventPolicy singleClickPolicy;
        final KeyEventPolicy longClickPolicy;

        KeyEventConfig(OnKeyEventListener listener, KeyEventPolicy singleClickPolicy, KeyEventPolicy longClickPolicy) {
            this.listener = listener;
            this.singleClickPolicy = singleClickPolicy;
            this.longClickPolicy = longClickPolicy;
        }
    }


    private long mLastPressTime = 0;
    private int mPendingKeyCode = 0;

    private Handler mHandler;

	private KeyEventConfig lastConfig;//按键按下获取到的keyConfig

	private boolean isLongClickUp = false;//在按键弹起时判断是否长按
	
	private Context mCtx;//用来显示弹窗的上下文，暂时用不到。
	
	
	// 处理长按和短按的Runnable
    private final Runnable mLongClickRunnable = new Runnable() {
        @Override
        public void run() {
                // 长按事件发生，触发回调
                Log.d(TAG, "onLongClick: " + mPendingKeyCode);
                KeyEventConfig config = sKeyConfigs.get(mPendingKeyCode);
                if (config != null && config.listener != null) {
					isLongClickUp = true;
                    config.listener.onLongClick(mPendingKeyCode);
                }
            // 无论是否触发长按，都重置状态
            resetState();
        }
    };

    private final Runnable mSingleClickRunnable = new Runnable() {
        @Override
        public void run() {
                // 短按事件发生，触发回调
                Log.d(TAG, "onSingleClick: " + mPendingKeyCode);
				KeyEventConfig config = sKeyConfigs.get(mPendingKeyCode);
                if (config != null && config.listener != null) {
                    config.listener.onSingleClick(mPendingKeyCode);
                }
            // 无论是否触发短按，都重置状态
            resetState();
        }
    };
	
	/**
     * 外部调用此方法为**单个按键**设置回调监听器和事件策略。
     * @param keyCode           需要监听的按键码
     * @param singleClickPolicy 短按事件的处理策略
     * @param doubleClickPolicy 双击事件的处理策略
     * @param longClickPolicy   长按事件的处理策略
     * @param listener          按键事件回调接口，如果为null则移除该按键的监听器
     */
    public static void setOnKeyEventListener(int keyCode, KeyEventPolicy singleClickPolicy,
                                             KeyEventPolicy longClickPolicy,
                                             OnKeyEventListener listener) {
        if (listener != null) {
            sKeyConfigs.put(keyCode, new KeyEventConfig(listener, singleClickPolicy, longClickPolicy));
            Log.d(TAG, "Added listener for key: " + keyCode + " with policies.");
        } else {
            sKeyConfigs.remove(keyCode);
            Log.d(TAG, "Removed listener for key: " + keyCode);
        }
    }

    /**
     * 外部调用此方法为**多个按键**设置同一个回调监听器和事件策略。
     * @param keyCodes          需要监听的按键码数组
     * @param singleClickPolicy 短按事件的处理策略
     * @param doubleClickPolicy 双击事件的处理策略
     * @param longClickPolicy   长按事件的处理策略
     * @param listener          按键事件回调接口
     */
    public static void setOnKeyEventListener(int[] keyCodes, KeyEventPolicy singleClickPolicy,
                                             KeyEventPolicy longClickPolicy,
                                             OnKeyEventListener listener) {
        if (keyCodes != null && listener != null) {
            KeyEventConfig config = new KeyEventConfig(listener, singleClickPolicy, longClickPolicy);
            for (int keyCode : keyCodes) {
                sKeyConfigs.put(keyCode, config);
            }
            Log.d(TAG, "Added a single listener for multiple keys: " + keyCodes.length);
        } else {
            // 如果监听器为null或按键码数组为null，则清空所有监听器
            sKeyConfigs.clear();
            Log.d(TAG, "Cleared all key listeners.");
        }
    }
	
	
	//初始化
	public KeyPressManager(Context ctx){
		mCtx = ctx;
		mHandler = new Handler(Looper.getMainLooper());
		resetState();//将所有的状态归零。
	}
	
	//按键长按判定时间
	public void setLongClickJudge(int time){
		if(time <= 0){
			time = 1;
		}
		this.LONG_CLICK_TIME_DELTA = time;
	}
	
	//处理传过来的按键事件
    public boolean handleKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

		if (!sKeyConfigs.containsKey(keyCode)) {
            return false; // 不在监听列表中的按键直接放行
        }

		
		lastConfig = sKeyConfigs.get(keyCode);

        try {
            // 只处理按下事件
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                // 如果当前按键是重复事件（长按触发的重复按下），直接忽略
                if (event.getRepeatCount() > 0) {
                    return false;
                }
				
				//获取当前系统时间
                long currentTime = System.currentTimeMillis();

				//弹起事件消耗标志
				isLongClickUp = false;

                mLastPressTime = currentTime;
                mPendingKeyCode = keyCode;

				if (mHandler != null) {
                    mHandler.removeCallbacksAndMessages(null);
                }
                // 启动长按检测
                mHandler.postDelayed(mLongClickRunnable, LONG_CLICK_TIME_DELTA);
                       

				lastConfig.listener.onKeyDown(keyCode);
				
				/***
				 短按优先级最高。
				 在这里判断是否放行。如果按键的短按事件被设置为DEFAULT，
				 那么无论他的长按或双击事件设置为什么，都放行按键按下事件。
				 如果为CONSUME，则相反，都消耗按键按下事件
				 ***/
				if (lastConfig.singleClickPolicy == KeyEventPolicy.DEFAULT) {
					return false;
				} else {
					return true;
				}

            } 
			
			if (event.getAction() == KeyEvent.ACTION_UP) {
				
				lastConfig.listener.onKeyUp(keyCode);//触发按键弹起
				
					if(isLongClickUp){
						if (lastConfig.longClickPolicy == KeyEventPolicy.DEFAULT) {
							return false;
						} else {
							return true;
						}
					}
				
                // 松开按键时
                if (mPendingKeyCode == keyCode) {
					
					
					
                    long timeDelta = System.currentTimeMillis() - mLastPressTime;
								
                    // 如果在长按时间阈值内松开，则为短按
                    if (timeDelta < LONG_CLICK_TIME_DELTA) {
						
						//取消长按事件并触发短按
						if (mHandler != null) {
                        	mHandler.removeCallbacks(mLongClickRunnable);
                        	// 启动短按的最后判断，以留出双击的时间
                        	mHandler.postDelayed(mSingleClickRunnable, 1);
						}
						
						//短按不管双击。根据短按设置的来…
						if (lastConfig.singleClickPolicy == KeyEventPolicy.DEFAULT) {
							return false;
						} else {
							return true;
						}
						
                    } else {
						//长按
						if (lastConfig.longClickPolicy == KeyEventPolicy.DEFAULT) {
							return false;
						} else {
							return true;
						}
					}				
				}
				
				return false;
			}
		
        } catch (Exception e) {
            Log.e(TAG, "处理按键事件时发生错误: " + e.getMessage(), e);
            // 发生异常时，尝试触发未知事件回调（如果存在）
            OnKeyEventListener listener = sKeyConfigs.get(keyCode).listener;
            if (listener != null) {
                listener.onUnrecognizedEvent(event);
            }
            // 无论如何，重置状态以防止状态机错误
            resetState();
			return false;
        }

        // 返回false表示我们不消耗这个事件，让系统继续处理
        return false;
    }

	
	/**
     * 清理资源，移除所有回调和监听器。
     */
    public void cleanUp() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mLongClickRunnable);
            mHandler.removeCallbacks(mSingleClickRunnable);
        }
        resetState();
        sKeyConfigs.clear();
    }
	
	
	
    /**
     * 重置状态机到初始状态
     */
    public void resetState() {
        mLastPressTime = 0;
        mPendingKeyCode = 0;
        // 移除所有待处理的回调，避免内存泄漏和逻辑错误
        if (mHandler != null) {
            mHandler.removeCallbacks(mLongClickRunnable);
            mHandler.removeCallbacks(mSingleClickRunnable);
        }
        Log.d(TAG, "状态已重置");
    }
	
	
	

    
}
