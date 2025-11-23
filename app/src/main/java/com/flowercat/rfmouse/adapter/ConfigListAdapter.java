package com.flowercat.rfmouse.adapter;

// ConfigListAdapter.java

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import com.flowercat.rfmouse.R;
import java.util.List;

public class ConfigListAdapter extends BaseAdapter {
    private Context context;
    private List<ConfigItem> configList;
    private LayoutInflater inflater;
    private OnConfigActionListener listener;

    public interface OnConfigActionListener {
        void onConfigSelected(int position);
        void onConfigExported(int position);
        void onConfigTouched(int position);
    }

    public ConfigListAdapter(Context context, List<ConfigItem> configList, OnConfigActionListener listener) {
        this.context = context;
        this.configList = configList;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return configList.size();
    }

    @Override
    public Object getItem(int position) {
        return configList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
	
	public void touchItem(int position){
		if (listener != null) {
			listener.onConfigTouched(position);
		}
	}
	
	public void exportItem(int position){
		if (listener != null) {
			listener.onConfigExported(position);
		}
	}
	
	
	public void selectCurrentConfig(int position){
		
	}
	

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.config_list_item, parent, false);
            holder = new ViewHolder();
            holder.radioButton = (RadioButton) convertView.findViewById(R.id.radio_config);
            holder.tvName = (TextView) convertView.findViewById(R.id.tv_config_name);
            holder.tvInfo = (TextView) convertView.findViewById(R.id.tv_config_info);
            holder.btnExport = (Button) convertView.findViewById(R.id.btn_export);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final ConfigItem item = configList.get(position);


        // 设置数据
        holder.tvName.setText(item.getConfigName());
        holder.tvInfo.setText(item.getFormattedFileSize() + " - " + item.getFormattedTime());
        // 关键：这里只根据数据模型设置 RadioButton 的状态，不根据点击立即改变
        holder.radioButton.setChecked(item.isSelected()); 

        // 单选按钮点击事件
        holder.radioButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
                        // 关键修改：调用 onConfigSelected()，但不要在这里改变 item.isSelected() 或 RadioButton 的状态
                        // 状态的改变和列表刷新将由 ConfigManagerActivity 在用户确认后处理
						listener.onConfigSelected(position); 
					}

                    // 阻止 RadioButton 立即响应点击而改变状态
                    // 因为 RadioButton 的 setOnClickListener 默认不会阻止其setChecked()的副作用，
                    // 最安全的方法是让 Activity 来驱动状态变化。
                    // 我们可以通过在 Activity 的 onConfigSelected 内部强制设置 RadioButton 的状态来实现控制。
                    // 但是，如果 RadioButton 是可点击的，它会自行切换状态。

                    // 更好且更简单的方法是：
                    // 在调用 listener 之后，将 RadioButton 强制设置为它应该保持的状态（即 item.isSelected()）
                    holder.radioButton.setChecked(item.isSelected());
				}
			});

        // 导出按钮点击事件
        holder.btnExport.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listener != null) {
						listener.onConfigExported(position);
					}
				}
			});

        // 长按删除
		/*
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (listener != null) {
						listener.onConfigDeleted(position);
					}
					return true;
				}
			});
			
			*/

        return convertView;
    }

    private static class ViewHolder {
        RadioButton radioButton;
        TextView tvName;
        TextView tvInfo;
        Button btnExport;
    }

    public void updateList(List<ConfigItem> newList) {
        this.configList = newList;
        notifyDataSetChanged();
    }
}


