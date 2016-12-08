/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import com.android.systemui.helper.LogHelper;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

public class CurrentUserTracker extends BroadcastReceiver {

    private static String TAG = "CurrentUserTracker";
    private static CurrentUserTracker user = null;
    public static void Init(Context context){
        if (user ==null){
            user = new CurrentUserTracker(context);
        }
    }
    private static int mCurrentUserId = UserHandle.USER_CURRENT;
    private static Context mContext = null;
    public static int getCurrentUserId() {
        return UserHandle.USER_CURRENT;//mCurrentUserId;
    }
    private static UserHandle CurrentHandle(){
        return UserHandle.CURRENT;//new UserHandle(mCurrentUserId);
    }
    public static void sendBroadcastAsCurrentUser(Intent intent){
        if(mContext != null){
            LogHelper.sd(TAG, "sendBroadcastAsCurrentUser getCurrentUserId() = " + getCurrentUserId());
            mContext.sendBroadcastAsUser(intent,CurrentHandle());
        }
    }
    public static void registerReceiverAsUser(BroadcastReceiver mReceiver,IntentFilter mFilter,String arg3, Handler arg4){

        if(mContext != null){
            LogHelper.sd(TAG, "registerReceiverAsUser is done" );
            mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, mFilter, null, arg4);
        }
    }


    public static void startActivityAsCurrentUser(Intent intent){
        if(mContext != null){
 //         LogHelper.sd(TAG, "startActivityAsCurrentUser getCurrentUserId() = " + getCurrentUserId());
            mContext.startActivityAsUser(intent,CurrentHandle());
        }
    }
    public static int getIntForCurrentUser(String name,int def){
        int ret = def;
        if(mContext != null){
            try{
 //             LogHelper.sd(TAG, "Settings.System.getIntForUser getCurrentUserId() = " + getCurrentUserId());
                ret = Settings.System.getIntForUser(mContext.getContentResolver(),name,ActivityManager.getCurrentUser());
            }
            catch(Exception e){
//              LogHelper.sd(TAG, "Settings.System.getIntForUser getCurrentUserId() = " + getCurrentUserId() + " Exception = " + e);
            }
        }
//      LogHelper.sd(TAG, "Settings.System.getIntForUser: " + name + " = " + ret);
        return ret;
    }

    public static void putIntForCurrentUser(String name,int value){
        if(mContext != null){
             Settings.System.putIntForUser(mContext.getContentResolver(),name,value,ActivityManager.getCurrentUser());
    //      LogHelper.sd(TAG, "Settings.System.putIntForUser getCurrentUserId() = " + getCurrentUserId() + " ret = " + ret + " : " + name + " = " + value);
        }
    }
    public static void registerContentObserver(String name, boolean notifyForDescendents, ContentObserver observer){
        if(mContext != null){
            final ContentResolver cr = mContext.getContentResolver();
//          cr.unregisterContentObserver(observer);
            cr.registerContentObserver(Settings.System
                    .getUriFor(name), notifyForDescendents,
                    observer,UserHandle.USER_ALL);
            LogHelper.sd(TAG, "registerContentObserver: " + name + " for user:" + UserHandle.USER_ALL);
        }
    }
    public static void registerContentObserverGlobal(String name, boolean notifyForDescendents, ContentObserver observer){
        if(mContext != null){
            final ContentResolver cr = mContext.getContentResolver();
//          cr.unregisterContentObserver(observer);
            cr.registerContentObserver(Settings.Global
                    .getUriFor(name), notifyForDescendents,
                    observer,UserHandle.USER_ALL);
            LogHelper.sd(TAG, "registerContentObserver: " + name + " for user:" + UserHandle.USER_ALL);
        }
    }

    private CurrentUserTracker(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
        context.registerReceiver(this, filter);
        mCurrentUserId = ActivityManager.getCurrentUser();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_USER_SWITCHED.equals(intent.getAction())) {
        	CurrentUserTracker.setCurrentUserId(intent.getIntExtra(Intent.EXTRA_USER_HANDLE, mCurrentUserId));
            LogHelper.sd(TAG, "onReceive mCurrentUserId = " + mCurrentUserId);
        }
    }
	private static void setCurrentUserId(int userId) {
		mCurrentUserId = userId;
	}
}
