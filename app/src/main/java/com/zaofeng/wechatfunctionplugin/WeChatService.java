package com.zaofeng.wechatfunctionplugin;

import static com.zaofeng.wechatfunctionplugin.model.ConstantData.delayTime;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.ClassAlbumPreviewUI;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.ClassContactInfoUI;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.ClassFMessageConversationUI;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.ClassLauncherUI;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.ClassSnsCommentDetailUI;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.ClassSnsTimeLineUI;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.ClassSnsTimeLineUploadUI;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.IdButtonSend;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.IdButtonVoiceChat;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.IdEditChat;
import static com.zaofeng.wechatfunctionplugin.model.ConstantTargetName.IdListViewChat;
import static com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.AlbumPreviewUI;
import static com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.ChatUI;
import static com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.ContactInfoUI;
import static com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.FMessageConversationUI;
import static com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.SnsCommentDetailUI;
import static com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.SnsTimeLineUI;
import static com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.SnsUploadUI;
import static com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.Unknown;
import static com.zaofeng.wechatfunctionplugin.utils.AccessibilityUtils.findViewById;
import static com.zaofeng.wechatfunctionplugin.utils.AccessibilityUtils.findViewClickById;
import static com.zaofeng.wechatfunctionplugin.utils.AccessibilityUtils.findViewClickByText;
import static com.zaofeng.wechatfunctionplugin.utils.AccessibilityUtils.hasViewById;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.zaofeng.wechatfunctionplugin.action.BaseAction;
import com.zaofeng.wechatfunctionplugin.action.MotionAutoCopyCommentAction;
import com.zaofeng.wechatfunctionplugin.action.MotionFastBackChatAction;
import com.zaofeng.wechatfunctionplugin.action.MotionFastCopyCommentAction;
import com.zaofeng.wechatfunctionplugin.action.MotionFastReleaseLineAction;
import com.zaofeng.wechatfunctionplugin.model.WeChatUIContract.StatusUI;
import com.zaofeng.wechatfunctionplugin.utils.Constant;
import com.zaofeng.wechatfunctionplugin.utils.Logger;
import com.zaofeng.wechatfunctionplugin.utils.PerformUtils;
import com.zaofeng.wechatfunctionplugin.utils.SPUtils;


/**
 * Created by 李可乐 on 2017/2/5 0005.
 */

public class WeChatService extends AccessibilityService {


  /**
   * 基本组件
   */
  private Context mContext;
  private Handler handler = new Handler();
  private AccessibilityService mService;
  private ClipboardManager mClipboardManager;
  private WindowView mWindowView;

  private boolean isDebug = BuildConfig.DEBUG;

  @StatusUI
  private int statusUi;

  private MotionFastReleaseLineAction motionFastReleaseLineAction;//复制快速发布操作
  private MotionFastBackChatAction motionFastBackChatAction;//发布快速返回

  private MotionAutoCopyCommentAction motionAutoCopyCommentAction;//自动复制评论
  private MotionFastCopyCommentAction motionFastCopyCommentAction;//快速评论复制回复

  /**
   * 系统会在成功连接上服务时候调用这个方法
   * 初始化参数和工具类
   */
  @Override
  protected void onServiceConnected() {
    mContext = getApplicationContext();
    mService = this;
    this.setServiceInfo(initServiceInfo());
    initManager();
    statusUi = Unknown;
    initOperationVariable();
    initWindowView();
    initAction();
    Logger.d();
  }

  private void initAction() {
    motionFastReleaseLineAction = new MotionFastReleaseLineAction(
        mContext, mWindowView, mService,
        (boolean) SPUtils.get(mContext, Constant.Release_Copy, false)
    );

    motionFastBackChatAction = new MotionFastBackChatAction(
        mContext, mWindowView, mService,
        (boolean) SPUtils.get(mContext, Constant.Release_Back, false), mClipboardManager
    );

    motionAutoCopyCommentAction = new MotionAutoCopyCommentAction(
        mContext, mWindowView, mService,
        (boolean) SPUtils.get(mContext, Constant.Comment_Auto, false));

    motionFastCopyCommentAction = new MotionFastCopyCommentAction(
        mContext, mWindowView, mService,
        (boolean) SPUtils.get(mContext, Constant.Comment_Copy, false)
    );

  }

  private void initWindowView() {
    mWindowView = new WindowView(mContext);
    mWindowView.setOnViewMainClick(new OnClickListener() {
      @Override
      public void onClick(View v) {
        motionAutoCopyCommentAction.action(BaseAction.Step0, statusUi, null);
      }
    });

    mWindowView
        .setOnWindowViewCheckChangeListener(new WindowView.OnWindowViewCheckChangeListener() {

          @Override
          public void onChange(@WindowView.Index int index, boolean isChecked) {
            String key = null;
            switch (index) {
              case WindowView.IndexRelease:
                motionFastReleaseLineAction.setOpen(isChecked);
                key = Constant.Release_Copy;
                break;
              case WindowView.IndexBack:
                motionFastBackChatAction.setOpen(isChecked);
                key = Constant.Release_Back;
                break;
              case WindowView.IndexComment:
                motionFastCopyCommentAction.setOpen(isChecked);
                key = Constant.Comment_Copy;
                break;
            }
            if (key != null) {
              SPUtils.putApply(mContext, key, isChecked);
            }
          }
        });
  }

  /**
   * SP数据监听器实例
   */
  private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      Logger.d("key=" + key);
      if (key.equals(Constant.Release_Copy)) {
        motionFastReleaseLineAction
            .setOpen(sharedPreferences.getBoolean(Constant.Release_Copy, false));
      } else if (key.equals(Constant.Release_Back)) {
        motionFastBackChatAction
            .setOpen(sharedPreferences.getBoolean(Constant.Release_Back, false));
      } else if (key.equals(Constant.Quick_Offline)) {

      } else if (key.equals(Constant.Comment_Copy)) {
        motionFastCopyCommentAction
            .setOpen(sharedPreferences.getBoolean(Constant.Comment_Copy, false));
      } else if (key.equals(Constant.Comment_Auto)) {
        motionAutoCopyCommentAction
            .setOpen(sharedPreferences.getBoolean(Constant.Comment_Auto, false));
      }

      if (mWindowView != null) {
        mWindowView.setOnChangeViewData(sharedPreferences);
      }
    }
  };


  @Override
  public void onInterrupt() {
    SPUtils.getSharedPreference(mContext)
        .unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    Logger.d("服务中断，如授权关闭或者将服务杀死");
    mWindowView.removeView();
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Logger.d("服务被解绑");
    mWindowView.removeView();
    return super.onUnbind(intent);
  }

  @Override
  protected boolean onKeyEvent(KeyEvent event) {
    Logger.d(event.toString());
    return super.onKeyEvent(event);
  }

  private void initManager() {
    mClipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
  }

  @NonNull
  private AccessibilityServiceInfo initServiceInfo() {
    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;//响应的事件类型
    info.packageNames = new String[]{"com.tencent.mm"};//响应的包名
    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;//反馈类型
    info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
    info.notificationTimeout = 80;//响应时间
    return info;
  }

  private void initOperationVariable() {

    SPUtils.getSharedPreference(mContext)
        .registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

  }

  /**
   * @param event [210,1098][1035,1157]
   * 9895
   */
  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    Logger.d("event date = " + event.toString());
    int type = event.getEventType();
    String className = event.getClassName().toString();
    String text = event.getText().isEmpty() ? Constant.Empty : event.getText().get(0).toString();
    switch (type) {
      case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED://窗口状态变化事件
        switch (className) {
          case ClassLauncherUI:
            Logger.d("正在主页或聊天页");
            statusUi = ChatUI;

            break;
          case ClassAlbumPreviewUI:
            Logger.d("正在相册选择页");
            statusUi = AlbumPreviewUI;
            if (motionFastReleaseLineAction.action(BaseAction.Step2, statusUi, event)) {
              return;
            }
            break;
          case ClassSnsTimeLineUI:
            Logger.d("正在朋友圈页");
            statusUi = SnsTimeLineUI;

            if (motionFastReleaseLineAction.action(BaseAction.Step1, statusUi, event)) {
              return;
            }

            break;
          case ClassSnsCommentDetailUI:
            Logger.d("正在朋友圈评论详情页");
            statusUi = SnsCommentDetailUI;

            break;
          case ClassFMessageConversationUI:
            Logger.d("正在新朋友功能列表");
            statusUi = FMessageConversationUI;

            break;
          case ClassSnsTimeLineUploadUI:
            statusUi = SnsUploadUI;
            if (motionFastReleaseLineAction.action(BaseAction.Step3, statusUi, event)) {
              return;
            }
            break;
          case ClassContactInfoUI:
            Logger.d("正在好友详细资料页");
            statusUi = ContactInfoUI;
            break;
        }
        break;

      case AccessibilityEvent.TYPE_VIEW_FOCUSED:
        if (className.equals("android.widget.ListView")) {
          if (hasViewById(mService, IdListViewChat)) {
            Logger.d("正在聊天页");
            statusUi = ChatUI;
          }
        }
        break;
      case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED://窗口内容变化事件
        if (className.equals("android.widget.TextView")) {
          if (hasInputBox()) {
            statusUi = ChatUI;
          }
        }

        break;
      case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED://通知事件 toast也包括

        if (className.equals("android.widget.Toast$TN") && "已复制".equals(text)) {
          if (motionFastCopyCommentAction.action(BaseAction.Step1, statusUi, event)) {
            return;
          }
          if (motionFastReleaseLineAction.action(BaseAction.Step0, statusUi, event)) {
            return;
          }
        }

        break;
      case AccessibilityEvent.TYPE_VIEW_CLICKED://点击事件

        if ("发送".equals(text)) {
          if (motionFastBackChatAction.action(BaseAction.Step0, statusUi, event)) {
            return;
          }
        }

        break;

      case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
        if (motionFastCopyCommentAction.action(BaseAction.Step0, statusUi, event)) {
          return;
        }

        break;

      case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED://view的文字内容改变

        break;

      case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:

        if (motionFastCopyCommentAction.action(BaseAction.Step2, statusUi, event)) {
          return;
        }

        break;

    }
    Logger.d("statusUi=" + statusUi);

  }

  private boolean hasInputBox() {
    if (hasViewById(mService, IdButtonVoiceChat)) {
      return true;
    }
    return false;
  }

  /**
   * 第二步 自动填写离线回复内容
   */
  private void autoOfflineFillOutReplyContent() {

    final AccessibilityNodeInfo nodeInfo = findViewById(mService, IdEditChat);
    //微信应该做了防抖动处理 所以需要延迟后执行
    int position = 0;
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        PerformUtils.performAction(nodeInfo, AccessibilityNodeInfo.ACTION_FOCUS);
      }
    }, delayTime * position++);

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        PerformUtils.performAction(nodeInfo, AccessibilityNodeInfo.ACTION_PASTE);
      }
    }, delayTime * position++);

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        PerformUtils.performAction(findViewClickById(mService, IdButtonSend));
      }
    }, delayTime * position++);

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {

        PerformUtils.performAction(findViewClickByText(mService, "返回"));
      }
    }, delayTime * position);

  }


  private void setClipBoarDate(String date) {
    mClipboardManager.setPrimaryClip(ClipData.newPlainText(null, date));
  }

  private String getClipBoardDate() {
    if (mClipboardManager.hasPrimaryClip()) {
      ClipData clipData = mClipboardManager.getPrimaryClip();
      if (clipData != null && clipData.getItemCount() > 0) {
        return clipData.getItemAt(0).coerceToText(mContext).toString();
      } else {
        Logger.e("not has clip date");
        return null;
      }
    } else {
      Logger.e("not has clip date");
      return null;
    }
  }


}
