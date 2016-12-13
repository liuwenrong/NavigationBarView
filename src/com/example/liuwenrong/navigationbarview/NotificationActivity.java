package com.example.liuwenrong.navigationbarview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.systemui.helper.LogHelper;

public class NotificationActivity extends Activity {
	
	protected static final String TAG = "NotificationActivity";
	Button btnSendNotifi;
	Button btnGetNotifi;
	Button btnSetAuth;
	NotificationManager mNotificationManager;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification);
		btnSendNotifi = (Button) findViewById(R.id.btn_send_notifi);
		btnGetNotifi = (Button) findViewById(R.id.btn_get_notifi);
		btnSetAuth = (Button) findViewById(R.id.btn_set_auth);
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		btnSendNotifi.setOnClickListener(onSendNotifiClickListener);
		btnGetNotifi.setOnClickListener(onGetNotifiClickListener);
		btnSetAuth.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				setAuth();
			}
		});
		
	}
	
	public void setAuth(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
			Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
			startActivity(intent);
		}else{
			Toast.makeText(getApplicationContext(), "当前系统不支持此功能", Toast.LENGTH_SHORT).show();
		}
		
	}
	
	OnClickListener onSendNotifiClickListener = new OnClickListener(){
		@Override
		public void onClick(View view){
			Intent intent = new Intent(getApplicationContext(),NotificationActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
//			notification.setLatestEventInfo(getApplicationContext(), "title", "content", contentIntent);
			// 6.0 发不出消息
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(NotificationActivity.this);
//			Notification.Builder mBuilder = new Notification.Builder(NotificationActivity.this);
			Notification notification = mBuilder.setContentTitle("HelloWorld!")
			.setContentText("Test Content")
			.setPriority(Notification.PRIORITY_MAX)
			.setTicker("通知来了")
			.setSmallIcon(R.drawable.ic_launcher)
			.setWhen(System.currentTimeMillis())
			.setNumber(12)
			.setAutoCancel(true)
			.setContentIntent(contentIntent)
			.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
			.build();
//			notification.flags = Notification.FLAG_ONGOING_EVENT;
			mNotificationManager.notify(0, notification);
			LogHelper.logE(TAG, "47---发送通知");
			
		}
	};
	OnClickListener onGetNotifiClickListener = new OnClickListener(){
		@SuppressLint("NewApi") @Override
		public void onClick(View view){
			// 只能拿到当前context的应用的消息
			StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
			LogHelper.logE(TAG, "notifications.length = " + notifications.length);
			for(int i = 0; i< notifications.length; i++){
				StatusBarNotification sbn = notifications[i];
				LogHelper.logE(TAG, "52---第" + i + "条消息notifi = " + sbn.toString());
			}
		}
	};
}
