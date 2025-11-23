package shellService;


import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import shellService.scrcpy.Workarounds;

public final class Main {

  
   
    private Main() {
        // 不应该初始化Main类
    }
	
    private static void startMouse() throws IOException {
        
        Workarounds.apply();
		new ServiceThread().start();
		Looper.loop(); // interrupted by the Completion implementation
    }

	
	//准备主Looper
    private static void prepareMainLooper() {
        // Like Looper.prepareMainLooper(), but with quitAllowed set to true
        Looper.prepare();
        synchronized (Looper.class) {
            try {
                @SuppressLint("DiscouragedPrivateApi")
					Field field = Looper.class.getDeclaredField("sMainLooper");
                field.setAccessible(true);
                field.set(null, Looper.myLooper());
            } catch (ReflectiveOperationException e) {
				EasyLog.logToFile("■Looper Error：" + e.toString());
                throw new AssertionError(e);
				
            }
        }
    }

	//■■鼠标入口
    public static void main(String[] args){
        try {
            internalMain();
        } catch (Throwable t) {
       		t.printStackTrace();
        }
    }
	
	
	//开始…
    private static void internalMain() throws Exception {
        prepareMainLooper();
        try {
            startMouse();
        } catch (Exception e) {
            e.printStackTrace();
			EasyLog.logToFile("Could not start mouse:" + e.toString());
			EasyLog.logStackTrace(e);
        }
    }
}




