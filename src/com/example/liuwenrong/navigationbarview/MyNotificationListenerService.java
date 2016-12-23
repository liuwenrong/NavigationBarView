package com.example.liuwenrong.navigationbarview;

import java.lang.reflect.Field;

import android.app.Notification;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.service.notification.NotificationListenerService.RankingMap;
import android.widget.Toast;

import com.android.systemui.helper.LogHelper;

public class MyNotificationListenerService extends NotificationListenerService {
	private static final String TAG = "xx--MyNotificationListenerService";

//	public class MyBinder extends Binder{
//		
//		public MyNotificationListenerService getService(){
//			return MyNotificationListenerService.this;
//		}
//	}
//	
//	@Override
//	public IBinder onBind(Intent intent){
//		return new MyBinder();
//	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		LogHelper.logE(TAG, "onStartCommand");
		return super.onStartCommand(intent, flags, startId);
	}
	
	// 有新的通知
	@Override
	public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap){
//		LogHelper.logE(TAG, "17--getNotify+ sbn==" + sbn.toString());
		getReadNotify(sbn);

	}

	private void logE(String notify) {
		
		LogHelper.logE(TAG, notify);
	}
	
	public void getReadNotify(StatusBarNotification sbn){
		Notification n = sbn.getNotification();
		if(n == null){
			return;
		}
		// 标题和时间
        String title = "";
        if (n.tickerText != null) {
            title = n.tickerText.toString();
        }
        long when = n.when;
        // 其它的信息存在一个bundle中，此bundle在android4.3及之前是私有的，需要通过反射来获取；android4.3之后可以直接获取
        Bundle bundle = null;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // android 4.3
            try {
                Field field = Notification.class.getDeclaredField("extras");
                bundle = (Bundle) field.get(n);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // android 4.3之后
            bundle = n.extras;
        }
        // 内容标题、内容、副内容
        String contentTitle = bundle.getString(Notification.EXTRA_TITLE);
        if (contentTitle == null) {
            contentTitle = "";
        }
        String contentText = bundle.getString(Notification.EXTRA_TEXT);
        if (contentText == null) {
            contentText = "";
        }
        String contentSubtext = bundle.getString(Notification.EXTRA_SUB_TEXT);
        if (contentSubtext == null) {
            contentSubtext = "";
        }
        String packageName = sbn.getPackageName();
		if("com.lwr.ireader".equals(packageName)){
			String notify = "63--notify msg: package = " + packageName + ", title=" + title + " ,when=" + when
			        + " ,contentTitle=" + contentTitle + " ,contentText="
			        + contentText + " ,contentSubtext=" + contentSubtext;
			logE(notify);
//			listener.postNotify(notify);
			Toast.makeText(getApplicationContext(), "过滤到IReader的通知:" + contentText, Toast.LENGTH_SHORT).show();
		}
	}
	
//	Listener listener;
//	public void setListener(Listener listener){
//		this.listener = listener;
//	}
//	
//	public interface Listener{
//		public void postNotify(String notify);
//	}
	@Override
	public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap){
		LogHelper.logE(TAG, "30--delete Notify");
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		LogHelper.logE(TAG, "119-----onDestroy");
	}
}
