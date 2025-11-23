package com.flowercat.rfmouse.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 封装Bitmap的存取操作，以简化文件处理。
 * 对外提供类似SharedPreferences的键值对接口。
 */
public class BitmapManager {

    private static final String TAG = "BitmapManager";
    private static final String BITMAP_FILE_PREFIX = "bitmap_";

    /**
     * 将Bitmap永久存储到应用的内部存储中。
     * * @param context 上下文对象
     * @param key 存储Bitmap的唯一键名
     * @param bitmap 要存储的Bitmap对象
     * @return 存储成功返回true，否则返回false
     */
    public static boolean putBitmap(Context context, String key, Bitmap bitmap) {
        if (context == null || key == null || key.isEmpty() || bitmap == null) {
            Log.e(TAG, "参数无效，无法存储Bitmap。");
            return false;
        }

        // 使用给定的key生成一个唯一的文件名
        String filename = BITMAP_FILE_PREFIX + key + ".png";
        File file = new File(context.getFilesDir(), filename);

        try {
			
		 FileOutputStream fos = new FileOutputStream(file);
            // 以PNG格式压缩Bitmap并写入文件，质量为100
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Log.d(TAG, "Bitmap已成功保存，键名: " + key);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "保存Bitmap时发生错误，键名: " + key, e);
            return false;
        }
    }

    /**
     * 从应用的内部存储中读取Bitmap。
     * * @param context 上下文对象
     * @param key 存储Bitmap的唯一键名
     * @return 找到并成功读取的Bitmap对象，如果不存在或读取失败则返回null
     */
    public static Bitmap getBitmap(Context context, String key) {
        if (context == null || key == null || key.isEmpty()) {
            Log.e(TAG, "参数无效，无法读取Bitmap。");
            return null;
        }

        // 根据键名找到对应的文件名
        String filename = BITMAP_FILE_PREFIX + key + ".png";
        File file = new File(context.getFilesDir(), filename);

        if (!file.exists()) {
            Log.w(TAG, "文件不存在，键名: " + key);
            return null;
        }

        try {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "读取Bitmap时发生错误，键名: " + key, e);
            return null;
        }
    }

    /**
     * 删除存储的Bitmap文件。
     * * @param context 上下文对象
     * @param key 要删除的Bitmap的键名
     * @return 删除成功返回true，否则返回false
     */
    public static boolean deleteBitmap(Context context, String key) {
        if (context == null || key == null || key.isEmpty()) {
            Log.e(TAG, "参数无效，无法删除Bitmap。");
            return false;
        }

        String filename = BITMAP_FILE_PREFIX + key + ".png";
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "文件已成功删除，键名: " + key);
            } else {
                Log.e(TAG, "删除文件失败，键名: " + key);
            }
            return deleted;
        }
        Log.w(TAG, "要删除的文件不存在，键名: " + key);
        return false;
    }
}
