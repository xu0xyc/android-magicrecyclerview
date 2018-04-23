package com.charlie.magicrecyclerview.sample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.charlie.rv.viewholder.BaseItemHolder;

/**
 * Created by Charlie on 2018/4/19.
 * Description:
 * Version:1.0
 * Revised:null
 * Email:xu0xyc@outlook.com
 */
public class SimpleItemHolder extends BaseItemHolder<String> {

    private static final int DEFAULT_ITEM_VIEW_RESID = R.layout.rv_default_item_view;
    private TextView mTextView;
    private Button mButton;

    public SimpleItemHolder(Context context, ViewGroup parent) {
        super(context, parent);
    }

    @Override
    protected View createView(ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(DEFAULT_ITEM_VIEW_RESID, parent, false);
        mTextView = (TextView) view.findViewById(R.id.tv_title);
        mButton = (Button) view.findViewById(R.id.button);
        view.setOnClickListener(this);
        mButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void bindView(String data) {
        mTextView.setText(data);
    }
}
