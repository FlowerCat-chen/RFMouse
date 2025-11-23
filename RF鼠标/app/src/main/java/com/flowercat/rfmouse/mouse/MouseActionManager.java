package com.flowercat.rfmouse.mouse;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import android.text.TextUtils;
import com.flowercat.rfmouse.adb.client.ClientManager;

/**
 * 封装了所有鼠标点击和滚动操作的单例类。
 * 通过构造函数传入辅助服务实例，以访问其API。
 */
public class MouseActionManager {

	private static final String TAG = "MouseActionManager";

	// 单例实例
	private static volatile MouseActionManager instance;
	private AccessibilityService service;
	private MouseManager2 mouseManager;
	private ClientManager clientManager = ClientManager.getInstance();
	public  boolean inDebugMode;
	private int screenWidth;
	private int screenHeight;

	private int short_click_time = 50;//ms
	private int long_click_time = 2;//s
	
	//触摸注入
	private int short_click_time_mgr = 50;//ms
	private int long_click_time_mgr = 2;//s
	
	//滚动相关
	public static int SCROLL_DISTANCE_UD = 25; // 上下滚动距离为屏幕尺寸的25%
	public static int SCROLL_DISTANCE_LR = 25; // 上下滚动距离为屏幕尺寸的25%
	public static int SCROLL_DURATION = 300; // 滚动持续时间(毫秒)


	//触摸注入滚动相关
	public static int SCROLL_DISTANCE_UD_MGR = 25; // 上下滚动距离为屏幕尺寸的25%
	public static int SCROLL_DISTANCE_LR_MGR = 25; // 上下滚动距离为屏幕尺寸的25%
	public static int SCROLL_DURATION_MGR = 300; // 滚动持续时间(毫秒)
	
	
	// 滚动方向常量
	public static final int SCROLL_UP = 0;
	public static final int SCROLL_DOWN = 1;
	public static final int SCROLL_LEFT = 2;
	public static final int SCROLL_RIGHT = 3;
	// 当前的点击模式，默认手势分发。
	public static int clickmode = 2;


	// 构造函数私有化
	private MouseActionManager(AccessibilityService service, MouseManager2 mouseManager, boolean inDebugMode) {
		this.service = service;
		this.mouseManager = mouseManager;
		this.inDebugMode = inDebugMode;
		// 确保服务不为空
		if (service != null) {
			this.screenWidth = service.getResources().getDisplayMetrics().widthPixels;
			this.screenHeight = service.getResources().getDisplayMetrics().heightPixels;
		} else {
			this.screenWidth = 0;
			this.screenHeight = 0;
			Log.e(TAG, "AccessibilityService is null!");
		}
	}


	/**
	 * 单例模式，获取唯一的 MouseActionManager 实例
	 * 第一次调用时，必须传入 AccessibilityService 实例
	 * 后续调用可直接使用 getInstance()
	 * @param service 辅助服务实例，仅在第一次调用时需要
	 * @param mouseManager 鼠标管理器实例，仅在第一次调用时需要
	 * @param inDebugMode 调试模式标志，仅在第一次调用时需要
	 * @return MouseActionManager 单例实例
	 */
	public static MouseActionManager getInstance(AccessibilityService service, MouseManager2 mouseManager) {
		if (instance == null) {
			synchronized (MouseActionManager.class) {
				if (instance == null) {
					instance = new MouseActionManager(service, mouseManager, false);
				}
			}
		}
		return instance;
	}


	// 重载方法，方便后续调用
	public static MouseActionManager getInstance() {
		if (instance == null) {
			throw new IllegalStateException("MouseActionManager has not been initialized. Call getInstance(service, mouseManager, inDebugMode) first.");
		}

		return instance;
	}

	//显示调试视图
	public void setDebugMode(boolean indebug) {
		inDebugMode = indebug;
	}


	// 辅助方法
    private AccessibilityNodeInfo getRootInActiveWindow() {
        return service.getRootInActiveWindow();
    }

    /**
	 * 尝试获取 AccessibilityNodeInfo 的文本内容。
	 *
	 * 该方法会按优先级从以下字段获取文本：
	 * 1. getText()
	 * 2. getContentDescription()
	 * 3. getHintText()
	 *
	 * @param nodeInfo 要获取文本的 AccessibilityNodeInfo 对象
	 * @return 节点的文本内容，如果所有字段都为空则返回 null
	 */
	public static String getNodeText(AccessibilityNodeInfo nodeInfo) {
		if (nodeInfo == null) {
			return null;
		}

		// 尝试获取getText()
		CharSequence text = nodeInfo.getText();
		if (!TextUtils.isEmpty(text)) {
			return text.toString();
		}

		// 如果getText()为空，尝试获取getContentDescription()
		CharSequence contentDescription = nodeInfo.getContentDescription();
		if (!TextUtils.isEmpty(contentDescription)) {
			return contentDescription.toString();
		}

		// 如果上述都为空，尝试获取getHintText()
		if (Build.VERSION.SDK_INT >= 26) {
			CharSequence hintText = nodeInfo.getHintText();
			if (!TextUtils.isEmpty(hintText)) {
				return hintText.toString();
			}
		}
		return "无可读内容";
	}

	//分发手势
    private boolean dispatchGesture(GestureDescription gesture, AccessibilityService.GestureResultCallback callback, android.os.Handler handler) {
        return service.dispatchGesture(gesture, callback, handler);
    }


	//设置点击模式
	public void setClickMode(int mode){
		this.clickmode = mode;
	}

	/*****
	 点击操作开始
	 ******/


    /**
     * 在鼠标指向的位置执行点击操作
     */
    public void clickAtMousePosition(boolean isLong) {
		
		//如果在触摸注入模式
		if(clickmode == 3){
			if (isLong) {
				performMgrClick(true);
			} else {
				performMgrClick(false);
            }
			return;
		}
			

        if (!isGestureSupported()) {
            Log.w(TAG, "Gesture not supported on this Android version");

			if (isLong) {
				performAccessibilityClick(true);
			} else {
				performAccessibilityClick(false);
            }

			return;
        }

        if (mouseManager == null) {
            Log.e(TAG, "MouseManager not set");
            return ;
        }
		
		//如果在节点点击模式
		if(clickmode == 1){
			if (isLong) {
				performAccessibilityClick(true);
			} else {
				performAccessibilityClick(false);
            }
			return;
		}
		

        // 获取鼠标中心位置坐标
        int mouseX = mouseManager.getMouseX();
        int mouseY = mouseManager.getMouseY();

        if (isLong) {
			performClick(mouseX, mouseY, long_click_time * 1000);
		} else {
        	performClick(mouseX, mouseY,short_click_time);
		}
    }

	//触摸注入
	public boolean performMgrClick(boolean isLong){
		
		if (mouseManager == null) {
            Log.e(TAG, "MouseManager not set");
            return false;
        }

        // 获取鼠标中心位置坐标
        int mouseX = mouseManager.getMouseX();
        int mouseY = mouseManager.getMouseY();
		
		
		if(!isLong){
			clientManager.sendMessage("click:" + String.valueOf(mouseX) + "," + String.valueOf(mouseY) +"," + String.valueOf(short_click_time_mgr));
		} else {
			clientManager.sendMessage("click:" + String.valueOf(mouseX) + "," + String.valueOf(mouseY) +"," + String.valueOf(long_click_time_mgr * 1000));
		}
		
		if(inDebugMode){
			mouseManager.highlightCurrentGesture(mouseX,mouseY);
		}
		
		return true;
	}
	


    /**
     * 使用AccessibilityNodeInfo执行点击（Android 7.0以下）
     */
    private boolean performAccessibilityClick(boolean isLong) {
        if (mouseManager == null) {
            Log.e(TAG, "MouseManager not set");
            return false;
        }

        // 获取鼠标中心位置坐标
        int mouseX = mouseManager.getMouseX();
        int mouseY = mouseManager.getMouseY();

        // 获取根节点
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Toast.makeText(service, "无法获取窗口内容", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 查找所有可点击节点
        List<AccessibilityNodeInfo> clickableNodes = new ArrayList<>();
        findClickableNodes(rootNode, clickableNodes);

		if (inDebugMode) {
			mouseManager.highlightClickables(clickableNodes);
		}

        // 查找最适合的节点
        AccessibilityNodeInfo targetNode = findBestMatchingNode(clickableNodes, mouseX, mouseY);

        // 执行点击
        if (targetNode != null) {
			// boolean result = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

			String content = getNodeText(targetNode);
			//ttsManager.speak(content,1.0f,1.0f);
			
			if (inDebugMode) {
				mouseManager.highlightCurrent(targetNode);
			}
			
			if (isLong) {
				targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
				targetNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
			} else {
		   		performNodeAction(targetNode);
			}
			targetNode.recycle();
			return true;
        } else {
            Toast.makeText(service, "未找到可点击的控件", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


	/**
	 * 根据节点类型执行不同的操作
	 */
	private boolean performNodeAction(AccessibilityNodeInfo node) {
		// 获取节点类名
		String className = node.getClassName().toString();
		// 根据不同控件类型执行不同操作
		if (className.contains("EditText")) {
			// 对于EditText,先获取焦点再执行点击
			node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
			return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
			//positionCursorInEditText(node,mouseManager.getMouseX(),mouseManager.getMouseY());
			//return true;
		} else if (className.contains("Switch") || className.contains("CheckBox")) {
			// 对于开关和复选框，使用点击操作
			return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
		} else if (className.contains("Spinner")) {
			// 对于下拉列表，使用展开操作
			return node.performAction(AccessibilityNodeInfo.ACTION_EXPAND);
			//} else if (node.isScrollable()) {
			// 对于可滚动视图，先滚动到视图内，没必要了
			//return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
			//return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
		} else {
			// 默认使用点击操作
			//这里也聚焦一下吧
			node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
			return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
		}
	}


	/***
	 没什么用的方法，我们无法准确的获取到文本的宽度，因此光标的插入也不会准确。
	 ***/

	// 新增方法：在 EditText 中精确定位光标
	private void positionCursorInEditText(AccessibilityNodeInfo editTextNode, int x, int y) {

		try {
			// 检查是否有文本
			CharSequence text = editTextNode.getText();
			if (text != null && text.length() > 0) {
				// 计算光标位置
				int cursorPosition = calculateCursorPosition(editTextNode, x, text);

				// 设置光标位置
				Bundle arguments = new Bundle();
				arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursorPosition);
				arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursorPosition);
				editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments);
			}
		} finally {
			editTextNode.recycle();
		}
	}


	// 新增方法：计算光标位置
	private int calculateCursorPosition(AccessibilityNodeInfo editTextNode, int x, CharSequence text) {
		Rect bounds = new Rect();
		editTextNode.getBoundsInScreen(bounds);

		// 获取文本布局信息（近似计算）
		// 注意：在辅助功能服务中无法获取精确的文本布局，所以使用近似算法

		// 计算点击位置相对于 EditText 的 X 坐标
		int relativeX = x - bounds.left;

		// 估算平均字符宽度
		float avgCharWidth = bounds.width() / (float) text.length();

		// 计算光标位置
		int cursorPosition = (int) (relativeX / avgCharWidth);

		// 确保光标位置在有效范围内
		cursorPosition = Math.max(0, Math.min(cursorPosition, text.length()));

		return cursorPosition;
	}


	/**
     * 根据坐标找到最匹配的节点
     * 优先选择面积最小且包含坐标的节点
     */
    private AccessibilityNodeInfo findBestMatchingNode(List<AccessibilityNodeInfo> nodes, int x, int y) {
        AccessibilityNodeInfo bestMatch = null;
        int smallestArea = Integer.MAX_VALUE;

        for (AccessibilityNodeInfo node : nodes) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            // 检查坐标是否在节点范围内
            if (bounds.contains(x, y)) {
                int area = bounds.width() * bounds.height();

                // 选择面积最小的节点
                if (area < smallestArea) {
                    smallestArea = area;
                    if (bestMatch != null) {
                        bestMatch.recycle();
                    }
                    bestMatch = AccessibilityNodeInfo.obtain(node);
                }
            }
        }

        // 回收不需要的节点
        for (AccessibilityNodeInfo node : nodes) {
            if (node != bestMatch) {
                node.recycle();
            }
        }

        return bestMatch;
    }


	//核心部分。寻找可滑动，可点击，可选择的节点。
    private void findClickableNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        try {
            if (node == null) {
				Toast.makeText(service, "获取节点为空", Toast.LENGTH_SHORT).show();
				return;
			}

			Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            // 检查节点是否可交互
            if (node.isClickable() || node.isLongClickable() || 
                node.isCheckable() || node.isScrollable() || node.isEditable() || node.isFocusable()) {
                result.add(AccessibilityNodeInfo.obtain(node));
            }

            // 递归子节点
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    findClickableNodes(child, result);
                    child.recycle();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding clickable nodes", e);
        }
    }


    /**
     * 检查当前Android版本是否支持手势
     */
    public boolean isGestureSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
		//return false;
    }

    /**
     * 在指定坐标执行点击操作
     * @param x 屏幕X坐标
     * @param y 屏幕Y坐标
     */
    public boolean performClick(int x, int y,int duration) {
        if (!isGestureSupported()) {
            return false;
        }

        // 创建点击手势
        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription clickStroke =
			new GestureDescription.StrokeDescription(clickPath, 0, duration);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(clickStroke);

		if(inDebugMode){
			mouseManager.highlightCurrentGesture(x,y);
		}
        // 分发手势
        return dispatchGesture(builder.build(), null, null);
    }

    /**
     * 在指定坐标执行长按操作
     * @param x 屏幕X坐标
     * @param y 屏幕Y坐标
     * @param duration 长按持续时间(毫秒)
     */
	 
	 /*
    public boolean performLongClick(int x, int y, int duration) {
        if (!isGestureSupported()) {
            return false;
        }

        // 创建长按手势
        Path longClickPath = new Path();
        longClickPath.moveTo(x, y);

        GestureDescription.StrokeDescription longClickStroke =
			new GestureDescription.StrokeDescription(longClickPath, 0, duration);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(longClickStroke);

        // 分发手势
        return dispatchGesture(builder.build(), null, null);
    }
*/

	/**
	 * 执行滚动操作
	 * @param direction 滚动方向 (SCROLL_UP, SCROLL_DOWN, SCROLL_LEFT, SCROLL_RIGHT)
	 */
	public boolean scrollAtMousePosition(int direction) {
		
		if(clickmode == 3){
			// 使用触摸注入
			return performMgrScroll(direction);
		}
		
		
		if (isGestureSupported()) {
			
			if(clickmode == 1){
				// 使用节点查找方式
				return performAccessibilityScroll(direction);
			}
			
			// 高版本使用手势分发
			return performGestureScroll(direction);
		} else {
			// 低版本使用节点查找方式
			return performAccessibilityScroll(direction);
		}
	}
	
	

	/**
	 * 使用触摸注入实现滚动
	 */
	private boolean performMgrScroll(int direction) {
		if (mouseManager == null) {
			Log.e(TAG, "MouseManager not set");
			return false;
		}


		// 计算滚动距离
		int scrollDistanceX = 0;
		int scrollDistanceY = 0;

		switch (direction) {
			case SCROLL_UP:
				scrollDistanceY = -(int) (screenHeight * (SCROLL_DISTANCE_UD_MGR / 100.0f));
				break;
			case SCROLL_DOWN:
				scrollDistanceY = (int) (screenHeight * (SCROLL_DISTANCE_UD_MGR / 100.0f));
				break;
			case SCROLL_LEFT:
				scrollDistanceX = -(int) (screenWidth * (SCROLL_DISTANCE_LR_MGR / 100.0f));
				break;
			case SCROLL_RIGHT:
				scrollDistanceX = (int) (screenWidth * (SCROLL_DISTANCE_LR_MGR / 100.0f));
				break;
		}

		// 获取鼠标当前位置
		int startX = mouseManager.getMouseX();
		int startY = mouseManager.getMouseY();

		// 确保起点在屏幕范围内
		startX = Math.max(0, Math.min(startX, screenWidth));
		startY = Math.max(0, Math.min(startY, screenHeight));

		// 计算终点位置
		int endX = startX + scrollDistanceX;
		int endY = startY + scrollDistanceY;

		// 确保终点在屏幕范围内
		endX = Math.max(0, Math.min(endX, screenWidth));
		endY = Math.max(0, Math.min(endY, screenHeight));

		if(inDebugMode){
			mouseManager.highlightLineGesture(startX,startY,endX,endY);
		}

		clientManager.sendMessage("scroll:" + String.valueOf(startX) + "," + String.valueOf(startY) +"," + String.valueOf(endX) + "," + String.valueOf(endY) + "," + String.valueOf(SCROLL_DURATION_MGR) + "," + "30");
		
		return true;
	}
	
	//鼠标拖动方法
	public void mouseDragFunction(String mode ,int x, int y) {
		clientManager.sendMessage("drag:" + mode + "," + String.valueOf(x) + "," + String.valueOf(y));
	}
	
	/**
	 * 使用手势分发实现滚动（Android 7.0及以上）
	 */
	private boolean performGestureScroll(int direction) {
		if (mouseManager == null) {
			Log.e(TAG, "MouseManager not set");
			return false;
		}


		// 计算滚动距离
		int scrollDistanceX = 0;
		int scrollDistanceY = 0;

		switch (direction) {
			case SCROLL_UP:
				scrollDistanceY = -(int) (screenHeight * (SCROLL_DISTANCE_UD / 100.0f));
				break;
			case SCROLL_DOWN:
				scrollDistanceY = (int) (screenHeight * (SCROLL_DISTANCE_UD / 100.0f));
				break;
			case SCROLL_LEFT:
				scrollDistanceX = -(int) (screenWidth * (SCROLL_DISTANCE_LR / 100.0f));
				break;
			case SCROLL_RIGHT:
				scrollDistanceX = (int) (screenWidth * (SCROLL_DISTANCE_LR / 100.0f));
				break;
		}

		// 获取鼠标当前位置
		int startX = mouseManager.getMouseX();
		int startY = mouseManager.getMouseY();

		// 确保起点在屏幕范围内
		startX = Math.max(0, Math.min(startX, screenWidth));
		startY = Math.max(0, Math.min(startY, screenHeight));

		// 计算终点位置
		int endX = startX + scrollDistanceX;
		int endY = startY + scrollDistanceY;

		// 确保终点在屏幕范围内
		endX = Math.max(0, Math.min(endX, screenWidth));
		endY = Math.max(0, Math.min(endY, screenHeight));

		if(inDebugMode){
			mouseManager.highlightLineGesture(startX,startY,endX,endY);
		}

		// 创建滑动手势
		Path scrollPath = new Path();
		scrollPath.moveTo(startX, startY);
		scrollPath.lineTo(endX, endY);

		GestureDescription.StrokeDescription scrollStroke =
			new GestureDescription.StrokeDescription(scrollPath, 0, SCROLL_DURATION);

		GestureDescription.Builder builder = new GestureDescription.Builder();
		builder.addStroke(scrollStroke);

		// 分发手势
		return dispatchGesture(builder.build(), null, null);
	}


	/**
	 * 使用Accessibility节点实现滚动（Android 7.0以下）
	 */
	private boolean performAccessibilityScroll(int direction) {
		// 获取根节点
		AccessibilityNodeInfo rootNode = getRootInActiveWindow();
		if (rootNode == null) {
			Toast.makeText(service, "无法获取窗口内容", Toast.LENGTH_SHORT).show();
			return false;
		}

		try {
			// 查找所有可滚动节点并分析其滚动方向
			List<AccessibilityNodeInfo> verticalNodes = new ArrayList<>();
			List<AccessibilityNodeInfo> horizontalNodes = new ArrayList<>();
			List<AccessibilityNodeInfo> unknownNodes = new ArrayList<>();

			findScrollableNodesByDirection(rootNode, verticalNodes, horizontalNodes, unknownNodes);

			// 根据滚动方向选择目标节点列表
			List<AccessibilityNodeInfo> targetNodes = new ArrayList<>();
			boolean isVerticalScroll = (direction == SCROLL_UP || direction == SCROLL_DOWN);

			if (isVerticalScroll) {
				// 垂直滚动：只考虑垂直节点
				targetNodes = !verticalNodes.isEmpty() ? verticalNodes : unknownNodes;
			} else {
				// 水平滚动：只考虑水平节点
				targetNodes = !horizontalNodes.isEmpty() ? horizontalNodes : unknownNodes;
			}

			if(inDebugMode){
				mouseManager.highlightClickables(targetNodes);
			}
			
			
			// 查找最适合的节点（最靠近鼠标位置的）
			AccessibilityNodeInfo targetNode = findBestScrollableNode(targetNodes);
			
			if(inDebugMode){
				mouseManager.highlightCurrent(targetNode);
			}

			// 执行滚动
			if (targetNode != null) {
				boolean result = false;

				switch (direction) {
					case SCROLL_UP:
					case SCROLL_LEFT:
						result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
						break;
					case SCROLL_DOWN:
					case SCROLL_RIGHT:
						result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
						break;
				}

				targetNode.recycle();
				return result;
			} else {
				Toast.makeText(service, "未找到可滚动的控件", Toast.LENGTH_SHORT).show();
				return false;
			}
		} finally {
			rootNode.recycle();
		}
	}


	// 定义滚动方向常量
	private static final int SCROLL_DIRECTION_UNKNOWN = 0;
	private static final int SCROLL_DIRECTION_VERTICAL = 1;
	private static final int SCROLL_DIRECTION_HORIZONTAL = 2;

	/**
	 * 使用Accessibility节点实现滚动（Android 7.0以下）
	 */
	private boolean performAccessibilityScroll2(int direction) {
		// 获取根节点
		AccessibilityNodeInfo rootNode = getRootInActiveWindow();
		if (rootNode == null) {
			Toast.makeText(service, "无法获取窗口内容", Toast.LENGTH_SHORT).show();
			return false;
		}

		try {
			// 查找所有可滚动节点并分析其滚动方向
			List<AccessibilityNodeInfo> verticalNodes = new ArrayList<>();
			List<AccessibilityNodeInfo> horizontalNodes = new ArrayList<>();
			List<AccessibilityNodeInfo> unknownNodes = new ArrayList<>();

			findScrollableNodesByDirection(rootNode, verticalNodes, horizontalNodes, unknownNodes);

			// 根据滚动方向选择目标节点列表
			List<AccessibilityNodeInfo> targetNodes;
			boolean isVerticalScroll = (direction == SCROLL_UP || direction == SCROLL_DOWN);

			if (isVerticalScroll) {
				// 优先选择垂直滚动节点，其次未知节点，最后水平节点
				targetNodes = !verticalNodes.isEmpty() ? verticalNodes : 
					!unknownNodes.isEmpty() ? unknownNodes : horizontalNodes;
			} else {
				// 优先选择水平滚动节点，其次未知节点，最后垂直节点
				targetNodes = !horizontalNodes.isEmpty() ? horizontalNodes : 
					!unknownNodes.isEmpty() ? unknownNodes : verticalNodes;
			}

			// 查找最适合的节点（最靠近鼠标位置的）
			AccessibilityNodeInfo targetNode = findBestScrollableNode(targetNodes);

			if (inDebugMode) {
				//mouseManager.highlightClickables(targetNodes);
			}

			// 执行滚动
			if (targetNode != null) {
				boolean result = false;

				switch (direction) {
					case SCROLL_UP:
					case SCROLL_LEFT:
						result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
						break;
					case SCROLL_DOWN:
					case SCROLL_RIGHT:
						result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
						break;
				}

				targetNode.recycle();
				return result;
			} else {
				Toast.makeText(service, "未找到可滚动的控件", Toast.LENGTH_SHORT).show();
				return false;
			}
		} finally {
			rootNode.recycle();
		}
	}

	/**
	 * 递归查找所有可滚动节点并按方向分类
	 */
	private void findScrollableNodesByDirection(AccessibilityNodeInfo node, 
												List<AccessibilityNodeInfo> verticalNodes, 
												List<AccessibilityNodeInfo> horizontalNodes,
												List<AccessibilityNodeInfo> unknownNodes) {
		try {
			if (node == null) {
				return;
			}

			// 检查节点是否可滚动
			if (node.isScrollable()) {
				int scrollDirection = determineScrollDirection(node);

				switch (scrollDirection) {
					case SCROLL_DIRECTION_VERTICAL:
						verticalNodes.add(AccessibilityNodeInfo.obtain(node));
						break;
					case SCROLL_DIRECTION_HORIZONTAL:
						horizontalNodes.add(AccessibilityNodeInfo.obtain(node));
						break;
					default:
						unknownNodes.add(AccessibilityNodeInfo.obtain(node));
						break;
				}
			}

			// 递归子节点
			int childCount = node.getChildCount();
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo child = node.getChild(i);
				if (child != null) {
					findScrollableNodesByDirection(child, verticalNodes, horizontalNodes, unknownNodes);
					child.recycle();
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error finding scrollable nodes", e);
		}
	}

	/**
	 * 判断节点的滚动方向
	 */
	private int determineScrollDirection(AccessibilityNodeInfo node) {
		try {
			// 方法1: 检查类名获取线索
			String className = node.getClassName() != null ? node.getClassName().toString() : "";

			// 常见垂直滚动容器
			if (className.contains("RecyclerView") || 
				className.contains("ListView") || 
				className.contains("ScrollView") || 
				className.contains("WebView") ||
				className.contains("NestedScrollView")) {
				return SCROLL_DIRECTION_VERTICAL;
			}

			// 常见水平滚动容器
			if (className.contains("HorizontalScrollView") || 
				className.contains("ViewPager") || 
				className.contains("ViewPager2")) {
				return SCROLL_DIRECTION_HORIZONTAL;
			}

			// 方法2: 检查内容描述和文本获取线索
			CharSequence contentDesc = node.getContentDescription();
			if (contentDesc != null && contentDesc.toString().toLowerCase().contains("vertical")) {
				return SCROLL_DIRECTION_VERTICAL;
			}
			if (contentDesc != null && contentDesc.toString().toLowerCase().contains("horizontal")) {
				return SCROLL_DIRECTION_HORIZONTAL;
			}

			// 方法3: 基于宽高比的启发式判断
			Rect bounds = new Rect();
			node.getBoundsInScreen(bounds);
			if (bounds != null) {
				int width = bounds.width();
				int height = bounds.height();

				// 避免除零错误
				if (width > 0 && height > 0) {
					float aspectRatio = (float) width / height;

					// 宽高比大的更可能是水平滚动
					if (aspectRatio > 1.5f) {
						return SCROLL_DIRECTION_HORIZONTAL;
					} else if (aspectRatio < 0.67f) {
						return SCROLL_DIRECTION_VERTICAL;
					}
				}
			}

			return SCROLL_DIRECTION_UNKNOWN;
		} catch (Exception e) {
			Log.e(TAG, "Error determining scroll direction", e);
			return SCROLL_DIRECTION_UNKNOWN;
		}
	}




	/**
	 * 使用Accessibility节点实现滚动（Android 7.0以下）
	 */

	/*
	 private boolean performAccessibilityScroll(int direction) {
	 // 获取根节点
	 AccessibilityNodeInfo rootNode = getRootInActiveWindow();
	 if (rootNode == null) {
	 Toast.makeText(this, "无法获取窗口内容", Toast.LENGTH_SHORT).show();
	 return false;
	 }

	 // 查找所有可滚动节点
	 List<AccessibilityNodeInfo> scrollableNodes = new ArrayList<>();
	 findScrollableNodes(rootNode, scrollableNodes);

	 // 查找最适合的节点（最靠近鼠标位置的）
	 AccessibilityNodeInfo targetNode = findBestScrollableNode(scrollableNodes);

	 // 执行滚动
	 if (targetNode != null) {
	 boolean result = false;

	 switch (direction) {
	 case SCROLL_UP:
	 case SCROLL_LEFT:
	 result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
	 break;
	 case SCROLL_DOWN:
	 case SCROLL_RIGHT:
	 result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
	 break;
	 }

	 targetNode.recycle();
	 return result;
	 } else {
	 Toast.makeText(this, "未找到可滚动的控件", Toast.LENGTH_SHORT).show();
	 return false;
	 }
	 }
	 */


	/**
	 * 递归查找所有可滚动节点
	 */
	private void findScrollableNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
		try {
			if (node == null) {
				return;
			}

			// 检查节点是否可滚动
			if (node.isScrollable()) {
				result.add(AccessibilityNodeInfo.obtain(node));
			}

			// 递归子节点
			int childCount = node.getChildCount();
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo child = node.getChild(i);
				if (child != null) {
					findScrollableNodes(child, result);
					child.recycle();
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error finding scrollable nodes", e);
		}
	}

	/**
	 * 查找最适合滚动的节点（最靠近鼠标位置的）
	 */
	private AccessibilityNodeInfo findBestScrollableNode(List<AccessibilityNodeInfo> nodes) {
		if (mouseManager == null || nodes.isEmpty()) {
			return null;
		}

		AccessibilityNodeInfo bestMatch = null;
		int minDistance = Integer.MAX_VALUE;
		int mouseX = mouseManager.getMouseX();
		int mouseY = mouseManager.getMouseY();

		for (AccessibilityNodeInfo node : nodes) {
			Rect bounds = new Rect();
			node.getBoundsInScreen(bounds);

			// 计算节点中心点到鼠标点的距离
			int centerX = bounds.centerX();
			int centerY = bounds.centerY();
			int distance = (int) Math.sqrt(Math.pow(centerX - mouseX, 2) + Math.pow(centerY - mouseY, 2));

			// 选择距离最小的节点
			if (distance < minDistance) {
				minDistance = distance;
				if (bestMatch != null) {
					bestMatch.recycle();
				}
				bestMatch = AccessibilityNodeInfo.obtain(node);
			}
		}

		// 回收不需要的节点
		for (AccessibilityNodeInfo node : nodes) {
			if (node != bestMatch) {
				node.recycle();
			}
		}

		return bestMatch;
	}
	
	
	
	
	
	

	//清除所有绘制的绿框
	public void clearHighlight(){
		if(mouseManager != null){
			mouseManager.removehighlight();
		}
	}

	//设置点击时间
	public void setShortClickTime(int dur){
		this.short_click_time = dur;
	}

	
	public void setLongClickTime(int dur){
		this.long_click_time = dur;
	}
	
	public void setScrollTime(int dur){
		this.SCROLL_DURATION = dur;
	}
	
	public void setScrollDisUD(int dis){
		this.SCROLL_DISTANCE_UD = dis;
	}
	
	public void setScrollDisLR(int dis){
		this.SCROLL_DISTANCE_LR = dis;
	}
	
	
	//设置触摸注入点击时间
	public void setShortClickTimeMgr(int dur){
		this.short_click_time_mgr = dur;
	}


	public void setLongClickTimeMgr(int dur){
		this.long_click_time_mgr = dur;
	}

	public void setScrollTimeMgr(int dur){
		this.SCROLL_DURATION_MGR = dur;
	}

	public void setScrollDisUDMgr(int dis){
		this.SCROLL_DISTANCE_UD_MGR = dis;
	}

	public void setScrollDisLRMgr(int dis){
		this.SCROLL_DISTANCE_LR_MGR = dis;
	}
	
	
}
