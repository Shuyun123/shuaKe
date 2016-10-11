package net.anumbrella.zhishan.app;

import android.app.Application;
import android.content.Context;

/**
 * author：anumbrella
 * Date:16/10/8 下午6:59
 */

public class App extends Application {

    private static Context context;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
