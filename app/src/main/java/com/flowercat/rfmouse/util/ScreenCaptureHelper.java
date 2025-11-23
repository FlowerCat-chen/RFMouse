package com.flowercat.rfmouse.util;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.graphics.PixelFormat;
import com.flowercat.rfmouse.ui.PermissionRequestActivity;

public class ScreenCaptureHelper {
    private static final String TAG = "ScreenCaptureHelper";
    private static final int CAPTURE_DELAY_MS = 300; // 首次捕获延迟
    private static final int MIN_RECORDING_DURATION_MS = 3000; // 最小有效录制时长（3秒）

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private final Handler mHandler;
    private final DisplayMetrics mMetrics;
    private final Context mContext;
    private boolean mIsInitialized = false;
    private CaptureCallback mCaptureCallback;

	// 单例模式实例
	private static ScreenCaptureHelper sInstance;

    // --- 新增录屏相关变量 ---
    private MediaRecorder mMediaRecorder;
    private boolean mIsRecording = false;
    private long mRecordingStartTime = 0;
    private String mVideoFilePath;

    public interface CaptureCallback {
        void onCaptureSuccess(Bitmap bitmap);
        void onCaptureError(String error);
    }

	// 录屏回调接口
	public interface RecordingCallback {
        void onRecordingStart();
        void onRecordingStop(String videoPath);
        void onRecordingError(String error);
    }

	private RecordingCallback mRecordingCallback;

    public ScreenCaptureHelper(Context context) {
        mContext = context.getApplicationContext();
        mHandler = new Handler(Looper.getMainLooper());

        // 获取屏幕尺寸
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(mMetrics);
    }


	// 单例获取方法
    public static synchronized ScreenCaptureHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ScreenCaptureHelper(context.getApplicationContext());
        }
        return sInstance;
    }


    public void initialize(int resultCode, Intent data) throws ScreenCaptureException {
        // 如果已经初始化过，先释放资源
        if (mIsInitialized) {
            release();
        }

        initMediaProjection(resultCode, data);
        mIsInitialized = true;
    }

    private void initMediaProjection(int resultCode, Intent data) throws ScreenCaptureException {
        MediaProjectionManager projectionManager = 
            (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (projectionManager == null) {
            throw new ScreenCaptureException("无法获取媒体投影服务");
        }

        try {
            mMediaProjection = projectionManager.getMediaProjection(resultCode, data);
            if (mMediaProjection == null) {
                throw new ScreenCaptureException("媒体投影对象为空");
            }
        } catch (SecurityException e) {
            throw new ScreenCaptureException("权限异常: " + e.getMessage());
        } catch (Exception e) {
            throw new ScreenCaptureException("创建媒体投影失败: " + e.getMessage());
        }
    }

    private void initImageReader() throws ScreenCaptureException {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        try {
            // 如果 ImageReader 已经存在，先关闭
            if (mImageReader != null) {
                mImageReader.close();
            }
            mImageReader = ImageReader.newInstance(width, height, 
												   PixelFormat.RGBA_8888, 2);
        } catch (IllegalArgumentException e) {
            throw new ScreenCaptureException("创建ImageReader失败: " + e.getMessage());
        }

        if (mImageReader == null) {
            throw new ScreenCaptureException("ImageReader初始化失败");
        }
    }

    /**
     * 为截屏创建虚拟显示
     * @throws ScreenCaptureException
     */
    private void createVirtualDisplayForScreenshot() throws ScreenCaptureException {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        int density = mMetrics.densityDpi;

        // 确保 ImageReader 已经初始化
        if(mImageReader == null) {
            initImageReader();
        }

        try {
            // 如果 VirtualDisplay 已经存在，先释放
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
            }
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "ScreenCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, mHandler
            );
        } catch (SecurityException e) {
            throw new ScreenCaptureException("安全异常: " + e.getMessage());
        } catch (IllegalStateException e) {
            throw new ScreenCaptureException("非法状态: " + e.getMessage());
        } catch (Exception e) {
            throw new ScreenCaptureException("创建虚拟显示失败: " + e.getMessage());
        }

        if (mVirtualDisplay == null) {
            throw new ScreenCaptureException("虚拟显示对象为空");
        }
    }

    /**
     * 为录屏创建虚拟显示
     * @param surface MediaRecorder的Surface
     * @throws ScreenCaptureException
     */
    private void createVirtualDisplayForRecording(Surface surface) throws ScreenCaptureException {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        int density = mMetrics.densityDpi;

        try {
            // 如果 VirtualDisplay 已经存在，先释放
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
            }
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "ScreenRecording", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, mHandler
            );
        } catch (SecurityException e) {
            throw new ScreenCaptureException("安全异常: " + e.getMessage());
        } catch (IllegalStateException e) {
            throw new ScreenCaptureException("非法状态: " + e.getMessage());
        } catch (Exception e) {
            throw new ScreenCaptureException("创建虚拟显示失败: " + e.getMessage());
        }

        if (mVirtualDisplay == null) {
            throw new ScreenCaptureException("虚拟显示对象为空");
        }
    }


    public void captureScreenshot(CaptureCallback callback) {
        this.mCaptureCallback = callback;

        if (!mIsInitialized) {
            // 立即处理错误并清空回调，避免持有外部引用
            if (mCaptureCallback != null) {
                mCaptureCallback.onCaptureError("屏幕捕捉未初始化，请授权");
                mCaptureCallback = null;
            }
			Intent permIntent = new Intent(mContext,PermissionRequestActivity.class);
			permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(permIntent);
            return;
        }

        // --- 检查是否正在录屏，如果是，则不允许截屏 ---
        if (mIsRecording) {
            // 立即处理错误并清空回调
            if (mCaptureCallback != null) {
                mCaptureCallback.onCaptureError("当前正在录屏，无法进行截屏操作");
                mCaptureCallback = null;
            }
            return;
        }

        try {
            // 为截屏创建虚拟显示
            createVirtualDisplayForScreenshot();
            // 首次捕获添加延迟，确保虚拟显示准备好
            int delay = mVirtualDisplay != null ? 0 : CAPTURE_DELAY_MS;
            mHandler.postDelayed(mCaptureRunnable, delay);
        } catch (ScreenCaptureException e) {
            // 捕获异常时，通知错误并清空回调
            notifyError(e.getMessage());
        }
    }

    private final Runnable mCaptureRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Bitmap bitmap = captureInternal();
                // captureInternal() 在重试时返回 null
                if (bitmap != null) {
                    notifySuccess(bitmap);
                } 
                // 如果返回 null，则表示正在重试，不需要在这里调用 notifyError
            } catch (ScreenCaptureException e) {
                notifyError("截屏失败: " + e.getMessage());
            }
        }
    };

    private Bitmap captureInternal() throws ScreenCaptureException {
        Image image = null;
        try {
            image = mImageReader.acquireLatestImage();
            if (image == null) {
                // 重试一次
                mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
                            // 传入当前的回调，但注意回调在 notifySuccess/notifyError 中会被清空
							captureScreenshot(mCaptureCallback); 
						}
					}, 100);
                return null;
            }

            Image.Plane[] planes = image.getPlanes();
            if (planes == null || planes.length == 0) {
                throw new ScreenCaptureException("无效的图像平面");
            }

            ByteBuffer buffer = planes[0].getBuffer();
            if (buffer == null) {
                throw new ScreenCaptureException("图像缓冲区为空");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            Bitmap bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride, 
                height, 
                Bitmap.Config.ARGB_8888
            );

            bitmap.copyPixelsFromBuffer(buffer);
            return Bitmap.createBitmap(bitmap, 0, 0, width, height);
        } catch (IllegalStateException e) {
            throw new ScreenCaptureException("图像读取器已关闭: " + e.getMessage());
        } catch (OutOfMemoryError e) {
            throw new ScreenCaptureException("内存不足: " + e.getMessage());
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    private void notifySuccess(final Bitmap bitmap) {
        mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mCaptureCallback != null) {
						mCaptureCallback.onCaptureSuccess(bitmap);
						// 任务成功，清空回调以避免内存泄漏
						mCaptureCallback = null;
					}
				}
			});
    }

    private void notifyError(final String message) {
        mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mCaptureCallback != null) {
						mCaptureCallback.onCaptureError(message);
						// 任务失败，清空回调以避免内存泄漏
						mCaptureCallback = null;
					}
				}
			});
    }

    // --- 新增录屏相关方法 ---

    /**
     * 开始录屏
     * @param filePath 视频文件保存路径
     * @param callback 录屏回调接口
     */
    public void startRecording(String filePath, RecordingCallback callback) {
        this.mRecordingCallback = callback;

        if (!mIsInitialized) {
			if (mRecordingCallback != null) {
                mRecordingCallback.onRecordingError("屏幕捕捉未初始化，请授权");
                mRecordingCallback = null; // 立即清空回调
            }
			Intent permIntent = new Intent(mContext,PermissionRequestActivity.class);
			permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(permIntent);
            return;
        }

        // 如果正在录屏，直接返回
        if (mIsRecording) {
			if (mRecordingCallback != null) {
                mRecordingCallback.onRecordingError("正在进行录屏，请勿重复操作");
                mRecordingCallback = null; // 立即清空回调
            }
            return;
        }

        try {
            mVideoFilePath = filePath;
            setupMediaRecorder();
            createVirtualDisplayForRecording(mMediaRecorder.getSurface());

            mMediaRecorder.start();
            mIsRecording = true;
            mRecordingStartTime = System.currentTimeMillis();

            if (mRecordingCallback != null) {
                mRecordingCallback.onRecordingStart();
            }

        } catch (ScreenCaptureException | IOException e) {
            // 录制失败，需要释放资源并通知错误
            releaseRecorder();
            if (mRecordingCallback != null) {
                mRecordingCallback.onRecordingError("录屏启动失败: " + e.getMessage());
                mRecordingCallback = null; // 启动失败，清空回调
            }
        }
    }

    /**
     * 停止录屏
     */
    public void stopRecording() {
        if (!mIsRecording) {
            if (mRecordingCallback != null) {
                mRecordingCallback.onRecordingError("当前未进行录屏");
                mRecordingCallback = null; // 清空回调
            }
            return;
        }

        mIsRecording = false;

        long recordingDuration = System.currentTimeMillis() - mRecordingStartTime;
        RecordingCallback tempCallback = mRecordingCallback; // 临时保存回调引用
        mRecordingCallback = null; // 提前清空回调，避免在通知后仍持有引用

        try {
            // 停止录制
            mMediaRecorder.stop();
            mMediaRecorder.reset();

            // 释放 VirtualDisplay
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }

            // 判断录制时长
            if (recordingDuration < MIN_RECORDING_DURATION_MS) {
                // 时长不足，删除文件
                File videoFile = new File(mVideoFilePath);
                if (videoFile.exists()) {
                    videoFile.delete();
                    Log.w(TAG, "录制时长不足3秒，已删除文件: " + mVideoFilePath);
                }
                if (tempCallback != null) {
                    tempCallback.onRecordingStop(null); // 返回null表示录制无效
                }
            } else {
                if (tempCallback != null) {
                    tempCallback.onRecordingStop(mVideoFilePath);
                }
            }

        } catch (IllegalStateException e) {
            // 可能会在MediaRecorder状态机异常时发生
            Log.e(TAG, "录屏停止失败", e);
            if (tempCallback != null) {
                tempCallback.onRecordingError("录屏停止失败: " + e.getMessage());
            }
            // 即使异常也尝试释放资源
            releaseRecorder();
        } finally {
            mVideoFilePath = null;
        }
		
		//录屏后截屏会截取到录屏最后的一帧，所以在此截屏清空
		captureScreenshot(new ScreenCaptureHelper.CaptureCallback() {
				@Override
				public void onCaptureSuccess(Bitmap bitmap) {
					
				}

				@Override
				public void onCaptureError(String error) {
		
				}
			});
    }

    /**
     * 配置 MediaRecorder
     * @throws IOException
     */
    private void setupMediaRecorder() throws IOException {
        // 如果 MediaRecorder 已经存在，先释放
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
        }

        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoSize(mMetrics.widthPixels, mMetrics.heightPixels);
        mMediaRecorder.setVideoFrameRate(30); // 帧率
		//设置音频编码 (如果需要音频，请取消注释)
        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); 
        mMediaRecorder.setOutputFile(mVideoFilePath);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            mMediaRecorder.release();
            mMediaRecorder = null;
            throw e;
        }
    }

    /**
     * 释放MediaRecorder资源
     */
    private void releaseRecorder() {
        if (mMediaRecorder != null) {
            // 避免在stop/reset/release中抛出异常
            try { mMediaRecorder.stop(); } catch (Exception ignored) { }
            try { mMediaRecorder.reset(); } catch (Exception ignored) { }
            try { mMediaRecorder.release(); } catch (Exception ignored) { }
            mMediaRecorder = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }


    /**
     * 释放所有资源，用于生命周期结束
     */
    public void release() {
        // 录屏时，需要先停止录屏
        if (mIsRecording) {
            // 确保停止时不会再次触发回调
            mRecordingCallback = null; 
            stopRecording();
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        // 释放 MediaRecorder
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        mIsInitialized = false;
        mCaptureCallback = null;    // 清空回调
        mRecordingCallback = null;  // 清空回调
        sInstance = null;           // 销毁单例
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public static class ScreenCaptureException extends Exception {
        public ScreenCaptureException(String message) {
            super(message);
        }
    }
}

