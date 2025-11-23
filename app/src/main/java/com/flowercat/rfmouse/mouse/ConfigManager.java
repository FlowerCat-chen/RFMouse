package com.flowercat.rfmouse.mouse;

// ConfigManager.java

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.flowercat.rfmouse.util.BitmapManager;
import com.flowercat.rfmouse.util.SPHelper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import android.os.Environment;
import android.Manifest;
import android.content.pm.PackageManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Build;

public class ConfigManager {
    private static final String TAG = "ConfigManager";
    private static final String CONFIG_DIR = "mouse_configs";
    private static final String CONFIG_EXTENSION = ".mcfg";
    private static final String CONFIG_JSON = "config.json";
    private static final String MOUSE_IMAGE = "mouse.png";
    private static final String SCROLL_IMAGE = "scroll.png";

    /**
     * 导出配置到压缩包
     */
    public static boolean exportConfig(Context context, String configName, 
									   String jsonConfig, Bitmap mouseBitmap, Bitmap scrollBitmap) {
        try {
            // 创建配置目录
            File configDir = new File(context.getExternalFilesDir(null), CONFIG_DIR);
            if (!configDir.exists() && !configDir.mkdirs()) {
                Log.e(TAG, "无法创建配置目录");
                return false;
            }

            // 创建压缩包文件
            String fileName = configName + CONFIG_EXTENSION;
            File zipFile = new File(configDir, fileName);

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            try {
                // 添加配置文件
                addStringToZip(zos, CONFIG_JSON, jsonConfig);

                // 添加鼠标图片（如果存在）
                if (mouseBitmap != null) {
                    addBitmapToZip(zos, MOUSE_IMAGE, mouseBitmap);
                }

                // 添加滚动图片（如果存在）
                if (scrollBitmap != null) {
                    addBitmapToZip(zos, SCROLL_IMAGE, scrollBitmap);
                }

                Log.d(TAG, "配置导出成功: " + fileName);
                return true;

            } finally {
                zos.close();
                fos.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "导出配置失败", e);
            return false;
        }
    }

    /**
     * 导入配置从压缩包
     */
    public static ImportResult importConfig(Context context, String zipFilePath) {
        ImportResult result = new ImportResult();

        try {
            FileInputStream fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;

            try {
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();

                    if (CONFIG_JSON.equals(entryName)) {
                        result.jsonConfig = readStringFromZip(zis);
                    } else if (MOUSE_IMAGE.equals(entryName)) {
                        result.mouseBitmap = readBitmapFromZip(zis);
                    } else if (SCROLL_IMAGE.equals(entryName)) {
                        result.scrollBitmap = readBitmapFromZip(zis);
                    }

                    zis.closeEntry();
                }

                result.success = true;
                Log.d(TAG, "配置导入成功");

            } finally {
                zis.close();
                fis.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "导入配置失败", e);
            result.success = false;
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    /**
     * 获取所有配置文件的列表
     */
    public static File[] getConfigFiles(Context context) {
        File configDir = new File(context.getExternalFilesDir(null), CONFIG_DIR);
        if (configDir.exists() && configDir.isDirectory()) {
            return configDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(CONFIG_EXTENSION);
					}
				});
        }
        return new File[0];
    }

	
	
	// 在 ConfigManager.java 中添加以下方法

	/**
	 * 导出指定配置到外部存储
	 */
	public static boolean exportConfigToExternal(Context context, String sourceConfigName, String exportName) {
		try {
			// 获取源配置文件
			File configDir = new File(context.getExternalFilesDir(null), CONFIG_DIR);
			File sourceFile = new File(configDir, sourceConfigName + CONFIG_EXTENSION);

			if (!sourceFile.exists()) {
				Log.e(TAG, "源配置文件不存在: " + sourceFile.getAbsolutePath());
				return false;
			}

			// 创建导出目录（外部存储的Download目录）
			File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MouseConfigs");
			if (!exportDir.exists() && !exportDir.mkdirs()) {
				Log.e(TAG, "无法创建导出目录");
				return false;
			}

			// 创建导出文件
			String exportFileName = exportName + CONFIG_EXTENSION;
			File exportFile = new File(exportDir, exportFileName);

			// 复制文件
			FileInputStream fis = new FileInputStream(sourceFile);
			FileOutputStream fos = new FileOutputStream(exportFile);

			try {
				byte[] buffer = new byte[1024];
				int length;
				while ((length = fis.read(buffer)) > 0) {
					fos.write(buffer, 0, length);
				}
				fos.flush();

				Log.d(TAG, "配置导出成功: " + exportFile.getAbsolutePath());
				return true;

			} finally {
				fis.close();
				fos.close();
			}

		} catch (Exception e) {
			Log.e(TAG, "导出配置失败", e);
			return false;
		}
	}

	/**
	 * 检查外部存储权限
	 */
	public static boolean hasExternalStoragePermission(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            	== PackageManager.PERMISSION_GRANTED;
		} else {
			return true;
		}
	}

	/**
	 * 获取默认导出名称（配置名+时间戳）
	 */
	public static String getDefaultExportName(String configName) {
		//String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
		return configName;
	}
	
	
    /**
     * 删除配置文件
     */
    public static boolean deleteConfig(Context context, String configName) {
        File configDir = new File(context.getExternalFilesDir(null), CONFIG_DIR);
        File configFile = new File(configDir, configName + CONFIG_EXTENSION);

        if (configFile.exists()) {
            boolean deleted = configFile.delete();
            if (deleted) {
                Log.d(TAG, "配置删除成功: " + configName);
            }
            return deleted;
        }
        return false;
    }

    /**
     * 保存当前配置
     */
    public static boolean saveCurrentConfig(Context context, String configName) {
        try {
            // 获取当前配置JSON
            String jsonConfig = SPHelper.exportToJson(context, "rfmouse");
            if (jsonConfig == null) {
                Log.e(TAG, "无法获取当前配置");
                return false;
            }

            // 获取当前图片
            Bitmap mouseBitmap = BitmapManager.getBitmap(context, "mouse");
            Bitmap scrollBitmap = BitmapManager.getBitmap(context, "scroll");

            // 导出配置
            return exportConfig(context, configName, jsonConfig, mouseBitmap, scrollBitmap);

        } catch (Exception e) {
            Log.e(TAG, "保存当前配置失败", e);
            return false;
        }
    }

    // 辅助方法
    private static void addStringToZip(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private static void addBitmapToZip(ZipOutputStream zos, String entryName, Bitmap bitmap) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, zos);
        zos.closeEntry();
    }

    private static String readStringFromZip(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }

    private static Bitmap readBitmapFromZip(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, length);
        }
        byte[] bitmapData = baos.toByteArray();
        return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
    }

    // 导入结果类
    public static class ImportResult {
        public boolean success = false;
        public String jsonConfig;
        public Bitmap mouseBitmap;
        public Bitmap scrollBitmap;
        public String errorMessage;
    }
}
