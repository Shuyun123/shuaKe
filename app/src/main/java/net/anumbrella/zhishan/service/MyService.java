package net.anumbrella.zhishan.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import net.anumbrella.zhishan.R;
import net.anumbrella.zhishan.activity.LoginActivity;
import net.anumbrella.zhishan.activity.MainActivity;
import net.anumbrella.zhishan.activity.ShuaKeActivity;
import net.anumbrella.zhishan.config.Config;
import net.anumbrella.zhishan.http.CallBackListener;
import net.anumbrella.zhishan.utils.ThreadHundle;

/**
 * author：anumbrella
 * Date:16/10/8 下午6:52
 */

public class MyService extends Service {

    private Notification notification;

    private Intent notificationIntent;

    private PendingIntent pendingIntent;

    private ThreadHundle zhiShan = new ThreadHundle();

    private String userName, passWord;

    private static String logText = "";

    private NotificationManager manger = null;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver();
        manger = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

        notificationIntent = new Intent(this, LoginActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        zhiShan.setCallBackListener(new CallBackListener() {
            @Override
            public void change(String title, String text, String str) {
                Intent i = new Intent(Config.PARAMETER_SERVICE_INTENT);
                logText += str;
                if ((!title.isEmpty()) && (!text.isEmpty()))
                    setNoti(title, text);
                if (title.equals("score")) {
                    i.putExtra("score", str);
                }
                i.putExtra("logInfo", logText);
                sendBroadcast(i);
            }

            @Override
            public void loginInfo(String info) {
                Intent intent = new Intent(Config.LOGIN_INFO_INTENT);
                intent.putExtra("loginInfo", info);
                sendBroadcast(intent);
            }

            @Override
            public void updateClassContent(String content, String result) {
                Intent i = new Intent(Config.UPDATECLASS_CONTENT_INTENT);
                i.putExtra("content", content);
                i.putExtra("result", result);
                sendBroadcast(i);
            }

        });

    }


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.SK_STOP_INTENT);
        filter.addAction(Config.SK_START_INTENT);
        filter.addAction(Config.LOGIN_INTENT);
        filter.addAction(Config.LOGOUT_INTENT);
        filter.addAction(Config.PARAMETER_SERVICE_INTENT);
        registerReceiver(broadcastReceiver, filter);
    }


    /**
     * 设置广播接受方法
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Config.LOGIN_INTENT)) {
                userName = intent.getStringExtra("userName");
                passWord = intent.getStringExtra("passWord");
                zhiShan.doLogin(userName, passWord);
            } else if (intent.getAction().equals(Config.LOGOUT_INTENT)) {
                zhiShan.goEnd();
                manger.cancel(1);
            } else if (intent.getAction().equals(Config.SK_START_INTENT)) {
                zhiShan.goShuake();
            } else if (intent.getAction().equals(Config.SK_STOP_INTENT)) {
                zhiShan.goEnd();
                manger.cancel(1);
            }
        }
    };


    /**
     * 设置通知通知栏信息
     *
     * @param title
     * @param text
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setNoti(String title, String text) {
        notification = new Notification.Builder(getApplicationContext())
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(null)
                .setSmallIcon(R.mipmap.small)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.large))//下拉下拉列表里面的图标
                .setContentIntent(pendingIntent)
                .build();
        notification.flags |= notification.FLAG_ONGOING_EVENT; //将此通知放到通知栏的"Ongoing"即"正在运行"组中
        // notification.flags |= notification.FLAG_NO_CLEAR; //表明在点击了通知栏中的"清除通知"后，此通知不清除
        startForeground(1, notification);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void setLogInfoEmpty() {
        logText = "";
    }


}
