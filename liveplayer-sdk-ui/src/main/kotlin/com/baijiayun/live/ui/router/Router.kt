package com.baijiayun.live.ui.router

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*

@Suppress("UNCHECKED_CAST")
class Router {
    private val routerMap = HashMap<String, Subject<*>>()
    private val caCheRouterMap = HashMap<String, Subject<*>>()

    fun <T> getSubjectByKey(key: String): PublishSubject<T> {
        var publishSubject = routerMap[key]
        if (publishSubject == null) {
            publishSubject = PublishSubject.create<Subject<*>>()
            routerMap[key] = publishSubject
        }
        return publishSubject as PublishSubject<T>
    }

    /**
     * 后注册可以收到事件，兼容LiveData
     */
    fun <T> getCacheSubjectByKey(key: String): BehaviorSubject<T> {
        var publishSubject = caCheRouterMap[key]
        if (publishSubject == null) {
            publishSubject = BehaviorSubject.create<Subject<*>>()
            caCheRouterMap[key] = publishSubject
        }
        return publishSubject as BehaviorSubject<T>
    }

    fun release() {
        routerMap.clear()
        caCheRouterMap.clear()
    }
    private object Holder {
        val INSTANCE = Router()
    }
    companion object {
        val instance: Router
            get() = Holder.INSTANCE
    }
}