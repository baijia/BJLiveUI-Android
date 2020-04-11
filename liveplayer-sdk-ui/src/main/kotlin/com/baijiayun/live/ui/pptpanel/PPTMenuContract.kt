package com.baijiayun.live.ui.pptpanel

import com.baijiayun.live.ui.menu.rightmenu.RightMenuContract
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.imodels.IMediaControlModel

interface PPTMenuContract {
    interface Presenter : RightMenuContract.Presenter {
        fun changeAudio()

        fun changeVideo()
    }

    interface View : RightMenuContract.View {
        fun showVideoStatus(isOn: Boolean)

        fun showAudioStatus(isOn: Boolean)

        fun enableSpeakerMode()

        fun disableSpeakerMode()

        fun showVolume(level: LPConstants.VolumeLevel)

        fun showAudioRoomError()

        fun showMessage(s: String)

        fun showSpeakInviteDlg(invite:Int)

        fun showForceSpeakDlg(tipRes: Int)

        fun checkCameraAndMicPermission(): Boolean
    }
}