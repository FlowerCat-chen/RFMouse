package com.flowercat.rfmouse.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * RootShellManager 类用于管理 Root (su) 会话，并提供执行 Root 命令的方法。
 * 它采用单例模式，确保整个应用只有一个 Root 会话实例。
 */
public class RootShellManager {

    // 亮度文件的路径，通常用于控制键盘背光或其他 LED
    private static final String BRIGHTNESS_FILE = "/sys/class/leds/button-backlight/brightness";

    // 用于持久化 su 进程
    private Process suProcess;
    // 用于向 su 进程的 stdin 写入命令
    private static OutputStream os;

    // 单例实例
    private static RootShellManager instance;

    /**
     * 私有构造函数，强制使用单例模式。
     */
    private RootShellManager() {}

    /**
     * 获取 RootShellManager 的单例实例。
     *
     * @return RootShellManager 的唯一实例。
     */
    public static synchronized RootShellManager getInstance() {
        if (instance == null) {
            instance = new RootShellManager();
        }
        return instance;
    }

    /**
     * 初始化并保持一个 su 会话。
     * 此方法应在后台线程中调用，以避免阻塞 UI。
     *
     * @throws IOException 如果无法启动 su 进程。
     */
    public synchronized void initializeSuSession() throws IOException {
        if (suProcess == null) {
            // 启动 su 进程
            suProcess = Runtime.getRuntime().exec("su");
            // 获取 su 进程的输出流，用于向其写入命令
            os = suProcess.getOutputStream();
        }
    }

    /**
     * 关闭 su 会话并释放相关资源。
     * 此方法应在应用关闭或不再需要 Root 会话时调用。
     */
    public synchronized void closeSuSession() {
        if (os != null) {
            try {
                os.write("exit\n".getBytes()); // 退出 su shell
                os.flush(); // 确保所有命令都已写入
                os.close(); // 关闭输出流
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (suProcess != null) {
            suProcess.destroy(); // 销毁 su 进程
        }
        // 重置为 null，以便下次可以重新初始化
        suProcess = null;
        os = null;
    }

    /**
     * 在已存在的 su 会话中执行命令。
     * 此方法不等待命令的返回码或输出，它只负责发送命令。
     *
     * @param command 要执行的命令字符串。
     * @throws IOException 如果写入命令时发生 I/O 错误。
     * @throws SecurityException 如果 suOutputStream 为 null (Root 会话未初始化)。
     */
    public synchronized void executeCommand(String command) throws IOException, SecurityException {
        if (os == null) {
            throw new SecurityException("Root 会话未建立，无法执行命令。");
        }
        os.write((command + "\n").getBytes()); // 写入命令并加上换行符
        os.flush(); // 刷新输出流，确保命令立即发送
    }

    /**
     * 打开或关闭键盘背光。
     * 此操作需要 Root 权限。
     *
     * @param on true 为打开背光，false 为关闭背光。
     * @throws IOException 如果在执行命令过程中发生 I/O 错误。
     * @throws SecurityException 如果 Root 会话未建立。
     * @throws InterruptedException 如果线程在等待时被中断。
     */
    public void setBacklight(boolean on) throws IOException, SecurityException, InterruptedException {
        // 根据开关状态设置亮度值，通常 255 为最大亮度，0 为关闭
        String brightnessValue = on ? "255" : "0";

        // 步骤 1: 授予亮度文件的写入权限，以便可以修改其内容
        executeCommand("chmod 666 " + BRIGHTNESS_FILE);

        // 步骤 2: 将亮度值写入亮度文件
        executeCommand("echo " + brightnessValue + " > " + BRIGHTNESS_FILE);

        // 步骤 3: 撤销亮度文件的写入权限，恢复其原始权限，提高安全性
        executeCommand("chmod 444 " + BRIGHTNESS_FILE);

        // 短暂等待，确保命令被系统处理
        Thread.sleep(100); // 等待 100 毫秒
    }



	/**
     * 使用 Root 权限注入 Key Code，模拟按键操作
     * @param keyCode KeyEvent.KEYCODE_... 对应的整数值
     */
    public void injectKeyCode(int keyCode) {
        try {
			executeCommand(String.format("input keyevent %d &", keyCode));
		} catch (SecurityException e) {} catch (IOException e) {}
    }


	/**
     * 执行命令并获取返回结果
     * @param command 要执行的命令
     * @param callback 结果回调
     */
    public void executeCommandWithResult(final String command, final CommandCallback callback) {
        new Thread(new Runnable() {
				@Override
				public void run() {
					Process process = null;
					DataOutputStream os = null;
					InputStream is = null;
					InputStream es = null;
					BufferedReader reader = null;
					BufferedReader errorReader = null;

					try {
						process = Runtime.getRuntime().exec("su");
						os = new DataOutputStream(process.getOutputStream());
						is = process.getInputStream();
						es = process.getErrorStream();

						// 写入命令
						os.writeBytes(command + "\n");
						os.writeBytes("exit\n");
						os.flush();

						// 读取输出
						reader = new BufferedReader(new InputStreamReader(is));
						errorReader = new BufferedReader(new InputStreamReader(es));

						StringBuilder output = new StringBuilder();
						StringBuilder error = new StringBuilder();

						String line;
						while ((line = reader.readLine()) != null) {
							output.append(line).append("\n");
						}

						while ((line = errorReader.readLine()) != null) {
							error.append(line).append("\n");
						}

						process.waitFor();

						if (process.exitValue() == 0) {
							if (callback != null) {
								callback.onSuccess(output.toString());
							}
						} else {
							if (callback != null) {
								callback.onFailure("命令执行失败: " + error.toString());
							}
						}
					} catch (IOException e) {
						if (callback != null) {
							callback.onFailure("IO异常: " + e.getMessage());
						}
					} catch (InterruptedException e) {
						if (callback != null) {
							callback.onFailure("执行中断: " + e.getMessage());
						}
					} finally {
						try {
							if (os != null) os.close();
							if (is != null) is.close();
							if (es != null) es.close();
							if (reader != null) reader.close();
							if (errorReader != null) errorReader.close();
							if (process != null) process.destroy();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
    }

    /**
     * 执行Shell脚本文件
     * @param scriptPath 脚本路径
     * @param callback 结果回调
     */
    public void executeScript(final String scriptPath, final CommandCallback callback) {
        executeCommandWithResult("sh " + scriptPath, callback);
    }


	/**
     * 命令执行回调接口
     */
    public interface CommandCallback {
        void onSuccess(String result);
        void onFailure(String error);
    }


	/**
	 * 检查设备是否已 Root 且当前应用能够获取 Root 权限。
	 * 注意：此方法会阻塞调用线程，建议在后台线程中调用。
	 *
	 * @return true 表示已 Root 且可获取权限，false 表示未 Root 或无法获取权限。
	 */
	public boolean isRootAvailable() {
		Process process = null;
		DataOutputStream os = null;
		InputStream is = null;
		BufferedReader reader = null;
		try {
			// 尝试执行 su 命令
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			is = process.getInputStream();
			reader = new BufferedReader(new InputStreamReader(is));

			// 执行 id 命令获取当前用户身份
			os.writeBytes("id\n");
			os.writeBytes("exit\n");
			os.flush();

			// 读取命令输出
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line);
			}

			// 等待进程结束
			process.waitFor();

			// 检查输出中是否包含 "uid=0"（root 用户的用户 ID）
			return output.toString().contains("uid=0");
		} catch (IOException | InterruptedException e) {
			// 出现异常说明可能没有 Root 权限
			return false;
		} finally {
			// 清理资源
			try {
				if (os != null) os.close();
				if (is != null) is.close();
				if (reader != null) reader.close();
				if (process != null) process.destroy();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	//切换背光
	public void toggleBacklight() throws IOException, SecurityException, InterruptedException {
        RootShellManager.getInstance().executeCommandWithResult("cat " + BRIGHTNESS_FILE, new RootShellManager.CommandCallback(){

				@Override
				public void onSuccess(String result) {
					if(result.contains("0")){
						try {
							setBacklight(true);
						} catch (IOException e) {} catch (InterruptedException e) {} catch (SecurityException e) {}

					} else {
						try {
							setBacklight(false);
						} catch (IOException e) {} catch (InterruptedException e) {} catch (SecurityException e) {}
					}
				}

				@Override
				public void onFailure(String error) {
				}

			});

        // 短暂等待，确保命令被系统处理
        Thread.sleep(100); // 等待 100 毫秒
    }

}

