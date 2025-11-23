package com.flowercat.rfmouse.service;

// QuickSettingsTileService.java
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.MouseMainActivity;

public class QuickSettingsTileService extends TileService {

    private static final String TAG = "QuickSettingsTileService";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

	
	@Override
	public void onStopListening() {
		super.onStopListening();
	}
	
	
	

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile != null) {
            // 设置图块状态
            tile.setState(Tile.STATE_ACTIVE);

            // 设置图标（使用矢量图标更美观）
            Icon icon = Icon.createWithResource(this, R.drawable.ic_launcher);
            tile.setIcon(icon);

            // 设置标签
            tile.setLabel("鼠标保活");
            tile.setContentDescription("保活工具");

            // 更新图块
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();

        // 点击图块时的操作
        Intent intent = new Intent(this, MouseMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);

        // 更新图块状态
        updateTileState();
    }
}
