package com.jhony.cardpay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CardPayFragment extends Fragment {
    private static final String TAG = "CardPayFragment";
    private static final int DOUBLE_CLICK_TIME = 3000;//双击间隔3000ms

    //双击处理
    private long firstClickTime = 0;
    private boolean isFirstClick = true;

    private Activity mContext;
    private WebView mWebView;
    private final OkHttpClient mClient;
    private Headers mHeaders;

    private String mUser;//账号
    private String mLoginPwd;//登录密码
    private String mChargePwd;//充值密码
    private String mAccount;//accountId
    private int mChargeMoney;//充值金额

    //流水信息
    private List<FlowInfoItem> mFlowInfoItems;

    //各组件
    private EditText mUserText;
    private EditText mLoginPwdText;
    private EditText mChargePwdText;
    private EditText mChargeMoneyText;
    private Button mChargeBtn;
    private TextView mBalanceText;
    private Button mCheckBtn;
    private Button mFlowInfosBtn;

    //登录脚本
    private String mScript = "$('#username').prop('value', '$mUser$');" +
            "$('#password').prop('value', '$mLoginPwd$');" +
            "$('.auth_login_btn.primary.full_width').click();";
    //统一认证登录url
    private final String mLoginUrl = "https://cas.whu.edu.cn/authserver/" +
            "login?service=http%3A%2F%2F202.114.64.167" +
            "%2Fias%2Fprelogin%3Fsysid%3DFWDT%26continueurl" +
            "%3Dhttp%253a%252f%252f202.114.64.162%252fcassyno%252findex";
    //请求头
    private final String mAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
            " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.104 Safari/537.36";
    //充值url
    private final String mChargeUrl = "http://202.114.64.162/User/Account_Pay";
    //获取accountId的url
    private final String mAccountUrl = "http://202.114.64.162/User/GetCardInfoByAccountNoParm";
    //首页url
    private final String mMainPageUrl = "http://202.114.64.162/user/user";
    //获取余额url
    private final String mBalanceUrl = "http://202.114.64.162/User/GetCardAccInfo";
    //获取流水信息的url
    private final String mFlowInfoUrl = "http://202.114.64.162/Report/GetPersonTrjn";

    //cookie
    private String mCookie;

    //允许充值标志
    private boolean canCharge;

    public CardPayFragment() {
        //单例client
        mClient = new OkHttpClient();
        //初始化请求头, 未加cookie
        mHeaders = new Headers.Builder()
                .add("Host: 202.114.64.162")
                .add("X-Requested-With: XMLHttpRequest")
                .add("User-Agent", mAgent)
                .add("Origin: http://202.114.64.162")
                .add("Referer: http://202.114.64.162/Page/Page")
                .build();
    }


    public static Fragment newInstance() {
        return new CardPayFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_pay, container, false);

        //获取context
        mContext = getActivity();

        //未点击按钮不允许充值
        canCharge = false;

        //初始化流水信息list
        mFlowInfoItems = new ArrayList<>();

        //初始化各组件
        mWebView = view.findViewById(R.id.web_view);
        mUserText = view.findViewById(R.id.user_text);
        mLoginPwdText = view.findViewById(R.id.login_pwd_text);
        mChargePwdText = view.findViewById(R.id.charge_pwd_text);
        mChargeMoneyText = view.findViewById(R.id.charge_money_text);
        mChargeBtn = view.findViewById(R.id.charge_btn);
        mBalanceText = view.findViewById(R.id.balance_text);
        mCheckBtn = view.findViewById(R.id.check_btn);
        mFlowInfosBtn = view.findViewById(R.id.flow_infos_btn);
        initCheckBtn();
        initChargeBtn();
        initFlowInfosBtn();
        getUserSettings();
        initWebView();

        return view;
    }

    //初始化流水信息查询按钮
    private void initFlowInfosBtn() {
        mFlowInfosBtn.setOnClickListener(v -> {
            if(mCookie != null && mAccount != null){
                getFlowInfo();
            }
        });
    }

    //初始化充值按钮
    private void initChargeBtn() {
        mChargeBtn.setOnClickListener(v -> {
            mChargeBtn.setText("充值中...");
            mChargeBtn.setClickable(false);
            canCharge = true;

            saveUserSettings();
            setLoginJs();
        });
    }

    //初始化纠错按钮
    private void initCheckBtn() {
        mCheckBtn.setOnClickListener(v -> {
            if (isFirstClick) {
                firstClickTime = System.currentTimeMillis();
                isFirstClick = false;
            } else {
                long deltaTime = System.currentTimeMillis() - firstClickTime;
                if (deltaTime < DOUBLE_CLICK_TIME) {
                    int visibility = mWebView.getVisibility();
                    if (visibility == View.VISIBLE) {
                        mWebView.setVisibility(View.INVISIBLE);
                    } else {
                        mWebView.setVisibility(View.VISIBLE);
                    }
                }
                isFirstClick = true;
            }
        });
    }

    //读取用户设置
    private void getUserSettings() {
        mUser = MyPreferences.getUser(mContext);
        mLoginPwd = MyPreferences.getLoginPwd(mContext);
        mChargePwd = MyPreferences.getChargePwd(mContext);
        mChargeMoney = Integer.parseInt(MyPreferences.getChargeMoney(mContext));
        mAccount = MyPreferences.getAccountId(mContext);

        mUserText.setText(mUser);
        mLoginPwdText.setText(mLoginPwd);
        mChargePwdText.setText(mChargePwd);
        mChargeMoneyText.setText(String.valueOf(mChargeMoney));
    }

    //设置登录脚本并尝试登录充值
    private void setLoginJs() {
        mScript = mScript.replace("$mUser$", mUser)
                .replace("$mLoginPwd$", mLoginPwd);
        if (mCookie == null) {
            mWebView.evaluateJavascript(mScript, null);
        } else {
            beginCharge();
        }
    }

    //设置余额
    @SuppressLint("SetTextI18n")
    private void setBalanceText(String balanceStr) {
        int balanceInt = Integer.parseInt(balanceStr);
        double balanceDb = balanceInt / 100.0;
        @SuppressLint("DefaultLocale")
        String balance = String.format("%.2f", balanceDb);
        mContext.runOnUiThread(() -> mBalanceText.setText("￥ " + balance));
    }

    //获取余额
    private void getBalance() {
        RequestBody requestBody = new FormBody.Builder()
                .add("acc", mAccount)
                .add("json", "true")
                .build();
        Request request = new Request.Builder()
                .url(mBalanceUrl)
                .post(requestBody)
                .headers(mHeaders)
                .build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                showToast("获取余额失败...");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
//                Log.i(TAG, result);
                String regex = "(balance.*\")(\\d+)(\\\\.*autotrans_limite)";//匹配余额
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(result);
                if (matcher.find()) {
                    String balance = matcher.group(2);
                    setBalanceText(balance);
//                    Log.i(TAG, balance);
                } else {
                    showToast("获取余额失败...");
                    Log.i(TAG, "balance not found");
                }
            }
        });
    }

    //保存用户设置
    private void saveUserSettings() {
        mUser = mUserText.getText().toString();
        mLoginPwd = mLoginPwdText.getText().toString();
        mChargePwd = mChargePwdText.getText().toString();
        mChargeMoney = Integer.parseInt(mChargeMoneyText.getText().toString());

        MyPreferences.setUser(mContext, mUser);
        MyPreferences.setLoginPwd(mContext, mLoginPwd);
        MyPreferences.setChargePwd(mContext, mChargePwd);
        MyPreferences.setChargeMoney(mContext, String.valueOf(mChargeMoney));
        MyPreferences.setAccountId(mContext, mAccount);
    }

    //充值流程
    private void beginCharge() {
        addCookie();//添加cookie
        if (mAccount == null) {
            getAccout();
        } else {
            getBalance();
            if (canCharge) {
                charge();
            }
        }
    }

    //获取流水信息
    private void getFlowInfo() {
        Calendar currentTime = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        currentTime.add(Calendar.DAY_OF_YEAR, -7);
        String startDate = format.format(currentTime.getTimeInMillis());
        currentTime.add(Calendar.DAY_OF_YEAR, 7);//回到现在的时间
        String endDate = format.format(currentTime.getTimeInMillis());
        RequestBody requestBody = new FormBody.Builder()
                .add("sdate", startDate)
                .add("edate", endDate)
                .add("account", mAccount)
                .add("page", "1")
                .add("rows", "100")
                .build();
        Request request = new Request.Builder()
                .url(mFlowInfoUrl)
                .post(requestBody)
                .headers(mHeaders)
                .build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                showToast("获取流水信息失败...");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                mFlowInfoItems.removeAll(mFlowInfoItems);
                //设置表头
                FlowInfoItem infoItem = new FlowInfoItem();
                infoItem.setEffectDate("交易时间");
                infoItem.setTranAmt("金额");
                infoItem.setTranName("交易方式");
                infoItem.setCardBal("账户余额");
                mFlowInfoItems.add(infoItem);
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray jsonArray = json.getJSONArray("rows");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        infoItem = new FlowInfoItem();
                        infoItem.setEffectDate(item.getString("EFFECTDATE"));
                        infoItem.setTranAmt(item.getString("TRANAMT"));
                        infoItem.setTranName(item.getString("TRANNAME").trim());//交易方式尾部有空白符
                        infoItem.setCardBal(item.getString("CARDBAL"));
                        mFlowInfoItems.add(infoItem);
                    }
                    FlowInfoRecycler.build(mContext, mFlowInfoItems);
                } catch (JSONException e) {
                    showToast("获取流水信息失败...");
                    e.printStackTrace();
                }
            }
        });
    }

    //初始化webview
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        //管理cookie
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.equals(mLoginUrl)) {
                    if (mUser != null && mLoginPwd != null) {
                        setLoginJs();
                    }
                } else if (url.equals(mMainPageUrl)) {
                    CookieManager manager = CookieManager.getInstance();
                    String cookie = manager.getCookie(url);
                    if (cookie != null) {
                        mCookie = cookie;
                        beginCharge();
//                        Log.i(TAG, mCookie);
                    }
                }
                super.onPageFinished(view, url);
            }
        });

        //拦截js
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                result.confirm();
                return true;
            }
        });

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);//允许js
        settings.setUserAgentString(mAgent);//桌面版网页
        mWebView.loadUrl(mLoginUrl);//加载统一认证页面
    }

    //获取account的id
    private void getAccout() {
        RequestBody requestBody = new FormBody.Builder()
                .add("json", "true")
                .build();
        Request request = new Request.Builder()
                .headers(mHeaders)
                .url(mAccountUrl)
                .post(requestBody)
                .build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                resetChargeBtn();
                showToast("网络故障...");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
//                Log.i(TAG, result);
                //真正的字符串需加双引号转义
                String regex = "(account.*\")(\\d+)(\\\\.*\"name)";//匹配Account ID
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(result);
                if (matcher.find()) {
                    mAccount = matcher.group(2);
                    if (mAccount != null) {
                        getBalance();
                    }
                    if(canCharge){
                        charge();
                    }
//                    Log.i(TAG, mAccount);
                } else {
                    resetChargeBtn();
                    showToast("查找用户ID失败...");
                    Log.i(TAG, "not found");
                }
            }
        });
    }

    //线程中展示消息
    private void showToast(String message) {
        mContext.runOnUiThread(() ->
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()//无分号
        );
    }

    //添加cookie
    private void addCookie() {
        String username = "username=" + mUser + ";";
        //Cookie存在则替换，Cookie不存在则添加
        if (mHeaders.get("Cookie") != null) {
            mHeaders = mHeaders.newBuilder()
                    .set("Cookie", username + mCookie)//替换原有cookie
                    .build();
        } else {
            mHeaders = mHeaders.newBuilder()
                    .add("Cookie", username + mCookie)//add不会替换已有cookie
                    .build();
        }
//        Log.i(TAG, mHeaders.toString());
    }

    //重置充值按钮
    private void resetChargeBtn() {
        mContext.runOnUiThread(() -> {
            mChargeBtn.setText("确认充值");
            mChargeBtn.setClickable(true);
            canCharge = false;
        });
    }

    //充值
    private void charge() {
        RequestBody requestBody = new FormBody.Builder()
                .add("account", mAccount)//账号Id
                .add("acctype", "000")
                .add("tranamt", String.valueOf(mChargeMoney * 100))//充值金额*100
                .add("qpwd", getEncodePwd())//加密后的密码
                .add("paymethod", "2")
                .add("paytype", "使用绑定的默认账号")
                .add("client_type", "web")
                .add("json", "true")
                .build();
        Request request = new Request.Builder()
                .headers(mHeaders)
                .url(mChargeUrl)
                .post(requestBody)
                .build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                resetChargeBtn();
                showToast("充值失败...");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                resetChargeBtn();
                getBalance();
//                Log.i(TAG, result);
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.getBoolean("IsSucceed")
                            && json.getString("Msg").length() > 5) {//返回-405为充值失败
                        showToast("充值成功!");
                    } else {
                        showToast("充值失败...");
                    }
                } catch (JSONException e) {
                    showToast("充值失败...");
                    e.printStackTrace();
                }
            }
        });
    }

    //获取加密后的充值密码
    private String getEncodePwd() {
        byte[] bytes = mChargePwd.getBytes(StandardCharsets.UTF_8);
        byte[] result = Base64.encode(bytes, Base64.DEFAULT);
        //        Log.i(TAG, new String(result, StandardCharsets.UTF_8));
        return new String(result, StandardCharsets.UTF_8);
    }
}
