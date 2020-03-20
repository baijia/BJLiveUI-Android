package com.baijiayun.live.ui.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.baijiayun.live.ui.DatabindingUtils
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.atMostViewSize
import com.baijiayun.live.ui.base.CommonAdapter
import com.baijiayun.live.ui.base.CommonViewHolder
import com.baijiayun.live.ui.base.RouterViewModel
import com.baijiayun.live.ui.getScreenHeight
import com.baijiayun.live.ui.toolbox.evaluation.EvaDialogContract
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.LPUserModel
import kotlinx.android.synthetic.main.layout_menu_opt_header.view.*
import java.util.*

class ChatOptMenuHelper {
    companion object {
        const val FORCE_SPEAK = "强制发言"
        const val INVITE_SPEAK = "邀请发言"
        const val INVITE_CANCEL = "取消邀请"
        const val END_SPEAK = "终止发言"
        const val ALLOW_CHAT = "允许聊天"
        const val FORBID_CHAT = "禁止聊天"
        const val PRVIATE_CHAT = "私聊"
        const val KICKOUT_ROOM = "踢出教室"
        const val MARGIN = 10
        const val FORBID_CHAT_DURATION = 24 * 3600L
        const val ALLOW_CHAT_DURATION = -1L

        /**
         * 1).弹出用户选项菜单
         * 1.无论向上还是向下弹出都保留一定的间距
         * 2.优先向下弹出，其次向上，都不满足向左弹出
         * 2).邀请发言和强制发言有配置项读取
         * 1.邀请发言3个状态 邀请发言 取消邀请 终止发言（邀请发言后30s超时自动发送取消请求）
         * 2.强制发言2个状态 强制发言 终止发言
         * 3.终止发言的判断条件是用户音视频关闭
         */
        @SuppressLint("SetTextI18n")
        fun showOptMenu(context: Context?, routerViewModel: RouterViewModel, view: View, lpUserModel: LPUserModel, fromChat: Boolean = false) {
            if (context == null) {
                return
            }
            if (context is Activity && (context.isDestroyed || context.isFinishing)) {
                return
            }
            if (routerViewModel.liveRoom.currentUser == lpUserModel) {
                return
            }
            if (routerViewModel.liveRoom.currentUser.type == LPConstants.LPUserType.Student || routerViewModel.liveRoom.currentUser.type == LPConstants.LPUserType.Visitor) {
                return
            }
            val items = ArrayList<String>()
            val enableChat = routerViewModel.chatLabelVisiable
            val enablePrivateChat = routerViewModel.liveRoom.chatVM.isLiveCanWhisper
            val isClassStarted = routerViewModel.isClassStarted.value == true
            if (lpUserModel.type == LPConstants.LPUserType.Assistant || lpUserModel.type == LPConstants.LPUserType.Teacher) {
                if (enableChat && enablePrivateChat) {
                    items.add(PRVIATE_CHAT)
                }
            } else {
                if (isClassStarted) {
                    items.add(getSpeakItem(routerViewModel, lpUserModel))
                }
                if (enableChat) {
                    if (routerViewModel.forbidChatUserNums.contains(lpUserModel.number)) {
                        items.add(ALLOW_CHAT)
                    } else {
                        items.add(FORBID_CHAT)
                    }
                    if (enablePrivateChat) {
                        items.add(PRVIATE_CHAT)
                    }
                }
//                items.add(KICKOUT_ROOM)
            }
            if (items.isEmpty()) {
                return
            }
            val popupWindow = PopupWindow(context)
            popupWindow.isFocusable = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                popupWindow.elevation = 2.0f
            }
            popupWindow.setBackgroundDrawable(ColorDrawable(0))
            popupWindow.width = context.resources.getDimensionPixelSize(R.dimen.main_video_menu_width)
            popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
            val bgDrawable = GradientDrawable()
            bgDrawable.setColor(ContextCompat.getColor(context, R.color.live_pad_menu_bg))
            bgDrawable.cornerRadius = DisplayUtils.dip2px(context, 6f).toFloat()
            val listView = ListView(context)
            val headerView = View.inflate(context, R.layout.layout_menu_opt_header, null)
            DatabindingUtils.loadImg(headerView.userAvatar, lpUserModel.avatar)
            when (lpUserModel.type) {
                LPConstants.LPUserType.Teacher -> {
                    if (fromChat) {
                        headerView.userRole.visibility = View.GONE
                        headerView.userName.text = "${lpUserModel.name}[${context.getString(R.string.live_teacher)}]"
                    } else {
                        headerView.userRole.visibility = View.VISIBLE
                        headerView.userRole.text = context.getString(R.string.live_teacher)
                        headerView.userRole.setTextColor(ContextCompat.getColor(context, R.color.live_blue))
                        headerView.userRole.background = ContextCompat.getDrawable(context, R.drawable.item_online_user_teacher_bg)
                        headerView.userName.text = lpUserModel.name
                    }
                }
                LPConstants.LPUserType.Assistant -> {
                    if (fromChat) {
                        headerView.userRole.visibility = View.GONE
                        headerView.userName.text = "${lpUserModel.name}[${context.getString(R.string.live_assistent)}]"
                    } else {
                        headerView.userRole.visibility = View.VISIBLE
                        headerView.userRole.text = context.getString(R.string.live_assistent)
                        headerView.userRole.setTextColor(ContextCompat.getColor(context, R.color.live_pad_orange))
                        headerView.userRole.background = ContextCompat.getDrawable(context, R.drawable.item_online_user_assistant_bg)
                        headerView.userName.text = lpUserModel.name
                    }
                }
                else -> {
                    headerView.userRole.visibility = View.GONE
                    headerView.userName.text = lpUserModel.name
                }
            }
            listView.addHeaderView(headerView)
            listView.setOnItemClickListener { _, _, position, _ ->
                if (position == 0) {
                    return@setOnItemClickListener
                }
                when (items[position - 1]) {
                    FORCE_SPEAK -> {
                        routerViewModel.liveRoom.speakQueueVM.controlRemoteSpeak(lpUserModel.userId, true, true)
                    }
                    INVITE_SPEAK -> {
                        routerViewModel.liveRoom.sendSpeakInviteReq(lpUserModel.userId, true)
                        routerViewModel.invitingUserIds.add(lpUserModel.userId)
                        routerViewModel.timeOutStart.value = lpUserModel.userId to true
                    }
                    INVITE_CANCEL -> {
                        routerViewModel.liveRoom.sendSpeakInviteReq(lpUserModel.userId, false)
                        routerViewModel.invitingUserIds.remove(lpUserModel.userId)
                        routerViewModel.timeOutStart.value = lpUserModel.userId to false
                    }
                    END_SPEAK -> {
                        routerViewModel.liveRoom.speakQueueVM.closeOtherSpeak(lpUserModel.userId)
                    }
                    ALLOW_CHAT -> {
                        routerViewModel.liveRoom.forbidChat(lpUserModel, ALLOW_CHAT_DURATION)
                    }
                    FORBID_CHAT -> {
                        routerViewModel.liveRoom.forbidChat(lpUserModel, FORBID_CHAT_DURATION)
                    }
                    PRVIATE_CHAT -> {
                        routerViewModel.privateChatUser.value = lpUserModel
                        routerViewModel.actionShowSendMessageFragment.value = true
                    }
                    KICKOUT_ROOM -> {
                        showKickOutDlg(context, routerViewModel, lpUserModel)
                    }
                }
                popupWindow.dismiss()
            }
            listView.background = bgDrawable
            listView.adapter = object : CommonAdapter<String>(context, R.layout.bjy_menu_chat_opt, items) {
                override fun convert(helper: CommonViewHolder, item: String?, position: Int) {
                    val text: TextView = helper.getView(R.id.textView)
                    text.text = item
                    if (item == KICKOUT_ROOM) {
                        text.setTextColor(ContextCompat.getColor(context, R.color.live_pad_disagree))
                    } else {
                        text.setTextColor(ContextCompat.getColor(context, R.color.live_pad_agree))
                    }
                }
            }
            listView.dividerHeight = 1
            listView.setPadding(0, DisplayUtils.dip2px(context, 2f), 0, DisplayUtils.dip2px(context, 2f))
            popupWindow.contentView = listView
            val measuredHeight = atMostViewSize(listView)[1]
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val screenHeight = getScreenHeight(context)
            val maxAvailableHeight = screenHeight - location[1] - view.height
            when {
                maxAvailableHeight > measuredHeight + MARGIN -> {
                    popupWindow.showAsDropDown(view)
                }
                location[1] > measuredHeight + MARGIN -> {
                    //向上弹出
                    popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0], location[1] - measuredHeight)
                }
                else -> {
                    //向左弹出
                    popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0] - popupWindow.width, location[1] - measuredHeight/2)
                }
            }
        }

        /**
         * 主动踢人
         */
        private fun showKickOutDlg(context: Context, routerViewModel: RouterViewModel, lpUserModel: LPUserModel) {
            if (context is Activity && (context.isDestroyed || context.isFinishing)) {
                return
            }
            MaterialDialog.Builder(context)
                    .apply {
                        title("您确定将\"${lpUserModel.name}\"踢出教室？")
                        content(context.getString(R.string.live_pad_kickout_tip))
                        contentColorRes(R.color.live_text_color_light)
                    }
                    .apply {
                        positiveText(context.getString(R.string.live_pad_kickout))
                        positiveColorRes(R.color.live_red)
                        onPositive { _, _ ->
                            routerViewModel.liveRoom.requestKickOutUser(lpUserModel.userId)
                        }
                    }
                    .apply {
                        negativeText(context.getString(R.string.live_cancel))
                        negativeColorRes(R.color.live_blue)
                    }
                    .build().show()
        }

        /**
         * 强制发言和邀请发言的item
         */
        private fun getSpeakItem(routerViewModel: RouterViewModel, lpUserModel: LPUserModel): String =
                //强制发言
                if (routerViewModel.liveRoom.partnerConfig.inviteSpeakType == 1) {
                    val lpMediaModel = routerViewModel.liveRoom.player.chmUserMediaModel[lpUserModel.userId]
                    if (lpMediaModel?.videoOn == true || lpMediaModel?.audioOn == true) {
                        END_SPEAK
                    } else {
                        FORCE_SPEAK
                    }
                } else {
                    val lpMediaModel = routerViewModel.liveRoom.player.chmUserMediaModel[lpUserModel.userId]
                    if (lpMediaModel?.audioOn == true || lpMediaModel?.videoOn == true) {
                        END_SPEAK
                    } else {
                        if (routerViewModel.invitingUserIds.contains(lpUserModel.userId)) {
                            INVITE_CANCEL
                        } else {
                            INVITE_SPEAK
                        }
                    }
                }
    }
}