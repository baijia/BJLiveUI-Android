package com.baijiayun.live.ui.base

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.router.Router
import com.baijiayun.live.ui.router.RouterCode
import com.baijiayun.live.ui.utils.FileUtil
import com.baijiayun.livecore.utils.ToastCompat
import com.baijiayun.livecore.wrapper.LPRecorder
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by Shubo on 2019-10-10.
 */
abstract class BasePadFragment : Fragment() {
    protected val REQUEST_CODE_PERMISSION_CAMERA_TEACHER = 0
    protected val REQUEST_CODE_PERMISSION_CAMERA = 1
    protected val REQUEST_CODE_PERMISSION_MIC = 2
    protected val REQUEST_CODE_PERMISSION_WRITE = 3
    protected val REQUEST_CODE_PERMISSION_CAMERA_MIC = 4

    protected lateinit var routerViewModel: RouterViewModel

    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.run {
            routerViewModel = getViewModel { RouterViewModel() }
        }
        init(view)
        observeActions()
    }

    open fun init(view: View) {}

    abstract fun getLayoutId(): Int

    protected fun showSystemSettingDialog(type: Int) {
        activity?.let {
            if (!it.isFinishing) {
                MaterialDialog.Builder(it)
                        .title(getString(R.string.live_sweet_hint))
                        .content(mapType2String(type))
                        .positiveText(getString(R.string.live_quiz_dialog_confirm))
                        .positiveColor(ContextCompat.getColor(it, R.color.live_blue))
                        .onPositive { materialDialog, _ -> materialDialog.dismiss() }
                        .canceledOnTouchOutside(true)
                        .build()
                        .show()
            }
        }
    }

    private fun mapType2String(type: Int): String {
        return when (type) {
            REQUEST_CODE_PERMISSION_CAMERA -> getString(R.string.live_no_camera_permission)
            REQUEST_CODE_PERMISSION_MIC -> getString(R.string.live_no_mic_permission)
            REQUEST_CODE_PERMISSION_WRITE -> getString(R.string.live_no_write_permission)
            REQUEST_CODE_PERMISSION_CAMERA_MIC -> getString(R.string.live_no_camera_mic_permission)
            else -> ""
        }
    }

    protected fun showToastMessage(message: String) {
        if (TextUtils.isEmpty(message)) return
        activity?.let {
            it.runOnUiThread {
                if (!it.isFinishing && !it.isDestroyed) {
                    ToastCompat.showToastCenter(it, message, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    protected fun isTeacherOrAssistant() = routerViewModel.liveRoom.isTeacherOrAssistant || routerViewModel.liveRoom.isGroupTeacherOrAssistant

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSION_CAMERA_TEACHER -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Router.instance.getCacheSubjectByKey<Unit>(RouterCode.ENTER_SUCCESS)
                    .onNext(Unit)
            routerViewModel.actionNavigateToMain = true
            } else if (grantResults.isNotEmpty()) {
                showToastMessage("拒绝了相机授权,不能进入房间")
                activity?.finish()
            }
            REQUEST_CODE_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                attachLocalVideo()
            } else if (grantResults.isNotEmpty()) {
                showSystemSettingDialog(REQUEST_CODE_PERMISSION_CAMERA)
            }
            REQUEST_CODE_PERMISSION_MIC -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                attachLocalAudio()
            } else if (grantResults.isNotEmpty()) {
                showSystemSettingDialog(REQUEST_CODE_PERMISSION_MIC)
            }
            REQUEST_CODE_PERMISSION_WRITE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToastMessage(FileUtil.copyFile(routerViewModel.liveRoom.avLogFilePath, FileUtil.getSDPath()))
            } else if (grantResults.isNotEmpty()) {
                showSystemSettingDialog(REQUEST_CODE_PERMISSION_WRITE)
            }
            REQUEST_CODE_PERMISSION_CAMERA_MIC ->
                //0 1 2;0是全部授权，1：相机 2：音频
                if (grantResults.isNotEmpty()) {
                    var denyPermissions = 0
                    for (i in grantResults.indices) {
                        if (PackageManager.PERMISSION_GRANTED != grantResults[i]) {
                            denyPermissions += i + 1
                        }
                    }
                    if (denyPermissions != 0) {
                        when (denyPermissions) {
                            1 -> {
                                showSystemSettingDialog(REQUEST_CODE_PERMISSION_CAMERA)
                                attachLocalAudio()
                            }
                            2 -> {
                                showSystemSettingDialog(REQUEST_CODE_PERMISSION_MIC)
                                attachLocalVideo()
                            }
                            else -> showSystemSettingDialog(REQUEST_CODE_PERMISSION_CAMERA_MIC)
                        }
                    } else {
                        //先开视频后开音频，因为webrtc publish不同步
                        //localVideoItem调用refreshPlayable会走videoOn = true,和audioOn = false
                        //即isAudioAttached 是false 即使调用attachLocalAudio也不会立即变化成true
                        //所以调整成先开视频
                        attachLocalVideo()
                        attachLocalAudio()
                    }
                }
            else -> {
            }
        }
    }

    protected fun checkCameraPermission(): Boolean {
        activity?.let {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA)) {
                return true
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSION_CAMERA)
            }
        }
        return false
    }

    fun checkCameraAndMicPermission(): Boolean {
        activity?.let {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA) &&
                    PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(it, Manifest.permission.RECORD_AUDIO)) {
                return true
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), REQUEST_CODE_PERMISSION_CAMERA_MIC)
            }
        }
        return false
    }

    protected fun checkTeacherCameraPermission(): Boolean {
        activity?.let {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA)) {
                return true
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSION_CAMERA_TEACHER)
            }
        }
        return false
    }

    protected fun checkMicPermission(): Boolean {
        activity?.let {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(it, Manifest.permission.RECORD_AUDIO)) {
                return true
            } else {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_PERMISSION_MIC)
            }
        }
        return false
    }

    protected fun checkWriteFilePermission(): Boolean {
        activity?.let {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return true
            } else {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION_WRITE)
            }
        }
        return false
    }

    private fun isActivityFinish() = activity?.run {
        isFinishing || isDestroyed
    }?:true


    protected fun showDialogFragment(dialogFragment: BaseDialogFragment) {
        //添加activity判断，保证fragment中mHost!=null
        //fragment的show是调用commit。会checkLossState,出现
        // Can not perform this action after onSaveInstanceState的IllegalStateException
        //isStateSaved在onSaveInstanceState调用保证onSaveInstanceState后不执行
        //也可以用反射重写show。调用commitAllowingStateLoss
        if (isActivityFinish()||isStateSaved) {
            return
        }
        val ft = childFragmentManager.beginTransaction()
        dialogFragment.show(ft, dialogFragment.javaClass.simpleName + dialogFragment.hashCode())
        childFragmentManager.executePendingTransactions()
        dialogFragment.dialog.setOnDismissListener(DialogInterface.OnDismissListener {
            if (isActivityFinish() || isDetached) return@OnDismissListener
            val prev = childFragmentManager.findFragmentByTag(dialogFragment.javaClass.simpleName + dialogFragment.hashCode())
            val ftm = childFragmentManager.beginTransaction()
            prev?.let {
                ftm.remove(it)
            }
            ftm.commitAllowingStateLoss()
        })
    }

    protected open fun attachLocalVideo() {
        if (checkCameraPermission()) {
            routerViewModel.notifyLocalPlayableChanged.value = true to routerViewModel.liveRoom.getRecorder<LPRecorder>().isAudioAttached
        }
    }

    protected open fun attachLocalAudio() {
        if (checkMicPermission()) {
            if(!routerViewModel.liveRoom.getRecorder<LPRecorder>().isPublishing){
                routerViewModel.liveRoom.getRecorder<LPRecorder>().publish()
            }
            routerViewModel.liveRoom.getRecorder<LPRecorder>().attachAudio()
        }
    }

    protected open fun detachLocalVideo() {
        routerViewModel.notifyLocalPlayableChanged.value = false to routerViewModel.liveRoom.getRecorder<LPRecorder>().isAudioAttached
    }

    protected open fun observeActions() {
    }

    protected fun addFragment(layoutId: Int, fragment: Fragment, fragmentTag: String? = null) {
        val transaction = childFragmentManager.beginTransaction()
        if (fragmentTag == null) {
            transaction.add(layoutId, fragment)
        } else {
            transaction.add(layoutId, fragment, fragmentTag)
        }
        transaction.commitAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }
}

