package com.charlie.rv.viewholder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Charlie on 2018/4/19.
 * Description:每种Item的基类。
 *      a 使用时需要配合XViewHolder(Adapter的泛型传XViewHolder)
 *      b 为了实现OnItemClickListener，子类需对相关View调用setOnClickListener(this)
 *
 * Version:1.0
 * Revised:null
 * Email:xu0xyc@outlook.com
 */
public abstract class BaseItemHolder<T> implements View.OnClickListener {

    protected Context mContext;
    private XViewHolder mXViewHolder;
    private OnItemClickListener mOnItemClickListener;

    public BaseItemHolder(Context context, ViewGroup parent) {
        mContext = context;
        mXViewHolder = new XViewHolder(createView(parent));
        mXViewHolder.setItemHolder(this);
    }

    public XViewHolder getXViewHolder() {
        return mXViewHolder;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public void onClick(View view) {
        if (null != mOnItemClickListener) {
            mOnItemClickListener.onItemClick(mXViewHolder, view);
        }
    }

    protected abstract View createView(ViewGroup parent);

    public abstract void bindView(T data);

}
