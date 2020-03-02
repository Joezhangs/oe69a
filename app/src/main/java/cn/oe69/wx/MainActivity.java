package cn.oe69.wx;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.SyncStateContract;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private String url;
    private String mainurl;
    String content = "";
    String down = "";
    int times = 0;
    int left = 0;
    String alert = "";
    String finalcontent = "";
    private static  final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION  = 100;
    private String appid;
    private String secret;
    private String shareContent;
    private IWXAPI api;
    private int V = 2;
    private LinearLayout goBackBtn;
    private int isFinal = 0;

    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime;

    private FrameLayout fullVideo;
    protected View customView;

    private ProgressDialog pBar;

    private ImageView mImageView;

    private MyHandler handler = new MyHandler();

    private boolean isDown = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_main);

        checkPermission();
        //clearList();
        //getSys();
        //checkUpdate();
        MyReceiver receiver = new MainActivity.MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.MY_BROADCAST");
        registerReceiver(receiver, filter);

        LinearLayout linearLayout = findViewById(R.id.ll_web);

        fullVideo = findViewById(R.id.fl_video_full);
        /*goBackBtn = findViewById(R.id.goback);
        goBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share();
            }
        });*/

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        webView = new WebView(getApplicationContext());
        webView.setLayoutParams(params);
        linearLayout.addView(webView);
        //mainurl = "http://www.360lvtao.com";
        //mainurl = "http://wallet.zp-tech.net/member";
        //1.创建OkHttpClient对象
            OkHttpClient okHttpClient = new OkHttpClient();
            //2.创建Request对象，设置一个url地址（百度地址）,设置请求方式。
            Request request = new Request.Builder().url("http://www.qpic.xyz/app/Config.txt").method("GET", null).build();
            //3.创建一个call对象,参数就是Request请求对象
            okhttp3.Call call = okHttpClient.newCall(request);
            //4.请求加入调度，重写回调方法
            call.enqueue(new okhttp3.Callback() {
                //请求失败执行的方法
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {

                }

                //请求成功执行的方法
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    byte[] b = response.body().bytes();
                    String res = new String(b, "GB2312");
                    Log.i("xxx", res);
                    mainurl = res.split("<分享首页>")[1].split("</分享首页>")[0];
                    content = res.split("<分享内容>")[1].split("</分享内容>")[0];
                    down = res.split("<下载地址>")[1].split("</下载地址>")[0];
                    times = Integer.parseInt(res.split("<分享次数>")[1].split("</分享次数>")[0]);
                    left = times;
                    //times = 0;
                    alert = res.split("<弹窗内容>")[1].split("</弹窗内容>")[0];

                    finalcontent = res.split("<完成弹窗>")[1].split("</完成弹窗>")[0];
                    //mWebView.loadUrl(homeurl);
                    Message msg = handler.obtainMessage();
                    msg.obj = mainurl;
                    msg.what = 1;
                    handler.sendMessage(msg);

                }
            });
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            String url = (String)msg.obj;
            mainurl = url;
            initWebView(url);
        }
    }
    private void initWebView(String url) {
        WebSettings mWebSettings = webView.getSettings();
        //mWebSettings.setSupportZoom(true);
        //mWebSettings.setLoadWithOverviewMode(true);
        //mWebSettings.setUseWideViewPort(true);
        //mWebSettings.setDefaultTextEncodingName("utf-8");
        //mWebSettings.setLoadsImagesAutomatically(true);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setAllowFileAccess(true);


        webView.setWebViewClient(new HWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.loadUrl(url);
        /*
        TimerDialog dialog = new TimerDialog(MainActivity.this);
        dialog.setTitle("温馨提示：");
        dialog.setMessage("如果出现白屏 请稍等 正在加载中....");
        dialog.setPositiveButton("确定",null, 3);
        dialog.show();
        dialog.setButtonType(Dialog.BUTTON_POSITIVE, 3, true);
        */
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int msg = intent.getIntExtra("msg", 0);
            //Log.i("接收到广播信息：", String.valueOf(msg));
            if(msg == 1)
            {
                //Log.i("跳转URL：", url);
                webView.loadUrl(url);
            }
        }
    }


    void downFile(final String durl) {
        pBar.show();
        new Thread() {
            public void run() {
                try {
                    int initial = 0;//初始下载进度
                    URL url = new URL(durl);
                    pBar.setProgress(initial);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.connect();

                    int length = conn.getContentLength();
                    InputStream is = conn.getInputStream();
                    FileOutputStream fileOutputStream = null;
                    if (is != null) {
                        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/SharedProject/update.apk");
                        fileOutputStream = new FileOutputStream(file);
                        byte[] buf = new byte[1024];
                        int ch = -1;
                        int count = 0;
                        while ((ch = is.read(buf)) != -1) {
                            fileOutputStream.write(buf, 0, ch);
                            count += ch;
                            initial = 100 * count / length;
                            Log.d("pBar", String.valueOf(initial));
                            pBar.setProgress(initial);
                            if (length > 0) {
                            }
                        }
                        pBar.dismiss();//进度完成时对话框消失
                    }
                    fileOutputStream.flush();
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    update();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    void update() {
        if(Build.VERSION.SDK_INT>=24){
            Uri apkUri = FileProvider.getUriForFile(this, "cn.oe69.wx.fileprovider", new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/SharedProject/update.apk"));

            Intent install = new Intent(Intent.ACTION_VIEW);

            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            install.setDataAndType(apkUri, "application/vnd.android.package-archive");

            startActivity(install);
        }
        else{
            //如果没有设置SDCard写权限，或者没有sdcard,apk文件保存在内存中，需要授予权限才能安装
            try {
                String[] command = {"chmod", "777", new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/SharedProject/update.apk").toString()}; //777代表权限 rwxrwxrwx
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.start();
            } catch (IOException ignored) {
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/SharedProject/update.apk")), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }


    private void DRegist() {
        IntentFilter intentFilter = new IntentFilter(DownloadService.BROADCAST_ACTION);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(new DReceiver(), intentFilter);
    }

    private class DReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra(DownloadService.EXTENDED_DATA_STATUS);
            //Log.i("test", data);

            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Toast.makeText(MainActivity.this, "下载任务已经完成！", Toast.LENGTH_SHORT).show();
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/myApp.apk")),
                    "application/vnd.android.package-archive");
            startActivity(intent);

        }
    }


    public static String readToText(String filePath) {
        //按字节流读取可保留原格式，但是有部分乱码情况，根据每次读取的byte数组大小而变化
        StringBuffer txtContent = new StringBuffer();
        byte[] b = new byte[2048];
        InputStream in = null;
        try {
            in = new FileInputStream(filePath);
            int n;
            while ((n = in.read(b)) != -1) {
                txtContent.append(new String(b, 0, n, "utf-8"));
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return txtContent.toString();
    }

    // 将字符串写入到文本文件中
    private void writeTxtToFile(String strcontent, String filePath, String fileName) {
        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath, fileName);

        String strFilePath = filePath + fileName;
        // 每次写入时，都换行写
        //String strContent = strcontent + "\r\n";
        String strContent = strcontent;
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }

    //生成文件

    private File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

//生成文件夹

    private static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //clearList();
        //doNext(requestCode, grantResults);
    }

    /**
     * 按键响应，在WebView中查看网页时，按返回键的时候按浏览历史退回,如果不做此项处理则整个WebView返回退出
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //判断是否可以返回操作
        //Log.e("监测keycode", Integer.toString(keyCode));
        if (webView.canGoBack() && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            //获取历史列表
            WebBackForwardList mWebBackForwardList = webView
                    .copyBackForwardList();
            //判断当前历史列表是否最顶端,其实canGoBack已经判断过
            if (mWebBackForwardList.getCurrentIndex() > 0) {
                //获取历史列表
                String historyUrl = mWebBackForwardList.getItemAtIndex(
                        mWebBackForwardList.getCurrentIndex() - 1).getUrl();
                //按照自己规则检查是否为可跳转地址
                //注意:这里可以根据自己逻辑循环判断,拿到可以跳转的那一个然后webView.goBackOrForward(steps)
                // steps为负数时，表示回退，正数表示向前
                String nurl= webView.getUrl();
                if (historyUrl != url && nurl != mainurl) {
                    //执行跳转逻辑
                    webView.goBack();
                    return true;
                } else {
                    //弹出确认框确认退出
                    dialog();
                    return true;
                }
            }
        }
        else {
            //判断用户是否点击了“返回键”
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                //与上次点击返回键时刻作差
                if ((System.currentTimeMillis() - mExitTime) > 2000) {
                    //大于2000ms则认为是误操作，使用Toast进行提示
                    Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                    //并记录下本次点击“返回键”的时刻，以便下次进行判断
                    mExitTime = System.currentTimeMillis();
                } else {
                    //小于2000ms则认为是用户确实希望退出程序-调用System.exit()方法进行退出
                    System.exit(0);
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("确认要退出么？");
        builder.setTitle("提示");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //TODOAuto-generatedmethodstub
                dialog.dismiss();
                finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //TODOAuto-generatedmethodstub
                dialog.dismiss();
            }
        });
        builder.show();
    }

    //Web视图
    private class HWebViewClient extends WebViewClient {
        /*
        @Override
        public void onPageStarted(WebView view, String nowurl, Bitmap favicon)
        {
            super.onPageStarted(view, nowurl, favicon);
            Log.e("111", nowurl);
            if(nowurl == "http://www.360lvtao.com") {
                mImageView.setVisibility(View.VISIBLE);
            }
        }
        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            mImageView.setVisibility(View.GONE);
        }
        */
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String nowurl) {
            Log.d("URL改变", nowurl);
            Log.d("down", down);
            url = nowurl;

            if (nowurl.indexOf(".apk") > 0)
            {
                        /*pBar = new ProgressDialog(MainActivity.this);
                        pBar.setTitle("正在下载");
                        pBar.setMessage("请稍候...");
                        pBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        // 点击外部返回
                        pBar.setCanceledOnTouchOutside(true);
                        //设置进度条
                        pBar.setMax(0);
                        //设置进度条是否明确
                        //pBar.setIndeterminate(true);
                        pBar.setIndeterminate(false);
                        //pBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        pBar.setCancelable(true);
                final  Handler dlgHandler=new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        int s = (int)msg.obj;
                        //pBar.setMessage(s);
                        if(msg.what>=s){
                            pBar.setProgress(s);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    pBar.dismiss();
                                }
                            },1000);
                        }else{
                            pBar.setProgress(msg.what);
                        }
                        return true;
                    }
                });
                pBar.show();
                new Thread() {
                    public void run() {
                        try {
                            int initial = 0;//初始下载进度
                            URL url = new URL(nowurl);
                            pBar.setProgress(initial);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.connect();

                            int length = conn.getContentLength();
                            pBar.setMax(length);
                            InputStream is = conn.getInputStream();
                            FileOutputStream fileOutputStream = null;
                            if (is != null) {
                                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/SharedProject/update.apk");
                                fileOutputStream = new FileOutputStream(file);
                                byte[] buf = new byte[1024];
                                int ch = -1;
                                int count = 0;
                                while ((ch = is.read(buf)) != -1) {
                                    fileOutputStream.write(buf, 0, ch);
                                    count += ch;
                                    pBar.setProgress(count);
                                    Message.obtain(dlgHandler,count,length).sendToTarget();
                                    if (length > 0) {
                                    }
                                }
                                pBar.dismiss();//进度完成时对话框消失
                            }
                            fileOutputStream.flush();
                            if (fileOutputStream != null) {
                                fileOutputStream.close();
                            }
                            update();
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();*/
                return true;
            }

            if(times > 0)
            {
                Log.d("URL改变：", "没问题啊");
                //Toast.makeText(MainActivity.this, "还须要分享" + String.valueOf(times) + "次", Toast.LENGTH_SHORT).show();
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("提示");
                if(left == times)
                {
                    dialog.setMessage(alert.replace("{code}", "请先分享" + String.valueOf(times)));

                }
                else
                {
                    dialog.setMessage(alert.replace("{code}", "再分享" + String.valueOf(times)));
                }

                dialog.setCancelable(false);
                dialog.setPositiveButton("去分享", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        shareQQ();
                        times--;
                        if(times == 0)
                        {
                            dialog.dismiss();
                            isFinal = 1;

                        }
                    }
                });
                dialog.setNeutralButton("检测通过", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder dl = new AlertDialog.Builder(MainActivity.this);
                        if(times > 0)
                        {
                            dl.setTitle("检测不通过");
                            dl.setMessage("经过检测，你没分享够"+String.valueOf(left)+"个QQ群，作弊骗不了系统监测，注意一下三点：\r\n1.创新群充数不行。\r\n2.分享后不能撤回。\r\n3.必须分享到"+String.valueOf(left)+"个不同的QQ群");
                            dl.setCancelable(true);
                            dl.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                        }

                        dl.show();
                    }
                });
                dialog.show();
                return true;
            }
            else
            {
                if(nowurl.indexOf(down) >= 0)
                {
                    Uri uri = Uri.parse(nowurl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if(checkAppInstalled(MainActivity.this, "com.tencent.mtt"))
                    {
                        intent.setClassName("com.tencent.mtt","com.tencent.mtt.MainActivity");//打开QQ浏览器
                    }
                    startActivity(intent);
                    return true;
                }
                if(nowurl.indexOf("/shipin") < 0)
                {
                    return false;
                }
                else
                {
                    Log.d("URL改变", "000000000000");
                    //isDown = true;
                    view.loadUrl(down);
                    return true;
                }
            }

        }
    }

    public static String gb2312ToUtf8(String str) {

        String urlEncode = "" ;

        try {

            urlEncode = URLEncoder.encode (str, "UTF-8" );

        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();

        }

        return urlEncode;

    }

    public void shareQQ() {
        AndroidShare androidShare = new AndroidShare(this);
        androidShare.shareQQFriend("", content, AndroidShare.TEXT, null);
    }



    private void checkPermission() {
        String[] perms = {
                // 把你想要申请的权限放进这里就行，注意用逗号隔开
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.REQUEST_INSTALL_PACKAGES,
        };
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
            //Toast.makeText(this, "Permissions Granted!", Toast.LENGTH_LONG).show();
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "APP须要获取以下权限来保证运行",
                    123, perms);
        }
    }

    public void clearList()
    {
        makeFilePath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/SharedProject/", "shared.txt");
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SharedProject/shared.txt";
        File file =new File(fileName);
        try {
            FileWriter fileWriter =new FileWriter(file);
            fileWriter.write("");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onHideCustomView() {
            //退出全屏
            if (customView == null){
                return;
            }
            //如果你的界面有actionbar的话：
            //getSupportActionBar().show();
            //移除全屏视图并隐藏
            fullVideo.removeView(customView);
            fullVideo.setVisibility(View.GONE);
            //设置竖屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            //清除全屏
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        }

        @Override
        public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
            //如果你的界面有actionbar的话：
            //getSupportActionBar().hide();
            //进入全屏
            customView = view;
            fullVideo.setVisibility(View.VISIBLE);
            fullVideo.addView(customView);
            fullVideo.bringToFront();
            //设置横屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            //设置全屏
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static int getSecondTimestamp(Date date){
        if (null == date) {
            return 0;
        }
        String timestamp = String.valueOf(date.getTime()/1000);
        return Integer.valueOf(timestamp);
    }

    public void share()
    {
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    private boolean checkAppInstalled(Context context,String pkgName) {
        if (pkgName== null || pkgName.isEmpty()) {
            return false;
        }
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        if(packageInfo == null) {
            return false;
        } else {
            return true;//true为安装了，false为未安装
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isFinal == 1)
        {
            new CommomDialog(MainActivity.this, R.style.dialog, finalcontent, new CommomDialog.OnCloseListener() {
                @Override
                public void onClick(Dialog dialog, boolean confirm) {
                    webView.loadUrl(down);
                    dialog.dismiss();

                }
            }).setTitle("提示").show();
        }
    }

}
