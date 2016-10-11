package net.anumbrella.zhishan.activity;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.anumbrella.customedittext.FloatLabelView;
import net.anumbrella.zhishan.R;
import net.anumbrella.zhishan.config.Config;
import net.anumbrella.zhishan.service.MyService;
import net.anumbrella.zhishan.utils.PreferenceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {

    private ProgressDialog mDialog;

    private String user;

    private String password;

    @BindView(R.id.login_user)
    FloatLabelView login_user;

    @BindView(R.id.login_password)
    FloatLabelView login_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isServiceRunning()) {
            Intent startIntent = new Intent(LoginActivity.this, MyService.class);
            startService(startIntent);
        }
        registerReceiver();
        if (PreferenceUtils.readString(this, "login", "userName") != null && PreferenceUtils.readString(this, "login", "passWord") != null) {
            Intent intent1 = new Intent(this, MainActivity.class);
            startActivity(intent1);
            finish();
            return;
        } else {
            setContentView(R.layout.activity_login);
            ButterKnife.bind(this);
            login_password.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        mDialog = new ProgressDialog(this);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setMessage("请稍等");
        mDialog.setIndeterminate(false);
        // 设置ProgressDialog 是否可以按退回按键取消
        mDialog.setCancelable(false);


        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TK/";
        if (!isFolderExist(path + "*5finish.txt")) {
            if (!creatFolder(path)) {
                Toast.makeText(this, "创建/TK目录失败", Toast.LENGTH_LONG).show();
            }
            if (goWrite(path)) {
                Toast.makeText(this, "题库成功写入到内部存储/TK目录下，您可以登录刷课了！", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "题库写入到内部存储/TK目录失败", Toast.LENGTH_LONG).show();

            }
        } else {
            Toast.makeText(this, "检测到题库已存在，您可以登录刷课了！", Toast.LENGTH_LONG).show();
        }
    }


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.LOGIN_INFO_INTENT);
        registerReceiver(broadcastReceiver, filter);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Config.LOGIN_INFO_INTENT)) {
                String str = intent.getStringExtra("loginInfo");
                if (str.equals("success")) {
                    //保存用户名和密码
                    PreferenceUtils.write(context, "login", "userName", user);
                    PreferenceUtils.write(context, "login", "passWord", password);
                    Intent intent1 = new Intent(context, MainActivity.class);
                    //intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent1);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, str, Toast.LENGTH_LONG).show();
                }
            }
        }
    };


    /**
     * 判断服务程序是否在运行中
     *
     * @return
     */
    public boolean isServiceRunning() {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager)
                LoginActivity.this.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(30);
        if (!(serviceList.size() > 0)) {
            return false;
        }
        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals("net.anumbrella.zhishan.service.MyService") == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }


    @OnClick({R.id.btn_login})
    public void Click(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                if (getData()) {
                    mDialog.show();
                    goLogin();
                }
        }

    }

    public boolean goWrite(String path) {
        if (!WriteAnswerFromFile(path + "3.txt", ReadRawFile(3)))
            return false;

        if (!WriteAnswerFromFile(path + "4.txt", ReadRawFile(4)))
            return false;

        if (!WriteAnswerFromFile(path + "5.txt", ReadRawFile(5)))
            return false;

        if (!WriteAnswerFromFile(path + "6.txt", ReadRawFile(6)))
            return false;

        if (!WriteAnswerFromFile(path + "7.txt", ReadRawFile(7)))
            return false;

        if (!WriteAnswerFromFile(path + "30.txt", ReadRawFile(30)))
            return false;

        if (!WriteAnswerFromFile(path + "31.txt", ReadRawFile(31)))
            return false;

        if (!WriteAnswerFromFile(path + "37.txt", ReadRawFile(37)))
            return false;

        if (!WriteAnswerFromFile(path + "38.txt", ReadRawFile(38)))
            return false;

        if (!WriteAnswerFromFile(path + "39.txt", ReadRawFile(39)))
            return false;

        if (!WriteAnswerFromFile(path + "40.txt", ReadRawFile(40)))
            return false;

        if (!WriteAnswerFromFile(path + "41.txt", ReadRawFile(41)))
            return false;

        String attention = "此目录为至善网题库\n是运行APP自生成的\n可添加相应课程的题库\n以[课程id].txt命名\ntxt文件必须以UTF-8无BOM格式编码\n删除或改变任意文件(点开后保存也算在内)\n会导致提交错误答案或程序异常\n如不小心更改可直接将整个目录删除\n再次运行APP会自动生成";
        if (!WriteAnswerFromFile(path + "*1此目录为至善网题库.txt", attention))
            return false;

        if (!WriteAnswerFromFile(path + "*2可添加文件.txt", attention))
            return false;

        if (!WriteAnswerFromFile(path + "*3但删除或改变任意已存在文件(点开后保存也算在内).txt", attention))
            return false;

        if (!WriteAnswerFromFile(path + "*4会导致提交错误答案或程序异常.txt", attention))
            return false;

        if (!WriteAnswerFromFile(path + "*5finish.txt", attention))
            return false;

        return true;
    }

    public String ReadRawFile(int i) {
        int id = getResources().getIdentifier(
                "tk" + i,
                "raw", getPackageName());
        InputStream in = getResources().openRawResource(id);
        String output = "";
        try {
            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String mimeTypeLine = null;
            while ((mimeTypeLine = br.readLine()) != null) {
                output = output + mimeTypeLine;
            }
            br.close();
            isr.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }


    public boolean WriteAnswerFromFile(String fileName, String content) {
        try {
            FileOutputStream fout = new FileOutputStream(fileName);
            byte[] bytes = content.getBytes();
            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private void goLogin() {
        Intent i = new Intent(Config.LOGIN_INTENT);
        i.putExtra("userName", user);
        i.putExtra("passWord", password);
        sendBroadcast(i);
    }

    private boolean getData() {
        user = login_user.getEditText().getText().toString();
        password = login_password.getEditText().getText().toString();
        if (user.equals("")) {
            Toast.makeText(this, "账号不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.equals("")) {
            Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }


    boolean isFolderExist(String strFolder) {
        try {
            File f = new File(strFolder);
            if (!f.exists()) {
                return false;
            }

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    boolean creatFolder(String strFolder) {
        File file = new File(strFolder);

        if (!file.exists()) {
            if (file.mkdirs()) {
                return true;
            } else
                return false;
        }
        return true;
    }


}
