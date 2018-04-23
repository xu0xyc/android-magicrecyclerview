package com.charlie.magicrecyclerview.sample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.charlie.rv.viewholder.BaseItemHolder;
import com.charlie.rv.viewholder.OnItemClickListener;
import com.charlie.rv.viewholder.XViewHolder;

import java.util.List;

/**
 * Created by Charlie on 2018/4/3.
 * Description:
 * Version:1.0
 * Revised:null
 * Email:xu0xyc@outlook.com
 */

public class SimpleAdapter extends RecyclerView.Adapter<XViewHolder> {

    private Context mContext;
    private List<String> mDatas;
    private OnItemClickListener mOnItemClickListener;

    public SimpleAdapter(Context context, List<String> datas, OnItemClickListener listener) {
        mContext = context;
        mDatas = datas;
        mOnItemClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public XViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SimpleItemHolder simpleItemHolder = new SimpleItemHolder(mContext, parent);
        simpleItemHolder.setOnItemClickListener(mOnItemClickListener);
        return simpleItemHolder.getXViewHolder();
    }

    @Override
    public void onBindViewHolder(XViewHolder holder, int position) {
        String title = mDatas.get(position);
        BaseItemHolder itemHolder = holder.getItemHolder();
        itemHolder.bindView(title);
    }

    @Override
    public int getItemCount() {
        return null == mDatas ? 0 : mDatas.size();
    }


}
