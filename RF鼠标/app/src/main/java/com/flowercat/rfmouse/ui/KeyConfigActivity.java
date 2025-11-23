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
import com.flowercat.rfmouse.util.KeyCodeUtil;

public class KeyConfigActivity extends Activity {

    private ListView listView;
    private Button addButton;
    private KeyActionAdapter adapter;
    private List<KeyAction> keyActionList;
    public Button exitButton, setDefaultButton;

    // 默认的功能列表
    private final String[] actionOptions = {"默认","无操作","系统按键一次","鼠标短按", "鼠标长按", "模式切换","点击菜单", "鼠标上移/上滑", "鼠标下移/下滑", "鼠标左移/左滑(*)", "鼠标右移/右滑(*)","鼠标加速上移", "鼠标加速下移", "鼠标加速左移", "鼠标加速右移", "鼠标处上滑", "鼠标处下滑", "鼠标处左滑(*)", "鼠标处右滑(*)", "显示/隐藏鼠标", "允许/禁止截屏", "进入/退出调试模式", "紧急禁用鼠标",/*原来A3+按键映射的内容*/"本应用主页","返回", "主页", "最近任务", "电源框", "展开通知栏","截屏","显示录屏界面","锁屏","拨号界面","音量加","音量减","一键静音","注入退格","注入*","音量增强界面","打开系统设置","飞行模式","打开流量","关闭流量","切换流量","打开wifi","关闭wifi","切换wifi","热点界面","打开键盘灯","关闭键盘灯","切换键盘灯"};

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
		//call.setLongPressAction("紧急禁用鼠标");
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

		KeyAction jing = new KeyAction("按键'#'");
		jing.setShortPressAction("默认");
		jing.setLongPressAction("紧急禁用鼠标");
		keyActionList.add(jing);

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
						input.setText(KeyCodeUtil.getKeyNameFromCode(keycode));
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
            if(String.valueOf(KeyCodeUtil.getKeyCodeFromName(actions.getKeyName())).equals(keycode)){
                return true;
            }
        }
        return false;
    }




	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}




}

