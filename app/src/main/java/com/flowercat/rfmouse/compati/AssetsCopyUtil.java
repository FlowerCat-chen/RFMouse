package com.flowercat.rfmouse.compati;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetsCopyUtil {

    /**
     * 将assets中的文件/目录复制到Android/data目录
     * @param context 上下文
     * @param assetsPath assets中的路径（可以是文件或目录）
     * @param targetDirName 目标目录名称（将在Android/data/package_name/files/下创建）
     * @return 是否复制成功
     */
    public static boolean copyAssetsToData(Context context, String assetsPath, String targetDirName) {
        AssetManager assetManager = context.getAssets();
        File targetDir = new File(context.getExternalFilesDir(null), targetDirName);

        try {
            // 尝试列出assets中的文件，如果能列出说明是目录
            String[] files = assetManager.list(assetsPath);
            if (files != null && files.length > 0) {
                // 这是目录
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    return false;
                }

                for (String file : files) {
                    String subAssetsPath = assetsPath.isEmpty() ? file : assetsPath + File.separator + file;
                    String subTargetPath = targetDirName + File.separator + file;
                    if (!copyAssetsToData(context, subAssetsPath, subTargetPath)) {
                        return false;
                    }
                }
                return true;
            } else {
                // 这是文件
                return copySingleFile(assetManager, assetsPath, 
									  new File(context.getExternalFilesDir(null), targetDirName));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 复制单个文件
     */
    private static boolean copySingleFile(AssetManager assetManager, String assetsPath, File targetFile) {
        InputStream in = null;
        OutputStream out = null;

        try {
            // 如果目标文件已存在，跳过复制
            if (targetFile.exists()) {
                return true;
            }

            // 确保目标目录存在
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                return false;
            }

            in = assetManager.open(assetsPath);
            out = new FileOutputStream(targetFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            out.flush();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            // 关闭流
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查文件是否已存在于目标目录
     */
    public static boolean isFileExists(Context context, String targetDirName, String fileName) {
        File targetFile = new File(context.getExternalFilesDir(null), 
								   targetDirName + File.separator + fileName);
        return targetFile.exists();
    }
}
