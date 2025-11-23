package shellService;
import android.os.SystemClock;
import shellService.scrcpy.InputUtil;
import android.view.InputDevice;
import android.view.MotionEvent;



public class TouchTest {
    private static volatile boolean isTesting = false;
    private static Thread testThread;

    // 开始测试滑动
    public static void startSwipeTest(final int screenWidth, final int screenHeight) {
        // 如果已经在测试，先停止之前的测试
        stopSwipeTest();

        isTesting = true;
        testThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						performSwipeTest(screenWidth, screenHeight);
					} catch (InterruptedException e) {
						System.out.println("Swipe test interrupted");
					} finally {
						isTesting = false;
					}
				}
			});
        testThread.start();
    }

    // 停止测试滑动
    public static void stopSwipeTest() {
        isTesting = false;
        if (testThread != null && testThread.isAlive()) {
            testThread.interrupt();
            try {
                testThread.join(1000); // 等待线程结束，最多1秒
            } catch (InterruptedException e) {
                // 忽略中断异常
            }
        }
    }

    // 执行滑动测试
    private static void performSwipeTest(int screenWidth, int screenHeight) throws InterruptedException {
        final int repeatCount = 5;
        final long totalDuration = 5000; // 5秒
        final int numSteps = 50; // 步骤数，越多越平滑

        // 计算坐标
        float startX = screenWidth / 2.0f;
        float startY = screenHeight / 2.0f;
        float endX = screenWidth - 10; // 留一点边距
        float endY = startY; // 水平滑动，Y坐标不变

        System.out.println("Starting swipe test: " + repeatCount + " repetitions");

        for (int i = 0; i < repeatCount && isTesting; i++) {
            if (!isTesting) break;

            System.out.println("Swipe " + (i + 1) + "/" + repeatCount);

            long startTime = SystemClock.uptimeMillis();

            // 按下开始
            InputUtil.injectMotionEvent(
                InputDevice.SOURCE_TOUCHSCREEN,
                MotionEvent.ACTION_DOWN,
                startTime,
                startX,
                startY,
                1.0f
            );

            // 平滑移动
            for (int step = 1; step <= numSteps && isTesting; step++) {
                long currentTime = startTime + (step * totalDuration / numSteps);
                float alpha = (float) step / numSteps;
                float currentX = InputUtil.lerp(startX, endX, alpha);

                InputUtil.injectMotionEvent(
                    InputDevice.SOURCE_TOUCHSCREEN,
                    MotionEvent.ACTION_MOVE,
                    currentTime,
                    currentX,
                    startY,
                    1.0f
                );

                // 短暂休眠，让滑动更平滑
                Thread.sleep(totalDuration / numSteps);
            }

            if (isTesting) {
                // 抬起结束
                long endTime = startTime + totalDuration;
                InputUtil.injectMotionEvent(
                    InputDevice.SOURCE_TOUCHSCREEN,
                    MotionEvent.ACTION_UP,
                    endTime,
                    endX,
                    endY,
                    0.0f
                );
            }

            // 每次滑动后短暂暂停（可选）
            if (i < repeatCount - 1 && isTesting) {
                Thread.sleep(500);
            }
        }

        System.out.println("Swipe test completed");
        isTesting = false;
    }

    // 检查是否正在测试
    public static boolean isTesting() {
        return isTesting;
    }

    // 简单的测试方法 - 直接调用这个开始测试（使用默认分辨率240×320）
    public static void testTouch() {
        int screenWidth = 240;
        int screenHeight = 320;
        startSwipeTest(screenWidth, screenHeight);
    }
}
