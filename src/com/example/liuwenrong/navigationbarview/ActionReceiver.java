package com.example.liuwenrong.navigationbarview;

import com.android.systemui.helper.LogHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActionReceiver extends BroadcastReceiver {

	public static final String TAG = "xx--ActionReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		LogHelper.logE(TAG, "16--intent.getAction" + action);
	}

}
