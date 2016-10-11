package net.anumbrella.zhishan.activity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.anumbrella.zhishan.R;
import net.anumbrella.zhishan.app.App;
import net.anumbrella.zhishan.bean.ClassModelBean;
import net.anumbrella.zhishan.config.Config;
import net.anumbrella.zhishan.service.MyService;
import net.anumbrella.zhishan.utils.PreferenceUtils;
import net.anumbrella.zhishan.utils.ThreadHundle;
import net.anumbrella.zhishan.widget.PromptDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * author：anumbrella
 * Date:16/10/9 下午6:44
 */

public class ShuaKeActivity extends AppCompatActivity {

    private boolean isRun = false;

    @BindView(R.id.textview)
    TextView tipContent;

    @BindView(R.id.btn_start)
    Button btnStart;

    @BindView(R.id.courseName)
    TextView courseName;

    @BindView(R.id.courseScore)
    TextView courseScore;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver();
        setContentView(R.layout.activity_shuake);
        ButterKnife.bind(this);
        tipContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        courseName.setText(ClassModelBean.getCourseName());
        String score = (String) ThreadHundle.getClassContent().get(ClassModelBean.getIndex()).get("2");
        courseScore.setText(score);
        isRun = PreferenceUtils.readBoolean(App.getContext(), "RunBackground", "run", false);
        if (isRun) {
            btnStart.setText("停止");
        }
    }


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.PARAMETER_SERVICE_INTENT);
        registerReceiver(broadcastReceiver, filter);
    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Config.PARAMETER_SERVICE_INTENT)) {
                String log = intent.getStringExtra("logInfo");
                String score = intent.getStringExtra("score");
                if (score != null) {
                    courseScore.setText(score);
                }
                tipContent.setText(log);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_delete:
                MyService.setLogInfoEmpty();
                tipContent.setText("");
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @OnClick({R.id.btn_start})
    public void click(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                Intent i = new Intent();
                if (btnStart.getText().equals("挂课") && !isRun) {
                    i.setAction(Config.SK_START_INTENT);
                    btnStart.setText("停止");
                    isRun = true;
                } else {
                    i.setAction(Config.SK_STOP_INTENT);
                    btnStart.setText("挂课");
                    isRun = false;
                }
                sendBroadcast(i);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isRun) {
                showTipDialog();
            } else {
                MyService.setLogInfoEmpty();
                tipContent.setText("");
                PreferenceUtils.write(App.getContext(), "RunBackground", "run", false);
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showTipDialog() {
        new PromptDialog.Builder(ShuaKeActivity.this)
                .setTitle("提示")
                .setTitleColor(R.color.white)
                .setViewStyle(PromptDialog.VIEW_STYLE_TITLEBAR_SKYBLUE)
                .setMessage("是否在后台继续运行挂课程序?")
                .setMessageSize(20f)
                .setButton1("运行", new PromptDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog, int which) {
                        PreferenceUtils.write(App.getContext(), "RunBackground", "run", true);
                        dialog.dismiss();
                        finish();
                    }
                })
                .setButton2("禁止", new PromptDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog, int which) {
                        PreferenceUtils.write(App.getContext(), "RunBackground", "run", false);
                        Intent i = new Intent();
                        i.setAction(Config.SK_STOP_INTENT);
                        sendBroadcast(i);
                        MyService.setLogInfoEmpty();
                        tipContent.setText("");
                        dialog.dismiss();
                        finish();
                    }
                })
                .show();
    }


}
