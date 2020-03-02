package com.baijiayun.live.ui.base

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

/**
 * Created by Shubo on 2019-10-11.
 */
class BaseViewModelFactory<T>(val creator: () -> T) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return creator() as T
    }
}

inline fun <reified T : ViewModel> FragmentActivity.getViewModel(noinline creator: (() -> T)? = null): T {
    return if (creator == null)
        ViewModelProviders.of(this).get(T::class.java)
    else
        ViewModelProviders.of(this, BaseViewModelFactory(creator)).get(T::class.java)
}

inline fun <reified T : ViewModel> Fragment.getViewModel(noinline creator: (() -> T)? = null): T {
    return if (creator == null)
        ViewModelProviders.of(this).get(T::class.java)
    else
        ViewModelProviders.of(this, BaseViewModelFactory(creator)).get(T::class.java)
}

inline fun <reified T : ViewModel> Fragment.getActivityViewModel(noinline creator: (() -> T)? = null): T? {
    return if (creator == null) {
        activity?.let {
            ViewModelProviders.of(it).get(T::class.java)
        }
    } else {
        activity?.let {
            ViewModelProviders.of(it, BaseViewModelFactory(creator)).get(T::class.java)
        }
    }
}
fun BaseDialogFragment.getRouterViewModel():RouterViewModel? {
    return activity?.let {
        ViewModelProviders.of(it)[RouterViewModel::class.java]
    }
}