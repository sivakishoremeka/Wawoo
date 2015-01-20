package com.wawoo.adapter;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.wawoo.data.ServiceDatum;
import com.wawoo.mobile.R;

public class ChannelGridViewAdapter extends BaseAdapter {
	private List<ServiceDatum> channelList;
	private LayoutInflater inflater;
	private Context mContext;

	public ChannelGridViewAdapter(List<ServiceDatum> channelList,
			Activity context) {
		this.channelList = channelList;
		inflater = LayoutInflater.from(context);
		this.mContext = context;
	}

	@Override
	public int getCount() {

		return channelList.size();
	}

	@Override
	public Object getItem(int position) {

		return channelList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView==null){
			holder = new ViewHolder();
			convertView =  inflater.inflate(
				R.layout.ch_gridview_item, parent,false);
		   holder.image = ((ImageView) convertView.findViewById(R.id.ch_gv_item_img));
		   int imgSize = mContext.getResources().getInteger(R.integer.ch_img_size);
			holder.image.setLayoutParams(new GridView.LayoutParams(imgSize,imgSize));
		   convertView.setTag(holder);
		}
	else{
		holder = (ViewHolder) convertView.getTag();
	}
		ServiceDatum data = channelList.get(position);
		 holder.image.setPadding(2, 2, 2, 2);
		com.nostra13.universalimageloader.core.ImageLoader.getInstance()
				.displayImage(data.getImage(), holder.image);
		return convertView;
	}
	
	class ViewHolder{
		ImageView image;
	} 
}
