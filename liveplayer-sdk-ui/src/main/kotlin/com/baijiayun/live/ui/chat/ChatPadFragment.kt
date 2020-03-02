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
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ImageSpan
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
import com.baijiayun.live.ui.chat.utils.TextLineHeightSpan
import com.baijiayun.live.ui.chat.utils.URLImageParser
import com.baijiayun.live.ui.chat.widget.ChatMessageView
import com.baijiayun.live.ui.databinding.ItemPadChatBinding
import com.baijiayun.live.ui.isPad
import com.baijiayun.live.ui.utils.ChatImageUtil
import com.baijiayun.live.ui.utils.DisplayUtils
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.livecore.models.imodels.IMessageModel
import com.baijiayun.livecore.ppt.util.AliCloudImageUtil
import com.baijiayun.livecore.utils.CommonUtils
import com.baijiayun.livecore.utils.LPRxUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_pad_chat_list.*
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
    private lateinit var sendMessageBtn : TextView
    private lateinit var showNoticeBtn : ImageView
    private lateinit var noMessageTv : TextView

    private var savePictureDisposable : Disposable? = null

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
        showNoticeBtn = view.findViewById(R.id.chat_notice_btn)
        sendMessageBtn.setOnClickListener{
            routerViewModel.actionShowSendMessageFragment.value = true
        }
        showNoticeBtn.setOnClickListener{
            routerViewModel.actionShowAnnouncementFragment.value = true
        }
        send_message_btn_back.setOnClickListener {
            routerViewModel.action2Chat.value = false
        }
        noMessageTv = view.findViewById(R.id.chat_no_message_tv)
    }

    override fun observeActions() {
        routerViewModel.actionNavigateToMain.observe(this, Observer { it2 ->
            if (it2 != true) {
                return@Observer
            }
            recyclerView.layoutManager = LinearLayoutWrapManager(context)
            recyclerView.adapter = messageAdapter

            chatViewModel.notifyDataSetChange.observe(this, Observer {
                val msgCount = chatViewModel.receivedNewMessageNumber.value
                if (currentPosition < chatViewModel.getCount() - 2 && msgCount != null && msgCount > 0) {
                    showMessageReminder(true)
                }
                messageAdapter.notifyDataSetChanged()
                noMessageTv.visibility = if(chatViewModel.getCount() > 0) View.GONE else View.VISIBLE
            })

            chatViewModel.notifyItemChange.observe(this, Observer {
                it?.run {
                    messageAdapter.notifyItemChanged(it)
                }
            })

            chatViewModel.notifyItemInsert.observe(this, Observer {
                it?.run {
                    messageAdapter.notifyItemInserted(it)
                    noMessageTv.visibility = if(chatViewModel.getCount() > 0) View.GONE else View.VISIBLE
                }
            })
            chatViewModel.hasMyNewMessage.observe(this, Observer {
                it?.let {
                    if (::recyclerView.isInitialized) {
                        //接收到新消息自动滑动底部
                        if (messageAdapter.itemCount != 0) {
                            recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                        }
                    }
                }
            })
            chatViewModel.subscribe()
            if (routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.Single || routerViewModel.liveRoom.roomType == LPConstants.LPRoomType.OneOnOne) {
                if (!isPad(context!!)) {
                    send_message_btn_back.visibility = View.VISIBLE
                }
                chat_notice_btn.visibility = View.GONE
            }
        })

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
        if (!needShow || chatViewModel.receivedNewMessageNumber.value == 0) {
            messageReminderContainer.visibility = View.GONE
            chatViewModel.receivedNewMessageNumber.value = 0
            return
        }
        messageReminderContainer.visibility = View.VISIBLE
        val reminderTextView = messageReminderContainer.findViewById<TextView>(R.id.activity_live_room_new_message_reminder)
        reminderTextView.text = getString(R.string.live_room_new_chat_message, chatViewModel.receivedNewMessageNumber.value)
        messageReminderContainer.setOnClickListener {
            recyclerView.smoothScrollToPosition(chatViewModel.getCount() - 1)
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

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            if (position == chatViewModel.getCount() - 1)
                showMessageReminder(false)
            val message: IMessageModel = chatViewModel.getMessage(position)
            when (viewHolder) {
                is TextViewHolder -> {
                    bindData(viewHolder.dataBinding, message)
                    val fontMetricsInt = viewHolder.contentTextView.paint.fontMetricsInt
                    val lineHeightSpan = TextLineHeightSpan(fontMetricsInt, 6)
                    val builder = getMixText(message.content,viewHolder.contentTextView)
                    builder.setSpan(lineHeightSpan,0,builder.length,Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                    viewHolder.contentTextView.text = builder
                    if (message.from.number == routerViewModel.liveRoom.currentUser.number) {
                        viewHolder.contentTextView.background = context?.resources?.getDrawable(R.drawable.item_pad_chat_text_bg)
                    } else {
                        viewHolder.contentTextView.background = context?.resources?.getDrawable(R.drawable.item_pad_chat_text_gray_bg)
                    }
                    val gestureDetectorCompat = GestureDetectorCompat(context, PressListener(message, viewHolder,MESSAGE_TYPE_TEXT))
                    viewHolder.itemView.setOnTouchListener { _, event ->
                        gestureDetectorCompat.onTouchEvent(event)
                        true
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
                    val gestureDetectorCompat = GestureDetectorCompat(context, PressListener(message, viewHolder,MESSAGE_TYPE_EMOJI))
                    viewHolder.itemView.setOnTouchListener { _, event ->
                        gestureDetectorCompat.onTouchEvent(event)
                        true
                    }
                }
                is ImageViewHolder -> {
                    bindData(viewHolder.dataBinding, message)
                    viewHolder.ivImg.setOnClickListener(null)
                    if(message is UploadingImageModel){
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
                            viewHolder.ivImg.setOnClickListener{
                                chatViewModel.continueUploadQueue()
                            }
                        }
                    } else{
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
                        with(viewHolder){
                            tvMask.visibility = View.GONE
                            tvExclamation.visibility = View.GONE
                        }
                        val gestureDetectorCompat = GestureDetectorCompat(context, PressListener(message, viewHolder,MESSAGE_TYPE_IMAGE))
                        viewHolder.ivImg.setOnTouchListener { _, event ->
                            gestureDetectorCompat.onTouchEvent(event)
                            true
                        }
                    }
                }
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

    private fun getMixText(content: String, textView: TextView): SpannableStringBuilder {
        var content = content
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

    class TextViewHolder(val dataBinding: ItemPadChatBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentTextView: TextView = itemView.findViewById(R.id.chat_message_content)
    }

    class EmojiViewHolder(val dataBinding: ItemPadChatBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiIv: ImageView = itemView.findViewById(R.id.item_chat_emoji)
    }

    class ImageViewHolder(val dataBinding: ItemPadChatBinding, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.item_chat_image_name)
        val tvExclamation: TextView = itemView.findViewById(R.id.item_chat_image_exclamation)
        val ivImg : ImageView = itemView.findViewById(R.id.item_chat_image)
        val tvMask : TextView = itemView.findViewById(R.id.item_chat_image_mask)
    }

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
                clientIcon = context?.resources?.getDrawable(R.drawable.ic_chat_client_pc)
            }
            LPConstants.LPEndType.PC_H5 -> {
                clientIcon = context?.resources?.getDrawable(R.drawable.ic_chat_client_phone_h5)
            }
            LPConstants.LPEndType.PC_HTML -> {
                clientIcon = context?.resources?.getDrawable(R.drawable.ic_chat_client_pc_web)
            }
            LPConstants.LPEndType.PC_MAC_Client -> {
                clientIcon = context?.resources?.getDrawable(R.drawable.ic_chat_client_mac)
            }
            LPConstants.LPEndType.Android -> {
                clientIcon = context?.resources?.getDrawable(R.drawable.ic_chat_client_android)
            }
            LPConstants.LPEndType.iOS -> {
                clientIcon = context?.resources?.getDrawable(R.drawable.ic_chat_client_ios)
            }
            else -> {
                clientIcon = context?.resources?.getDrawable(R.drawable.ic_chat_client_unkown)
            }
        }
        return clientIcon
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
                .subscribe { file -> showToastMessage("图片保存在" + file.absolutePath)}
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
        bgDrawable.setColor(ContextCompat.getColor(context!!,R.color.live_pad_stroke_grey))
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
    inner class PressListener : GestureDetector.SimpleOnGestureListener {
        private var iMessageModel: IMessageModel? = null
        private var parent: View? = null
        private var type = 0
        private var position = 0

        constructor(iMessageModel: IMessageModel, holder: RecyclerView.ViewHolder, type: Int) {
            this.iMessageModel = iMessageModel
            this.parent = holder.itemView
            this.type = type
            this.position = holder.adapterPosition
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            showMenu(e.rawX.toInt(), e.rawY.toInt(), parent!!, iMessageModel!!, type)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (type != MESSAGE_TYPE_IMAGE) {
                return true
            }
            showBigChatPic(iMessageModel!!.url)
            return true
        }
    }
    companion object{
        fun newInstance() = ChatPadFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        LPRxUtils.dispose(savePictureDisposable)
    }
}