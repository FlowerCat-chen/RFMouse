// SectionAdapter.java
package com.flowercat.rfmouse.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import com.flowercat.rfmouse.R;

public class SectionAdapter extends ArrayAdapter<SectionItem> { // 泛型改为 SectionItem

    private Context mContext;
    private int mResource;
	private float cornerRadiusPx; // 圆角半径，现在存储像素值
	
	
    public SectionAdapter(Context context, int resource, List<SectionItem> objects) { // 接收 SectionItem 列表
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
		
		// 获取屏幕密度信息
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        // 将 8dp 转换为对应的像素值
        this.cornerRadiusPx = 8 * displayMetrics.density;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取当前板块项的数据
        SectionItem sectionItem = getItem(position);

        ViewHolder holder;
        if (convertView == null) {
            // 如果视图是第一次创建，则加载布局
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);

            holder = new ViewHolder();
            holder.titleTextView = (TextView) convertView.findViewById(R.id.section_title_text_view);
            holder.arrowTextView = (TextView) convertView.findViewById(R.id.tv_arrow);
			
            convertView.setTag(holder); // 将ViewHolder存储在convertView的tag中
        } else {
            // 如果视图已经存在（复用），则直接取出ViewHolder
            holder = (ViewHolder) convertView.getTag();
        }
		

        // 设置文本和背景颜色
        if (sectionItem != null) {
            holder.titleTextView.setText(sectionItem.getTitle());
			// 1. 创建一个新的 GradientDrawable
			GradientDrawable shapeDrawable = new GradientDrawable();
			// 2. 设置形状为矩形 (默认就是矩形，也可以显式设置)
			shapeDrawable.setShape(GradientDrawable.RECTANGLE);
			// 3. 设置背景颜色
			shapeDrawable.setColor(sectionItem.getBackgroundColor());
			// 4. 设置圆角半径
			shapeDrawable.setCornerRadius(cornerRadiusPx);
			// 5. 将这个动态创建的Drawable设置为视图的背景
			holder.titleTextView.setBackground(shapeDrawable);
            holder.titleTextView.setTextColor(sectionItem.getTextColor());
        }

        return convertView;
    }

    // ViewHolder类：用于缓存视图组件
    private static class ViewHolder {
        TextView titleTextView;
        TextView arrowTextView;
    }
}
