package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.key.KeyAction;
import com.flowercat.rfmouse.key.KeyActionAdapter;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.service.KeyRecordListener;
import com.flowercat.rfmouse.util.SPUtil;
import java.util.List;
import android.content.Context;
import android.view.KeyEvent;
import android.content.Intent;

public class KeyConfigActivity extends Activity {

    private ListView listView;
    private Button addButton;
    private KeyActionAdapter adapter;
    private List<KeyAction> keyActionList;
    public Button exitButton, setDefaultButton;

    // 默认的功能列表
    private final String[] actionOptions = {"默认","无操作","系统按键一次","鼠标短按", "鼠标长按", "模式切换","点击菜单", "鼠标上移/上滑", "鼠标下移/下滑", "鼠标左移/左滑(*)", "鼠标右移/右滑(*)","鼠标加速上移", "鼠标加速下移", "鼠标加速左移", "鼠标加速右移", "鼠标处上滑", "鼠标处下滑", "鼠标处左滑(*)", "鼠标处右滑(*)", "显示/隐藏鼠标", "允许/禁止截屏", "进入/退出调试模式", "紧急禁用鼠标",/*原来A3+按键映射的内容*/"本应用主页","返回", "主页", "最近任务", "电源框", "展开通知栏","截屏","显示录屏界面","锁屏","拨号界面","音量加","音量减","一键静音","注入退格","注入*","音量增强界面","打开系统设置","飞行模式","打开流量","关闭流量","切换流量","打开wifi","关闭wifi","切换wifi","热点界面","打开键盘灯","关闭键盘灯"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
							 WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.key_config);

        listView = findViewById(R.id.keylist);
        addButton = findViewById(R.id.addButton);
        exitButton = findViewById(R.id.exitButton);
        setDefaultButton = findViewById(R.id.setDefaultButton);

        keyActionList = SPUtil.loadData();
        setupListView();
        setupAddButton();
		
    }

    private void setupListView() {
        adapter = new KeyActionAdapter(this, keyActionList, new KeyActionAdapter.OnActionClickListener() {
				@Override
				public void onActionClick(int position, boolean isShortPress) {
					showActionSelectionDialog(position, isShortPress);
				}
			});
        listView.setAdapter(adapter);

        // 处理列表项点击事件，展开或收起
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					adapter.setExpandedPosition(position);
				}
			});

        // 添加长按事件监听器
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					showPressTypeSelectionDialog(position);
					return true; // 返回true表示已经处理了长按事件，不再触发普通点击事件
				}
			});
    }

    private void setupAddButton() {
        addButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showAddKeyDialog();
				}
			});

        exitButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
					overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
				}
			});

        setDefaultButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					//setDefaultKeyFunction();
					showRestoreConfirmationDialog();
				}
			});
    }

	
	
	
	// 显示恢复默认对话框
    private void showRestoreConfirmationDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("确定恢复默认？")
			.setMessage("您添加的所有按键，以及自定义功能都将被重置。")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setDefaultKeyFunction();
					//Toast.makeText(KeyConfigActivity.this, "已删除", Toast.LENGTH_SHORT).show();
				}
			})
			.setNegativeButton("取消", null);

		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dialog = builder.show();

		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
			negativeBtn.setLayoutParams(negativeParams);
			negativeBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//negativeBtn.setPadding(30, 10, 30, 10); // 可选：同确认按钮，保持样式一致
		}

    }
	
	
	

    //将所有的按键功能设回默认
    public void setDefaultKeyFunction() {
        if (keyActionList != null) {
            keyActionList.clear();
        }
		// 初始数据
		KeyAction menu = new KeyAction("菜单键");
		menu.setLongPressAction("模式切换");
		keyActionList.add(menu);

		KeyAction back = new KeyAction("返回键");
		back.setLongPressAction("最近任务");
		keyActionList.add(back);

		KeyAction ok = new KeyAction("回车键");
		ok.setShortPressAction("鼠标短按");
		ok.setLongPressAction("点击菜单");
		keyActionList.add(ok);

		KeyAction up = new KeyAction("导航键-向上");
		up.setShortPressAction("鼠标上移/上滑");
		up.setLongPressAction("鼠标加速上移");
		keyActionList.add(up);

		KeyAction down = new KeyAction("导航键-向下");
		down.setShortPressAction("鼠标下移/下滑");
		down.setLongPressAction("鼠标加速下移");
		keyActionList.add(down);

		KeyAction left = new KeyAction("导航键-向左");
		left.setShortPressAction("鼠标左移/左滑(*)");
		left.setLongPressAction("鼠标加速左移");
		keyActionList.add(left);

		KeyAction right = new KeyAction("导航键-向右");
		right.setShortPressAction("鼠标右移/右滑(*)");
		right.setLongPressAction("鼠标加速右移");
		keyActionList.add(right);

		KeyAction call = new KeyAction("拨号键");
		call.setLongPressAction("紧急禁用鼠标");
		keyActionList.add(call);

		keyActionList.add(new KeyAction("音量增加键"));
		keyActionList.add(new KeyAction("音量减小键"));
		keyActionList.add(new KeyAction("按键'0'"));
		keyActionList.add(new KeyAction("按键'1'"));
		keyActionList.add(new KeyAction("按键'2'"));
		keyActionList.add(new KeyAction("按键'3'"));
		keyActionList.add(new KeyAction("按键'4'"));
		keyActionList.add(new KeyAction("按键'5'"));
		keyActionList.add(new KeyAction("按键'6'"));
		keyActionList.add(new KeyAction("按键'7'"));
		keyActionList.add(new KeyAction("按键'8'"));
		keyActionList.add(new KeyAction("按键'9'"));
		keyActionList.add(new KeyAction("按键'#'"));
		keyActionList.add(new KeyAction("按键'*'"));
      
        SPUtil.saveData(keyActionList);
        adapter.notifyDataSetChanged();
        if(FlowerMouseService.getInstance() != null){
            FlowerMouseService.getInstance().updateKeyListeners(keyActionList);
        }
        Toast.makeText(KeyConfigActivity.this, "已重置完成", Toast.LENGTH_SHORT).show();
    }

    private void showAddKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加新按键");
        final EditText input = new EditText(this);
        input.setEnabled(false);
        input.setHint("请按下按键");
        builder.setView(input);
        if(FlowerMouseService.getInstance() != null){
            FlowerMouseService.getInstance().recordKeyPress(new KeyRecordListener(){

					@Override
					public void onKeyPress(int keycode) {
						input.setText(getKeyNameFromCode(keycode));
					}
				});
        }

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String keyName = input.getText().toString().trim();
					if (!keyName.isEmpty() && !isinActionList(keyName) &&!isinActionList("▶" + keyName) &&!isKeycodeinActionList(keyName) &&!isKeycodeinActionList("▶" + keyName)) {
						// 将新按键添加到列表的开头
						keyActionList.add(0, new KeyAction("▶" + keyName));
						adapter.notifyDataSetChanged();
						SPUtil.saveData(keyActionList);
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().updateKeyListeners(keyActionList);
						}
					} else {
						Toast.makeText(KeyConfigActivity.this, "空按键或重复添加", Toast.LENGTH_SHORT).show();
					}
				}
			});
        builder.setNegativeButton("取消", null);
        
		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dialog = builder.show();

		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字
			
		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
			negativeBtn.setLayoutParams(negativeParams);
			negativeBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//negativeBtn.setPadding(30, 10, 30, 10); // 可选：同确认按钮，保持样式一致
		}

    }
	
	

	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}
	

    // 新增方法：显示选择“短按”或“长按”或“删除”的对话框
    private void showPressTypeSelectionDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择功能类型");

        // 判断是否为用户添加的按键（通过键名判断）
        KeyAction keyAction = keyActionList.get(position);
        final boolean isUserAddedKey = keyAction.getKeyName().startsWith("▶");

        final String[] pressTypes;
        if (isUserAddedKey) {
            // 用户添加的按键有删除选项
            pressTypes = new String[]{"短按功能", "长按功能", "删除此项"};
        } else {
            // 默认按键没有删除选项
            pressTypes = new String[]{"短按功能", "长按功能"};
        }

        builder.setItems(pressTypes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0 || which == 1) { // 0代表短按，1代表长按
						boolean isShortPress = (which == 0);
						showActionSelectionDialog(position, isShortPress);
					} else if (which == 2 && isUserAddedKey) {
						// 点击了删除选项，且是用户添加的按键
						showDeleteConfirmationDialog(position);
					}
				}
			});
        builder.show();
    }

	
	
	
	
	/*
    private void showActionSelectionDialog(final int position, final boolean isShortPress) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择功能");
        builder.setItems(actionOptions, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String selectedAction = actionOptions[which];
					KeyAction keyAction = keyActionList.get(position);

					// 临时保存当前功能，以便在需要时回退
					String originalShortPressAction = keyAction.getShortPressAction();
					String originalLongPressAction = keyAction.getLongPressAction();

					// 临时设置新功能
					if (isShortPress) {
						keyAction.setShortPressAction(selectedAction);
					} else {
						keyAction.setLongPressAction(selectedAction);
					}
					// 检查整个列表是否还包含“模式切换”功能
					if (!isModeSwitchActionList()) {
						// 如果不包含，则回退更改并提示用户
						keyAction.setShortPressAction(originalShortPressAction);
						keyAction.setLongPressAction(originalLongPressAction);
						Toast.makeText(KeyConfigActivity.this, "至少需要保留一个“模式切换”功能", Toast.LENGTH_SHORT).show();
					} else {
						// 如果包含，则保存更改
						adapter.notifyDataSetChanged();
						SPUtil.saveData(keyActionList);
						if(FlowerMouseService.getInstance() != null){
							FlowerMouseService.getInstance().updateKeyListeners(keyActionList);
						}
					}
				}
			});
        builder.show();
    }
*/
	
	private void showActionSelectionDialog(final int position, final boolean isShortPress) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择功能");
        builder.setItems(actionOptions, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						String selectedAction = actionOptions[which];
						KeyAction keyAction = keyActionList.get(position);

						// 临时保存当前功能，以便在需要时回退
						String originalShortPressAction = keyAction.getShortPressAction();
						String originalLongPressAction = keyAction.getLongPressAction();

						// 统一处理逻辑：根据选择的功能自动设置对应的短按和长按
						switch (selectedAction) {
							case "鼠标上移/上滑":
							case "鼠标加速上移":
								keyAction.setShortPressAction("鼠标上移/上滑");
								keyAction.setLongPressAction("鼠标加速上移");
								break;
							case "鼠标下移/下滑":
							case "鼠标加速下移":
								keyAction.setShortPressAction("鼠标下移/下滑");
								keyAction.setLongPressAction("鼠标加速下移");
								break;
							case "鼠标左移/左滑(*)":
							case "鼠标加速左移":
								keyAction.setShortPressAction("鼠标左移/左滑(*)");
								keyAction.setLongPressAction("鼠标加速左移");
								break;
							case "鼠标右移/右滑(*)":
							case "鼠标加速右移":
								keyAction.setShortPressAction("鼠标右移/右滑(*)");
								keyAction.setLongPressAction("鼠标加速右移");
								break;
							default:
								// 如果选择的不是鼠标移动/加速功能，则只设置当前选择的功能
								if (isShortPress) {
									keyAction.setShortPressAction(selectedAction);
								} else {
									keyAction.setLongPressAction(selectedAction);
								}
								break;
						}

						// 检查整个列表是否还包含“模式切换”功能
						if (!isModeSwitchActionList()) {
							// 如果不包含，则回退更改并提示用户
							keyAction.setShortPressAction(originalShortPressAction);
							keyAction.setLongPressAction(originalLongPressAction);
							Toast.makeText(KeyConfigActivity.this, "至少需要保留一个“模式切换”功能", Toast.LENGTH_SHORT).show();
						} else {
							// 如果包含，则保存更改
							adapter.notifyDataSetChanged();
							SPUtil.saveData(keyActionList);
							if (FlowerMouseService.getInstance() != null) {
								FlowerMouseService.getInstance().updateKeyListeners(keyActionList);
							}
						}
					} catch (Exception e) {
						// 完善的异常处理
						Toast.makeText(KeyConfigActivity.this, "保存功能时发生错误：" + e.getMessage(), Toast.LENGTH_LONG).show();
						e.printStackTrace(); // 打印堆栈信息，便于调试
					}
				}
			});
        builder.show();
    }
	
    // 显示删除确认对话框
    private void showDeleteConfirmationDialog(final int position) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
			builder
			.setTitle("删除按键")
			.setMessage("确定要删除此按键吗？")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					keyActionList.remove(position);
					adapter.notifyDataSetChanged();
					SPUtil.saveData(keyActionList);
					if(FlowerMouseService.getInstance() != null){
						FlowerMouseService.getInstance().updateKeyListeners(keyActionList);
					}
					Toast.makeText(KeyConfigActivity.this, "已删除", Toast.LENGTH_SHORT).show();
				}
			})
			.setNegativeButton("取消", null);
			
		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dialog = builder.show();

		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
			negativeBtn.setLayoutParams(negativeParams);
			negativeBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//negativeBtn.setPadding(30, 10, 30, 10); // 可选：同确认按钮，保持样式一致
		}
			 
    }


    public boolean isModeSwitchActionList() {
        for (KeyAction action : keyActionList) {
            if ("模式切换".equals(action.getShortPressAction()) || "模式切换".equals(action.getLongPressAction())) {
                return true;
            }
        }
        return false;
    }

    public boolean isinActionList(String keyName) {
        for (KeyAction actions : keyActionList) {
            if(actions.getKeyName().equals(keyName)){
                return true;
            }
        }
        return false;
    }
	
	public boolean isKeycodeinActionList(String keycode) {
        for (KeyAction actions : keyActionList) {
            if(String.valueOf(getKeyCodeFromName(actions.getKeyName())).equals(keycode)){
                return true;
            }
        }
        return false;
    }
	
	
	/**
	 * 根据传入的 Keycode 返回对应的按键中文名称。
	 * 如果 Keycode 无法识别，返回 "KEYCODE_XXX" 格式的字符串。
	 */
	public String getKeyNameFromCode(int keyCode) {
		switch (keyCode) {
				// 电话/通用功能键
			case KeyEvent.KEYCODE_CALL:
				return "拨号键";
			case KeyEvent.KEYCODE_ENDCALL:
				return "挂机键";
			case KeyEvent.KEYCODE_HOME:
				return "主页键";
			case KeyEvent.KEYCODE_MENU:
				return "菜单键";
			case KeyEvent.KEYCODE_BACK:
				return "返回键";
			case KeyEvent.KEYCODE_SEARCH:
				return "搜索键";
			case KeyEvent.KEYCODE_CAMERA:
				return "拍照键";
			case KeyEvent.KEYCODE_FOCUS:
				return "拍照对焦键";
			case KeyEvent.KEYCODE_POWER:
				return "电源键";
			case KeyEvent.KEYCODE_NOTIFICATION:
				return "通知键";
			case KeyEvent.KEYCODE_MUTE:
				return "话筒静音键";
			case KeyEvent.KEYCODE_VOLUME_MUTE:
				return "扬声器静音键";
			case KeyEvent.KEYCODE_VOLUME_UP:
				return "音量增加键";
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return "音量减小键";
			case KeyEvent.KEYCODE_APP_SWITCH:
				return "应用切换键";
			case KeyEvent.KEYCODE_SETTINGS:
				return "设置键";
			case KeyEvent.KEYCODE_HEADSETHOOK:
				return "耳机挂断键";

				// 导航/编辑键
			case KeyEvent.KEYCODE_ENTER:
				return "回车键";
			case KeyEvent.KEYCODE_ESCAPE:
				return "ESC键";
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return "导航键-确定";
			case KeyEvent.KEYCODE_DPAD_UP:
				return "导航键-向上";
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return "导航键-向下";
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return "导航键-向左";
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return "导航键-向右";
			case KeyEvent.KEYCODE_MOVE_HOME:
				return "光标移动到开始";
			case KeyEvent.KEYCODE_MOVE_END:
				return "光标移动到末尾";
			case KeyEvent.KEYCODE_PAGE_UP:
				return "向上翻页";
			case KeyEvent.KEYCODE_PAGE_DOWN:
				return "向下翻页";
			case KeyEvent.KEYCODE_DEL:
				return "退格键";
			case KeyEvent.KEYCODE_FORWARD_DEL:
				return "删除键";
			case KeyEvent.KEYCODE_INSERT:
				return "插入键";
			case KeyEvent.KEYCODE_TAB:
				return "Tab键";

				// 数字/字母键
			case KeyEvent.KEYCODE_0:
				return "按键'0'";
			case KeyEvent.KEYCODE_1:
				return "按键'1'";
			case KeyEvent.KEYCODE_2:
				return "按键'2'";
			case KeyEvent.KEYCODE_3:
				return "按键'3'";
			case KeyEvent.KEYCODE_4:
				return "按键'4'";
			case KeyEvent.KEYCODE_5:
				return "按键'5'";
			case KeyEvent.KEYCODE_6:
				return "按键'6'";
			case KeyEvent.KEYCODE_7:
				return "按键'7'";
			case KeyEvent.KEYCODE_8:
				return "按键'8'";
			case KeyEvent.KEYCODE_9:
				return "按键'9'";
			case KeyEvent.KEYCODE_A:
				return "按键'A'";
			case KeyEvent.KEYCODE_B:
				return "按键'B'";
			case KeyEvent.KEYCODE_C:
				return "按键'C'";
			case KeyEvent.KEYCODE_D:
				return "按键'D'";
			case KeyEvent.KEYCODE_E:
				return "按键'E'";
			case KeyEvent.KEYCODE_F:
				return "按键'F'";
			case KeyEvent.KEYCODE_G:
				return "按键'G'";
			case KeyEvent.KEYCODE_H:
				return "按键'H'";
			case KeyEvent.KEYCODE_I:
				return "按键'I'";
			case KeyEvent.KEYCODE_J:
				return "按键'J'";
			case KeyEvent.KEYCODE_K:
				return "按键'K'";
			case KeyEvent.KEYCODE_L:
				return "按键'L'";
			case KeyEvent.KEYCODE_M:
				return "按键'M'";
			case KeyEvent.KEYCODE_N:
				return "按键'N'";
			case KeyEvent.KEYCODE_O:
				return "按键'O'";
			case KeyEvent.KEYCODE_P:
				return "按键'P'";
			case KeyEvent.KEYCODE_Q:
				return "按键'Q'";
			case KeyEvent.KEYCODE_R:
				return "按键'R'";
			case KeyEvent.KEYCODE_S:
				return "按键'S'";
			case KeyEvent.KEYCODE_T:
				return "按键'T'";
			case KeyEvent.KEYCODE_U:
				return "按键'U'";
			case KeyEvent.KEYCODE_V:
				return "按键'V'";
			case KeyEvent.KEYCODE_W:
				return "按键'W'";
			case KeyEvent.KEYCODE_X:
				return "按键'X'";
			case KeyEvent.KEYCODE_Y:
				return "按键'Y'";
			case KeyEvent.KEYCODE_Z:
				return "按键'Z'";

				// 符号键
			case KeyEvent.KEYCODE_PLUS:
				return "按键'+'";
			case KeyEvent.KEYCODE_MINUS:
				return "按键'-'";
			case KeyEvent.KEYCODE_STAR:
				return "按键'*'";
			case KeyEvent.KEYCODE_SLASH:
				return "按键'/'";
			case KeyEvent.KEYCODE_EQUALS:
				return "按键'='";
			case KeyEvent.KEYCODE_AT:
				return "按键'@'";
			case KeyEvent.KEYCODE_POUND:
				return "按键'#'";
			case KeyEvent.KEYCODE_APOSTROPHE:
				return "按键'' (单引号)";
			case KeyEvent.KEYCODE_BACKSLASH:
				return "按键'\\' (反斜杠)";
			case KeyEvent.KEYCODE_COMMA:
				return "按键','";
			case KeyEvent.KEYCODE_PERIOD:
				return "按键'.'";
			case KeyEvent.KEYCODE_LEFT_BRACKET:
				return "按键'['";
			case KeyEvent.KEYCODE_RIGHT_BRACKET:
				return "按键']'";
			case KeyEvent.KEYCODE_SEMICOLON:
				return "按键';'";
			case KeyEvent.KEYCODE_GRAVE:
				return "按键'`'";
			case KeyEvent.KEYCODE_SPACE:
				return "空格键";

				// 修饰键
			case KeyEvent.KEYCODE_ALT_LEFT:
				return "左Alt键";
			case KeyEvent.KEYCODE_ALT_RIGHT:
				return "右Alt键";
			case KeyEvent.KEYCODE_CTRL_LEFT:
				return "左Control键";
			case KeyEvent.KEYCODE_CTRL_RIGHT:
				return "右Control键";
			case KeyEvent.KEYCODE_SHIFT_LEFT:
				return "左Shift键";
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				return "右Shift键";

				// 多媒体键
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				return "多媒体键-播放";
			case KeyEvent.KEYCODE_MEDIA_STOP:
				return "多媒体键-停止";
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				return "多媒体键-暂停";
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				return "多媒体键-播放/暂停";
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				return "多媒体键-快进";
			case KeyEvent.KEYCODE_MEDIA_REWIND:
				return "多媒体键-快退";
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				return "多媒体键-下一首";
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				return "多媒体键-上一首";
			case KeyEvent.KEYCODE_MEDIA_CLOSE:
				return "多媒体键-关闭";
			case KeyEvent.KEYCODE_MEDIA_EJECT:
				return "多媒体键-弹出";
			case KeyEvent.KEYCODE_MEDIA_RECORD:
				return "多媒体键-录音";

				// 游戏手柄键
			case KeyEvent.KEYCODE_BUTTON_A:
				return "游戏手柄按钮-A";
			case KeyEvent.KEYCODE_BUTTON_B:
				return "游戏手柄按钮-B";
			case KeyEvent.KEYCODE_BUTTON_X:
				return "游戏手柄按钮-X";
			case KeyEvent.KEYCODE_BUTTON_Y:
				return "游戏手柄按钮-Y";
				// ... (根据需要添加更多游戏手柄键)
			default:
				return String.valueOf(keyCode);
		}
	}
	
	

	/**
	 * 根据传入的按键中文名称返回对应的 Keycode。
	 * 如果名称无法识别，返回 -1。
	 * @param keyName 要查询的按键中文名称
	 * @return 对应的 Keycode 或 -1
	 */
	public int getKeyCodeFromName(String keyName) {
		if (keyName == null || keyName.isEmpty()) {
			return -1; // 输入为空，返回无效键值
		}

		// 检查是否是用户添加的键值
		if (keyName.startsWith("▶")) {
			keyName = keyName.replace("▶", "");
		}

		switch (keyName) {
				// 电话/通用功能键
			case "拨号键":
				return KeyEvent.KEYCODE_CALL;
			case "挂机键":
				return KeyEvent.KEYCODE_ENDCALL;
			case "主页键":
				return KeyEvent.KEYCODE_HOME;
			case "菜单键":
				return KeyEvent.KEYCODE_MENU;
			case "返回键":
				return KeyEvent.KEYCODE_BACK;
			case "搜索键":
				return KeyEvent.KEYCODE_SEARCH;
			case "拍照键":
				return KeyEvent.KEYCODE_CAMERA;
			case "拍照对焦键":
				return KeyEvent.KEYCODE_FOCUS;
			case "电源键":
				return KeyEvent.KEYCODE_POWER;
			case "通知键":
				return KeyEvent.KEYCODE_NOTIFICATION;
			case "话筒静音键":
				return KeyEvent.KEYCODE_MUTE;
			case "扬声器静音键":
				return KeyEvent.KEYCODE_VOLUME_MUTE;
			case "音量增加键":
				return KeyEvent.KEYCODE_VOLUME_UP;
			case "音量减小键":
				return KeyEvent.KEYCODE_VOLUME_DOWN;
			case "应用切换键":
				return KeyEvent.KEYCODE_APP_SWITCH;
			case "设置键":
				return KeyEvent.KEYCODE_SETTINGS;
			case "耳机挂断键":
				return KeyEvent.KEYCODE_HEADSETHOOK;

				// 导航/编辑键
			case "回车键":
				return KeyEvent.KEYCODE_ENTER;
			case "ESC键":
				return KeyEvent.KEYCODE_ESCAPE;
			case "导航键-确定":
				return KeyEvent.KEYCODE_DPAD_CENTER;
			case "导航键-向上":
				return KeyEvent.KEYCODE_DPAD_UP;
			case "导航键-向下":
				return KeyEvent.KEYCODE_DPAD_DOWN;
			case "导航键-向左":
				return KeyEvent.KEYCODE_DPAD_LEFT;
			case "导航键-向右":
				return KeyEvent.KEYCODE_DPAD_RIGHT;
			case "光标移动到开始":
				return KeyEvent.KEYCODE_MOVE_HOME;
			case "光标移动到末尾":
				return KeyEvent.KEYCODE_MOVE_END;
			case "向上翻页":
				return KeyEvent.KEYCODE_PAGE_UP;
			case "向下翻页":
				return KeyEvent.KEYCODE_PAGE_DOWN;
			case "退格键":
				return KeyEvent.KEYCODE_DEL;
			case "删除键":
				return KeyEvent.KEYCODE_FORWARD_DEL;
			case "插入键":
				return KeyEvent.KEYCODE_INSERT;
			case "Tab键":
				return KeyEvent.KEYCODE_TAB;

				// 数字/字母键
			case "按键'0'":
				return KeyEvent.KEYCODE_0;
			case "按键'1'":
				return KeyEvent.KEYCODE_1;
			case "按键'2'":
				return KeyEvent.KEYCODE_2;
			case "按键'3'":
				return KeyEvent.KEYCODE_3;
			case "按键'4'":
				return KeyEvent.KEYCODE_4;
			case "按键'5'":
				return KeyEvent.KEYCODE_5;
			case "按键'6'":
				return KeyEvent.KEYCODE_6;
			case "按键'7'":
				return KeyEvent.KEYCODE_7;
			case "按键'8'":
				return KeyEvent.KEYCODE_8;
			case "按键'9'":
				return KeyEvent.KEYCODE_9;
			case "按键'A'":
				return KeyEvent.KEYCODE_A;
			case "按键'B'":
				return KeyEvent.KEYCODE_B;
			case "按键'C'":
				return KeyEvent.KEYCODE_C;
			case "按键'D'":
				return KeyEvent.KEYCODE_D;
			case "按键'E'":
				return KeyEvent.KEYCODE_E;
			case "按键'F'":
				return KeyEvent.KEYCODE_F;
			case "按键'G'":
				return KeyEvent.KEYCODE_G;
			case "按键'H'":
				return KeyEvent.KEYCODE_H;
			case "按键'I'":
				return KeyEvent.KEYCODE_I;
			case "按键'J'":
				return KeyEvent.KEYCODE_J;
			case "按键'K'":
				return KeyEvent.KEYCODE_K;
			case "按键'L'":
				return KeyEvent.KEYCODE_L;
			case "按键'M'":
				return KeyEvent.KEYCODE_M;
			case "按键'N'":
				return KeyEvent.KEYCODE_N;
			case "按键'O'":
				return KeyEvent.KEYCODE_O;
			case "按键'P'":
				return KeyEvent.KEYCODE_P;
			case "按键'Q'":
				return KeyEvent.KEYCODE_Q;
			case "按键'R'":
				return KeyEvent.KEYCODE_R;
			case "按键'S'":
				return KeyEvent.KEYCODE_S;
			case "按键'T'":
				return KeyEvent.KEYCODE_T;
			case "按键'U'":
				return KeyEvent.KEYCODE_U;
			case "按键'V'":
				return KeyEvent.KEYCODE_V;
			case "按键'W'":
				return KeyEvent.KEYCODE_W;
			case "按键'X'":
				return KeyEvent.KEYCODE_X;
			case "按键'Y'":
				return KeyEvent.KEYCODE_Y;
			case "按键'Z'":
				return KeyEvent.KEYCODE_Z;

				// 符号键
			case "按键'+'":
				return KeyEvent.KEYCODE_PLUS;
			case "按键'-'":
				return KeyEvent.KEYCODE_MINUS;
			case "按键'*'":
				return KeyEvent.KEYCODE_STAR;
			case "按键'/'":
				return KeyEvent.KEYCODE_SLASH;
			case "按键'='":
				return KeyEvent.KEYCODE_EQUALS;
			case "按键'@'":
				return KeyEvent.KEYCODE_AT;
			case "按键'#'":
				return KeyEvent.KEYCODE_POUND;
			case "按键'' (单引号)":
				return KeyEvent.KEYCODE_APOSTROPHE;
			case "按键'\\' (反斜杠)":
				return KeyEvent.KEYCODE_BACKSLASH;
			case "按键','":
				return KeyEvent.KEYCODE_COMMA;
			case "按键'.'":
				return KeyEvent.KEYCODE_PERIOD;
			case "按键'['":
				return KeyEvent.KEYCODE_LEFT_BRACKET;
			case "按键']'":
				return KeyEvent.KEYCODE_RIGHT_BRACKET;
			case "按键';'":
				return KeyEvent.KEYCODE_SEMICOLON;
			case "按键'`'":
				return KeyEvent.KEYCODE_GRAVE;
			case "空格键":
				return KeyEvent.KEYCODE_SPACE;

				// 修饰键
			case "左Alt键":
				return KeyEvent.KEYCODE_ALT_LEFT;
			case "右Alt键":
				return KeyEvent.KEYCODE_ALT_RIGHT;
			case "左Control键":
				return KeyEvent.KEYCODE_CTRL_LEFT;
			case "右Control键":
				return KeyEvent.KEYCODE_CTRL_RIGHT;
			case "左Shift键":
				return KeyEvent.KEYCODE_SHIFT_LEFT;
			case "右Shift键":
				return KeyEvent.KEYCODE_SHIFT_RIGHT;

				// 多媒体键
			case "多媒体键-播放":
				return KeyEvent.KEYCODE_MEDIA_PLAY;
			case "多媒体键-停止":
				return KeyEvent.KEYCODE_MEDIA_STOP;
			case "多媒体键-暂停":
				return KeyEvent.KEYCODE_MEDIA_PAUSE;
			case "多媒体键-播放/暂停":
				return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
			case "多媒体键-快进":
				return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
			case "多媒体键-快退":
				return KeyEvent.KEYCODE_MEDIA_REWIND;
			case "多媒体键-下一首":
				return KeyEvent.KEYCODE_MEDIA_NEXT;
			case "多媒体键-上一首":
				return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
			case "多媒体键-关闭":
				return KeyEvent.KEYCODE_MEDIA_CLOSE;
			case "多媒体键-弹出":
				return KeyEvent.KEYCODE_MEDIA_EJECT;
			case "多媒体键-录音":
				return KeyEvent.KEYCODE_MEDIA_RECORD;

				// 游戏手柄键
			case "游戏手柄按钮-A":
				return KeyEvent.KEYCODE_BUTTON_A;
			case "游戏手柄按钮-B":
				return KeyEvent.KEYCODE_BUTTON_B;
			case "游戏手柄按钮-X":
				return KeyEvent.KEYCODE_BUTTON_X;
			case "游戏手柄按钮-Y":
				return KeyEvent.KEYCODE_BUTTON_Y;
				// ... (根据需要添加更多游戏手柄键)
			default:
				try {
					return Integer.parseInt(keyName);
				} catch (NumberFormatException e) {
					return -1; // 格式不正确，返回无效键值
				}
		}
	}
	
	
	
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	
	
	
}
