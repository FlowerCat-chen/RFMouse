package com.flowercat.rfmouse.key;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import com.flowercat.rfmouse.R;


public class KeyActionAdapter extends BaseAdapter {

    private Context context;
    private List<KeyAction> keyActionList;
    private int expandedPosition = -1; // 记录当前展开的列表项位置
    private OnActionClickListener onActionClickListener;

    public interface OnActionClickListener {
        void onActionClick(int position, boolean isShortPress);
    }

    public KeyActionAdapter(Context context, List<KeyAction> keyActionList, OnActionClickListener listener) {
        this.context = context;
        this.keyActionList = keyActionList;
        this.onActionClickListener = listener;
    }

    public void setExpandedPosition(int position) {
        if (expandedPosition == position) {
            expandedPosition = -1; // 点击同一项则收起
        } else {
            expandedPosition = position;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return keyActionList.size();
    }

    @Override
    public Object getItem(int position) {
        return keyActionList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.key_list, parent, false);
            holder = new ViewHolder();
            holder.keyNameTextView = convertView.findViewById(R.id.keyNameTextView);
            holder.actionsLayout = convertView.findViewById(R.id.actionsLayout);
            holder.shortPressTextView = convertView.findViewById(R.id.shortPressTextView);
            holder.longPressTextView = convertView.findViewById(R.id.longPressTextView);
			holder.arrowIndicator = convertView.findViewById(R.id.arrow_indicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final KeyAction currentKey = keyActionList.get(position);
        holder.keyNameTextView.setText(currentKey.getKeyName());
        holder.shortPressTextView.setText("短按：" + currentKey.getShortPressAction());
        holder.longPressTextView.setText("长按：" + currentKey.getLongPressAction());

        // 控制折叠区域的显示与隐藏
        if (position == expandedPosition) {
            holder.actionsLayout.setVisibility(View.VISIBLE);
			holder.actionsLayout.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
            holder.arrowIndicator.setRotation(90);
			
        } else {
            holder.actionsLayout.setVisibility(View.GONE);
			holder.actionsLayout.getLayoutParams().height = 0;
            holder.arrowIndicator.setRotation(0);
        }

        // 短按点击事件
        holder.shortPressTextView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (onActionClickListener != null) {
						onActionClickListener.onActionClick(position, true);
					}
				}
			});

        // 长按点击事件
        holder.longPressTextView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (onActionClickListener != null) {
						onActionClickListener.onActionClick(position, false);
					}
				}
			});

        return convertView;
    }

    static class ViewHolder {
        TextView keyNameTextView;
        LinearLayout actionsLayout;
        TextView shortPressTextView;
        TextView longPressTextView;
		TextView arrowIndicator;
    }
}

