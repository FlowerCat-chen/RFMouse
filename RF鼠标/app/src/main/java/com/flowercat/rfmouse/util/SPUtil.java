package com.flowercat.rfmouse.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.flowercat.rfmouse.key.KeyAction;

public class SPUtil {

    private static final String FIRST_RUN_KEY = "is_first_run_rf";
    private static SharedPreferences sharedPreferences;

	private static String KEY_ACTIONS_DATA = "keys_added";
	
	// 定义SharedPreferences的键名
    public static final String KEY_KEYBOARD_LIGHT = "keyboard_light";
    public static final String KEY_WIFI_ON_BOOT = "wifi_on_boot";
	public static final String KEY_MOUSE_ON_BOOT = "mouse_on_boot";
    public static final String KEY_KEY_MAPPING_HINT = "key_mapping_hint";
    public static final String KEY_HOME_KEY_LOCK = "home_key_lock";
	public static final String KEY_SKIP_LOCK_SCREEN = "skip_lock_screen";
	//音量提高
	public static final String KEY_UP_PERCENT = "up_percent";
	
	//预设鼠标大小
	public static final String KEY_PRESET_MOUSE_SIZE = "mouse_pre_set_size";
	//用户设置大小
	public static final String KEY_USER_MOUSE_SIZE = "mouse_user_set_size";
	//鼠标移动速率
	public static final String KEY_MOUSE_SPEED = "mouse_speed";
	//鼠标单次移动距离
	public static final String KEY_MOUSE_STEP = "mouse_step";
	//鼠标更新间隔
	public static final String KEY_MOUSE_TIME = "mouse_time";
	//鼠标自动隐藏时间
	public static final String KEY_MOUSE_HIDE_TIME = "mouse_hide_time";
	//是否自动隐藏鼠标？
	public static final String KEY_MOUSE_HIDE_ENABLE= "mouse_hide_enable";
	//当前点击模式
	public static final String KEY_CLICK_MODE= "mouse_click_mode";
	
	//锁屏时自动回正?
	public static final String KEY_LOCK_BACK = "mouse_lock_back";
	public static final String KEY_KEEP_ON = "mouse_screen_on";
	public static final String KEY_FIX_X = "mouse_fix_x";
	public static final String KEY_FIX_Y = "mouse_fix_y";
	public static final String KEY_LAYON_SETTING = "mouse_lay_on_setting";
	public static final String KEY_CLICK_DEBUG = "mouse_click_debug";
	public static final String KEY_SPACE_INPUT = "mouse_space_input";
	public static final String KEY_DISABLE_INPUT= "mouse_disable_input";
	
	public static final String KEY_GESTURE_SHORT= "gesture_short";
	public static final String KEY_GESTURE_LONG= "gesture_long";
	public static final String KEY_GESTURE_SCROLL= "gesture_scroll_time";
	public static final String KEY_GESTURE_UD_DIS= "gesture_up_down_dis";
	public static final String KEY_GESTURE_LR_DIS= "gesture_left_right_dis";
	
	public static final String KEY_LONG_CLICK_JUDGE = "long_click_gudge";
	
    private SPUtil() {
        // 私有构造方法
    }

    public static void init(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getApplicationContext()
                .getSharedPreferences("mousever_rfmouse", Context.MODE_MULTI_PROCESS);
        }
    }

    // String 类型存取
    public static void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // boolean 类型存取
    public static void putBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // int 类型存取
    public static void putInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public static int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    // 判断是否初次运行
    public static boolean isFirstRun() {
        boolean isFirst = sharedPreferences.getBoolean(FIRST_RUN_KEY, true);
        if (isFirst) {
            sharedPreferences.edit().putBoolean(FIRST_RUN_KEY, false).apply();
        }
        return isFirst;
    }

    // 清除所有数据
    public static void clear() {
        sharedPreferences.edit().clear().apply();
    }

    // 移除指定key
    public static void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }
	
	
	public static List<KeyAction> loadData() {
        String serializedData = getString(KEY_ACTIONS_DATA, null);
		
		List<KeyAction> keyActionList = null;
		
        if (serializedData != null) {
            try {
                keyActionList = (List<KeyAction>) fromString(serializedData);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                keyActionList = new ArrayList<>();
            }
        } else {
            keyActionList = new ArrayList<>();
			
            // 初始数据
			KeyAction menu = new KeyAction("菜单键");
			menu.setLongPressAction("模式切换");
			keyActionList.add(menu);

			KeyAction back = new KeyAction("返回键");
			back.setLongPressAction("最近任务");
			keyActionList.add(back);
			
			KeyAction ok = new KeyAction("回车键");
			ok.setShortPressAction("鼠标短按");
			ok.setLongPressAction("点击菜单");
			keyActionList.add(ok);

			KeyAction up = new KeyAction("导航键-向上");
			up.setShortPressAction("鼠标上移/上滑");
			up.setLongPressAction("鼠标加速上移");
			keyActionList.add(up);

			KeyAction down = new KeyAction("导航键-向下");
			down.setShortPressAction("鼠标下移/下滑");
			down.setLongPressAction("鼠标加速下移");
			keyActionList.add(down);

			KeyAction left = new KeyAction("导航键-向左");
			left.setShortPressAction("鼠标左移/左滑(*)");
			left.setLongPressAction("鼠标加速左移");
			keyActionList.add(left);

			KeyAction right = new KeyAction("导航键-向右");
			right.setShortPressAction("鼠标右移/右滑(*)");
			right.setLongPressAction("鼠标加速右移");
			keyActionList.add(right);

			KeyAction call = new KeyAction("拨号键");
			call.setLongPressAction("紧急禁用鼠标");
			keyActionList.add(call);

			keyActionList.add(new KeyAction("音量增加键"));
			keyActionList.add(new KeyAction("音量减小键"));
			keyActionList.add(new KeyAction("按键'0'"));
			keyActionList.add(new KeyAction("按键'1'"));
			keyActionList.add(new KeyAction("按键'2'"));
			keyActionList.add(new KeyAction("按键'3'"));
			keyActionList.add(new KeyAction("按键'4'"));
			keyActionList.add(new KeyAction("按键'5'"));
			keyActionList.add(new KeyAction("按键'6'"));
			keyActionList.add(new KeyAction("按键'7'"));
			keyActionList.add(new KeyAction("按键'8'"));
			keyActionList.add(new KeyAction("按键'9'"));
			keyActionList.add(new KeyAction("按键'#'"));
			keyActionList.add(new KeyAction("按键'*'"));
        }
		return keyActionList;
    }

	//保存所有用户设置
    public static void saveData(List<KeyAction> keyActionList) {
		try {
			String serializedData = mtoString((Serializable) keyActionList);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(KEY_ACTIONS_DATA, serializedData);
			editor.apply(); // 使用 apply() 来异步保存，避免阻塞主线程
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	// 将对象序列化为字符串
    private static String mtoString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
    }

    // 将字符串反序列化为对象
    private static Object fromString(String s) throws IOException, ClassNotFoundException {
        byte[] data = android.util.Base64.decode(s, android.util.Base64.DEFAULT);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }
	
	
}
