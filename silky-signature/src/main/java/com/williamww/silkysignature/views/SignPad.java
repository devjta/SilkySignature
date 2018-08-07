package com.williamww.silkysignature.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by taschekj on 06.08.18.
 * Currently not using
 */
final class SignPad extends RelativeLayout {
    protected SignaturePad signaturePad;
    protected FrameLayout divider;
    protected TextView textView;
    protected AskStaticText askStaticText;
    protected String defaultStaticText;


    public SignPad(Context context) {
        super(context);
        init(null);
    }

    public SignPad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SignPad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public void enableStaticText(@StringRes int emptyText, AskStaticText askStaticText){
        enableStaticText(emptyText, askStaticText, Color.BLACK);
    }

    public void enableStaticText(@StringRes int emptyText, final AskStaticText askStaticText, int textColor){
        this.askStaticText = askStaticText;
        textView.setTextColor(textColor);
        textView.setText(emptyText);
        defaultStaticText = textView.getText().toString();
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setStaticText(askStaticText.getStaticText(null));
            }
        });
        textView.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);
    }

    public void enableStaticText(String emptyText, AskStaticText askStaticText){
        enableStaticText(emptyText, askStaticText, Color.BLACK);
    }

    public void enableStaticText(String emptyText, final AskStaticText askStaticText, int textColor){
        this.askStaticText = askStaticText;
        textView.setTextColor(textColor);
        textView.setText(emptyText);
        defaultStaticText = textView.getText().toString();
        textView.bringToFront();
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setStaticText(askStaticText.getStaticText(null));
            }
        });
        textView.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);
    }

    public void disableStaticText(){
        textView.setVisibility(View.GONE);
        divider.setVisibility(View.GONE);
    }

    public void clearStaticText(){
        if(textView != null) {
            textView.setText(defaultStaticText);
        }
    }

    public void setStaticText(String textOnSignBitmap){
        textView.setText(textOnSignBitmap);
    }

    protected void init(AttributeSet attrs){
        if(attrs != null){
            signaturePad = new SignaturePad(this.getContext(), attrs);
        }
        else{
            signaturePad = new SignaturePad(getContext());
        }
        textView = new TextView(this.getContext(), attrs);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        textParams.bottomMargin = signaturePad.convertPxToDp(16);
        textView.setTextColor(getResources().getColor(android.R.color.darker_gray));

        divider = new FrameLayout(this.getContext(), attrs);
        divider.setBackgroundColor(Color.BLACK);
        RelativeLayout.LayoutParams frameParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, signaturePad.convertPxToDp(1));
        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        frameParams.bottomMargin = signaturePad.convertPxToDp(8);
        frameParams.addRule(RelativeLayout.ABOVE, textView.getId());


        addView(divider, frameParams);
        addView(textView, textParams);
        textView.setVisibility(View.GONE);
        divider.setVisibility(View.GONE);

        addView(signaturePad);
    }

    public void setOnSignedListener(SignaturePad.OnSignedListener listener) {
        signaturePad.setOnSignedListener(listener);
    }

    public boolean isEmpty() {
        return signaturePad.isEmpty();
    }

    public String getSignatureSvg() {
        return signaturePad.getSignatureSvg();
    }

    public Bitmap getSignatureBitmap() {
        return signaturePad.getSignatureBitmap();
    }

    /**
     * @param compressPercentage Hint to the compressor, 0-100 percent. 0 meaning compress for
     *                           small size, 100 meaning compress for max quality. Some
     *                           formats, like PNG which is lossless, will ignore the
     *                           quality setting
     */
    public Bitmap getCompressedSignatureBitmap(int compressPercentage) {
        return signaturePad.getCompressedSignatureBitmap(compressPercentage);
    }

    /**
     * @param deiredWidth Desired width of the bitmap
     */
    public Bitmap getFixedSizeSignatureBitmap(int deiredWidth) {
        return getFixedSizeSignatureBitmap(deiredWidth);
    }

    public Bitmap getFixedSizeSignatureBitmap(int deiredWidth,int desiredHeight){
        return getFixedSizeSignatureBitmap(deiredWidth, desiredHeight);
    }

    public void setSignatureBitmap(final Bitmap signature) {
        signaturePad.setSignatureBitmap(signature);
    }

    public Bitmap getTransparentSignatureBitmap() {
        return signaturePad.getTransparentSignatureBitmap();
    }


    public Bitmap getTransparentSignatureBitmap(boolean trimBlankSpace) {
        return signaturePad.getTransparentSignatureBitmap(trimBlankSpace);
    }

    public Bitmap getCompressedTransparentSignatureBitmapTrimOnStrokes(int compressPercentage){
        return signaturePad.getCompressedTransparentSignatureBitmapTrimOnStrokes(compressPercentage);
    }

    public Bitmap getTransparentSignatureBitmapTrimOnStrokes() {
        return signaturePad.getTransparentSignatureBitmapTrimOnStrokes();
    }



    public void clear() {
        clear(true);
    }

    public void clear(boolean clearStaticText){
        signaturePad.clear();
        if(clearStaticText){
            clearStaticText();
        }
    }

    /**
     * Set the pen color from a given resource.
     * If the resource is not found, {@link Color#BLACK} is assumed.
     *
     * @param colorRes the color resource.
     */
    public void setPenColorRes(int colorRes) {
        signaturePad.setPenColorRes(colorRes);
    }

    /**
     * Set the pen color from a given color.
     *
     * @param color the color.
     */
    public void setPenColor(int color) {
        signaturePad.setPenColor(color);
    }

    /**
     * Set the minimum width of the stroke in pixel.
     *
     * @param minWidth the width in dp.
     */
    public void setMinWidth(float minWidth) {
        signaturePad.setMinWidth(minWidth);
    }

    /**
     * Set the maximum width of the stroke in pixel.
     *
     * @param maxWidth the width in dp.
     */
    public void setMaxWidth(float maxWidth) {
        signaturePad.setMaxWidth(maxWidth);
    }

    /**
     * Set the velocity filter weight.
     *
     * @param velocityFilterWeight the weight.
     */
    public void setVelocityFilterWeight(float velocityFilterWeight) {
        signaturePad.setVelocityFilterWeight(velocityFilterWeight);
    }

    public interface AskStaticText {
        public String getStaticText(String optionalDefault);
    }
}
