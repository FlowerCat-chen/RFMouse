package com.flowercat.rfmouse.util;

import android.view.KeyEvent;

public class KeyCodeUtil {
    
    /**
	 * 根据传入的 Keycode 返回对应的按键中文名称。
	 * 如果 Keycode 无法识别，返回 "KEYCODE_XXX" 格式的字符串。
	 */
	public static String getKeyNameFromCode(int keyCode) {
		switch (keyCode) {
				// 电话/通用功能键
			case KeyEvent.KEYCODE_CALL:
				return "拨号键";
			case KeyEvent.KEYCODE_ENDCALL:
				return "挂机键";
			case KeyEvent.KEYCODE_HOME:
				return "主页键";
			case KeyEvent.KEYCODE_MENU:
				return "菜单键";
			case KeyEvent.KEYCODE_BACK:
				return "返回键";
			case KeyEvent.KEYCODE_SEARCH:
				return "搜索键";
			case KeyEvent.KEYCODE_CAMERA:
				return "拍照键";
			case KeyEvent.KEYCODE_FOCUS:
				return "拍照对焦键";
			case KeyEvent.KEYCODE_POWER:
				return "电源键";
			case KeyEvent.KEYCODE_NOTIFICATION:
				return "通知键";
			case KeyEvent.KEYCODE_MUTE:
				return "话筒静音键";
			case KeyEvent.KEYCODE_VOLUME_MUTE:
				return "扬声器静音键";
			case KeyEvent.KEYCODE_VOLUME_UP:
				return "音量增加键";
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return "音量减小键";
			case KeyEvent.KEYCODE_APP_SWITCH:
				return "应用切换键";
			case KeyEvent.KEYCODE_SETTINGS:
				return "设置键";
			case KeyEvent.KEYCODE_HEADSETHOOK:
				return "耳机挂断键";

				// 导航/编辑键
			case KeyEvent.KEYCODE_ENTER:
				return "回车键";
			case KeyEvent.KEYCODE_ESCAPE:
				return "ESC键";
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return "导航键-确定";
			case KeyEvent.KEYCODE_DPAD_UP:
				return "导航键-向上";
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return "导航键-向下";
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return "导航键-向左";
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return "导航键-向右";
			case KeyEvent.KEYCODE_MOVE_HOME:
				return "光标移动到开始";
			case KeyEvent.KEYCODE_MOVE_END:
				return "光标移动到末尾";
			case KeyEvent.KEYCODE_PAGE_UP:
				return "向上翻页";
			case KeyEvent.KEYCODE_PAGE_DOWN:
				return "向下翻页";
			case KeyEvent.KEYCODE_DEL:
				return "退格键";
			case KeyEvent.KEYCODE_FORWARD_DEL:
				return "删除键";
			case KeyEvent.KEYCODE_INSERT:
				return "插入键";
			case KeyEvent.KEYCODE_TAB:
				return "Tab键";

				// 数字/字母键
			case KeyEvent.KEYCODE_0:
				return "按键'0'";
			case KeyEvent.KEYCODE_1:
				return "按键'1'";
			case KeyEvent.KEYCODE_2:
				return "按键'2'";
			case KeyEvent.KEYCODE_3:
				return "按键'3'";
			case KeyEvent.KEYCODE_4:
				return "按键'4'";
			case KeyEvent.KEYCODE_5:
				return "按键'5'";
			case KeyEvent.KEYCODE_6:
				return "按键'6'";
			case KeyEvent.KEYCODE_7:
				return "按键'7'";
			case KeyEvent.KEYCODE_8:
				return "按键'8'";
			case KeyEvent.KEYCODE_9:
				return "按键'9'";
			case KeyEvent.KEYCODE_A:
				return "按键'A'";
			case KeyEvent.KEYCODE_B:
				return "按键'B'";
			case KeyEvent.KEYCODE_C:
				return "按键'C'";
			case KeyEvent.KEYCODE_D:
				return "按键'D'";
			case KeyEvent.KEYCODE_E:
				return "按键'E'";
			case KeyEvent.KEYCODE_F:
				return "按键'F'";
			case KeyEvent.KEYCODE_G:
				return "按键'G'";
			case KeyEvent.KEYCODE_H:
				return "按键'H'";
			case KeyEvent.KEYCODE_I:
				return "按键'I'";
			case KeyEvent.KEYCODE_J:
				return "按键'J'";
			case KeyEvent.KEYCODE_K:
				return "按键'K'";
			case KeyEvent.KEYCODE_L:
				return "按键'L'";
			case KeyEvent.KEYCODE_M:
				return "按键'M'";
			case KeyEvent.KEYCODE_N:
				return "按键'N'";
			case KeyEvent.KEYCODE_O:
				return "按键'O'";
			case KeyEvent.KEYCODE_P:
				return "按键'P'";
			case KeyEvent.KEYCODE_Q:
				return "按键'Q'";
			case KeyEvent.KEYCODE_R:
				return "按键'R'";
			case KeyEvent.KEYCODE_S:
				return "按键'S'";
			case KeyEvent.KEYCODE_T:
				return "按键'T'";
			case KeyEvent.KEYCODE_U:
				return "按键'U'";
			case KeyEvent.KEYCODE_V:
				return "按键'V'";
			case KeyEvent.KEYCODE_W:
				return "按键'W'";
			case KeyEvent.KEYCODE_X:
				return "按键'X'";
			case KeyEvent.KEYCODE_Y:
				return "按键'Y'";
			case KeyEvent.KEYCODE_Z:
				return "按键'Z'";

				// 符号键
			case KeyEvent.KEYCODE_PLUS:
				return "按键'+'";
			case KeyEvent.KEYCODE_MINUS:
				return "按键'-'";
			case KeyEvent.KEYCODE_STAR:
				return "按键'*'";
			case KeyEvent.KEYCODE_SLASH:
				return "按键'/'";
			case KeyEvent.KEYCODE_EQUALS:
				return "按键'='";
			case KeyEvent.KEYCODE_AT:
				return "按键'@'";
			case KeyEvent.KEYCODE_POUND:
				return "按键'#'";
			case KeyEvent.KEYCODE_APOSTROPHE:
				return "按键'' (单引号)";
			case KeyEvent.KEYCODE_BACKSLASH:
				return "按键'\\' (反斜杠)";
			case KeyEvent.KEYCODE_COMMA:
				return "按键','";
			case KeyEvent.KEYCODE_PERIOD:
				return "按键'.'";
			case KeyEvent.KEYCODE_LEFT_BRACKET:
				return "按键'['";
			case KeyEvent.KEYCODE_RIGHT_BRACKET:
				return "按键']'";
			case KeyEvent.KEYCODE_SEMICOLON:
				return "按键';'";
			case KeyEvent.KEYCODE_GRAVE:
				return "按键'`'";
			case KeyEvent.KEYCODE_SPACE:
				return "空格键";

				// 修饰键
			case KeyEvent.KEYCODE_ALT_LEFT:
				return "左Alt键";
			case KeyEvent.KEYCODE_ALT_RIGHT:
				return "右Alt键";
			case KeyEvent.KEYCODE_CTRL_LEFT:
				return "左Control键";
			case KeyEvent.KEYCODE_CTRL_RIGHT:
				return "右Control键";
			case KeyEvent.KEYCODE_SHIFT_LEFT:
				return "左Shift键";
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				return "右Shift键";

				// 多媒体键
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				return "多媒体键-播放";
			case KeyEvent.KEYCODE_MEDIA_STOP:
				return "多媒体键-停止";
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				return "多媒体键-暂停";
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				return "多媒体键-播放/暂停";
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				return "多媒体键-快进";
			case KeyEvent.KEYCODE_MEDIA_REWIND:
				return "多媒体键-快退";
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				return "多媒体键-下一首";
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				return "多媒体键-上一首";
			case KeyEvent.KEYCODE_MEDIA_CLOSE:
				return "多媒体键-关闭";
			case KeyEvent.KEYCODE_MEDIA_EJECT:
				return "多媒体键-弹出";
			case KeyEvent.KEYCODE_MEDIA_RECORD:
				return "多媒体键-录音";

				// 游戏手柄键
			case KeyEvent.KEYCODE_BUTTON_A:
				return "游戏手柄按钮-A";
			case KeyEvent.KEYCODE_BUTTON_B:
				return "游戏手柄按钮-B";
			case KeyEvent.KEYCODE_BUTTON_X:
				return "游戏手柄按钮-X";
			case KeyEvent.KEYCODE_BUTTON_Y:
				return "游戏手柄按钮-Y";
				// ... (根据需要添加更多游戏手柄键)
			default:
				return String.valueOf(keyCode);
		}
	}



	/**
	 * 根据传入的按键中文名称返回对应的 Keycode。
	 * 如果名称无法识别，返回 -1。
	 * @param keyName 要查询的按键中文名称
	 * @return 对应的 Keycode 或 -1
	 */
	public static int getKeyCodeFromName(String keyName) {
		if (keyName == null || keyName.isEmpty()) {
			return -1; // 输入为空，返回无效键值
		}

		// 检查是否是用户添加的键值
		if (keyName.startsWith("▶")) {
			keyName = keyName.replace("▶", "");
		}

		switch (keyName) {
				// 电话/通用功能键
			case "拨号键":
				return KeyEvent.KEYCODE_CALL;
			case "挂机键":
				return KeyEvent.KEYCODE_ENDCALL;
			case "主页键":
				return KeyEvent.KEYCODE_HOME;
			case "菜单键":
				return KeyEvent.KEYCODE_MENU;
			case "返回键":
				return KeyEvent.KEYCODE_BACK;
			case "搜索键":
				return KeyEvent.KEYCODE_SEARCH;
			case "拍照键":
				return KeyEvent.KEYCODE_CAMERA;
			case "拍照对焦键":
				return KeyEvent.KEYCODE_FOCUS;
			case "电源键":
				return KeyEvent.KEYCODE_POWER;
			case "通知键":
				return KeyEvent.KEYCODE_NOTIFICATION;
			case "话筒静音键":
				return KeyEvent.KEYCODE_MUTE;
			case "扬声器静音键":
				return KeyEvent.KEYCODE_VOLUME_MUTE;
			case "音量增加键":
				return KeyEvent.KEYCODE_VOLUME_UP;
			case "音量减小键":
				return KeyEvent.KEYCODE_VOLUME_DOWN;
			case "应用切换键":
				return KeyEvent.KEYCODE_APP_SWITCH;
			case "设置键":
				return KeyEvent.KEYCODE_SETTINGS;
			case "耳机挂断键":
				return KeyEvent.KEYCODE_HEADSETHOOK;

				// 导航/编辑键
			case "回车键":
				return KeyEvent.KEYCODE_ENTER;
			case "ESC键":
				return KeyEvent.KEYCODE_ESCAPE;
			case "导航键-确定":
				return KeyEvent.KEYCODE_DPAD_CENTER;
			case "导航键-向上":
				return KeyEvent.KEYCODE_DPAD_UP;
			case "导航键-向下":
				return KeyEvent.KEYCODE_DPAD_DOWN;
			case "导航键-向左":
				return KeyEvent.KEYCODE_DPAD_LEFT;
			case "导航键-向右":
				return KeyEvent.KEYCODE_DPAD_RIGHT;
			case "光标移动到开始":
				return KeyEvent.KEYCODE_MOVE_HOME;
			case "光标移动到末尾":
				return KeyEvent.KEYCODE_MOVE_END;
			case "向上翻页":
				return KeyEvent.KEYCODE_PAGE_UP;
			case "向下翻页":
				return KeyEvent.KEYCODE_PAGE_DOWN;
			case "退格键":
				return KeyEvent.KEYCODE_DEL;
			case "删除键":
				return KeyEvent.KEYCODE_FORWARD_DEL;
			case "插入键":
				return KeyEvent.KEYCODE_INSERT;
			case "Tab键":
				return KeyEvent.KEYCODE_TAB;

				// 数字/字母键
			case "按键'0'":
				return KeyEvent.KEYCODE_0;
			case "按键'1'":
				return KeyEvent.KEYCODE_1;
			case "按键'2'":
				return KeyEvent.KEYCODE_2;
			case "按键'3'":
				return KeyEvent.KEYCODE_3;
			case "按键'4'":
				return KeyEvent.KEYCODE_4;
			case "按键'5'":
				return KeyEvent.KEYCODE_5;
			case "按键'6'":
				return KeyEvent.KEYCODE_6;
			case "按键'7'":
				return KeyEvent.KEYCODE_7;
			case "按键'8'":
				return KeyEvent.KEYCODE_8;
			case "按键'9'":
				return KeyEvent.KEYCODE_9;
			case "按键'A'":
				return KeyEvent.KEYCODE_A;
			case "按键'B'":
				return KeyEvent.KEYCODE_B;
			case "按键'C'":
				return KeyEvent.KEYCODE_C;
			case "按键'D'":
				return KeyEvent.KEYCODE_D;
			case "按键'E'":
				return KeyEvent.KEYCODE_E;
			case "按键'F'":
				return KeyEvent.KEYCODE_F;
			case "按键'G'":
				return KeyEvent.KEYCODE_G;
			case "按键'H'":
				return KeyEvent.KEYCODE_H;
			case "按键'I'":
				return KeyEvent.KEYCODE_I;
			case "按键'J'":
				return KeyEvent.KEYCODE_J;
			case "按键'K'":
				return KeyEvent.KEYCODE_K;
			case "按键'L'":
				return KeyEvent.KEYCODE_L;
			case "按键'M'":
				return KeyEvent.KEYCODE_M;
			case "按键'N'":
				return KeyEvent.KEYCODE_N;
			case "按键'O'":
				return KeyEvent.KEYCODE_O;
			case "按键'P'":
				return KeyEvent.KEYCODE_P;
			case "按键'Q'":
				return KeyEvent.KEYCODE_Q;
			case "按键'R'":
				return KeyEvent.KEYCODE_R;
			case "按键'S'":
				return KeyEvent.KEYCODE_S;
			case "按键'T'":
				return KeyEvent.KEYCODE_T;
			case "按键'U'":
				return KeyEvent.KEYCODE_U;
			case "按键'V'":
				return KeyEvent.KEYCODE_V;
			case "按键'W'":
				return KeyEvent.KEYCODE_W;
			case "按键'X'":
				return KeyEvent.KEYCODE_X;
			case "按键'Y'":
				return KeyEvent.KEYCODE_Y;
			case "按键'Z'":
				return KeyEvent.KEYCODE_Z;

				// 符号键
			case "按键'+'":
				return KeyEvent.KEYCODE_PLUS;
			case "按键'-'":
				return KeyEvent.KEYCODE_MINUS;
			case "按键'*'":
				return KeyEvent.KEYCODE_STAR;
			case "按键'/'":
				return KeyEvent.KEYCODE_SLASH;
			case "按键'='":
				return KeyEvent.KEYCODE_EQUALS;
			case "按键'@'":
				return KeyEvent.KEYCODE_AT;
			case "按键'#'":
				return KeyEvent.KEYCODE_POUND;
			case "按键'' (单引号)":
				return KeyEvent.KEYCODE_APOSTROPHE;
			case "按键'\\' (反斜杠)":
				return KeyEvent.KEYCODE_BACKSLASH;
			case "按键','":
				return KeyEvent.KEYCODE_COMMA;
			case "按键'.'":
				return KeyEvent.KEYCODE_PERIOD;
			case "按键'['":
				return KeyEvent.KEYCODE_LEFT_BRACKET;
			case "按键']'":
				return KeyEvent.KEYCODE_RIGHT_BRACKET;
			case "按键';'":
				return KeyEvent.KEYCODE_SEMICOLON;
			case "按键'`'":
				return KeyEvent.KEYCODE_GRAVE;
			case "空格键":
				return KeyEvent.KEYCODE_SPACE;

				// 修饰键
			case "左Alt键":
				return KeyEvent.KEYCODE_ALT_LEFT;
			case "右Alt键":
				return KeyEvent.KEYCODE_ALT_RIGHT;
			case "左Control键":
				return KeyEvent.KEYCODE_CTRL_LEFT;
			case "右Control键":
				return KeyEvent.KEYCODE_CTRL_RIGHT;
			case "左Shift键":
				return KeyEvent.KEYCODE_SHIFT_LEFT;
			case "右Shift键":
				return KeyEvent.KEYCODE_SHIFT_RIGHT;

				// 多媒体键
			case "多媒体键-播放":
				return KeyEvent.KEYCODE_MEDIA_PLAY;
			case "多媒体键-停止":
				return KeyEvent.KEYCODE_MEDIA_STOP;
			case "多媒体键-暂停":
				return KeyEvent.KEYCODE_MEDIA_PAUSE;
			case "多媒体键-播放/暂停":
				return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
			case "多媒体键-快进":
				return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
			case "多媒体键-快退":
				return KeyEvent.KEYCODE_MEDIA_REWIND;
			case "多媒体键-下一首":
				return KeyEvent.KEYCODE_MEDIA_NEXT;
			case "多媒体键-上一首":
				return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
			case "多媒体键-关闭":
				return KeyEvent.KEYCODE_MEDIA_CLOSE;
			case "多媒体键-弹出":
				return KeyEvent.KEYCODE_MEDIA_EJECT;
			case "多媒体键-录音":
				return KeyEvent.KEYCODE_MEDIA_RECORD;

				// 游戏手柄键
			case "游戏手柄按钮-A":
				return KeyEvent.KEYCODE_BUTTON_A;
			case "游戏手柄按钮-B":
				return KeyEvent.KEYCODE_BUTTON_B;
			case "游戏手柄按钮-X":
				return KeyEvent.KEYCODE_BUTTON_X;
			case "游戏手柄按钮-Y":
				return KeyEvent.KEYCODE_BUTTON_Y;
				// ... (根据需要添加更多游戏手柄键)
			default:
				try {
					return Integer.parseInt(keyName);
				} catch (NumberFormatException e) {
					return -1; // 格式不正确，返回无效键值
				}
		}
	}
	
    
}
