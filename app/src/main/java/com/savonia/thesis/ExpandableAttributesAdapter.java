package com.savonia.thesis;

import android.content.Context;
import android.graphics.Typeface;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;


public class ExpandableAttributesAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> servicesList;
    private HashMap<String, List<String>> characteristicsList;

    public ExpandableAttributesAdapter(Context context, List<String> servicesList,
                                 HashMap<String, List<String>> characteristicsList) {
        this.context = context;
        this.servicesList = servicesList;
        this.characteristicsList = characteristicsList;
    }


    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this.characteristicsList.get(this.servicesList.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater lInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = lInflater.inflate(R.layout.attribute_item, null);
        }

        TextView characteristicName = (TextView) convertView
                .findViewById(R.id.attributeTextView);

        characteristicName.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.characteristicsList.get(this.servicesList.get(groupPosition))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.servicesList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.servicesList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        String headerTitle = (String) getGroup(groupPosition);

        // implementing ViewHolder design pattern to increase performance
        if (convertView == null) {
            LayoutInflater lInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = lInflater.inflate(R.layout.services_item, null);
            viewHolder = new ViewHolder();
            viewHolder.serviceName = (TextView) convertView.findViewById(R.id.serviceName);
            viewHolder.groupIndicator = (ImageView) convertView.findViewById(R.id.groupIndicator);
            viewHolder.serviceName.setText(headerTitle);
            viewHolder.groupIndicator.setSelected(isExpanded);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    class ViewHolder {
        TextView serviceName;
        ImageView groupIndicator;
    }
}

