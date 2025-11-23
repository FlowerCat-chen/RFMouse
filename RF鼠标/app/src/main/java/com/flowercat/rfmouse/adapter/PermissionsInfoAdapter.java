package com.flowercat.rfmouse.adapter;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;
import com.flowercat.rfmouse.R;

public class PermissionsInfoAdapter extends BaseAdapter {

    private Context context;
    private List<String> permissions;
    private LayoutInflater inflater;

    public PermissionsInfoAdapter(Context context, List<String> permissions) {
        this.context = context;
        this.permissions = permissions;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return permissions.size();
    }

    @Override
    public Object getItem(int position) {
        return permissions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.tool_permitem, parent, false);
        }

        TextView permissionNameTextView = convertView.findViewById(R.id.permission_name);
        TextView permissionDescTextView = convertView.findViewById(R.id.permission_description);

        String fullPermissionString = permissions.get(position);
        String[] parts = fullPermissionString.split("@@");
        String name = parts[0];
        String description = parts.length > 1 ? parts[1] : "No description available";

        permissionNameTextView.setText(name);
        permissionDescTextView.setText(description);

        // 检查并标红特殊权限
        if (name.contains("SYSTEM_ALERT_WINDOW") ||
            name.contains("BIND_DEVICE_ADMIN") ||
            name.contains("BIND_ACCESSIBILITY_SERVICE")) {
            permissionNameTextView.setTextColor(Color.RED);
            permissionDescTextView.setTextColor(Color.RED);
        } else {
            permissionNameTextView.setTextColor(Color.BLACK);
            permissionDescTextView.setTextColor(Color.BLACK);
        }

        return convertView;
    }
}
