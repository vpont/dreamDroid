package com.melnykov.fab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.ImageButton;

import com.nineoldandroids.animation.ObjectAnimator;

/**
 * Android Google+ like floating action button which reacts on the attached list view scrolling events.
 *
 * @author Oleksandr Melnykov
 */
public class FloatingActionButton extends ImageButton {
    private static final int TRANSLATE_DURATION_MILLIS = 200;
    private FabOnScrollListener mOnScrollListener;
    private FabRecyclerOnViewScrollListener mRecyclerViewOnScrollListener;

    @IntDef({TYPE_NORMAL, TYPE_MINI})
    public @interface TYPE {
    }

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_MINI = 1;

    protected AbsListView mListView;
    protected RecyclerView mRecyclerView;

    private boolean mVisible;
    private boolean mTopAligned;
    private boolean mIsInverted;

    private int mColorNormal;
    private int mColorPressed;
    private int mColorRipple;
    private boolean mShadow;
    private int mType;

    private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = getDimension(
                mType == TYPE_NORMAL ? R.dimen.fab_size_normal : R.dimen.fab_size_mini);
        if (mShadow && !hasLollipopApi()) {
            int shadowSize = getDimension(R.dimen.fab_shadow_size);
            size += shadowSize * 2;
        }
        setMeasuredDimension(size, size);
    }

    private void init(Context context, AttributeSet attributeSet) {
        mVisible = true;
        mTopAligned = false;
        mIsInverted = false;
        mColorNormal = getColor(R.color.material_blue_500);
        mColorPressed = getColor(R.color.material_blue_600);
        mColorRipple = getColor(android.R.color.white);
        mType = TYPE_NORMAL;
        mShadow = true;
        if (attributeSet != null) {
            initAttributes(context, attributeSet);
        }
        updateBackground();
    }

    private void initAttributes(Context context, AttributeSet attributeSet) {
        TypedArray attr = getTypedArray(context, attributeSet, R.styleable.FloatingActionButton);
        if (attr != null) {
            try {
                mColorNormal = attr.getColor(R.styleable.FloatingActionButton_fab_colorNormal,
                        getColor(R.color.material_blue_500));
                mColorPressed = attr.getColor(R.styleable.FloatingActionButton_fab_colorPressed,
                        getColor(R.color.material_blue_600));
                mColorRipple = attr.getColor(R.styleable.FloatingActionButton_fab_colorRipple,
                        getColor(android.R.color.white));
                mShadow = attr.getBoolean(R.styleable.FloatingActionButton_fab_shadow, true);
                mType = attr.getInt(R.styleable.FloatingActionButton_fab_type, TYPE_NORMAL);
            } finally {
                attr.recycle();
            }
        }
    }

    private void updateBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, createDrawable(mColorPressed));
        drawable.addState(new int[]{}, createDrawable(mColorNormal));
        setBackgroundCompat(drawable);
    }

    private Drawable createDrawable(int color) {
        OvalShape ovalShape = new OvalShape();
        ShapeDrawable shapeDrawable = new ShapeDrawable(ovalShape);
        shapeDrawable.getPaint().setColor(color);

        if (mShadow && !hasLollipopApi()) {
            LayerDrawable layerDrawable = new LayerDrawable(
                    new Drawable[]{getResources().getDrawable(R.drawable.shadow),
                            shapeDrawable});
            int shadowSize = getDimension(
                    mType == TYPE_NORMAL ? R.dimen.fab_shadow_size : R.dimen.fab_mini_shadow_size);
            layerDrawable.setLayerInset(1, shadowSize, shadowSize, shadowSize, shadowSize);
            return layerDrawable;
        } else {
            return shapeDrawable;
        }
    }

    private TypedArray getTypedArray(Context context, AttributeSet attributeSet, int[] attr) {
        return context.obtainStyledAttributes(attributeSet, attr, 0, 0);
    }

    private int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    private int getDimension(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void setBackgroundCompat(Drawable drawable) {
        if (hasLollipopApi()) {
            setElevation(mShadow ? getDimension(R.dimen.fab_elevation_lollipop) : 0.0f);
            RippleDrawable rippleDrawable = new RippleDrawable(new ColorStateList(new int[][]{{}},
                    new int[]{mColorRipple}), drawable, null);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int size = getDimension(mType == TYPE_NORMAL ? R.dimen.fab_size_normal : R.dimen.fab_size_mini);
                    outline.setOval(0, 0, size, size);
                }
            });
            setClipToOutline(true);
            setBackground(rippleDrawable);
        } else if (hasJellyBeanApi()) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
    }

    /**
     * @deprecated to be removed in next release.
     * Now {@link com.melnykov.fab.ScrollDirectionDetector} is used to detect scrolling direction.
     */
    @Deprecated
    protected int getListViewScrollY() {
        View topChild = mListView.getChildAt(0);
        return topChild == null ? 0 : mListView.getFirstVisiblePosition() * topChild.getHeight() -
                topChild.getTop();
    }

    private int getMarginBottom() {
        int marginBottom = 0;
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            marginBottom = ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
        }
        return marginBottom;
    }

    private int getMarginTop() {
        int marginTop = 0;
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            marginTop = ((ViewGroup.MarginLayoutParams) layoutParams).topMargin;
        }
        return marginTop;
    }

    public void setColorNormal(int color) {
        if (color != mColorNormal) {
            mColorNormal = color;
            updateBackground();
        }
    }

    public void setColorNormalResId(@ColorRes int colorResId) {
        setColorNormal(getColor(colorResId));
    }

    public int getColorNormal() {
        return mColorNormal;
    }

    public void setColorPressed(int color) {
        if (color != mColorPressed) {
            mColorPressed = color;
            updateBackground();
        }
    }

    public void setColorPressedResId(@ColorRes int colorResId) {
        setColorPressed(getColor(colorResId));
    }

    public int getColorPressed() {
        return mColorPressed;
    }

    public void setColorRipple(int color) {
        if (color != mColorRipple) {
            mColorRipple = color;
            updateBackground();
        }
    }

    public void setColorRippleResId(@ColorRes int colorResId) {
        setColorRipple(getColor(colorResId));
    }

    public int getColorRipple() {
        return mColorRipple;
    }

    public void setShadow(boolean shadow) {
        if (shadow != mShadow) {
            mShadow = shadow;
            updateBackground();
        }
    }

    public boolean hasShadow() {
        return mShadow;
    }

    public void setType(@TYPE int type) {
        if (type != mType) {
            mType = type;
            updateBackground();
        }
    }

    @TYPE
    public int getType() {
        return mType;
    }

    protected AbsListView.OnScrollListener getOnScrollListener() {
        return mOnScrollListener;
    }

    protected RecyclerView.OnScrollListener getRecyclerViewOnScrollListener() {
        return mRecyclerViewOnScrollListener;
    }

    public boolean isInverted() {
        return mIsInverted;
    }

    public void show() {
        show(true);
    }

    public void hide() {
        hide(true);
    }

    public void show(boolean animate) {
        toggle(true, animate, false);
    }

    public void hide(boolean animate) {
        toggle(false, animate, false);
    }

    private void toggle(final boolean visible, final boolean animate, boolean force) {
        if (mVisible != visible || force) {
            mVisible = visible;
            int height = getHeight();
            if (height == 0 && !force) {
                ViewTreeObserver vto = getViewTreeObserver();
                if (vto.isAlive()) {
                    vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            ViewTreeObserver currentVto = getViewTreeObserver();
                            if (currentVto.isAlive()) {
                                currentVto.removeOnPreDrawListener(this);
                            }
                            toggle(visible, animate, true);
                            return true;
                        }
                    });
                    return;
                }
            }

            int translationY = visible ? 0 : height + getMarginBottom();
            int from = visible ? height : 0;
            if (mTopAligned) {
                translationY = visible ? 0 : -(2 * height + getMarginTop());
                from = visible ? -(2 * height) : 0;
            }

            ObjectAnimator animator = ObjectAnimator.ofFloat(this,
                    "translationY", from, translationY);
            animator.setInterpolator(mInterpolator);
            if (animate) {
                animator.setDuration(TRANSLATE_DURATION_MILLIS).start();
            } else {
                animator.setDuration(0).start();
            }
        }
    }

    /**
     * If need to use custom {@link android.widget.AbsListView.OnScrollListener},
     * pass it to {@link #attachToListView(android.widget.AbsListView, com.melnykov.fab.FloatingActionButton.FabOnScrollListener, boolean inverted)}
     */
    public void attachToListView(@NonNull AbsListView listView) {
        attachToListView(listView, new FabOnScrollListener(), false);
    }

    public void attachToListView(@NonNull AbsListView listView, boolean isTopAligned) {
		attachToListView(listView, new FabOnScrollListener(), isTopAligned);
	}

	public void attachToListView(@NonNull AbsListView listView, boolean isTopAligned, boolean isInverted) {
		attachToListView(listView, new FabOnScrollListener(), isTopAligned, isInverted);
	}

    /**
     * If need to use custom {@link android.widget.AbsListView.OnScrollListener},
     * pass it to {@link #attachToListView(android.widget.AbsListView, com.melnykov.fab.FloatingActionButton.FabOnScrollListener, boolean inverted)}
     */
    public void attachToRecyclerView(@NonNull RecyclerView recyclerView) {
        attachToRecyclerView(recyclerView, new FabRecyclerOnViewScrollListener());
    }

	public void attachToListView(@NonNull AbsListView listView, @NonNull FabOnScrollListener onScrollListener) {
		attachToListView(listView, onScrollListener, false, false);
	}

    public void attachToListView(@NonNull AbsListView listView, @NonNull FabOnScrollListener onScrollListener, boolean topAligned) {
        attachToListView(listView, onScrollListener, topAligned, false);
    }

    public void attachToListView(@NonNull AbsListView listView, @NonNull FabOnScrollListener onScrollListener, boolean topAligned, boolean isInverted) {
        mListView = listView;
        mOnScrollListener = onScrollListener;
        mTopAligned = topAligned;
        mIsInverted = isInverted;
        onScrollListener.setFloatingActionButton(this);
        onScrollListener.setListView(listView);
        mListView.setOnScrollListener(onScrollListener);
    }

    public void attachToRecyclerView(@NonNull RecyclerView recyclerView, @NonNull FabRecyclerOnViewScrollListener onScrollListener) {
        mRecyclerView = recyclerView;
        mRecyclerViewOnScrollListener = onScrollListener;
        onScrollListener.setFloatingActionButton(this);
        onScrollListener.setRecyclerView(recyclerView);
        mRecyclerView.setOnScrollListener(onScrollListener);
    }

    private boolean hasLollipopApi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private boolean hasJellyBeanApi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Shows/hides the FAB when the attached {@link android.widget.AbsListView} scrolling events occur.
     * Extend this class and override {@link com.melnykov.fab.FloatingActionButton.FabOnScrollListener#onScrollDown()}/{@link com.melnykov.fab.FloatingActionButton.FabOnScrollListener#onScrollUp()}
     * if you need custom code to be executed on these events.
     */
    public static class FabOnScrollListener extends ScrollDirectionDetector implements ScrollDirectionListener {
        private FloatingActionButton mFloatingActionButton;

        public FabOnScrollListener() {
            setScrollDirectionListener(this);
        }

        private void setFloatingActionButton(@NonNull FloatingActionButton floatingActionButton) {
            mFloatingActionButton = floatingActionButton;
        }

        /**
         * Called when the attached {@link android.widget.AbsListView} is scrolled down.
         * <br />
         * <br />
         * <i>Derived classes should call the super class's implementation of this method.
         * If they do not, the FAB will not react to AbsListView's scrolling events.</i>
         */
        @Override
        public void onScrollDown() {
            if (mFloatingActionButton.isInverted())
                mFloatingActionButton.hide();
            else
                mFloatingActionButton.show();
        }

        /**
         * Called when the attached {@link android.widget.AbsListView} is scrolled up.
         * <br />
         * <br />
         * <i>Derived classes should call the super class's implementation of this method.
         * If they do not, the FAB will not react to AbsListView's scrolling events.</i>
         */
        @Override
        public void onScrollUp() {
            if (mFloatingActionButton.isInverted())
                mFloatingActionButton.show();
            else
                mFloatingActionButton.hide();
        }
    }

    /**
     * Shows/hides the FAB when the attached {@link RecyclerView} scrolling events occur.
     * Extend this class and override {@link com.melnykov.fab.FloatingActionButton.FabOnScrollListener#onScrollDown()}/{@link com.melnykov.fab.FloatingActionButton.FabOnScrollListener#onScrollUp()}
     * if you need custom code to be executed on these events.
     */
    public static class FabRecyclerOnViewScrollListener extends ScrollDirectionRecyclerViewDetector implements ScrollDirectionListener {
        private FloatingActionButton mFloatingActionButton;

        public FabRecyclerOnViewScrollListener() {
            setScrollDirectionListener(this);
        }

        private void setFloatingActionButton(@NonNull FloatingActionButton floatingActionButton) {
            mFloatingActionButton = floatingActionButton;
        }

        /**
         * Called when the attached {@link RecyclerView} is scrolled down.
         * <br />
         * <br />
         * <i>Derived classes should call the super class's implementation of this method.
         * If they do not, the FAB will not react to RecyclerView's scrolling events.</i>
         */
        @Override
        public void onScrollDown() {
            mFloatingActionButton.show();
        }

        /**
         * Called when the attached {@link RecyclerView} is scrolled up.
         * <br />
         * <br />
         * <i>Derived classes should call the super class's implementation of this method.
         * If they do not, the FAB will not react to RecyclerView's scrolling events.</i>
         */
        @Override
        public void onScrollUp() {
            mFloatingActionButton.hide();
        }
    }
}
