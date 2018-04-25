package com.charlie.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Charlie on 2017/8/20.
 * Description:
 *      基于RecyclerView实现，可以用于实现 Gallery效果、叠加效果、3D旋转等效果；
 *      内部默认使用LinearLayoutManager作为布局管理器，因此，外部只能选择布局方向，不能再调用RV的setLayoutManager(LayoutManager)方法；
 *      该View的宽高必须设置为match_parent或者精确值。
 * Version:1.0
 * Revised:null
 * Email:xu0xyc@outlook.com
 */
public class MagicRecyclerView extends RecyclerView {
    private static final String TAG = MagicRecyclerView.class.getSimpleName();

    private MagicRecyclerView.Orientation mOrientation = Orientation.HORIZONTAL;

    private LinearLayoutManager mLinearLayoutManager;
    private MagicRecyclerView.ChildViewMetrics mChildViewMetrics;
    private float mDensity;

    private int mMarginOffset = 0;// margin偏移(单位：dp)
    private float mMax3DRotate = 0;// 最大3D旋转角度(单位：度)
    private float mMinAlpha = 1.0f;// 最小的透明度[0,1]
    private float mMinScale = 0.0f;// 最小缩放大小[0,1]

    //摩擦系数(0,1]
    private float mFrictionFactor = 1.0f;
    // true只有中心那个child可点击，false所有item可点击
    private boolean isOnlyCenterChildClickable = true;
    // true点击某个Item之后，该Item移动到中间位置
    private boolean isTapItemToCenter = true;

    private int mSelectedPosition = -1;// 选中的位置
    private OnChildSelectedListener mOnChildSelectedListener;
    private VariousMarginOffsetInterf mVariousMarginOffsetInterf;
    private int mPendingPosition = -1;// 还不可见但是需要去的位置

    private boolean userScrolling;
    private boolean scrolling;
    private int scrollState = SCROLL_STATE_IDLE;

    private EdgeMargin mEdgeMargin;
    private Rect mHitRect;

    private OffsetTranslation m3DRotateOffsetTranslation = new Default3DRotateOffsetTranslation();
    private OffsetTranslation mScaleOffsetTranslation = new DefaultScaleOffsetTranslation();

    public MagicRecyclerView(Context context) {
        this(context, null);
    }

    public MagicRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MagicRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        mEdgeMargin = new EdgeMargin();
        mDensity = getResources().getDisplayMetrics().density;
        setHasFixedSize(true);
        setOrientation(mOrientation);
        setChildDrawingOrderCallback(new ChildDrawingOrderCallbackInner());
        addItemDecoration(new ItemDecorationInner());
        addOnScrollListener(new ScrollListenerInner());

    }

    /**
     * 设置方向 水平 or 竖直
     * @param orientation LinearLayoutManager.HORIZONTAL or LinearLayoutManager.VERTICAL
     */
    public void setOrientation(Orientation orientation) {
        this.mOrientation = orientation;
        mChildViewMetrics = new ChildViewMetrics(orientation);
        mLinearLayoutManager = new LinearLayoutManagerInner(getContext(), orientation.value, false);
        super.setLayoutManager(mLinearLayoutManager);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "onGlobalLayout");
                Log.d(TAG, "mPendingPosition:"+ mPendingPosition);
                if (mPendingPosition >= 0 && mPendingPosition < mLinearLayoutManager.getItemCount()) {
                    scrollToPosition(mPendingPosition);
                }
                Log.d(TAG, "Adjustment "+userScrolling+" "+scrollState+" "+scrolling);
                if (!userScrolling && scrollState == SCROLL_STATE_IDLE && !scrolling) {
                    View centerChild = getCenterView();
                    if (centerChild != null && offsetPercent(centerChild) != 0) {
                        scrollToView(centerChild);
                    }
                    selectedPositionChanged();
                }
            }
        });
    }

    /**
     * 设置ItemView之间的偏移
     * @param offset 单位dp
     */
    public void setMarginOffset(int offset) {
        this.mMarginOffset = (int)(offset * mDensity);
    }

    /**
     * 设置最大3D旋转大小
     * @param degree 单位度
     */
    public void setMax3DRotate(float degree) {
        this.mMax3DRotate = degree;
    }

    /**
     * 设置摩擦力系数
     * @param factor [0,1] 越小越难滑
     */
    public void setFrictionFactor(float factor) {
        if(factor<0 || factor>1) throw new IllegalArgumentException("factor scope is [0,1]");
        mFrictionFactor = factor;
    }

    /**
     * 最小的透明度
     * @param minAlpha [0,1]
     */
    public void setMinAlpha(float minAlpha) {
        if(minAlpha<0 || minAlpha>1) throw new IllegalArgumentException("minAlpha scope is [0,1]");
        mMinAlpha = minAlpha;
    }

    /**
     * 最小的缩放大小
     * @param minScale [0,1]
     */
    public void setMinScale(float minScale) {
        if(minScale<0 || minScale>1) throw new IllegalArgumentException("minScale scope is [0,1]");
        mMinScale = minScale;
    }

    /**
     * 设置是否只有中心的child可点击
     * @param onlyCenterChild 默认true
     */
    public void setIsyCenterChildClickable(boolean onlyCenterChild) {
        isOnlyCenterChildClickable = onlyCenterChild;
    }

    /**
     * 设置是否点击某个Item使其居中
     * @param tapItemToCenter 默认true
     */
    public void setIsTapItemToCenter(boolean tapItemToCenter){
        isTapItemToCenter = tapItemToCenter;
    }

    /**
     * 中间Item的位置
     * @return 中间Item的位置
     */
    public int getSelectedPosition(){
        return mSelectedPosition;
    }

    /**
     * 中间的Item改变时的回调
     * @param listener 中间的Item改变时的回调
     */
    public void setOnChildSelectedListener(OnChildSelectedListener listener) {
        mOnChildSelectedListener = listener;
    }

    /**
     * 需要差异化设置每个Item的MarginOffset时调用
     * @param interf 一旦设置则setMarginOffset不起作用
     */
    public void setVariousMarginOffsetInterf(VariousMarginOffsetInterf interf) {
        mVariousMarginOffsetInterf = interf;
    }

    /**
     * 设置3D角度旋转的规律
     * @param m3DRotateOffsetTranslation 默认是线性
     */
    public void set3DRotateOffsetTranslation(OffsetTranslation m3DRotateOffsetTranslation) {
        if (null != m3DRotateOffsetTranslation) {
            this.m3DRotateOffsetTranslation = m3DRotateOffsetTranslation;
        }
    }

    /**
     * 设置缩放规律
     * @param scaleOffsetTranslation 默认是线性
     */
    public void setScaleOffsetTranslation(OffsetTranslation scaleOffsetTranslation) {
        if (null != scaleOffsetTranslation) {
            mScaleOffsetTranslation = scaleOffsetTranslation;
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        throw new UnsupportedOperationException("Please call setOrientation()");
    }

    @Override
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        transformateChild(child, offsetPercent(child));
        return super.drawChild(canvas, child, drawingTime);
    }

    // 各种对子View的转换
    private void transformateChild(View child, float offsetPercent) {
//        Log.d(TAG, "offsetPercent:" + offsetPercent);
        // 设置3D旋转
        if (mOrientation == Orientation.HORIZONTAL) {
            child.setRotationY(m3DRotateOffsetTranslation.translate(offsetPercent));
        } else {
            child.setRotationX(m3DRotateOffsetTranslation.translate(offsetPercent));
        }

        // 设置透明度
        float alpha = 1 + Math.abs(offsetPercent) * (mMinAlpha - 1);
        child.setAlpha(alpha);

        // 设置缩放大小
        float scale = mScaleOffsetTranslation.translate(offsetPercent);
        child.setScaleX(scale);
        child.setScaleY(scale);
    }

    // 计算子View到中心的距离
    private float offset(View child) {
        int pCenter = center();
        float cCenter = mChildViewMetrics.center(child);

        return cCenter - pCenter;
    }

    // 计算子View到中心的距离的百分比
    private float offsetPercent(View child) {
        float offset = offset(child);
        float maxOffset = center() + mChildViewMetrics.span(child) / 2;

        return offset / maxOffset;
    }

    // 获取本View的横向或者纵向的中心点
    private int center() {
        if (mOrientation == Orientation.VERTICAL)
            return getMeasuredHeight() / 2;

        return getMeasuredWidth() / 2;
    }

    // 子View相关计算工具
    private static class ChildViewMetrics {
        private Orientation orientation;

        ChildViewMetrics(Orientation orientation) {
            this.orientation = orientation;
        }

        /**
         * @param view 子View
         * @return 子View的宽或者高
         */
        int span(View view) {
            if (orientation == Orientation.VERTICAL)
                return view.getHeight();

            return view.getWidth();
        }

        /**
         * @param view 子View
         * @return 子View左或上据父View左/上的距离
         */
        float edge(View view) {
            if (orientation == Orientation.VERTICAL)
                return view.getY();

            return view.getX();
        }

        /**
         * @param view 子View
         * @return 子View中心点据父View左/上的距离
         */
        float center(View view) {
            return edge(view) + (span(view) / 2);
        }
    }

    /**
     * Layout的方向
     */
    public enum Orientation {
        HORIZONTAL(LinearLayout.HORIZONTAL),
        VERTICAL(LinearLayout.VERTICAL);

        int value;
        Orientation(int value) {
            this.value = value;
        }
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return super.fling((int)(velocityX*mFrictionFactor), (int)(velocityY*mFrictionFactor));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if(isOnlyCenterChildClickable){
            View centerView = getCenterView();
            View targetView = getTapedView(centerView, event);
            if (targetView != centerView) {
                Log.d(TAG, "intercept !center");
                return true;
            }
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mPendingPosition = -1;
//        userScrolling = true;
        if (isTapItemToCenter && event.getAction() == MotionEvent.ACTION_UP && SCROLL_STATE_DRAGGING != scrollState) {
            View centerView = getCenterView();
            View targetView = getTapedView(centerView, event);
            if (targetView != centerView) {
                Log.d(TAG, "tapToCenter");
                smoothScrollToView(targetView);
                userScrolling = false;
                if(isOnlyCenterChildClickable) return true;
            }
        }

        return super.dispatchTouchEvent(event);
    }

    private EdgeMargin getSpecialChildMargin(View child) {
        int lastItemIndex = getLayoutManager().getItemCount() - 1;
        int childIndex = getChildAdapterPosition(child);

        int startMargin = 0;
        int endMargin = 0;
        int topMargin = 0;
        int bottomMargin = 0;

        if (mOrientation == Orientation.VERTICAL) {
            topMargin = childIndex == 0 ? center() : 0;
            bottomMargin = childIndex == lastItemIndex ? center() : 0;
        } else {
            startMargin = childIndex == 0 ? center() : 0;
            endMargin = childIndex == lastItemIndex ? center() : 0;
        }

        mEdgeMargin.mChildPoiType = EdgeMargin.OTHER_CHILD;
        mEdgeMargin.mEdgeMargin = 0;

        if (startMargin > 0 || topMargin > 0) {
            mEdgeMargin.mChildPoiType = EdgeMargin.FIRST_CHILD;
            mEdgeMargin.mEdgeMargin = startMargin >= topMargin ? startMargin : topMargin;
        }

        if (endMargin > 0 || bottomMargin > 0) {
            mEdgeMargin.mChildPoiType = EdgeMargin.LAST_CHILD;
            mEdgeMargin.mEdgeMargin = endMargin >= bottomMargin ? endMargin : bottomMargin;
        }

        return mEdgeMargin;
    }

    private static class EdgeMargin{
        static final int FIRST_CHILD = 0;
        static final int OTHER_CHILD = 1;
        static final int LAST_CHILD = 2;

        public int mChildPoiType;
        public int mEdgeMargin;
    }

    /**
     * 滚动动到指定位置
     * @param position 指定位置
     */
    @Override
    public void smoothScrollToPosition(int position) {
        Log.d(TAG, "smoothScrollToPosition:"+position);
        View child = mLinearLayoutManager.findViewByPosition(position);
        if (null != child) {
            mPendingPosition = -1;
            smoothScrollToView(child);
        }else{
            mLinearLayoutManager.scrollToPosition(position);
            mPendingPosition = position;
        }
    }

    /**
     * 跳到指定位置
     * @param position 指定位置
     */
    @Override
    public void scrollToPosition(final int position) {
        Log.d(TAG, "scrollToPosition:"+position);
        View child = mLinearLayoutManager.findViewByPosition(position);
        if (null != child) {
            mPendingPosition = -1;
            scrollToView(child);
            selectedPositionChanged();
        }else{
            mLinearLayoutManager.scrollToPosition(position);
            mPendingPosition = position;
        }
    }

    /*
     * 平滑滚动到指定的view
     */
    private void smoothScrollToView(View child) {
        if (child == null) return;

        stopScroll();

        int scrollDistance = (int) offset(child);
        Log.d(TAG, "smooth_scrollDistance:" + scrollDistance);
        if (scrollDistance != 0){
            scrolling = true;
            smoothScrollBy(scrollDistance);
        }
    }

    /*
     * 滚动到指定的view
     */
    private void scrollToView(View child) {
        if (child == null) return;

        stopScroll();

        int scrollDistance = (int) offset(child);
        Log.d(TAG, "scrollDistance:" + scrollDistance);
        if (scrollDistance != 0)
            scrollBy(scrollDistance);
    }

    /*
     * 滑动一定距离
     */
    private void scrollBy(int distance) {
        if (mOrientation == Orientation.HORIZONTAL) {
            super.scrollBy(distance, 0);
        } else {
            super.scrollBy(0, distance);
        }
    }

    /*
     * 平滑滑动一定距离
     */
    private void smoothScrollBy(int distance) {
        if (mOrientation == Orientation.HORIZONTAL) {
            super.smoothScrollBy(distance, 0);
        } else {
            super.smoothScrollBy(0, distance);
        }
    }

    private View getCenterView() {
        return getNearestChildToLocation(center());
    }

    private View getNearestChildToLocation(int location) {
        View nearestChild = null;

        int nearPoi = mOrientation == Orientation.HORIZONTAL ? getMeasuredWidth() : getMeasuredHeight();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            int childCenterLocation = (int) mChildViewMetrics.center(child);
            int distance = childCenterLocation - location;

            if (Math.abs(distance) < Math.abs(nearPoi)) {
                nearPoi = distance;
                nearestChild = child;
            }
        }

        return nearestChild;
    }

    private View getTapedView(View centerView, MotionEvent event) {
        if(null == centerView) return null;

        int cerIndex = ((ViewGroup) centerView.getParent()).indexOfChild(centerView);
        if (cerIndex >= 0) {
            float tapP =  mOrientation == Orientation.HORIZONTAL ? event.getX() : event.getY();
            if (tapP >= center()) {
                for (int i = cerIndex; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (inRangeOfView(child, event)) {

                        return child;
                    }
                }
            } else {
                for(int i = cerIndex; i>=0; i--) {
                    View child = getChildAt(i);
                    if (inRangeOfView(child, event)) {

                        return child;
                    }
                }
            }
        }

        return null;
    }

    private boolean inRangeOfView(View view, MotionEvent ev) {
        if (mHitRect == null) {
            mHitRect = new Rect();
        }
        view.getHitRect(mHitRect);

        return mHitRect.contains((int)ev.getX(), (int)ev.getY());
    }

    private class ScrollListenerInner extends OnScrollListener {

        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            Log.d(TAG, "newState:" + newState);

            if (newState == SCROLL_STATE_DRAGGING) {
                userScrolling = true;
            } else if (newState == SCROLL_STATE_IDLE) {
                scrolling = false;

                if (userScrolling) {
                    userScrolling = false;
                    smoothScrollToView(getCenterView());
                } else {
                    selectedPositionChanged();
                }

            } else if (newState == SCROLL_STATE_SETTLING) {
                scrolling = true;
            }

            scrollState = newState;
        }
    }

    private class ChildDrawingOrderCallbackInner implements ChildDrawingOrderCallback {

        private int mIndex = -1;

        @Override
        public int onGetChildDrawingOrder(int childCount, int i) {// i是画的顺序
            return orderChildren(childCount, i);
        }

        private int orderChildren(int childCount, int i) {
            if (0 == i) mIndex = -1;

            List<Float> offsetList = new ArrayList<>(childCount);
            for (int j = 0; j < childCount; j++) {
                offsetList.add(-Math.abs(offsetPercent(getChildAt(j))));
            }
            List<Float> cloneOffsetList = new ArrayList<>(offsetList);
            Collections.sort(offsetList);

            int index = cloneOffsetList.indexOf(offsetList.get(i));
            if (index == mIndex) {
                mIndex = cloneOffsetList.lastIndexOf(offsetList.get(i));
            } else {
                mIndex = index;
            }
            return mIndex;
        }
    }

    private class ItemDecorationInner extends ItemDecoration{
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            if (null == mVariousMarginOffsetInterf) {
                if (mOrientation == Orientation.HORIZONTAL) {
                    outRect.left = mMarginOffset / 2;
                    outRect.right = mMarginOffset / 2;
                } else {
                    outRect.top = mMarginOffset / 2;
                    outRect.bottom = mMarginOffset / 2;
                }
            } else {
                if (mOrientation == Orientation.HORIZONTAL) {
                    outRect.left = mVariousMarginOffsetInterf.getMarginOffsetFront(view, getChildAdapterPosition(view));
                    outRect.right = mVariousMarginOffsetInterf.getMarginOffsetBehind(view, getChildAdapterPosition(view));
                } else {
                    outRect.top = mVariousMarginOffsetInterf.getMarginOffsetFront(view, getChildAdapterPosition(view));
                    outRect.bottom = mVariousMarginOffsetInterf.getMarginOffsetBehind(view, getChildAdapterPosition(view));
                }
            }

            EdgeMargin specialChildMargin = getSpecialChildMargin(view);
            if (specialChildMargin.mChildPoiType == EdgeMargin.FIRST_CHILD) {
                if (mOrientation == Orientation.HORIZONTAL) {
                    outRect.left = specialChildMargin.mEdgeMargin;
                } else {
                    outRect.top = specialChildMargin.mEdgeMargin;
                }
            } else if (specialChildMargin.mChildPoiType == EdgeMargin.LAST_CHILD) {
                if (mOrientation == Orientation.HORIZONTAL) {
                    outRect.right = specialChildMargin.mEdgeMargin;
                } else {
                    outRect.bottom = specialChildMargin.mEdgeMargin;
                }
            }
        }
    }

    private class LinearLayoutManagerInner extends LinearLayoutManager{

        LinearLayoutManagerInner(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        @Override
        public void onLayoutChildren(Recycler recycler, State state) {
            super.onLayoutChildren(recycler, state);
            Log.d(TAG, "onLayoutChildren");
        }

        @Override
        public void onLayoutCompleted(State state) {
            super.onLayoutCompleted(state);
            Log.d(TAG, "onLayoutCompleted");
        }

        @Override
        public boolean onRequestChildFocus(RecyclerView parent, State state, View child, View focused) {
            return true;// 禁止此RV因为内部Item请求焦点而发生滚动
        }

    }

    private void selectedPositionChanged() {
//        if (mPendingPosition >= 0) {
//            mPendingPosition = -1;
//            return;
//        }

        View view = getCenterView();
        int position = getChildAdapterPosition(view);
        if (mOnChildSelectedListener != null && position != mSelectedPosition) {
            mOnChildSelectedListener.onSelected(view, position);
        }
        mSelectedPosition = position;
        Log.d(TAG, "selectedPosition:"+position);
    }

    public interface OnChildSelectedListener{
        void onSelected(View child, int position);
    }

    public interface VariousMarginOffsetInterf {
        int getMarginOffsetFront(View child, int position);
        int getMarginOffsetBehind(View child, int position);
    }

    public interface OffsetTranslation {
        float translate(float offsetPercent);
    }

    private class Default3DRotateOffsetTranslation implements OffsetTranslation{
        @Override
        public float translate(float offsetPercent) {
            return offsetPercent * mMax3DRotate;
        }
    }

    private class DefaultScaleOffsetTranslation implements OffsetTranslation{
        @Override
        public float translate(float offsetPercent) {
            return 1 + Math.abs(offsetPercent) * (mMinScale - 1);
        }
    }
}
