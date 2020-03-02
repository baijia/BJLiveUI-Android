package com.baijiayun.live.ui.error

import android.arch.lifecycle.ViewModel

/**
 * Created by yongjiaming on 2019-10-15
 * Describe:
 */
class ErrorFragmentModel : ViewModel(){

    var handlerWay : ErrorPadFragment.ErrorType = ErrorPadFragment.ErrorType.ERROR_HANDLE_CONFILICT

    var shouldShowTechContact = true

    var checkUnique = true

    lateinit var title : String

    lateinit var content : String

    fun init(handlerWay: ErrorPadFragment.ErrorType, shouldShowTechContact: Boolean = false, title: String = "", content: String = ""){
        this.handlerWay = handlerWay
        this.shouldShowTechContact = shouldShowTechContact
        this.title = title
        this.content = content
    }
}