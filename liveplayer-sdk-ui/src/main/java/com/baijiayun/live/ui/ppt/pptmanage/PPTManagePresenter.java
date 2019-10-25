package com.baijiayun.live.ui.ppt.pptmanage;

import com.baijiahulian.common.networkv2.BJProgressCallback;
import com.baijiahulian.common.networkv2.BJResponse;
import com.baijiahulian.common.networkv2.HttpException;
import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPDocumentModel;
import com.baijiayun.livecore.models.LPShortResult;
import com.baijiayun.livecore.models.LPUploadDocModel;
import com.baijiayun.livecore.utils.LPJsonUtils;
import com.baijiayun.livecore.utils.LPLogger;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * 管理PPT文档 生命周期不与fragment绑定
 * <p>
 * Created by Shubo on 2017/4/26.
 */

public class PPTManagePresenter implements PPTManageContract.Presenter {

    private LiveRoomRouterListener routerListener;
    private PPTManageContract.View view;

    private List<DocumentModel> addedDocuments;
    // 图片上传完成，从uploadingQueue中移除添加到waitDocAddQueue，为了保证添加顺序一个一个发docAdd并等待docAdd信令返回
    private LinkedBlockingQueue<DocumentUploadingModel> uploadingQueue;
    private Disposable subscriptionOfDocAdd, subscriptionOfDocDel, subscriptionOfReconnect;

    private List<String> deleteDocIds;

    public PPTManagePresenter() {
        uploadingQueue = new LinkedBlockingQueue<>();
        deleteDocIds = new ArrayList<>();
    }

    @Override
    public void attachView(PPTManageContract.View view) {
        this.view = view;
        if (addedDocuments.size() > 0 || uploadingQueue.size() > 0) {
            view.showPPTNotEmpty();
        } else {
            view.showPPTEmpty();
        }
    }

    @Override
    public boolean canOperateDocument() {
        return !(routerListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Assistant &&
                routerListener.getLiveRoom().getAdminAuth() != null && !routerListener.getLiveRoom().getAdminAuth().documentUpload);
    }


    @Override
    public void detachView() {
        view = null;
        deleteDocIds.clear();
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        addedDocuments = new ArrayList<>();
        for (LPDocumentModel m : routerListener.getLiveRoom().getDocListVM().getDocumentList()) {
            if (!m.name.equals("board"))
                addedDocuments.add(new DocumentModel(m));
        }

        subscriptionOfDocAdd = routerListener.getLiveRoom().getDocListVM().getObservableOfDocAdd()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lpDocumentModel -> {
                    addedDocuments.add(new DocumentModel(lpDocumentModel));
                    if (view != null)
                        view.notifyItemChanged(addedDocuments.size() - 1);
                    DocumentUploadingModel model = uploadingQueue.peek();
                    // 如果是本地上传，等待DocAdd信令返回后再队列里移除
                    if (model != null && model.status == DocumentUploadingModel.WAIT_SIGNAL
                            && String.valueOf(model.uploadModel.fileId).equals(lpDocumentModel.number)) {
                        // lpDocumentModel.number 既文档服务器分配的fileId
                        uploadingQueue.poll();
                        continueQueue();
                    }
                });

        subscriptionOfDocDel = routerListener.getLiveRoom().getDocListVM().getObservableOfDocDelete()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Consumer<String>) s -> {
                    for (int i = 0; i < addedDocuments.size(); i++) {
                        if (addedDocuments.get(i).id.equals(s)) {
                            addedDocuments.remove(i);
                            if (view != null) {
                                view.notifyItemRemoved(i);
                                if (addedDocuments.size() == 0) view.showPPTEmpty();
                            }
                            return;
                        }
                    }
                });

        subscriptionOfReconnect = routerListener.getLiveRoom().getObservableOfReconnected()
                .subscribe(aVoid -> {
                    if (uploadingQueue.size() > 0) {
                        startQueue();
                        continueQueue();
                    }
                });
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfDocAdd);
        RxUtils.dispose(subscriptionOfDocDel);
        RxUtils.dispose(subscriptionOfReconnect);
    }

    @Override
    public void destroy() {
        unSubscribe();
        if (view != null)
            view = null;
        routerListener = null;
    }

    @Override
    public IDocumentModel getItem(int position) {
        if (position < addedDocuments.size()) {
            return addedDocuments.get(position);
        } else {
            int count = position - addedDocuments.size();
            return (DocumentUploadingModel) uploadingQueue.toArray()[count];
        }
    }

    @Override
    public int getCount() {
        return addedDocuments.size() + uploadingQueue.size();
    }

    @Override
    public void uploadNewPics(List<String> picsPath) {
        for (String path : picsPath) {
            DocumentUploadingModel model = new DocumentUploadingModel(path);
            uploadingQueue.offer(model);
            view.notifyItemInserted(addedDocuments.size() + uploadingQueue.size() - 1);
        }
        if (addedDocuments.size() > 0 || uploadingQueue.size() > 0) {
            view.showPPTNotEmpty();
        } else {
            view.showPPTEmpty();
        }
        startQueue();
    }

    private void startQueue() {
        for (final DocumentUploadingModel model : uploadingQueue) {
            if (model.status == DocumentUploadingModel.INITIAL || model.status == DocumentUploadingModel.UPLOAD_FAIL) {
                routerListener.getLiveRoom().getDocListVM().uploadImageWithProgress(model.imgPath, this, new BJProgressCallback() {
                    @Override
                    public void onProgress(long l, long l1) {
                        model.uploadPercentage = (int) (l * 100 / l1);
                        LPLogger.i(String.valueOf(model.uploadPercentage));
                        List<Object> list = Arrays.asList(uploadingQueue.toArray());
                        if (view != null)// 如果关闭了当前dialog view为空
                            view.notifyItemChanged(addedDocuments.size() + list.indexOf(model));
                    }

                    @Override
                    public void onFailure(HttpException e) {
                        e.printStackTrace();
                        model.status = DocumentUploadingModel.UPLOAD_FAIL;
                        List<Object> list = Arrays.asList(uploadingQueue.toArray());
                        if (view != null)  // 如果关闭了当前dialog view为空
                            view.notifyItemChanged(addedDocuments.size() + list.indexOf(model));
                    }

                    @Override
                    public void onResponse(BJResponse bjResponse) {
                        try {
                            LPShortResult shortResult = LPJsonUtils.parseString(bjResponse.getResponse().body().string(), LPShortResult.class);
                            model.uploadModel = LPJsonUtils.parseJsonObject((JsonObject) shortResult.data, LPUploadDocModel.class);
                            model.status = DocumentUploadingModel.UPLOADED;
                            model.uploadPercentage = 90;
                            List<Object> list = Arrays.asList(uploadingQueue.toArray());
                            if (view != null)  // 如果关闭了当前dialog view为空
                                view.notifyItemChanged(addedDocuments.size() + list.indexOf(model));
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            continueQueue();
                        }
                    }
                });
                model.status = DocumentUploadingModel.UPLOADING;
            }
        }
    }

    private synchronized void continueQueue() {
        DocumentUploadingModel model = uploadingQueue.peek();
        if (model == null || routerListener == null) return;
        if (model.status == DocumentUploadingModel.UPLOADED) {
            model.status = DocumentUploadingModel.WAIT_SIGNAL;
            routerListener.getLiveRoom().getDocListVM().addPictureDocument(String.valueOf(model.uploadModel.fileId)
                    , model.uploadModel.fext, model.uploadModel.name, model.uploadModel.width, model.uploadModel.height, model.uploadModel.url);
        }
    }

    @Override
    public void selectItem(int position) {
        deleteDocIds.add(addedDocuments.get(position).id);
        if (deleteDocIds.size() > 0 ) view.showRemoveBtnEnable();
    }

    @Override
    public void deselectItem(int position) {
        deleteDocIds.remove(addedDocuments.get(position).id);
        if (deleteDocIds.size() == 0) view.showRemoveBtnDisable();
    }

    @Override
    public boolean isItemSelected(int position) {
        return deleteDocIds.contains(addedDocuments.get(position).id);
    }

    @Override
    public void removeSelectedItems() {
        Observable.fromIterable(deleteDocIds)
                .zipWith(Observable.interval(50, TimeUnit.MILLISECONDS),
                        (s, aLong) -> s)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onComplete() {
                        deleteDocIds.clear();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        routerListener.getLiveRoom().getDocListVM().deleteDocument(s);
                    }
                });
        view.showRemoveBtnDisable();
    }

    @Override
    public boolean isDocumentAdded(int position) {
        return position < addedDocuments.size();
    }

}
