package com.charlie.rv.viewholder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Charlie on 2018/4/19.
 * Description:自定义的ViewHolder
 * Version:1.0
 * Revised:null
 * Email:xu0xyc@outlook.com
 */
public class XViewHolder extends RecyclerView.ViewHolder {

    private BaseItemHolder mItemHolder;

    XViewHolder(View itemView) {
        super(itemView);
    }

    void setItemHolder(BaseItemHolder itemHolder) {
        mItemHolder = itemHolder;
    }

    public BaseItemHolder getItemHolder() {
        return mItemHolder;
    }
}
