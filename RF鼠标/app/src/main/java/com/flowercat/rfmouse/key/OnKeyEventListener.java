package com.flowercat.rfmouse.key;

import android.view.KeyEvent;

/**
 * 按键事件回调接口，用于监听物理按键的单次点击、双击、长按等手势。
 * 使用默认方法，使得实现者可以只重写感兴趣的事件。
 */
public interface OnKeyEventListener {

    /**
     * 当按键被短按（单击）时触发。
     * @param keyCode 被按下的按键码
     */
     void onSingleClick(int keyCode)

    /**
     * 当按键被长按时触发。
     * @param keyCode 被按下的按键码
     */
     void onLongClick(int keyCode)

    /**
     * 当按键事件无法被识别或处理时触发，可用于错误处理或日志记录。
     * @param event 无法识别的按键事件
     */
    void onUnrecognizedEvent(KeyEvent event)
	
	/**
     * 当键被弹起时，用于取消长按时鼠标的移动。。
     * @param event 无法识别的按键事件
     */
	void onKeyUp(int keyCode)
	
	/**按键被按下**/
	void onKeyDown(int keyCode)
}
