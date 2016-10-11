package net.anumbrella.zhishan.activity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.anumbrella.zhishan.R;
import net.anumbrella.zhishan.app.App;
import net.anumbrella.zhishan.bean.ClassModelBean;
import net.anumbrella.zhishan.config.Config;
import net.anumbrella.zhishan.utils.PreferenceUtils;
import net.anumbrella.zhishan.utils.ThreadHundle;
import net.anumbrella.zhishan.widget.PromptDialog;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * author：anumbrella
 * Date:16/10/8 下午3:19
 */

public class MainActivity extends AppCompatActivity {


    private List<Map<String, Object>> classContent;

    @BindView(R.id.userName)
    TextView userName;

    @BindView(R.id.listContent)
    ListView listContent;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver();
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        userName.setText(PreferenceUtils.readString(this, "login", "userName"));
        goLogin();
    }


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.UPDATECLASS_CONTENT_INTENT);
        registerReceiver(broadcastReceiver, filter);
    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Config.UPDATECLASS_CONTENT_INTENT)) {
                String content = intent.getStringExtra("content");
                String result = intent.getStringExtra("result");
                if (result.equals("success")) {
                    Toast.makeText(MainActivity.this, content, Toast.LENGTH_LONG).show();
                    classContent = ThreadHundle.getClassContent();
                    if (classContent.size() > 0) {
                        listContent.setAdapter(new Adapter(MainActivity.this));
                    }
                } else {
                    Toast.makeText(MainActivity.this, content, Toast.LENGTH_LONG).show();
                }

            }
        }
    };

    private void goLogin() {
        Intent i = new Intent(Config.LOGIN_INTENT);
        i.putExtra("userName", PreferenceUtils.readString(this, "login", "userName"));
        i.putExtra("passWord", PreferenceUtils.readString(this, "login", "passWord"));
        sendBroadcast(i);
    }


    @OnClick({R.id.btn_logout})
    public void Click(View view) {
        switch (view.getId()) {
            case R.id.btn_logout:
                Intent i = new Intent(Config.LOGOUT_INTENT);
                sendBroadcast(i);
                PreferenceUtils.write(this, "login", "userName", null);
                PreferenceUtils.write(this, "login", "passWord", null);
                Intent intent = new Intent();
                intent.setClass(this, LoginActivity.class);
                startActivity(intent);
                finish();
                break;
        }

    }


    class Adapter extends BaseAdapter {

        private Context mContext;

        public Adapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return classContent.size();
        }

        @Override
        public Object getItem(int position) {
            return classContent.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_layout, null);
                holder = new ViewHolder();
                /*得到各个控件的对象*/
                holder.classId = (TextView) convertView.findViewById(R.id.classId);
                holder.className = (TextView) convertView.findViewById(R.id.className);
                holder.score = (TextView) convertView.findViewById(R.id.score);
                holder.time = (TextView) convertView.findViewById(R.id.time);
                holder.status = (TextView) convertView.findViewById(R.id.status);
                convertView.setTag(holder); //绑定ViewHolder对象
            } else {
                holder = (ViewHolder) convertView.getTag(); //取出ViewHolder对象
            }
            holder.status.setText((String) classContent.get(position).get("4"));
            holder.time.setText((String) classContent.get(position).get("3"));
            holder.score.setText((String) classContent.get(position).get("2"));
            holder.className.setText((String) classContent.get(position).get("1"));
            holder.classId.setText((String) classContent.get(position).get("0"));
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new PromptDialog.Builder(MainActivity.this)
                            .setTitle("提示")
                            .setTitleColor(R.color.white)
                            .setViewStyle(PromptDialog.VIEW_STYLE_TITLEBAR_SKYBLUE)
                            .setMessage("是否对该课程进行挂课?")
                            .setMessageSize(20f)
                            .setButton1("确定", new PromptDialog.OnClickListener() {
                                @Override
                                public void onClick(Dialog dialog, int which) {
                                    if (classContent.get(position).get("4").equals("已结业")) {
                                        Toast.makeText(MainActivity.this, "sorry,该课程已经结束了！：(", Toast.LENGTH_LONG).show();
                                    }
                                    if (classContent.get(position).get("4").equals("学习中") && classContent.get(position).get("2").equals("100")) {
                                        Toast.makeText(MainActivity.this, "您已经完成该课程的学习了！：)", Toast.LENGTH_LONG).show();
                                    }
                                    if (classContent.get(position).get("4").equals("学习中") && !classContent.get(position).get("2").equals("100")) {
                                        Intent intent = new Intent();
                                        intent.setClass(MainActivity.this, ShuaKeActivity.class);
                                        String courseId = (String) classContent.get(position).get("0");
                                        String courseName = (String) classContent.get(position).get("1");
                                        ClassModelBean.setIndex(position);
                                        ClassModelBean.setCourseId(courseId);
                                        ClassModelBean.setCourseName(courseName);
                                        startActivity(intent);
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .setButton2("取消", new PromptDialog.OnClickListener() {
                                @Override
                                public void onClick(Dialog dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            });
            return convertView;
        }
    }


    class ViewHolder {
        TextView classId;
        TextView className;
        TextView score;
        TextView time;
        TextView status;
    }


}
