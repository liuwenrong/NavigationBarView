/**
 * @author zhangyang1
 * @date 2012-9-19
 * 
 */
package com.android.systemui.statusbar.phone;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.android.systemui.helper.LogHelper;
import com.example.liuwenrong.navigationbarview.R;
//import com.android.systemui.statusbar.policy.NetworkController;
//import com.android.systemui.statusbar.policy.NetworkControllerCG;
//import com.android.systemui.statusbar.policy.NetworkControllerGG;
//import com.android.systemui.statusbar.policy.NetworkControllerSG;
//import com.android.systemui.statusbar.policy.NetworkControllerSW;
//import com.android.systemui.statusbar.policy.NetworkControllerWG;
//import com.android.systemui.statusbar.policy.NetworkControllerSC;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;

public class YulongConfig {
    
    // 鍗曚竴鍖朅PK鐨勮祫婧怚D
    public int mYulongResNocard = 0;            // 鎵樼洏鍙充笂瑙掓棤鍗℃彁绀�
    public int mYulongResApnNocard = 0;         // 鏁版嵁涓氬姟鍙宠竟鏃犲崱鎻愮ず
    public int mYulongResStartNetwork = 0;      // 寮�鍚暟鎹笟鍔�
    public int mYulongResStopNetwork  = 0;      // 鍏抽棴鏁版嵁涓氬姟 
    public final int mYulongMaxShortcut = 50;   // 蹇�熻缃渶澶ф暟閲�
    public final int mYulongMajorMaxShortcut = 50; // 涓荤晫闈㈡渶澶氭樉绀烘暟閲�
    public int mCButtonDefStatus = 1;
    
    private static final String TAG = "YulongConfig";
    private String mNetworkType = "CG";
    private String mShortcutFromXml;// 蹇嵎鎺掑簭
    private String mShortcutExcludeFromXml;// 鎺掗櫎鍒楄〃
    private String mShortcutFromXml_primary;
    private String mShortcutExcludeFromXml_primary;
    private int    mDataNetNameType = -1;       // 鏁版嵁涓氬姟寮�鍏崇殑鍚嶇О绫诲瀷 -1 榛樿锛�0锛屽紑鍚叧闂綉缁滐紝1锛岀Щ鍔ㄦ暟鎹�
    private final ArrayList<Integer>  mShortcutList = new ArrayList<Integer>();
    private final ArrayList<Integer>  mShortcutList_primary = new ArrayList<Integer>();
    
    
    private Context mContext;
    private static YulongConfig mDefaultConfig = null;
    
    //add by wz
    private final ArrayList<String>   mNotificationTopPkg = new ArrayList<String>();
    private String mDefNotificationTopPkg; 
    
    private final ArrayList<String>   mNotificationShowPkg = new ArrayList<String>();
    private String mDefNotificationShowPkg;
     //end wz
    private boolean mIsDS = false;
    private boolean mIsAllNetwork = false;
    //private boolean mMultiUserSpace = false;
    private PackageManager mPm;
    public YulongConfig(Context context){
        mContext = context;
        mPm = mContext.getPackageManager();
    }
    public static YulongConfig createDefault(Context context){
        mDefaultConfig = new YulongConfig(context);
        return mDefaultConfig;
    }
    public static YulongConfig getDefault(){
        return mDefaultConfig;
    }
    public int getMode(){
        if (mNetworkType.endsWith("GG") || mNetworkType.endsWith("SG")){
            return 1;
        }
        return 0;
    }
    public String getNetworkType(){
    	return mNetworkType;
    }
    public void init(){
        printVersionInfo();
//        mMultiUserSpace = QuickSettingsModel.getMiscInterfaceResult("show_multi_user_space");
//        LogHelper.sd(TAG, " mMultiUserSpace = suport_multi_user_space = " + mMultiUserSpace);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(new File("system/lib/uitechno/cfg.xml"), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName,
                        String qName, Attributes attributes)
                        throws SAXException {
                    super.startElement(uri, localName, qName, attributes);
                    LogHelper.sd(TAG, "localName = " + localName 
                            + " key = " + attributes.getValue("key")
                            + " value = " + attributes.getValue("value"));
                    if (localName.equals("item") ){
                        if (attributes.getValue("key").equals("networkType")){
                            mNetworkType = attributes.getValue("value");
                            LogHelper.sd(TAG, "mNetworkType = " + mNetworkType);
                        } else if (attributes.getValue("key").equalsIgnoreCase("shortcut6")
                                || attributes.getValue("key").equalsIgnoreCase("shortcutDefOrder6")) {
                        	mShortcutFromXml = attributes.getValue("value");
                            LogHelper.sd(TAG, "mShortcutFromXml6 = " + mShortcutFromXml);
                        } else if (attributes.getValue("key").equalsIgnoreCase("shortcut_primary6")
                                || attributes.getValue("key").equalsIgnoreCase("shortcutDefOrder_primary6")) {
                        	mShortcutFromXml_primary = attributes.getValue("value");
                            LogHelper.sd(TAG, "mShortcutFromXml_primary6 = " + mShortcutFromXml_primary);
                        } else if (attributes.getValue("key").equalsIgnoreCase("DataNetNameType")){
                            //mDataNetNameType = Integer.valueOf(attributes.getValue("value"));
                            try {
                                mDataNetNameType = Integer.parseInt(attributes.getValue("value"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (attributes.getValue("key").equalsIgnoreCase("shortcutExclude_primary6")){
                            mShortcutExcludeFromXml_primary = attributes.getValue("value");
                            LogHelper.sd(TAG, "mShortcutExclude_primary6 = " + mShortcutExcludeFromXml_primary);
                        } else if (attributes.getValue("key").equalsIgnoreCase("shortcutExclude6")){
                            mShortcutExcludeFromXml = attributes.getValue("value");
                            LogHelper.sd(TAG, "mShortcutExclude6 = " + mShortcutExcludeFromXml);
                        } else if (attributes.getValue("key").equalsIgnoreCase("guestModeExclude")){
                            String guestModeExclude = attributes.getValue("value");
                            LogHelper.sd(TAG, "guestModeExclude = " + guestModeExclude);
                            String[] arExclude= guestModeExclude.split("\\s*[,;]\\s*");
                            guestModeHideSettings = new int[arExclude.length];
                            for(int i=0; i < arExclude.length; i++){
                            	guestModeHideSettings[i] = Integer.parseInt(arExclude[i]);
                            }
                        } else if (attributes.getValue("key").equalsIgnoreCase("cbutton")
                        		||attributes.getValue("key").equalsIgnoreCase("cbuttondefstatus")){
                        	String cbuttonstat = attributes.getValue("value");
                        	if (cbuttonstat.equalsIgnoreCase("on"))
                        		mCButtonDefStatus = 0;
                        	else if (cbuttonstat.equalsIgnoreCase("off"))
                        		mCButtonDefStatus = 1;
                        	else
                        		mCButtonDefStatus = Integer.parseInt(cbuttonstat);
                        	LogHelper.sd(TAG,"CButton Stat = " + mCButtonDefStatus + " from:" + cbuttonstat);
                        }else if(attributes.getValue("key").equalsIgnoreCase("dsType")){
                        	String isDS = attributes.getValue("value");
                        	if(isDS.equalsIgnoreCase("ds")){
                        		mIsDS = true;
                        	}
                        }else if(attributes.getValue("key").equalsIgnoreCase("AllNetwork")){
                        	String v = attributes.getValue("value");
                        	if(v.equalsIgnoreCase("yes")){
                        		mIsAllNetwork = true;
                        	}
//                        }else if(attributes.getValue("key").equalsIgnoreCase("show_multi_user_space")){
//                        	String v = attributes.getValue("value");
//                        	if(v.equalsIgnoreCase("true") || v.equalsIgnoreCase("1")){
//                        		mMultiUserSpace = true;
//                        	}else if(v.equalsIgnoreCase("false") || v.equalsIgnoreCase("0")){
//                        		mMultiUserSpace = false;
//                        	}
//                        	LogHelper.sd(TAG,"show_multi_user_space = " + mMultiUserSpace + " from: " + v);
                        }
                        
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            LogHelper.se(TAG, "read system/lib/uitechno/cfg.xml error " + e.toString());
        }
        
        // 澶勭悊杩愯惀鍟嗗樊寮�
        mYulongResNocard = 0;
        mYulongResApnNocard = R.string.status_bar_expanded_check_sim;
        
        mYulongResStartNetwork = R.string.status_bar_expanded_start_network;
        mYulongResStopNetwork = R.string.status_bar_expanded_stop_network;
        // 鐢典俊浣跨敤
        if (mNetworkType.equals("CG") || mNetworkType.equals("SC")){
            mYulongResNocard = R.string.kg_no_uim;
            mYulongResApnNocard = R.string.status_bar_expanded_check_uim;
        } else if(mNetworkType.equals("WG")||mNetworkType.equals("SW")){
        // 鑱旈��
            mYulongResStartNetwork = R.string.status_bar_expanded_start_network_g;
            mYulongResStopNetwork = R.string.status_bar_expanded_stop_network_g;
            mYulongResNocard = R.string.kg_no_usim;
            mYulongResApnNocard = R.string.status_bar_expanded_check_uimsim;
        } else {
        // 绉诲姩
            mYulongResStartNetwork = R.string.status_bar_expanded_start_network;
            mYulongResStopNetwork = R.string.status_bar_expanded_stop_network;
            mYulongResApnNocard = R.string.status_bar_expanded_check_sim;
            mYulongResNocard = 0;
        }
   
        LogHelper.sd(TAG,"CreateShortcutList()");
        CreateShortcutList(mShortcutList,mShortcutFromXml,mShortcutExcludeFromXml,false);
        
        // 澶勭悊寮�鍏虫暟鎹笟鍔＄殑鏂囨湰,0锛屽紑鍚叧闂綉缁滐紝1锛岀Щ鍔ㄦ暟鎹�
        CreateShortcutList(mShortcutList_primary,mShortcutFromXml_primary,
        		mShortcutExcludeFromXml_primary,true);
        if (mDataNetNameType == 0){
            mYulongResStartNetwork = R.string.status_bar_expanded_start_network_g;
            mYulongResStopNetwork = R.string.status_bar_expanded_stop_network_g;
        } else if (mDataNetNameType == 1){
            mYulongResStartNetwork = R.string.status_bar_expanded_start_network;
            mYulongResStopNetwork = R.string.status_bar_expanded_stop_network;
        }
        
        //mMultiUserSpace = true;
        //LogHelper.sd(TAG, " debug set mMultiUserSpace = " + mMultiUserSpace);
    }
    public int getCButtonDefStatus(Boolean isPrimary){
    	return mCButtonDefStatus;
    }
	//閽冩繄澧�,娴ｅ秶鐤嗛張宥呭,缁夎濮╅弫鐗堝祦,缂冩垹绮跺鍙夋焽瀵拷,閸欏苯宕辩拋鍓х枂,缁夎濮╅悜顓犲仯,闂冨弶澧﹂幍锟�,娑擄拷闁款喖濮為柅锟�,濞翠線鍣洪惄鎴炲付,閼奉亜濮╅弮瀣祮,妤犳碍澹堥幏锔藉焻,C闁匡拷,閺呴缚鍏樻径姘潌,NFC,濮瑰�熸簠濡�崇础,閸楋拷1閺佺増宓�,閹懏娅欏Ο鈥崇础
    //2,9,17,18,7,10,11,12,13,3,16,19,20,21,23,28,5
    private int[] guestModeHideSettings={2,9,17,18,7,10,11,12,13,3,16,19,20,21,23,28,5};
    public int[] getGuestModeHideSettings(){
    	return guestModeHideSettings;
    }
    
    public static final int CARRIER_ALL = 1;
    public static final int CARRIER_CHINA_TELECOM = 2;
    public static final int CARRIER_CHINA_MOBILE  = 3;
    public static final int CARRIER_CHINA_UNICOM  = 4;    
    public static int getOperatorTypeFromSys() {
        Class<?> myClassSystemUtil = null;
        try {
            myClassSystemUtil = Class
                    .forName("com.yulong.android.server.systeminterface.util.SystemUtil");
        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (null != myClassSystemUtil) {
            try {
                Object Object_SystemUtil = myClassSystemUtil.newInstance();
                Method MyMethod_CarrierType = myClassSystemUtil
                        .getMethod("getCarrierType");
                if (null != MyMethod_CarrierType) {
                	LogHelper.sd(TAG,"getOperatorTypeFromSys: getCarrierType"
                                    + (Integer) MyMethod_CarrierType.invoke(Object_SystemUtil));
                    return (Integer) MyMethod_CarrierType
                            .invoke(Object_SystemUtil);
                }
                LogHelper.sd(TAG,"getOperatorTypeFromSys: MyMethod_CarrierType is null");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LogHelper.sd(TAG,"getOperatorTypeFromSys: myClassSystemUtil is null");
        return -1;
    }
    
/*    public NetworkController createNetworkController(){
        
        LogHelper.sd(TAG, "createNetworkController mNetworkType = " + mNetworkType);
//        if (CARRIER_ALL == getOperatorTypeFromSys()){
//        	
//        } else if (mNetworkType.equals("CG")) {
//            return new NetworkControllerCG(mContext);
//        } else if (mNetworkType.equals("GG")) {
//            return new NetworkControllerGG(mContext);
//        } else if (mNetworkType.equals("SG")) {
//            return new NetworkControllerSG(mContext);
//        } else if (mNetworkType.equals("WG")) {
//            return new NetworkControllerWG(mContext);
//        } else if (mNetworkType.equals("SC")) {
//           return new NetworkControllerSC(mContext);
//        } else if(mNetworkType.equals("SW")){
//          return new NetworkControllerSW(mContext);
//        }
//        mIsAllNetwork = true;
//        return new NetworkController(mContext);
        return null;
    }*/
    private void CreateShortcutList(ArrayList<Integer>  mShortcutList, String mShortcutFromXml, 
    		String mShortcutExcludeFromXml, Boolean isPrimay){
    	//0-WLAN 1-閺佺増宓佸锟介崗锟�   2-妞嬬偠顢戝Ο鈥崇础  5-閼奉亜濮╅弮瀣祮 6-娴ｅ秶鐤嗛張宥呭  
    	//7-缁夎濮╅悜顓犲仯 8-閽冩繄澧�  9-娑擄拷闁款噣娼ら棅锟� 12 閸忓秵澧﹂幍锟� 13闂囧洤濮� 
    	//14 鐡掑懐楠囬惇浣烘暩  15 C闁匡拷 17閸椻�茬閸椻�茬癌  18 3G 4G 19 APN
    	String shortcutDef;// = "0,1,19,9,13,5,37,8,6,12,14,7,2,15,17,10";
    	String shortcutExcludeDef;// = "3,4,11,16";//閸樼粯甯�娑擄拷闁款喖濮為柅鐕傜礉濞翠線鍣洪惄鎴炲付閿涘本娅ら懗鐣岀級鐏烇拷4,10,16
    	//閺傛壆澧楅張顒婄礉娑撳秳濞囬悽鈺焒g.xml娑擃厾娈戦弫鐗堝祦閿涘矂鍣伴悽銊ㄥ殰鐎规矮绠熼敍锟�3閸滐拷11閺勵垱澧滈悽鐢电摏閸滃本鍩呴崶鎾呯礉閸ョ姳璐熷▽鈩冩箒鐎圭偟骞囬敍灞炬畯閺冭埖甯撻梽锟�
    	if(!isPrimay){
    		shortcutDef = "0,1,8,13,6,5,37,2,7";
    		shortcutExcludeDef = "";
    	}else{
    		shortcutDef = "0,1,8,13,9,5,37,2,6,16";
    		shortcutExcludeDef = "";
    	}
    	if (mShortcutFromXml == null)
    		mShortcutFromXml = shortcutDef;
    	if (mShortcutExcludeFromXml == null)
    		mShortcutExcludeFromXml = shortcutExcludeDef;
        try {
            mShortcutList.clear();
            String [] orders = mShortcutFromXml.split("\\s*[,;]\\s*");
            for (String v : orders){
                Integer i = Integer.valueOf(v);
                mShortcutList.add(i);                    
            }
            // 鎺掗櫎蹇界暐鐨勫厓绱�
            if (mShortcutExcludeFromXml != null && !mShortcutExcludeFromXml.isEmpty()){
            	String [] exclude = mShortcutExcludeFromXml.split("\\s*[,;]\\s*");
                for (String v : exclude){
                    Integer i = Integer.valueOf(v);
                    mShortcutList.remove(i);
                }
            }
            int remove=-1;
            String flag=SystemProperties.get("ro.yulong.has_nfc");
            LogHelper.se(TAG, "SystemProperties.get(ro.yulong.has_nfc)=" + flag);
            if(flag.equals("0")){
      	       for(int i=0;i<mShortcutList.size();i++){
//      	    	   if((mShortcutList.get(i)==QuickSettingsData.QS_ID_NFC)){
//      	    		   remove=i;
//      	    	   }
      	       }
      	       if(remove>=0){
      	        mShortcutList.remove(remove);
      	       }
            }
        } catch (Exception e) {
            LogHelper.se(TAG, "cfg.xml shortcut order err " + e.toString());
            e.printStackTrace();
        }
        
        LogHelper.sd(TAG," mShortcutList = " + mShortcutList.toString());    	
    }
    public ArrayList<Integer> getDefShortcutOrder(Boolean isPrimary){
    	if(isPrimary){
    		return mShortcutList_primary;
    	}else{
    		return mShortcutList;
    	}
    }
    
    public static Object SystemInterfaceFactory_getSysteminterface() {
        try{
            Class<?> classObject = Class.forName("com.yulong.android.server.systeminterface.SystemInterfaceFactory");
             Method method = classObject.getDeclaredMethod("getSysteminterface");
             return method.invoke(null);
        }catch(Exception e) {
            
        }
        return null;
     }

     public static boolean ISystemInterface_fangdaoValidated(Object object) {
        try{
            Class<?> classObject = Class.forName("com.yulong.android.server.systeminterface.ISystemInterface");
             Method method = classObject.getDeclaredMethod("fangdaoValidated");
             return (Boolean)method.invoke(object);
        }catch(Exception e) {
            
        }
        return false;
     }
     
     // 鏄惁涓洪槻鐩楁ā寮�
     public static boolean isGuardMode() {
         Object ISystemInterfaceObject = SystemInterfaceFactory_getSysteminterface();
         boolean fangdaoValidated = ISystemInterface_fangdaoValidated(ISystemInterfaceObject);
         LogHelper.sd(TAG, "isGuardMode == " + !fangdaoValidated);
         return !fangdaoValidated;
     }
     
     public static boolean ISystemInterface_PrivateModeVailidate(Object object) {
         try{
             Class<?> classObject = Class.forName("com.yulong.android.server.systeminterface.ISystemInterface");
              Method method = classObject.getMethod("getPrivateInfo",String.class);
              String mode = (String) method.invoke(object,"entryMode");
              LogHelper.sd(TAG, "getPrivateInfo(\"entryMode\") == " + mode);
              if (mode != null && mode.length() > 3 && (!mode.equalsIgnoreCase("TrayAndDialpad"))){
            	  return false;
              }
         }catch(Exception e) {
        	 LogHelper.se(TAG, "isPrivateModeVailidate " + e);
         }
         return true;
      }     
     //绉佸瘑妯″紡鎷夎捣鏂规硶
     public static boolean isPrivateModeVailidate() {

         Object ISystemInterfaceObject = SystemInterfaceFactory_getSysteminterface();
         boolean privateModeVailidate = ISystemInterface_PrivateModeVailidate(ISystemInterfaceObject);
         LogHelper.sd(TAG, "privateModeVailidate == " + privateModeVailidate);
         return privateModeVailidate;
     }    
     
     // 鎵撳嵃鐗堟湰淇℃伅
     private void printVersionInfo(){
         try{
            PackageInfo info = mPm.getPackageInfo("com.android.systemui", 0);
            String versionName = info.versionName;
            LogHelper.sd(TAG, "start statusbar version name is " + versionName);
        } catch (Exception e) {
        	//
        }
     }
     public boolean isDS(){
    	 return mIsDS;
     }
     public boolean isAllNetwork(){//鏄惁鍏ㄧ綉鏈哄櫒
//    	 if (NetworkController.isSEDRegion())
//    		 return true;
    	 return mIsAllNetwork;
     }
     
     public static boolean isMultiUserSpace() {
         String isDoubleSpace = SystemProperties.get("ro.ss.version", "");
         return !isDoubleSpace.equals("");
        }
//     public boolean isMultiUserSpace(){
//    	 return mMultiUserSpace;
//     }
}
