package com.williamww.silkysignature.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.williamww.silkysignature.R;
import com.williamww.silkysignature.utils.Bezier;
import com.williamww.silkysignature.utils.ControlTimedPoints;
import com.williamww.silkysignature.utils.SvgBuilder;
import com.williamww.silkysignature.utils.TimedPoint;
import com.williamww.silkysignature.view.ViewCompat;
import com.williamww.silkysignature.view.ViewTreeObserverCompat;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;


public class SignaturePad extends View {
    private static final int HINT_TEXT_SIZE_START = 60;
    private static final int TEXT_PADDING_PX = 10;
    public static final int TEXT_HORIZONTAL_ALIGN_CENTER = 1;
    public static final int TEXT_HORIZONTAL_ALIGN_START = 2;
    public static final int TEXT_HORIZONTAL_ALIGN_END = 3;

    public static final int TEXT_VERTICLAL_ALIGN_CENTER = 1;
    public static final int TEXT_VERTICLAL_ALIGN_TOP = 2;
    public static final int TEXT_VERTICLAL_ALIGN_BOTTOM = 3;

    //View state
    private List<TimedPoint> mPoints;
    private boolean mIsEmpty;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mLastVelocity;
    private float mLastWidth;
    private RectF mDirtyRect;

    private final SvgBuilder mSvgBuilder = new SvgBuilder();

    // Cache
    private List<TimedPoint> mPointsCache = new ArrayList<>();
    private ControlTimedPoints mControlTimedPointsCached = new ControlTimedPoints();
    private Bezier mBezierCached = new Bezier();

    //Configurable parameters
    private int mMinWidth;
    private int mMaxWidth;
    private float mVelocityFilterWeight;
    private OnSignedListener mOnSignedListener;
    private boolean mClearOnDoubleClick;

    //Click values
    private long mFirstClick;
    private int mCountClick;
    private static final int DOUBLE_CLICK_DELAY_MS = 200;

    //Default attribute values
    private final int DEFAULT_ATTR_PEN_MIN_WIDTH_PX = 3;
    private final int DEFAULT_ATTR_PEN_MAX_WIDTH_PX = 7;
    private final int DEFAULT_ATTR_PEN_COLOR = Color.BLACK;
    private final float DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT = 0.9f;
    private final boolean DEFAULT_ATTR_CLEAR_ON_DOUBLE_CLICK = false;

    private Paint mPaint = new Paint();
    private Bitmap mSignatureBitmap = null;
    private Canvas mSignatureBitmapCanvas = null;
    protected float strokeLeft = -1, strokeRight = -1, strokeTop = -1, strokeBottom = -1;
    protected TextBuilder lastText;
    private String hintText = null;
    private boolean hintCleared = false;
    private int hintTextColor = Color.BLACK, hintBorderColor = Color.BLACK;

    /**
     *
     * @param context
     */
    public SignaturePad(Context context){
        super(context);
        init(null);
    }

    /**
     *
     * @param context
     * @param attrs
     */
    public SignaturePad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    /**
     *
     * @param attrs
     */
    protected void init(AttributeSet attrs){
        TypedArray a;
        if(attrs != null) {
            a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SignaturePad,0, 0);
        }
        else{
            a = getContext().getTheme().obtainStyledAttributes(R.styleable.SignaturePad);
        }

        //Configurable parameters
        try {
            mMinWidth = a.getDimensionPixelSize(R.styleable.SignaturePad_penMinWidth, convertDpToPx(DEFAULT_ATTR_PEN_MIN_WIDTH_PX));
            mMaxWidth = a.getDimensionPixelSize(R.styleable.SignaturePad_penMaxWidth, convertDpToPx(DEFAULT_ATTR_PEN_MAX_WIDTH_PX));
            mPaint.setColor(a.getColor(R.styleable.SignaturePad_penColor, DEFAULT_ATTR_PEN_COLOR));
            mVelocityFilterWeight = a.getFloat(R.styleable.SignaturePad_velocityFilterWeight, DEFAULT_ATTR_VELOCITY_FILTER_WEIGHT);
            mClearOnDoubleClick = a.getBoolean(R.styleable.SignaturePad_clearOnDoubleClick, DEFAULT_ATTR_CLEAR_ON_DOUBLE_CLICK);
        } finally {
            a.recycle();
        }

        //Fixed parameters
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);

        //Dirty rectangle to update only the changed portion of the view
        mDirtyRect = new RectF();

        clear();
    }

    /**
     * Set the pen color from a given resource.
     * If the resource is not found, {@link Color#BLACK} is assumed.
     *
     * @param colorRes the color resource.
     */
    public void setPenColorRes(int colorRes) {
        try {
            setPenColor(getResources().getColor(colorRes));
        } catch (Resources.NotFoundException ex) {
            setPenColor(Color.parseColor("#000000"));
        }
    }

    /**
     * Set the pen color from a given color.
     *
     * @param color the color.
     */
    public void setPenColor(int color) {
        mPaint.setColor(color);
    }

    /**
     * Set the minimum width of the stroke in pixel.
     *
     * @param minWidth the width in dp.
     */
    public void setMinWidth(float minWidth) {
        mMinWidth = convertDpToPx(minWidth);
    }

    /**
     * Set the maximum width of the stroke in pixel.
     *
     * @param maxWidth the width in dp.
     */
    public void setMaxWidth(float maxWidth) {
        mMaxWidth = convertDpToPx(maxWidth);
    }

    /**
     * Set the velocity filter weight.
     *
     * @param velocityFilterWeight the weight.
     */
    public void setVelocityFilterWeight(float velocityFilterWeight) {
        mVelocityFilterWeight = velocityFilterWeight;
    }

    public void clear() {
        strokeBottom = -1; strokeLeft = -1; strokeRight = -1; strokeTop = -1;
        mSvgBuilder.clear();
        mPoints = new ArrayList<>();
        mLastVelocity = 0;
        mLastWidth = (mMinWidth + mMaxWidth) / 2;

        if (mSignatureBitmap != null) {
            if(!mSignatureBitmap.isRecycled()){
                mSignatureBitmap.recycle();
            }
            mSignatureBitmap = null;
            ensureSignatureBitmap();
        }

        setIsEmpty(true);

        invalidate();
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return false;
        if(!hintCleared){
            //removes the hint
            if(mSignatureBitmapCanvas != null) {
                mSignatureBitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
            hintCleared = true;
        }
        float eventX = event.getX();
        float eventY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                mPoints.clear();
                if (isDoubleClick()) break;
                mLastTouchX = eventX;
                mLastTouchY = eventY;
                addPoint(getNewPoint(eventX, eventY));
                if (mOnSignedListener != null) mOnSignedListener.onStartSigning();

            case MotionEvent.ACTION_MOVE:
                resetDirtyRect(eventX, eventY);
                addPoint(getNewPoint(eventX, eventY));
                break;

            case MotionEvent.ACTION_UP:
                resetDirtyRect(eventX, eventY);
                addPoint(getNewPoint(eventX, eventY));
                getParent().requestDisallowInterceptTouchEvent(true);
                setIsEmpty(false);
                break;

            default:
                return false;
        }

        //invalidate();
        invalidate(
                (int) (mDirtyRect.left - mMaxWidth),
                (int) (mDirtyRect.top - mMaxWidth),
                (int) (mDirtyRect.right + mMaxWidth),
                (int) (mDirtyRect.bottom + mMaxWidth));

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //if there is a hint-text set and the canvas is still empty - create it, because this method will add
        if(this.hintText != null && !this.hintText.isEmpty() && mSignatureBitmapCanvas == null){
            ensureSignatureBitmap();
        }
        if (mSignatureBitmap != null) {
            canvas.drawBitmap(mSignatureBitmap, 0, 0, mPaint);
        }
    }

    public void setOnSignedListener(OnSignedListener listener) {
        mOnSignedListener = listener;
    }

    public boolean isEmpty() {
        return mIsEmpty;
    }

    public String getSignatureSvg() {
        int width = getTransparentSignatureBitmap().getWidth();
        int height = getTransparentSignatureBitmap().getHeight();
        return mSvgBuilder.build(width, height);
    }

    public Bitmap getSignatureBitmap() {
        Bitmap originalBitmap = getTransparentSignatureBitmap();
        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        return whiteBgBitmap;
    }

    /**
     * @param compressPercentage Hint to the compressor, 0-100 percent. 0 meaning compress for
     *                           small size, 100 meaning compress for max quality. Some
     *                           formats, like PNG which is lossless, will ignore the
     *                           quality setting
     */
    public Bitmap getCompressedSignatureBitmap(int compressPercentage) {

        if (compressPercentage < 0) {
            compressPercentage = 0;
        } else if (compressPercentage > 100) {
            compressPercentage = 100;
        }
        Bitmap originalBitmap = getTransparentSignatureBitmap();
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        int targetWidth = originalWidth * compressPercentage / 100; // your arbitrary fixed limit
        int targetHeight = (int) (originalHeight * targetWidth / (double) originalWidth);

        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        whiteBgBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
        return whiteBgBitmap;
    }

    /**
     * @param deiredWidth Desired width of the bitmap
     */
    public Bitmap getFixedSizeSignatureBitmap(int deiredWidth) {

        Bitmap originalBitmap = getTransparentSignatureBitmap();
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        int targetWidth = deiredWidth; // your arbitrary fixed limit
        int targetHeight = (int) (originalHeight * targetWidth / (double) originalWidth);

        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        whiteBgBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
        return whiteBgBitmap;
    }

    /**
     * @param deiredWidth Desired width of the bitmap
     */
    public Bitmap getFixedSizeSignatureBitmap(int deiredWidth,int desiredHeight) {

        Bitmap originalBitmap = getTransparentSignatureBitmap();
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        int targetWidth = deiredWidth; // your arbitrary fixed limit
        int targetHeight = desiredHeight;

        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        whiteBgBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
        return whiteBgBitmap;
    }

    public void setSignatureBitmap(final Bitmap signature) {
        // View was laid out...
        if (ViewCompat.isLaidOut(this)) {
            clear();
            ensureSignatureBitmap();

            RectF tempSrc = new RectF();
            RectF tempDst = new RectF();

            int dWidth = signature.getWidth();
            int dHeight = signature.getHeight();
            int vWidth = getWidth();
            int vHeight = getHeight();

            // Generate the required transform.
            tempSrc.set(0, 0, dWidth, dHeight);
            tempDst.set(0, 0, vWidth, vHeight);

            Matrix drawMatrix = new Matrix();
            drawMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);

            Canvas canvas = new Canvas(mSignatureBitmap);
            canvas.drawBitmap(signature, drawMatrix, null);
            setIsEmpty(false);
            invalidate();
        }
        // View not laid out yet e.g. called from onCreate(), onRestoreInstanceState()...
        else {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Remove layout listener...
                    ViewTreeObserverCompat.removeOnGlobalLayoutListener(getViewTreeObserver(), this);

                    // Signature bitmap...
                    setSignatureBitmap(signature);
                }
            });
        }
    }

    public Bitmap getTransparentSignatureBitmap() {
        ensureSignatureBitmap();
        return mSignatureBitmap;
    }

    public Bitmap getTransparentSignatureBitmap(boolean trimBlankSpace) {

        if (!trimBlankSpace) {
            return getTransparentSignatureBitmap();
        }

        ensureSignatureBitmap();

        int imgHeight = mSignatureBitmap.getHeight();
        int imgWidth = mSignatureBitmap.getWidth();

        int backgroundColor = Color.TRANSPARENT;

        int xMin = Integer.MAX_VALUE,
                xMax = Integer.MIN_VALUE,
                yMin = Integer.MAX_VALUE,
                yMax = Integer.MIN_VALUE;

        boolean foundPixel = false;

        // Find xMin
        for (int x = 0; x < imgWidth; x++) {
            boolean stop = false;
            for (int y = 0; y < imgHeight; y++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    xMin = x;
                    stop = true;
                    foundPixel = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Image is empty...
        if (!foundPixel)
            return null;

        // Find yMin
        for (int y = 0; y < imgHeight; y++) {
            boolean stop = false;
            for (int x = xMin; x < imgWidth; x++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    yMin = y;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Find xMax
        for (int x = imgWidth - 1; x >= xMin; x--) {
            boolean stop = false;
            for (int y = yMin; y < imgHeight; y++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    xMax = x;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Find yMax
        for (int y = imgHeight - 1; y >= yMin; y--) {
            boolean stop = false;
            for (int x = xMin; x <= xMax; x++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    yMax = y;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        return Bitmap.createBitmap(mSignatureBitmap, xMin, yMin, xMax - xMin, yMax - yMin);
    }

    private boolean isDoubleClick() {
        if (mClearOnDoubleClick) {
            if (mFirstClick != 0 && System.currentTimeMillis() - mFirstClick > DOUBLE_CLICK_DELAY_MS) {
                mCountClick = 0;
            }
            mCountClick++;
            if (mCountClick == 1) {
                mFirstClick = System.currentTimeMillis();
            } else if (mCountClick == 2) {
                long lastClick = System.currentTimeMillis();
                if (lastClick - mFirstClick < DOUBLE_CLICK_DELAY_MS) {
                    this.clear();
                    return true;
                }
            }
        }
        return false;
    }

    private TimedPoint getNewPoint(float x, float y) {
        int mCacheSize = mPointsCache.size();
        TimedPoint timedPoint;
        if (mCacheSize == 0) {
            // Cache is empty, create a new point
            timedPoint = new TimedPoint();
        } else {
            // Get point from cache
            timedPoint = mPointsCache.remove(mCacheSize - 1);
        }

        return timedPoint.set(x, y);
    }

    private void recyclePoint(TimedPoint point) {
        mPointsCache.add(point);
    }

    private void addPoint(TimedPoint newPoint) {
        mPoints.add(newPoint);

        int pointsCount = mPoints.size();
        if (pointsCount > 3) {

            ControlTimedPoints tmp = calculateCurveControlPoints(mPoints.get(0), mPoints.get(1), mPoints.get(2));
            TimedPoint c2 = tmp.c2;
            recyclePoint(tmp.c1);

            tmp = calculateCurveControlPoints(mPoints.get(1), mPoints.get(2), mPoints.get(3));
            TimedPoint c3 = tmp.c1;
            recyclePoint(tmp.c2);

            Bezier curve = mBezierCached.set(mPoints.get(1), c2, c3, mPoints.get(2));

            TimedPoint startPoint = curve.startPoint;
            TimedPoint endPoint = curve.endPoint;

            float velocity = endPoint.velocityFrom(startPoint);
            velocity = Float.isNaN(velocity) ? 0.0f : velocity;

            velocity = mVelocityFilterWeight * velocity
                    + (1 - mVelocityFilterWeight) * mLastVelocity;

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            float newWidth = strokeWidth(velocity);

            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mPoints.
            addBezier(curve, mLastWidth, newWidth);

            mLastVelocity = velocity;
            mLastWidth = newWidth;

            // Remove the first element from the list,
            // so that we always have no more than 4 mPoints in mPoints array.
            recyclePoint(mPoints.remove(0));

            recyclePoint(c2);
            recyclePoint(c3);

        } else if (pointsCount == 1) {
            // To reduce the initial lag make it work with 3 mPoints
            // by duplicating the first point
            TimedPoint firstPoint = mPoints.get(0);
            mPoints.add(getNewPoint(firstPoint.x, firstPoint.y));
        }
    }

    private void addBezier(Bezier curve, float startWidth, float endWidth) {
        mSvgBuilder.append(curve, (startWidth + endWidth) / 2);
        ensureSignatureBitmap();
        float originalWidth = mPaint.getStrokeWidth();
        float widthDelta = endWidth - startWidth;
        float drawSteps = (float) Math.floor(curve.length());

        for (int i = 0; i < drawSteps; i++) {
            // Calculate the Bezier (x, y) coordinate for this step.
            float t = ((float) i) / drawSteps;
            float tt = t * t;
            float ttt = tt * t;
            float u = 1 - t;
            float uu = u * u;
            float uuu = uu * u;

            float x = uuu * curve.startPoint.x;
            x += 3 * uu * t * curve.control1.x;
            x += 3 * u * tt * curve.control2.x;
            x += ttt * curve.endPoint.x;

            float y = uuu * curve.startPoint.y;
            y += 3 * uu * t * curve.control1.y;
            y += 3 * u * tt * curve.control2.y;
            y += ttt * curve.endPoint.y;

            float strokeWidth = startWidth + ttt * widthDelta;
            // Set the incremental stroke width and draw.
            mPaint.setStrokeWidth(strokeWidth);
            setStrokes(x, y, strokeWidth);
            mSignatureBitmapCanvas.drawPoint(x, y, mPaint);
            expandDirtyRect(x, y);
        }

        mPaint.setStrokeWidth(originalWidth);
    }

    /**
     *
     * @param x
     * @param y
     * @param strokeWidth
     */
    protected void setStrokes(float x, float y, float strokeWidth){
        if(strokeLeft == -1 || x - strokeWidth < strokeLeft){
            strokeLeft = x - strokeWidth;
        }
        if(strokeRight == -1 || x + strokeWidth > strokeRight){
            strokeRight = x + strokeWidth;
        }
        if(strokeTop == -1 || y - strokeWidth < strokeTop){
            strokeTop = y - strokeWidth;
        }
        if(strokeBottom == -1 || y + strokeWidth > strokeBottom){
            strokeBottom = y + strokeWidth;
        }
    }

    /**
     *
     * @return
     */
    public float[] getSafeStrokes(){
        float strokes[] = new float[4];
        ensureSignatureBitmap();
        int imgHeight = mSignatureBitmap.getHeight();
        int imgWidth = mSignatureBitmap.getWidth();

        strokes[0] = Math.max(0, strokeLeft);
        strokes[1] = Math.max(0, strokeTop);
        strokes[2] = Math.min(imgWidth, strokeRight);
        strokes[3] = Math.min(imgHeight, strokeBottom);
        return strokes;
    }

    /**
     *
     * @param compressPercentage
     * @return
     */
    public Bitmap getCompressedTransparentSignatureBitmapTrimOnStrokes(int compressPercentage){
        if (compressPercentage < 0) {
            compressPercentage = 0;
        } else if (compressPercentage > 100) {
            compressPercentage = 100;
        }
        Bitmap originalBitmap = getTransparentSignatureBitmapTrimOnStrokes();
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        int targetWidth = originalWidth * compressPercentage / 100; // your arbitrary fixed limit
        int targetHeight = (int) (originalHeight * targetWidth / (double) originalWidth);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
        originalBitmap.recycle();
        return scaledBitmap;
    }

    /**
     * Method returns the transparent picture
     * @return
     */
    public Bitmap getTransparentSignatureBitmapTrimOnStrokes() {
        ensureSignatureBitmap();
        int imgHeight = mSignatureBitmap.getHeight();
        int imgWidth = mSignatureBitmap.getWidth();
        int backgroundColor = Color.TRANSPARENT;
        int xMin = Integer.MAX_VALUE,
                xMax = Integer.MIN_VALUE,
                yMin = Integer.MAX_VALUE,
                yMax = Integer.MIN_VALUE;

        boolean foundPixel = false;
        float strokes[] = getSafeStrokes();
        // Find xMin
        for (int x = (int)strokes[0]; x < imgWidth; x++) {
            boolean stop = false;
            for (int y = 0; y < imgHeight; y++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    xMin = x;
                    stop = true;
                    foundPixel = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Image is empty...
        if (!foundPixel)
            return null;

        // Find yMin
        for (int y = (int)strokes[1]; y < imgHeight; y++) {
            boolean stop = false;
            for (int x = xMin; x < imgWidth; x++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    yMin = y;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Find xMax
        for (int x = (int)strokes[2] - 1; x >= xMin; x--) {
            boolean stop = false;
            for (int y = yMin; y < imgHeight; y++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    xMax = x;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Find yMax
        for (int y = (int)strokes[3] - 1; y >= yMin; y--) {
            boolean stop = false;
            for (int x = xMin; x <= xMax; x++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    yMax = y;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        return Bitmap.createBitmap(mSignatureBitmap, xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
    }

    /**
     * Adds a text with defaults
     * @param text
     * @return
     */
    public void insertStaticText(String text){
        if(lastText != null){
            lastText.clear();
        }
        lastText =  new TextBuilder(text);
        lastText.build();
    }

    /**
     * Returns a builder to modify the properties of text-inserting - need to run build to add the text
     * @param text
     * @return
     */
    public TextBuilder addStaticText(String text){
        if(lastText != null){
            lastText.clear();
        }
        lastText = new TextBuilder(text);
        return lastText;
    }

    public void setHintText(@StringRes int hintText){
        setHintText(this.getContext().getString(hintText));
    }

    public void setHintText(@Nullable String hintText){
        this.hintText = hintText;
    }

    public void setHintTextColor(int color){
        this.hintTextColor = color;
    }

    public void setHintTextColorRes(@ColorRes int resourceColor){
        setHintTextColor(getResources().getColor(resourceColor));
    }

    public void setHintBorderColor(int color){
        this.hintBorderColor = color;
    }

    public void setHintBorderColorRes(@ColorRes int resourceColor){
        setHintBorderColor(getResources().getColor(resourceColor));
    }


    private ControlTimedPoints calculateCurveControlPoints(TimedPoint s1, TimedPoint s2, TimedPoint s3) {
        float dx1 = s1.x - s2.x;
        float dy1 = s1.y - s2.y;
        float dx2 = s2.x - s3.x;
        float dy2 = s2.y - s3.y;

        float m1X = (s1.x + s2.x) / 2.0f;
        float m1Y = (s1.y + s2.y) / 2.0f;
        float m2X = (s2.x + s3.x) / 2.0f;
        float m2Y = (s2.y + s3.y) / 2.0f;

        float l1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
        float l2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

        float dxm = (m1X - m2X);
        float dym = (m1Y - m2Y);
        float k = l2 / (l1 + l2);
        if (Float.isNaN(k)) k = 0.0f;
        float cmX = m2X + dxm * k;
        float cmY = m2Y + dym * k;

        float tx = s2.x - cmX;
        float ty = s2.y - cmY;

        return mControlTimedPointsCached.set(getNewPoint(m1X + tx, m1Y + ty), getNewPoint(m2X + tx, m2Y + ty));
    }

    private float strokeWidth(float velocity) {
        return Math.max(mMaxWidth / (velocity + 1), mMinWidth);
    }

    /**
     * Called when replaying history to ensure the dirty region includes all
     * mPoints.
     *
     * @param historicalX the previous x coordinate.
     * @param historicalY the previous y coordinate.
     */
    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < mDirtyRect.left) {
            mDirtyRect.left = historicalX;
        } else if (historicalX > mDirtyRect.right) {
            mDirtyRect.right = historicalX;
        }
        if (historicalY < mDirtyRect.top) {
            mDirtyRect.top = historicalY;
        } else if (historicalY > mDirtyRect.bottom) {
            mDirtyRect.bottom = historicalY;
        }
    }

    /**
     * Resets the dirty region when the motion event occurs.
     *
     * @param eventX the event x coordinate.
     * @param eventY the event y coordinate.
     */
    private void resetDirtyRect(float eventX, float eventY) {

        // The mLastTouchX and mLastTouchY were set when the ACTION_DOWN motion event occurred.
        mDirtyRect.left = Math.min(mLastTouchX, eventX);
        mDirtyRect.right = Math.max(mLastTouchX, eventX);
        mDirtyRect.top = Math.min(mLastTouchY, eventY);
        mDirtyRect.bottom = Math.max(mLastTouchY, eventY);
    }

    private void setIsEmpty(boolean newValue) {
        mIsEmpty = newValue;
        if (mOnSignedListener != null) {
            if (mIsEmpty) {
                mOnSignedListener.onClear();
            } else {
                mOnSignedListener.onSigned();
            }
        }
    }

    /**
     * Method create the transparent image + canvas and also adds the hint text + border on it
     */
    private void ensureSignatureBitmap() {
        if (mSignatureBitmap == null) {
            mSignatureBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
                    Bitmap.Config.ARGB_8888);
            mSignatureBitmapCanvas = new Canvas(mSignatureBitmap);
            hintCleared = false;
            if(this.hintText != null){
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
                paint.setColor(Color.BLACK);
                paint.setTextSize(HINT_TEXT_SIZE_START * getResources().getDisplayMetrics().density);
                float textLen = paint.measureText(hintText);
                int tmpSize = HINT_TEXT_SIZE_START;
                int width = (int) (getWidth() * 0.85f);
                while(textLen > width){
                    tmpSize--;
                    if(tmpSize == 0){
                        //never 0 as size, take at least 1
                        paint.setTextSize(1 * getResources().getDisplayMetrics().density);
                        break;
                    }
                    paint.setTextSize(tmpSize * getResources().getDisplayMetrics().density);
                    textLen = paint.measureText(hintText);
                }
                float textHeight = paint.descent() - paint.ascent();
                float x = getWidth() / 2 - textLen / 2; //default horizontal is center
                float y = getHeight() / 2 + (textHeight / 2);
                mSignatureBitmapCanvas.drawText(hintText, x, y, paint);
                int horizontalPadding = convertPxToDp(30);//(int) (getWidth() * 0.05f);
                int verticalPadding = convertPxToDp(30);//convertDpToPx(20);//(int) (getHeight() * 0.05f);
                int horizontalLength = (int) (getWidth() * 0.2f);
                int verticalLength = (int) (getHeight() * 0.2f);
                Path path = new Path();
                path.moveTo(horizontalPadding, verticalPadding + verticalLength);
                path.lineTo(horizontalPadding, verticalPadding); //left upper corner
                path.lineTo(horizontalPadding + horizontalLength, verticalPadding);
                path.moveTo(getWidth() - horizontalPadding - horizontalLength, verticalPadding);
                path.lineTo(getWidth() - horizontalPadding, verticalPadding); //right upper corner
                path.lineTo(getWidth() - horizontalPadding, verticalPadding + verticalLength);
                path.moveTo(getWidth() - horizontalPadding, getHeight() - verticalPadding - verticalLength);
                path.lineTo(getWidth() - horizontalPadding, getHeight() - verticalPadding); //right lower corner
                path.lineTo(getWidth() - horizontalPadding- horizontalLength, getHeight() - verticalPadding);
                path.moveTo(horizontalPadding + horizontalLength, getHeight() - verticalPadding);
                path.lineTo(horizontalPadding, getHeight() - verticalPadding);
                path.lineTo(horizontalPadding, getHeight() - verticalPadding - verticalLength);
                Paint pathPaint = new Paint();
                pathPaint.setStrokeWidth(convertPxToDp(15));
                //rect.setColor(Color.rgb(255,0,0));
                pathPaint.setColor(Color.BLACK);
                pathPaint.setStyle(Paint.Style.STROKE);
                pathPaint.setPathEffect(new DashPathEffect(new float[]{20, 20,}, 0));
                mSignatureBitmapCanvas.drawPath(path, pathPaint);
            }
        }
    }

    int convertDpToPx(float dp) {
        return Math.round(getContext().getResources().getDisplayMetrics().density * dp);
    }

    int convertPxToDp(float px){
        return Math.round(px / ((float)getContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public interface OnSignedListener {
        void onStartSigning();

        void onSigned();

        void onClear();
    }

    public class TextBuilder{
        private String text;
        private @ColorInt int color = Color.BLACK;
        private int maxTextSize = 25 , minTextSize = 5;
        private int verticalAlign = TEXT_VERTICLAL_ALIGN_BOTTOM, horizontalAlign = TEXT_HORIZONTAL_ALIGN_CENTER;
        private Typeface typeface = Typeface.MONOSPACE;
        private float x,y, textLen, textHeight;

        private TextBuilder(String text){
            this.text = text;
        }

        public TextBuilder withFontTypeFace(Typeface typeFace){
            this.typeface = typeFace;
            return this;
        }

        @Deprecated
        public TextBuilder withFontSize(int textSize){
            return withMinFontSize(5).withMaxFontSize(textSize);
        }

        public TextBuilder withMaxFontSize(int maxTextSize){
            this.maxTextSize = maxTextSize;
            return this;
        }

        public TextBuilder withMinFontSize(int minTextSize){
            this.minTextSize = minTextSize;
            return this;
        }

        public TextBuilder withFontColor(@ColorInt int color){
            this.color = color;
            return this;
        }

        public TextBuilder alignVertical(int verticalAlign){
            this.verticalAlign = verticalAlign;
            return this;
        }

        public TextBuilder alignHorizontal(int horizontalAlign){
            this.horizontalAlign = horizontalAlign;
            return this;
        }

        public void build(){
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            paint.setTypeface(typeface);
            paint.setColor(color);
            paint.setTextSize(maxTextSize * getResources().getDisplayMetrics().density);
            paint(paint);
        }

        private void paint(Paint paint){
            textLen = paint.measureText(text);
            int tmpSize = maxTextSize;
            while(textLen > getWidth()){
                tmpSize--;
                if(tmpSize < minTextSize || tmpSize == 0){
                    break;
                }
                paint.setTextSize(tmpSize * getResources().getDisplayMetrics().density);
                textLen = paint.measureText(text);
            }
            textHeight = paint.descent() - paint.ascent();
            float padding = convertPxToDp(TEXT_PADDING_PX);
            x = getWidth() / 2 - textLen / 2; //default horizontal is center
            if(horizontalAlign == TEXT_HORIZONTAL_ALIGN_START){
                x = 0 + convertPxToDp(TEXT_PADDING_PX);
            }else if(horizontalAlign == TEXT_HORIZONTAL_ALIGN_END){
                x = getWidth() - textLen - padding;
            }
            if(verticalAlign == TEXT_VERTICLAL_ALIGN_TOP){
                y = 0 + padding + textHeight;
            }
            else if(verticalAlign == TEXT_VERTICLAL_ALIGN_CENTER){
                y = getHeight() / 2 - (textHeight / 2);
            }
            else if(verticalAlign == TEXT_VERTICLAL_ALIGN_BOTTOM){
                y = getHeight() - padding - textHeight;
            }
            ensureSignatureBitmap();
            mSignatureBitmapCanvas.drawText(text, x, y, paint);
//            invalidate(Math.round(x), Math.round(y), Math.round(x + textLen), Math.round(y - textHeight));
            invalidate();
//            System.out.println("SETTING STROKES x:" + (x - padding) +" y: "+ ( y - textHeight - padding * 2));
            setStrokes(x - padding, y - textHeight - padding * 2, 2);
//            System.out.println("SETTING STROKES x:" + (x + textLen + padding) +" y: "+ ( y + textHeight + padding));
            setStrokes(x + textLen + padding, y + textHeight + padding, 2);
            if(mOnSignedListener != null){
                mOnSignedListener.onSigned();
            }
        }

        private void clear(){
            Paint clearPaint = new Paint();
            clearPaint.setTypeface(typeface);
            clearPaint.setColor(Color.TRANSPARENT);
            clearPaint.setTextSize(maxTextSize * getResources().getDisplayMetrics().density);
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            mSignatureBitmapCanvas.drawRect(x, y - textHeight, x + textLen, y + convertPxToDp(TEXT_PADDING_PX), clearPaint);
//            invalidate(Math.round(x), Math.round(y), Math.round(x + textLen), Math.round(y - textHeight));
        }
    }
}
