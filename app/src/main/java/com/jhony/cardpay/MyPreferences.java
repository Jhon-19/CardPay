package com.jhony.cardpay;

import android.content.Context;
import android.preference.PreferenceManager;

public class MyPreferences {
    private static final String PREF_USER = "user";//账号
    private static final String PREF_LOGIN_PWD = "loginPwd";//登录密码
    private static final String PREF_CHARGE_PWD = "chargePwd";//充值密码
    private static final String PREF_CHARGE_MONEY = "chargeMoney";//充值金额
    private static final String PREF_ACCOUNT_ID = "accountId";//账户ID

    //保存一般字符串
    public static void setCommonString(Context context, String key, String value){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, value)
                .apply();
    }

    //保存账号
    public static void setUser(Context context, String user){
        setCommonString(context, PREF_USER, user);
    }

    //保存登录密码
    public static void setLoginPwd(Context context, String loginPwd){
        setCommonString(context, PREF_LOGIN_PWD, loginPwd);
    }

    //保存充值密码
    public static void setChargePwd(Context context, String chargePwd){
        setCommonString(context, PREF_CHARGE_PWD, chargePwd);
    }

    //保存充值金额
    public static void setChargeMoney(Context context, String chargeMoney){
        setCommonString(context, PREF_CHARGE_MONEY, chargeMoney);
    }

    //保存用户ID
    public static void setAccountId(Context context, String accountId){
        setCommonString(context, PREF_ACCOUNT_ID, accountId);
    }

    //读取一般字符串
    public static String getCommonString(Context context, String key, String defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key, defaultValue);
    }

    //读取用户
    public static String getUser(Context context){
        return getCommonString(context, PREF_USER, null);
    }

    //读取登录密码
    public static String getLoginPwd(Context context){
        return getCommonString(context, PREF_LOGIN_PWD, null);
    }

    //读取充值密码
    public static String getChargePwd(Context context){
        return getCommonString(context, PREF_CHARGE_PWD, null);
    }

    //读取充值金额
    public static String getChargeMoney(Context context){
        return getCommonString(context, PREF_CHARGE_MONEY, String.valueOf(100));
    }

    //读取用户ID
    public static String getAccountId(Context context){
        return getCommonString(context, PREF_ACCOUNT_ID, null);
    }
}
