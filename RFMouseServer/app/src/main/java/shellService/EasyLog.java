package shellService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class EasyLog {

	private static final String LOG_FILE = "/sdcard/shell_service.log";
    private static boolean enableLog = true;
	
	
	
    public static void logToFile(String message) {
		
		if(!enableLog){
			return;
		}
		
		try {
            writeToLog(message);
        } catch (Exception e) {
            // 如果文件日志失败，尝试输出到系统日志
            System.err.println("Log failed: " + e.toString());
        }
    }
	

	//跟踪信息
    public static void logStackTrace(Throwable e) {
		
		if(!enableLog){
			return;
		}
		
        try {
            // 记录异常类名和消息
            logToFile("Stack Trace for: " + e.toString()); 

            // 获取堆栈元素数组
            StackTraceElement[] stackTrace = e.getStackTrace();

            // 遍历并记录每个堆栈元素
            for (StackTraceElement element : stackTrace) {
                // 格式通常是 "at package.Class.method(File:Line)"
                logToFile("\tat " + element.toString()); 
            }

            // 检查是否有“原因”异常（Cause），如果有则递归记录
            Throwable cause = e.getCause();
            if (cause != null) {
                logToFile("Caused by:");
                logStackTrace(cause); // 递归调用记录原因
            }

        } catch (Exception ex) {
            System.err.println("StackTrace log failed: " + ex.toString());
        }
    }

	
    //写入日志文件，默认UTF-8
	public static void writeToLog(String content) {

		if(!enableLog){
			return;
		}
		
        // 使用FileOutputStream以追加模式写入文件
        try {
			FileOutputStream fileOutputStream = new FileOutputStream(new File(LOG_FILE),true); 
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

            // 写入内容并换行
            writer.write(content);
            writer.newLine();
			writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	//设置是否开启日志
	public static void setLogEnabled(boolean en){
		enableLog = en;
	}
    
    
}
