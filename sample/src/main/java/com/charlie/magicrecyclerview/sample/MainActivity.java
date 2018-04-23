package com.charlie.magicrecyclerview.sample;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.charlie.rv.viewholder.OnItemClickListener;
import com.charlie.rv.viewholder.XViewHolder;
import com.charlie.widget.MagicRecyclerView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] titiles = {"放大额额放大额额放大额额放大额额", "批量生产批量生产批量生产批量生产批量生产", "骑虎难下骑虎难下骑虎难下骑虎难下骑虎难下", "意气风发方法意气风发方法意气风发方法意气风发方法", "蓝色位置啧啧啧蓝色位置啧啧啧蓝色位置啧啧啧", "发发发方法发发发方法发发发方法发发发方法", "房间爱非健康发发大姐夫", "放假案件多发发发积分拉"};

    private MagicRecyclerView mMagicRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.hide();
        }

        mMagicRecyclerView = (MagicRecyclerView) findViewById(R.id.gly_recycler_view);
        mMagicRecyclerView.setMarginOffset(10);
        mMagicRecyclerView.setMinScale(1.0f);
        mMagicRecyclerView.setFrictionFactor(0.3f);
        mMagicRecyclerView.setMax3DRotate(-20);
        mMagicRecyclerView.scrollToPosition(5);

        mAdapter = new SimpleAdapter(this, Arrays.asList(titiles), this);
        mMagicRecyclerView.setAdapter(mAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public void onItemClick(XViewHolder viewHolder, View view) {
        int layoutPosition = viewHolder.getLayoutPosition();
        Toast.makeText(this, "layoutPosition:" + layoutPosition + "\nview:" + view, Toast.LENGTH_SHORT).show();
    }
}
