package com.baijiayun.live.ui.chat

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.util.Linkify
import android.view.*
import android.widget.*
import com.baijiayun.glide.Glide
import com.baijiayun.glide.request.RequestOptions
import com.baijiayun.glide.request.target.SimpleTarget
import com.baijiayun.glide.request.transition.Transition
import com.baijiayun.live.ui.R
import com.baijiayun.live.ui.activity.LiveRoomBaseActivity
import com.baijiayun.live.ui.base.BasePadFragment
import com.baijiayun.live.ui.base.getViewModel
import com.baijiayun.live.ui.chat.preview.ChatPictureViewFragment
import com.baijiayun.live.ui.chat.preview.ChatPictureViewPresenter
import com.baijiayun.live.ui.chat.preview.ChatSavePicDialogFragment
import com.baijiayun.live.ui.chat.preview.ChatSavePicDialogPresenter
import com.baijiayun.live.ui.chat.utils.CenterImageSpan
import com.baijiayun.live.ui.chat.utils.URLImageParser
import com.baijiayun.live.ui.chat.widget.ChatMessageView
import com.baijiayun.live.ui.databinding.ItemPadChatBinding
import com.baijiayun.live.ui.isPad
import com.baijiayun.live.ui.router.Router
import com.baijiayun.live.ui.router.RouterCode
import com.baijiayun.live.ui.utils.ChatImageUtil
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.LPUserModel
import com.baijiayun.livecore.models.imodels.IMessageModel
import com.baijiayun.livecore.models.imodels.IUserModel
import com.baijiayun.livecore.ppt.util.AliCloudImageUtil
import com.baijiayun.livecore.utils.CommonUtils
import com.baijiayun.livecore.utils.LPRxUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_pad_chat_list.*
import kotlinx.android.synthetic.main.fragment_pad_top_menu.*
import kotlinx.android.synthetic.main.item_pad_chat.view.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Created by yongjiaming on 2019-10-23
 * Describe:
 */
class ChatPadFragment : BasePadFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageReminderContainer: LinearLayout
    private var currentPosition = 0
    private val MESSAGE_TYPE_TEXT = 0
    private val MESSAGE_TYPE_EMOJI = 1
    private val MESSAGE_TYPE_IMAGE = 2

    private val messageAdapter by lazy {
        MessageAdapter()
    }

    private lateinit var chatViewModel: ChatViewModel

    private val simpleDataFormat by lazy {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    private val emojiSize by lazy {
        (DisplayUtils.getScreenDensity(context) * 32).toInt()
    }
    private lateinit var failedColorDrawable: ColorDrawable
    private lateinit var sendMessageBtn: TextView
    private lateinit var showNoticeBtn: ImageView
    private lateinit var noMessageTv: TextView

    private var savePictureDisposable: Disposable? = null

    override fun init(view: View) {

        activity?.run {
            chatViewModel = getViewModel {
                ChatViewModel(routerViewModel)
            }
        }

        recyclerView = view.findViewById(R.id.chat_recycler_view)
        messageReminderContainer = view.findViewById(R.id.activity_live_room_new_message_reminder_container)
        context?.let {
            failedColorDrawable = ColorDrawable(ContextCompat.getColor(it, R.color.live_half_transparent))
        }
        sendMessageBtn = view.findViewById(R.id.send_message_btn)
        sendMessageBtn.setOnClickListener {
            routerViewModel.actionShowSendMessageFragment.value = true
        }
        send_message_btn_back.setOnClickListener {
            routerViewModel.action2Chat.value = false
        }
        fragment_chat_private_end_btn.setOnClickListener {
            context?.let {
                routerViewModel.privateChatUser.value = null
                showToastMessage(it.getString(R.string.live_room_private_chat_cancel))
                messageAdapter.notifyDataSetChanged()
            }
        }
        fragment_chat_filter_close.setOnClickListener {
            filterMessage(false)
        }
        //私聊打开消息发送界面同时打开用户列表
        chat_private_start.setOnClickListener {
            routerViewModel.choosePrivateChatUser = true
            routerViewModel.actionShowSendMessageFragment.value = true
        }
        noMessageTv = view.findViewById(R.id.chat_no_message_tv)
    }

    override fun observeActions() {
        compositeDisposable.add(Router.instance.getCacheSubjectByKey<Unit>(RouterCode.ENTER_SUCCESS)
                .subscribe {
                    initSuccess()
                })
    }
    private fun initSuccess() {
        chat_private_start.visibility = if(routerViewModel.liveRoom.chatVM.isLiveCanWhisper)View.VISIBLE else View.GONE
        recyclerView.layoutManager = LinearLayoutWrapManager(context)
        recyclerView.adapter = messageAdapter

        chatViewModel.notifyDataSetChange.observe(this, Observer {
            val msgCount = chatViewModel.receivedNewMsgNum
            var needScroll = true
            if ((routerViewModel.action2Chat.value != true || currentPosition < chatViewModel.getCount() - 2) && msgCount > 0) {
                showMessageReminder(true)
                needScroll = false
            }
            messageAdapter.notifyDataSetChanged()
            if (needScroll && ::recyclerView.isInitialized && messageAdapter.itemCount > 0) {
                recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
            }
            noMessageTv.visibility = if (chatViewModel.getCount() > 0) View.GONE else View.VISIBLE
        })

        chatViewModel.notifyItemChange.observe(this, Observer {
            it?.run {
                messageAdapter.notifyItemChanged(it)
            }
        })

        chatViewModel.notifyItemInsert.observe(this, Observer {
            it?.run {
                messageAdapter.notifyItemInserted(it)
                noMessageTv.visibility = if (chatViewModel.getCount() > 0) View.GONE else View.VISIBLE
            }
        })
        routerViewModel.privateChatUser.observe(this, Observer {
            if (chatViewModel.isPrivateChatMode()) {
                showHavingPrivateChat(routerViewModel.privateChatUser.value!!)
                //进入私聊取消只看老师
                filterMessage(false)
            } else {
                showNoPrivateChat()
            }
            messageAdapter.notifyDataSetChanged()
        })
        chatViewModel.subscribe()
        if (routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.Single || routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.OneOnOne) {
            if (!isPad(context!!)) {
                send_message_btn_back.visibility = View.VISIBLE
            }
            chat_private_start.visibility = View.GONE
        }
        routerViewModel.sendPictureMessage.observe(this, Observer {
            it?.run {
                chatViewModel.sendImageMessage(this)
            }
        })

        routerViewModel.showSavePicDialog.observe(this, Observer {
            it?.run {
                showSavePicDialog(this)
            }
        })

        routerViewModel.saveChatPictureToGallery.observe(this, Observer {
            it?.run {
                saveImageToGallery(this)
            }
        })
    }

    override fun getLayoutId() = R.layout.fragment_pad_chat_list

    private fun showMessageReminder(needShow: Boolean) {
        if (!needShow || chatViewModel.receivedNewMsgNum == 0) {
            messageReminderContainer.visibility = View.GONE
            chatViewModel.receivedNewMsgNum = 0
            chatViewModel.redPointNumber.value = 0
            return
        }
        messageReminderContainer.visibility = View.VISIBLE
        val reminderTextView = messageReminderContainer.findViewById<TextView>(R.id.activity_live_room_new_message_reminder)
        reminderTextView.text = getString(R.string.live_room_new_chat_message, chatViewModel.receivedNewMsgNum)
        messageReminderContainer.setOnClickListener {
            if (chatViewModel.getCount() > 0) {
                recyclerView.smoothScrollToPosition(chatViewModel.getCount() - 1)
            }
            chatViewModel.receivedNewMsgNum = 0
            chatViewModel.redPointNumber.value = 0
        }
    }

    inner class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val dataBinding: ItemPadChatBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_pad_chat, parent, false)
            val messageContainer = dataBinding.root.findViewById<FrameLayout>(R.id.chat_container)
            when (viewType) {
                MESSAGE_TYPE_TEXT -> {
                    LayoutInflater.from(context).inflate(R.layout.item_pad_chat_text, messageContainer)
                    return TextViewHolder(dataBinding, dataBinding.root)
                }
                MESSAGE_TYPE_EMOJI -> {
                    LayoutInflater.from(context).inflate(R.layout.item_pad_chat_emoji, messageContainer)
                    return EmojiViewHolder(dataBinding, dataBinding.root)
                }
                MESSAGE_TYPE_IMAGE -> {
                    LayoutInflater.from(context).inflate(R.layout.bjy_item_chat_image, messageContainer)
                    return ImageViewHolder(dataBinding, dataBinding.root)
                }
                else -> {
                    return TextViewHolder(dataBinding, dataBinding.root)
                }
            }
        }

        override fun getItemCount() = chatViewModel.getCount()

        @SuppressLint("ClickableViewAccessibility", "RecyclerView")
        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            if (position < 0 || position >= itemCount) {
                return
            }
            currentPosition = position
            if (position == chatViewModel.getCount() - 1) {
                showMessageReminder(false)
            }
            val message: IMessageModel = chatViewModel.getMessage(position)
            showOptMenu(viewHolder,message.from)
            val ssb = SpannableStringBuilder()
            if (!chatViewModel.isPrivateChatMode() && message.isPrivateChat) {
                // 私聊item  首行显示私聊老师/助教
                if (!routerViewModel.liveRoom.chatVM.isLiveCanWhisper) return
                var spanText: SpannableString
                val isFromMe = message.from.userId == currentUserId()
                val isToMe = message.to == currentUserId()
                val toName = if (message.toUser == null) message.to else getEncodedName(message.toUser)
                if (isFromMe) {
                    val source = "私聊  "
                    spanText = SpannableString(source + toName)
                    if (message.toUser.type == LPConstants.LPUserType.Teacher || message.toUser.type == LPConstants.LPUserType.Assistant) {
                        spanText.setSpan(ForegroundColorSpan(ContextCompat.getColor(context!!, R.color.live_blue)), source.length, source.length+toName.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    } else {
                        spanText.setSpan(ForegroundColorSpan(ContextCompat.getColor(context!!, R.color.live_text_color_light)), source.length, source.length + toName.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                    ssb.append(spanText).append("\n")
                }
                if (isToMe) {
                    val source = "私聊  "
                    spanText = SpannableString("${source}我")
                    spanText.setSpan(ForegroundColorSpan(ContextCompat.getColor(context!!, R.color.live_blue)), source.length, "${source}我".length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    ssb.append(spanText).append("\n")
                }
            }
            when (viewHolder) {
                is TextViewHolder -> {
                    bindData(viewHolder.dataBinding, message)
                    viewHolder.chatMessageView.textViewChat.movementMethod = LinkMovementClickMethod.getInstance()
                    viewHolder.chatMessageView.textViewChat.setTextColor(ContextCompat.getColor(context!!, R.color.pad_message_text_color))
                    ssb.append(getMixText(message.content, viewHolder.chatMessageView.textViewChat))
                    ssb.setSpan(viewHolder.chatMessageView.lineHeightSpan, 0, ssb.length - 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                    viewHolder.chatMessageView.textViewChat.text = ssb
                    if (message.from.number == routerViewModel.liveRoom.currentUser.number) {
                        viewHolder.chatMessageView.background = ContextCompat.getDrawable(context!!, R.drawable.item_pad_chat_text_bg)
                    } else {
                        viewHolder.chatMessageView.background = ContextCompat.getDrawable(context!!, R.drawable.item_pad_chat_text_gray_bg)
                    }
                    viewHolder.chatMessageView.textViewChat.isFocusable = false
                    viewHolder.chatMessageView.textViewChat.isClickable = false
                    viewHolder.chatMessageView.textViewChat.isLongClickable = false
                    viewHolder.chatMessageView.enableTranslation(routerViewModel.liveRoom.partnerConfig.isEnableChatTranslation)
                    viewHolder.chatMessageView.setRecallStatus(chatViewModel.getRecallStatus(message))
                    viewHolder.chatMessageView.enableFilter((message.from.type == LPConstants.LPUserType.Teacher
                            || message.from.type == LPConstants.LPUserType.Assistant) && !chatViewModel.isPrivateChatMode())
                    viewHolder.chatMessageView.setFiltered(chatViewModel.filterMessage)
                    viewHolder.chatMessageView.message = message.content
                    viewHolder.chatMessageView.addTranslateMessage(chatViewModel.getTranslateResult(position))
                    viewHolder.chatMessageView.setOnProgressListener{
                        //判断是否有中文，有就翻译成英文，没有就翻译成中文
                        val fromLanguage: String
                        val toLanguage: String
                        val pattern = Pattern.compile("[\\u4E00-\\u9FBF]+")
                        if (pattern.matcher(message.content).find()) {
                            fromLanguage = "zh"
                            toLanguage = "en"
                        } else {
                            fromLanguage = "en"
                            toLanguage = "zh"
                        }
                        chatViewModel.translateMessage(getTranslateText(message.content), message.from.userId + message.time.time, fromLanguage, toLanguage)
                    }
                    viewHolder.chatMessageView.setOnFilterListener{ filterMessage(true)}
                    viewHolder.chatMessageView.setOnReCallListener{ chatViewModel.reCallMessage(message) }
                    if (message.from.type == LPConstants.LPUserType.Teacher || message.from.type == LPConstants.LPUserType.Assistant) {
                        Linkify.addLinks(viewHolder.chatMessageView.textViewChat, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
                    } else {
                        viewHolder.chatMessageView.textViewChat.autoLinkMask = 0
                    }
                }
                is EmojiViewHolder -> {
                    bindData(viewHolder.dataBinding, message)
                    context?.let {
                        val options = RequestOptions().error(R.drawable.live_ic_emoji_holder)
                                .placeholder(R.drawable.live_ic_emoji_holder)
                                .override(emojiSize, emojiSize)
                        Glide.with(it).load(message.url)
                                .apply(options)
                                .into(viewHolder.emojiIv)
                    }
                    val gestureDetectorCompat = GestureDetectorCompat(context, PressListener(message, viewHolder, MESSAGE_TYPE_EMOJI))
                    viewHolder.itemView.setOnTouchListener { _, event ->
                        gestureDetectorCompat.onTouchEvent(event)
                        true
                    }
                }
                is ImageViewHolder -> {
                    bindData(viewHolder.dataBinding, message)
                    viewHolder.ivImg.setOnClickListener(null)
                    if (message is UploadingImageModel) {
                        val options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(message.getUrl(), options)
                        val size = intArrayOf(options.outWidth, options.outHeight)
                        ChatImageUtil.calculateImageSize(size, DisplayUtils.dip2px(context, 100f), DisplayUtils.dip2px(context!!, 50f))

                        var requestOptions = RequestOptions()
                        requestOptions = requestOptions.override(size[0], size[1])
                                .placeholder(failedColorDrawable)
                                .error(failedColorDrawable)
                        context?.let {
                            Glide.with(it).load(File(message.getUrl()))
                                    .apply(requestOptions)
                                    .into(viewHolder.ivImg)
                        }

                        if (message.status == UploadingImageModel.STATUS_UPLOADING) {
                            viewHolder.tvMask.visibility = View.VISIBLE
                            viewHolder.tvExclamation.visibility = View.GONE
                        } else if (message.status == UploadingImageModel.STATUS_UPLOAD_FAILED) {
                            viewHolder.tvMask.visibility = View.GONE
                            viewHolder.tvExclamation.visibility = View.VISIBLE
                            viewHolder.ivImg.setOnClickListener {
                                chatViewModel.continueUploadQueue()
                            }
                        }
                    } else {
                        val target = ImageTarget(context, viewHolder.ivImg)
                        var requestOptions = RequestOptions()
                        requestOptions = requestOptions.override(300, 300)
                                .placeholder(failedColorDrawable)
                                .error(failedColorDrawable)
                        context?.let {
                            Glide.with(it).asBitmap()
                                    .load(AliCloudImageUtil.getScaledUrl(message.url, AliCloudImageUtil.SCALED_MFIT, 300, 300))
                                    .apply(requestOptions)
                                    .into(target)
                        }
                        with(viewHolder) {
                            tvMask.visibility = View.GONE
                            tvExclamation.visibility = View.GONE
                        }
                        val gestureDetectorCompat = GestureDetectorCompat(context, PressListener(message, viewHolder, MESSAGE_TYPE_IMAGE))
                        viewHolder.ivImg.setOnTouchListener { _, event ->
                            gestureDetectorCompat.onTouchEvent(event)
                            true
                        }
                    }
                }
            }
        }

        private fun showOptMenu(viewHolder: RecyclerView.ViewHolder, userModel: IUserModel) {
            if (userModel !is LPUserModel) {
                return
            }
            viewHolder.itemView.chat_user_name.setOnClickListener {
                ChatOptMenuHelper.showOptMenu(context,routerViewModel,it,userModel,true)
            }
            viewHolder.itemView.chat_user_avatar.setOnClickListener {
                ChatOptMenuHelper.showOptMenu(context,routerViewModel,it,userModel,true)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (chatViewModel.getMessage(position).messageType) {
                LPConstants.MessageType.Text -> MESSAGE_TYPE_TEXT
                LPConstants.MessageType.Emoji, LPConstants.MessageType.EmojiWithName -> MESSAGE_TYPE_EMOJI
                LPConstants.MessageType.Image -> MESSAGE_TYPE_IMAGE
                else -> MESSAGE_TYPE_TEXT
            }
        }

        private fun bindData(dataBinding: ItemPadChatBinding, message: IMessageModel) {
            dataBinding.message = message
            dataBinding.chatFragment = this@ChatPadFragment
        }
    }

    private fun currentUserId() = routerViewModel.liveRoom.currentUser.userId
    
    fun getMessageFromText(message: IMessageModel): String {
        var role = ""
        if (message.from.type == LPConstants.LPUserType.Teacher) {
            role = context?.resources?.getString(R.string.live_teacher) ?: ""
        }
        if (message.from.type == LPConstants.LPUserType.Assistant) {
            role = context?.resources?.getString(R.string.live_assistent) ?: ""
        }
        return if (role.isNotEmpty()) {
            CommonUtils.getEncodePhoneNumber(message.from.name) + "[" + role + "]"
        } else {
            CommonUtils.getEncodePhoneNumber(message.from.name)
        }
    }

    fun getMessageTime(message: IMessageModel): String = simpleDataFormat.format(message.time)

    fun getClientTypeRes(message: IMessageModel): Drawable? {
        val clientIcon: Drawable?
        when (message.from.endType) {
            LPConstants.LPEndType.PC_Client -> {
                clientIcon = context?.let {  ContextCompat.getDrawable(it,R.drawable.ic_chat_client_pc)}
            }
            LPConstants.LPEndType.PC_H5 -> {
                clientIcon = context?.let {  ContextCompat.getDrawable(it,R.drawable.ic_chat_client_phone_h5)}
            }
            LPConstants.LPEndType.PC_HTML -> {
                clientIcon = context?.let {  ContextCompat.getDrawable(it,R.drawable.ic_chat_client_pc_web)}
            }
            LPConstants.LPEndType.PC_MAC_Client -> {
                clientIcon = context?.let {  ContextCompat.getDrawable(it,R.drawable.ic_chat_client_mac) }
            }
            LPConstants.LPEndType.Android -> {
                clientIcon = context?.let {  ContextCompat.getDrawable(it,R.drawable.ic_chat_client_android)}
            }
            LPConstants.LPEndType.iOS -> {
                clientIcon = context?.let {  ContextCompat.getDrawable(it,R.drawable.ic_chat_client_ios)}
            }
            else -> {
                clientIcon = context?.let {  ContextCompat.getDrawable(it,R.drawable.ic_chat_client_unkown)}
            }
        }
        return clientIcon
    }

    /**
     * 翻译去除表情中的[大笑]
     *
     * @param content
     * @return
     */
    private fun getTranslateText(srcContent: String): String {
        var content = srcContent
        val p = Pattern.compile("\\[[a-zA-Z0-9\u4e00-\u9fa5]+]")
        val m = p.matcher(content)
        while (m.find()) {
            val group = m.group()
            if (chatViewModel.expressions.containsKey(group)) {
                content = content.replace(group, "")
            }
        }
        return content
    }

    private fun getMixText(srcContent: String, textView: TextView): SpannableStringBuilder {
        var content = srcContent
        val p = Pattern.compile("\\[[a-zA-Z0-9\u4e00-\u9fa5]+]")
        val m = p.matcher(content)
        while (m.find()) {
            val group = m.group()
            if (chatViewModel.expressionNames.containsKey(group)) {
                val name = chatViewModel.expressionNames[group]
                if (name != null) {
                    content = content.replace(group, name)
                }
            }
        }
        val matcher = p.matcher(content)
        val ssb = SpannableStringBuilder(content)
        while (matcher.find()) {
            val group = matcher.group()
            if (chatViewModel.expressions.containsKey(group)) {
                val drawable = URLImageParser(textView, textView.textSize).getDrawable(chatViewModel.expressions[group])
                val centerImageSpan = CenterImageSpan(drawable, ImageSpan.ALIGN_BASELINE)
                ssb.setSpan(centerImageSpan, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                ssb.removeSpan(group)
            }
        }
        return ssb
    }

    private fun showBigChatPic(url: String) {
        val fragment = ChatPictureViewFragment.newInstance(url)
        val presenter = ChatPictureViewPresenter()
        presenter.setRouter((activity as LiveRoomBaseActivity).routerListener)
        fragment.setPresenter(presenter)
        showDialogFragment(fragment)
    }

    private fun showSavePicDialog(bmpArray: ByteArray) {
        val fragment = ChatSavePicDialogFragment()
        val presenter = ChatSavePicDialogPresenter(bmpArray)
        presenter.setRouter((activity as LiveRoomBaseActivity).routerListener)
        fragment.setPresenter(presenter)
        showDialogFragment(fragment)
    }

    /**
     * 保存图片
     */
    private fun saveImageToGallery(bmpArray: ByteArray) {
        LPRxUtils.dispose(savePictureDisposable)
        savePictureDisposable = Observable.just(1)
                .observeOn(Schedulers.io())
                .map {
                    // 首先保存图片
                    val appDir = File(Environment.getExternalStorageDirectory(), "bjhl_lp_image")
                    if (!appDir.exists()) {
                        appDir.mkdir()
                    }
                    val fileName = System.currentTimeMillis().toString() + ".jpg"
                    val file = File(appDir, fileName)
                    try {
                        val bmp = BitmapFactory.decodeByteArray(bmpArray, 0, bmpArray.size)
                        val fos = FileOutputStream(file)
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                        fos.flush()
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    // 其次把文件插入到系统图库
                    try {
                        MediaStore.Images.Media.insertImage(context?.contentResolver,
                                file.absolutePath, fileName, null)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                    file
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { file -> showToastMessage("图片保存在" + file.absolutePath) }
    }

    private fun showNoPrivateChat() {
        fragment_chat_private_status_container.visibility = View.GONE
    }

    private fun showHavingPrivateChat(privateChatUser: IUserModel) {
        if (!routerViewModel.liveRoom.chatVM.isLiveCanWhisper) {
            return
        }
        if (context == null) {
            return
        }
        fragment_chat_private_status_container.visibility = View.VISIBLE
        val content = "私聊：${privateChatUser.name}"
        val spannableString = SpannableString(content)
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(context!!,R.color.live_blue)),3,content.length,Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        fragment_chat_private_user.text = spannableString
    }

    private fun filterMessage(filter: Boolean) {
        chatViewModel.filterMessage = filter
        messageAdapter.notifyDataSetChanged()
        fragment_chat_filter.visibility = if (filter) View.VISIBLE else View.GONE
    }

    private fun getEncodedName(userModel: IUserModel): String {
        return CommonUtils.getEncodePhoneNumber(userModel.name)
    }

    private fun showMenu(x: Int, y: Int, parentView: View, iMessageModel: IMessageModel, type: Int) {
        if (context == null) {
            return
        }
        val popupWindow = PopupWindow(context)
        popupWindow.isFocusable = true
        popupWindow.width = DisplayUtils.dip2px(context, 60f)
        popupWindow.setBackgroundDrawable(ColorDrawable(0))

        val items = ArrayList<String>()
        val recallStatus = chatViewModel.getRecallStatus(iMessageModel)
        if (recallStatus == ChatMessageView.RECALL) {
            items.add(context!!.getString(R.string.live_chat_recall))
        }
        if (recallStatus == ChatMessageView.DELETE) {
            items.add(context!!.getString(R.string.live_chat_delete))
        }
        items.add(context!!.getString(R.string.live_chat_copy))
        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
        val adapter = ArrayAdapter(context!!, R.layout.bjy_menu_chat_message, items.toTypedArray())
        val listView = ListView(context)

        val bgDrawable = GradientDrawable()
        bgDrawable.setColor(ContextCompat.getColor(context!!, R.color.live_pad_menu_bg))
        bgDrawable.cornerRadius = DisplayUtils.dip2px(context!!, 6f).toFloat()
        listView.background = bgDrawable
        listView.adapter = adapter
        listView.dividerHeight = 0
        listView.setPadding(0, DisplayUtils.dip2px(context!!, 2f), 0, DisplayUtils.dip2px(context!!, 2f))
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = items[position]
            if (context!!.resources.getString(R.string.live_chat_copy) == item) {
                when (type) {
                    MESSAGE_TYPE_EMOJI -> {
                        var content = chatViewModel.expressionNames[iMessageModel.content]
                        if (TextUtils.isEmpty(content)) {
                            content = iMessageModel.content
                        }
                        copy(content!!)
                    }
                    MESSAGE_TYPE_IMAGE -> copy("[img:" + iMessageModel.url + "]")
                    else -> copy(iMessageModel.content)

                }
            } else {
                chatViewModel.reCallMessage(iMessageModel)
            }
            popupWindow.dismiss()
        }
        popupWindow.contentView = listView
        popupWindow.showAtLocation(parentView, Gravity.NO_GRAVITY, x - popupWindow.width / 2, y - popupWindow.height)
    }

    private fun copy(copyStr: String): Boolean {
        return try {
            //获取剪贴板管理器
            val cm = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 创建普通字符型ClipData
            val mClipData = ClipData.newPlainText("Label", copyStr)
            // 将ClipData内容放到系统剪贴板里。
            cm.primaryClip = mClipData
            true
        } catch (e: Exception) {
            false
        }

    }

    inner class PressListener(val iMessageModel: IMessageModel?, holder: RecyclerView.ViewHolder, private var type: Int) : GestureDetector.SimpleOnGestureListener() {
        private var parent: View? = null
        private var position = 0

        init {
            this.parent = holder.itemView
            this.position = holder.adapterPosition
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            if (parent == null || iMessageModel == null) {
                return
            }
            showMenu(e.rawX.toInt(), e.rawY.toInt(), parent!!, iMessageModel, type)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (type != MESSAGE_TYPE_IMAGE || iMessageModel == null) {
                return true
            }
            showBigChatPic(iMessageModel.url)
            return true
        }
    }
    class TextViewHolder(val dataBinding: ItemPadChatBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatMessageView: ChatMessageView = itemView.findViewById(R.id.chat_message_content)
    }

    class EmojiViewHolder(val dataBinding: ItemPadChatBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiIv: ImageView = itemView.findViewById(R.id.item_chat_emoji)
    }

    class ImageViewHolder(val dataBinding: ItemPadChatBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.item_chat_image_name)
        val tvExclamation: TextView = itemView.findViewById(R.id.item_chat_image_exclamation)
        val ivImg: ImageView = itemView.findViewById(R.id.item_chat_image)
        val tvMask: TextView = itemView.findViewById(R.id.item_chat_image_mask)
    }

    private class ImageTarget internal constructor(context: Context?, private val imageView: ImageView) : SimpleTarget<Bitmap>() {
        private val mContext: WeakReference<Context?> = WeakReference(context)

        override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
            val context = mContext.get() ?: return
            val lp = imageView.layoutParams as RelativeLayout.LayoutParams
            val size = intArrayOf(bitmap.width, bitmap.height)
            ChatImageUtil.calculateImageSize(size, DisplayUtils.dip2px(context, 100f), DisplayUtils.dip2px(context, 50f))
            lp.width = size[0]
            lp.height = size[1]
            imageView.layoutParams = lp
            imageView.setImageBitmap(bitmap)
        }
    }
    companion object {
        fun newInstance() = ChatPadFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LPRxUtils.dispose(savePictureDisposable)
    }
}