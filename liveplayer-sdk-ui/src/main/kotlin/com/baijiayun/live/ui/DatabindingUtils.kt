package com.baijiayun.live.ui

import android.annotation.SuppressLint
import android.databinding.BindingAdapter
import android.widget.ImageView
import com.baijiayun.glide.Glide
import com.baijiayun.glide.load.resource.bitmap.CircleCrop
import com.baijiayun.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.*

class DatabindingUtils {
    @SuppressLint("ConstantLocale")
    companion object {

        private val simpleDataFormat by lazy {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        }

        @JvmStatic
        @BindingAdapter("imageUrl")
        fun loadImg(imageView: ImageView, url: String?) {
            val options = RequestOptions.bitmapTransform(CircleCrop()).error(R.drawable.ic_baijiayun_logo)
            url?.let {
                Glide.with(imageView.context).load(it).apply(options).into(imageView)
            }
        }


        fun formatTime(time : Long) : String{
            return simpleDataFormat.format(time)
        }
    }
}
