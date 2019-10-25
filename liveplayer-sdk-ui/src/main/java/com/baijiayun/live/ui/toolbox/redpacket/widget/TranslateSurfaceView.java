package com.baijiayun.live.ui.toolbox.redpacket.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.baijiayun.live.ui.R;
import com.baijiayun.livecore.utils.DisplayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TranslateSurfaceView extends SurfaceView implements DrawInterface {

    private final String TAG = TranslateSurfaceView.class.getName();

    private final int MAX_RED_PACKET_NUMBER = 12;//红包最多个数
    private final int OPEN_SHOW_TIME = 500;//点开显示时间
    private final int OPEN_CALLBACK_TIME= 1000;//打开红包有效时间间隔
    private final int COUNT_OPEN_BITMAP = 14;//打开效果图序列
    private final int OPEN_SHOW_OPEN_TIME = 50;//打开序列单个显示时间

    private final int TIME_START_SLEEP = 1500;//启动后红包雨开始的延时时间ms

    private long callbackTime;
    private long mStartTime = 0;
    private OnClickRedPacketListener onClickRedPacketListener;

    private DrawHandler drawHandler;

    private int width;
    private int height;

    private Bitmap bitmap;
    private Bitmap []bitmapOpen;

    private int bitmapWidth;
    private int bitmapHeight;
    private int bitmapWidthOpen;
    private int bitmapHeightOpen;

    private Paint mPaintTitle;
    private Paint mPaintContext;

    //红包雨状态是否可以抢        true 可以     false 不可以
    private boolean isRobEnable = false;

    private List<MoveModel> moveList = new ArrayList<>();

    public TranslateSurfaceView(Context context) {
        this(context, null);
        init(context);
    }

    public TranslateSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
        init(context);
    }

    public TranslateSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        SurfaceHolder holder = getHolder();
        setZOrderOnTop(true);
        holder.setFormat(PixelFormat.TRANSLUCENT);

        mPaintTitle = new Paint();
        mPaintTitle.setAntiAlias(true);
//        mPaintTitle.setStrokeWidth(5);
        mPaintTitle.setColor(Color.WHITE);
        mPaintTitle.setTextSize(DisplayUtils.dip2px(context, 10));

        mPaintContext = new Paint();
        mPaintContext.setAntiAlias(true);
        mPaintContext.setColor(Color.parseColor("#FFDBDB"));
        mPaintContext.setTextSize(DisplayUtils.dip2px(context, 14));

        DrawThread drawThread = new DrawThread();
        drawThread.start();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        drawHandler = new DrawHandler(drawThread.getLooper(), this);
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_packet_down, options);
        bitmapWidth = DisplayUtils.dip2px(getContext(), 44);
        bitmapHeight = DisplayUtils.dip2px(getContext(), 112);

//        Matrix matrix = new Matrix();
//        matrix.postScale(, );

        bitmapOpen = new Bitmap[COUNT_OPEN_BITMAP];
        bitmapOpen[0] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_1, options);
        bitmapOpen[1] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_2, options);
        bitmapOpen[2] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_3, options);
        bitmapOpen[3] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_4, options);
        bitmapOpen[4] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_5, options);
        bitmapOpen[5] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_6, options);
        bitmapOpen[6] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_7, options);
        bitmapOpen[7] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_8, options);
        bitmapOpen[8] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_9, options);
        bitmapOpen[9] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_10, options);
        bitmapOpen[10] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_11, options);
        bitmapOpen[11] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_12, options);
        bitmapOpen[12] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_13, options);
        bitmapOpen[13] = BitmapFactory.decodeResource(getResources(), R.drawable.iv_lp_ui_red_open_14, options);

        bitmapWidthOpen = DisplayUtils.dip2px(getContext(), 120);
        bitmapHeightOpen = bitmapWidthOpen;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
    }

    public void addMoveModel(MoveModel moveModel) {
        moveList.add(moveModel);
    }

    public void start() {

        moveList.clear();
        for (int i = 0; i < MAX_RED_PACKET_NUMBER; i++) {
            generateModel();
        }
        mStartTime = System.currentTimeMillis();
        drawHandler.sendEmptyMessage(DrawHandler.START_DRAW_KEY);
    }

    public void pause() {
        drawHandler.sendEmptyMessage(DrawHandler.STOP_DRAW_KEY);
    }

    public void setOnClickRedPacketListenert(OnClickRedPacketListener listener) {
        onClickRedPacketListener = listener;
    }

    @Override
    public void startDraw() {

        if ((System.currentTimeMillis() - mStartTime) < TIME_START_SLEEP) {
            drawHandler.sendEmptyMessage(DrawHandler.START_DRAW_KEY);
            return;
        }

        if (moveList == null || moveList.size() == 0)
            return;

        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (null == canvas) {
            return;
        }

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        for (MoveModel moveModel : moveList) {

            if (moveModel.isOpen) {

                long time = System.currentTimeMillis();
                if (moveModel.openTime  == 0 ||
                        (time - moveModel.openTime) < OPEN_SHOW_TIME
                ) {
                    //首次渲染打开效果
                    moveModel.openTime = moveModel.openTime == 0 ? time : moveModel.openTime;

                    float x = (moveModel.x + (bitmapWidth / 2)) - (bitmapWidthOpen / 2);
                    float y = (moveModel.y + (bitmapHeight / 2)) - (bitmapHeightOpen / 2);

                    int index = (int) ((time - moveModel.openTime) / OPEN_SHOW_OPEN_TIME);
                    if (index > COUNT_OPEN_BITMAP - 1)
                        index = COUNT_OPEN_BITMAP - 1;

                    RectF rectf = new RectF();
                    rectf.left = x;
                    rectf.top = y;
                    rectf.right = rectf.left + bitmapWidthOpen;
                    rectf.bottom = rectf.top + bitmapHeightOpen;

                    canvas.drawBitmap(bitmapOpen[index], null, rectf, paint);

                    int moveY = (int) (y + bitmapHeightOpen / 3 - (index * (bitmapHeightOpen / 3 * 2) / COUNT_OPEN_BITMAP));

                    canvas.drawText("+" + moveModel.scoreAmount,
                            x + (rectf.right - rectf.left) / 8 * 3,
                            moveY, mPaintContext);
                } else if ((time - moveModel.openTime) >= OPEN_SHOW_TIME) {
                    resetMoveModel(moveModel);
                }
            } else {

                RectF rectf = new RectF();
                rectf.left = (int) moveModel.x;
                rectf.top = (int) moveModel.y;
                rectf.right = rectf.left + bitmapWidth;
                rectf.bottom = rectf.top + bitmapHeight;

                canvas.drawBitmap(bitmap, null, rectf, paint);
                if (moveModel.x > width || moveModel.y > height) {
                    resetMoveModel(moveModel);
                } else {
                    moveModel.y += moveModel.randomY;
                }
            }
        }
        holder.unlockCanvasAndPost(canvas);
        drawHandler.sendEmptyMessage(DrawHandler.START_DRAW_KEY);
    }

    @Override
    public void stopDraw() {
        drawHandler.removeMessages(DrawHandler.START_DRAW_KEY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                checkInRect((int) event.getX(), (int) event.getY());
                break;
        }
        return true;
    }

    /**
     * 是否点击在红包区域
     * @param x
     * @param y
     */
    private void checkInRect(int x, int y) {

        //是否可以抢
        if (!isRobEnable) {
            return;
        }

        int length = moveList.size();
        for (int i = 0; i < length; i++) {
            MoveModel moveModel = moveList.get(i);
            Rect rect = new Rect((int) moveModel.x, (int) moveModel.y, (int) moveModel.x + bitmapWidth, (int) moveModel.y + bitmapHeight);
            if (rect.contains(x, y)) {
                openMoveModel(moveModel);
                break;
            }
        }
    }

    private void openMoveModel(MoveModel moveModel) {

        moveModel.isRob = true;
//        long time = System.currentTimeMillis();

        if (onClickRedPacketListener == null)
            return;
        onClickRedPacketListener.onClick(moveModel);

//        if (callbackTime == 0) {
//            callbackTime = time;
//            onClickRedPacketListener.onClick(moveModel);
//        } else if ((time - callbackTime) >= OPEN_CALLBACK_TIME) {
//            callbackTime = time;
//            onClickRedPacketListener.onClick(moveModel);
//        } else {
//            moveModel.isOpen = true;
//            moveModel.scoreAmount = 0;
//        }
    }

    private void resetMoveModel(MoveModel moveModel) {
        Random random = new Random();
        moveModel.isOpen = false;
        moveModel.isRob = false;
        moveModel.openTime = 0;
        moveModel.x = random.nextInt(11) * (width / 10);
        moveModel.y = 0 - bitmapHeight;
        moveModel.rotationAngle = 0;
        moveModel.randomY = (random.nextInt(3) + 2) * getResources().getDisplayMetrics().density * 1.4f;
    }

    private void generateModel() {
        Random random = new Random();
        MoveModel moveModel = new MoveModel();
        moveModel.isRob = false;
        moveModel.moveId = moveList.size() + 1;
        moveModel.x = random.nextInt(11) * (width / 10);
        moveModel.y = 0 - bitmapHeight;
        moveModel.randomY = (random.nextInt(5) + 2) * getResources().getDisplayMetrics().density * 1.4f;
        moveList.add(moveModel);
    }

    public void setRobEnable(boolean robEnable) {
        this.isRobEnable = robEnable;
    }

    public interface OnClickRedPacketListener {

        void onClick(MoveModel moveModel);
    }

    public void destory() {
        if (null != drawHandler) {
            drawHandler.removeCallbacksAndMessages(null);
            drawHandler.getLooper().quit();
        }

        moveList.clear();
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        if (bitmapOpen != null) {
            for (int i = 0; i < bitmapOpen.length; i++) {
                bitmapOpen[i].recycle();
                bitmapOpen[i] = null;
            }
            bitmapOpen = null;
        }
    }
}
