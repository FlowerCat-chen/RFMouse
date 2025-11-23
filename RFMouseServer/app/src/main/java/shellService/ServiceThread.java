package shellService;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import shellService.command.CommandExecutor;
import shellService.scrcpy.FakeContext;

public class ServiceThread extends Thread {
    private static int ShellPORT = 4521;
	// 获取执行器单例实例
	CommandExecutor executor = CommandExecutor.getInstance();
	
    @Override
    public void run() {
		
        // 设置全局异常处理器
        setupGlobalExceptionHandler();

        System.out.println(">>>>>>■Shell服务端程序被调用<<<<<<");
		
		//广播服务端开启
		sendServerStartedBroadcast(FakeContext.get().getApplicationContext());
		
        // 记录启动日志
        //EasyLog.logToFile("Service started at: " + System.currentTimeMillis());

        Service service = new Service(new Service.ServiceGetText() {
				@Override
				public String getText(String text) {
					try {
						
						if (text.startsWith("ARE_YOU_OK")) {
							return "■OK";
						}
						
						if (text.startsWith("ENABLE_LOG")) {
							EasyLog.setLogEnabled(true);
							return "■OK";
						}
						
						if (text.startsWith("DISABLE_LOG")) {
							EasyLog.setLogEnabled(false);
							return "■OK";
						}
						
						if (text.startsWith("EXIT_NOW")) {
							System.exit(0); // 0代表正常退出
							return "";
						}
						
						
						//返回执行器执行的结果
						return executor.execute(text);
					
					} catch (Exception e) {
						String errorMsg = "Error in getText: " + e.toString();
						EasyLog.logToFile(errorMsg);
						EasyLog.logStackTrace(e);
						return "■CodeError:" + e.toString();
					}
				}
			}, ShellPORT);

        service.start();
    }

   
	
    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable throwable) {
					EasyLog.logToFile("=== UNCAUGHT EXCEPTION ===");
					EasyLog.logToFile("Thread: " + thread.getName());
					EasyLog.logToFile("Exception: " + throwable.toString());
					EasyLog.logStackTrace(throwable);

					// 你也可以在这里重新启动服务
					EasyLog.logToFile("Service crashed, attempting restart...");
					
					try {
						Thread.sleep(1000);
						new ServiceThread().start();
					} catch (Exception e) {
						EasyLog.logToFile("Failed to restart: " + e.toString());
					}
				}
			});
    }

	
	// 在服务端启动成功的地方发送广播
	private void sendServerStartedBroadcast(Context context) {
		try {
			Intent intent = new Intent("com.flowercat.rfmouse.SERVER_STARTED");
			context.sendBroadcast(intent);
			Log.d("Server", "发送服务器启动成功广播");
		} catch (Exception e) {
			Log.e("Server", "发送启动广播失败: " + e.getMessage());
		}
	}

	// 在服务端启动失败的地方发送广播
	private void sendServerStartFailedBroadcast(Context context, String errorMessage) {
		try {
			Intent intent = new Intent("com.flowercat.rfmouse.SERVER_START_FAILED");
			intent.putExtra("error_message", errorMessage);
			context.sendBroadcast(intent);
			Log.d("Server", "发送服务器启动失败广播: " + errorMessage);
		} catch (Exception e) {
			Log.e("Server", "发送启动失败广播失败: " + e.getMessage());
		}
	}
	
	
	
	
}
