package com.baijiayun.live.ui.chat.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.baijiayun.glide.Glide;
import com.baijiayun.glide.request.target.SimpleTarget;
import com.baijiayun.glide.request.transition.Transition;

public class URLImageParser {
    private TextView mTextView;
    private float mImageSize;

    /**
     *
     * @param textView 图文混排TextView
     * @param imageSize 图片显示高度
     */
    public URLImageParser(TextView textView, float imageSize) {
        mTextView = textView;
        mImageSize = imageSize;
    }

    public Drawable getDrawable(String url) {
        URLDrawable urlDrawable = new URLDrawable();
        Glide.with(mTextView.getContext()).asBitmap().load(url).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                urlDrawable.bitmap = changeBitmapSize(resource,mImageSize);
                urlDrawable.setBounds(0, 0, urlDrawable.bitmap.getWidth(), urlDrawable.bitmap.getHeight());
                mTextView.invalidate();
                mTextView.setText(mTextView.getText());
            }
        });
        return urlDrawable;
    }
    private Bitmap changeBitmapSize(Bitmap bitmap, float mImageSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float newWidth = mImageSize * 1.6f;
        float newHeight = height * newWidth / width;
        float scaleWidth = newWidth / width;
        float scaleHeight = newHeight / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return bitmap;
    }

    public class URLDrawable extends BitmapDrawable {
        public Bitmap bitmap;

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, 0, 0, getPaint());
            }
        }
    }
}
