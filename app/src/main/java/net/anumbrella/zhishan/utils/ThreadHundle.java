package net.anumbrella.zhishan.utils;

import android.os.Environment;

import net.anumbrella.zhishan.app.App;
import net.anumbrella.zhishan.bean.ClassModelBean;
import net.anumbrella.zhishan.http.CallBackListener;
import net.anumbrella.zhishan.http.HttpInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * author：anumbrella
 * Date:16/10/8 下午6:50
 */

public class ThreadHundle {

    private Thread mThreadLogin;

    private HttpUtils httpUtils = new HttpUtils();

    private HttpInfo httpInfo = new HttpInfo();

    private String returnResult;

    private CallBackListener sender;

    private String DWRSESSIONID;

    public static boolean isSleep = false;

    /**
     * 课程答案
     */
    private String CourseAnswer;


    /**
     * 每节的媒体id
     */
    private List<String> MediaId = new ArrayList<String>();


    /**
     * 每个课程里面每章下面每节的ID
     */
    private List<String> AllSectionID = new ArrayList<String>();

    /**
     * 每个课程里面每章下面每节的名字
     */
    private List<String> AllSectionName = new ArrayList<String>();


    /**
     * 刷课挂时间队列
     */
    private List<String> queneSection = new ArrayList<String>();


    /**
     * 包含各个课程id，成绩等的list
     */
    private static List<Map<String, Object>> classContentList = new ArrayList<Map<String, Object>>();

    /**
     * 是否结束挂提线程
     */
    private boolean Endthread = false;

    private String userName;

    private String passWord;

    /**
     * 等待线程提示
     */
    private String countDownTitle;

    /**
     * 等待线程时间
     */
    private int countDownTime;


    private int TRpos = 0;// RefreshCourse取每科目的position
    private int TDpos = 0;// RefreshCourse取每科的id名称

    private int zhang_GetSection = 0;   //GetSection取每个课程的章节
    private int jie_GetSection = 0;     //GetSection取每个课程章下面具体的节数


    private int pos_MediaPj = 0;  //媒体评价截取index


    private int iPos_InitPostTime = 0; //课程挂时间截取index


    private boolean wrong = false;//挂题网络错误


    private int currentIndex = 0;


    public void setCallBackListener(CallBackListener listener) {
        sender = listener;
    }


    /**
     * 开始登录线程
     *
     * @param userName
     * @param passWord
     */
    public void doLogin(String userName, String passWord) {
        this.userName = userName;
        this.passWord = passWord;
        if (mThreadLogin == null) {
            mThreadLogin = new Thread(runnableLogin);
            mThreadLogin.start();
        } else {
            mThreadLogin = null;
            mThreadLogin = new Thread(runnableLogin);
            mThreadLogin.start();
        }
    }


    /**
     * 开始刷课线程
     */
    public void goShuake() {
        if (mThreadLogin == null) {
            mThreadLogin = new Thread(runnableShuaKe);
            mThreadLogin.start();
        } else {
            mThreadLogin = null;
            mThreadLogin = new Thread(runnableShuaKe);
            mThreadLogin.start();
        }
    }


    /**
     * 手动停止刷课
     */
    public void goEnd() {
        Endthread = true;
    }


    Runnable runnableLogin = new Runnable() {
        public void run() {
            Endthread = false;
            wrong = false;
            if (login()) {
                sender.loginInfo("success");
            }
            RefreshCourse();
            return;
        }
    };

    Runnable runnableCountDown = new Runnable() {
        public void run() {
            countDown();
            return;
        }
    };


    Runnable runnableShuaKe = new Runnable() {
        public void run() {
            shuaKe();
        }
    };


    /**
     * 线程结束等待
     */
    private void countDown() {
        int minute;
        int second;
        while (countDownTime > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                Endthread = true;
                e.printStackTrace();
            }
            if (Endthread == true) {
                endCourse();
                return;
            }
            countDownTime -= 1;
            minute = countDownTime / 60;
            second = countDownTime - minute * 60;
            if (minute >= 0 && second >= 0) {
                sender.change(countDownTitle, "剩余时间->" + minute + ":" + second, "");
            }
        }

    }


    /**
     * 开始刷课
     */
    public void shuaKe() {
        String user = PreferenceUtils.readString(App.getContext(), "login", "userName");
        sender.change("", "", "\n开始挂课：");
        Endthread = false;
        wrong = false;

        ThreadNextCourse();
        PreferenceUtils.write(App.getContext(), "saveProgress", user, currentIndex);
        if (Endthread == true && wrong == false) {
            sender.change("挂课停止", "挂课已手动停止", "\n挂课已手动停止。\n");
            return;
        } else if (Endthread == true && wrong == true) {
            sender.change("", "", "\n\n网络不太好，请在良好的网络环境下重试\n");
            return;
        }
        RefreshCourse();
        sender.change("挂课完毕", "挂课完毕", "挂课完毕\n");
        return;
    }

    /**
     * 这是挂时间的线程
     * 客户端每15秒像服务器发送一段内容，每次发送，batchId++，当发送第四次时
     * 客户端会再次发送内容来获取当前课程的剩余时间
     * 这里使用N个线程来挂时间，可以大幅度加快挂时间
     */
    private void InitPostTime() {
        sender.change("正在挂时间", "挂时间开始", "开始挂时间：\n");
        String section;
        String CourseID;//当前进行的课程代码
        String CourseName;//当前进行的课程名字
        String SectionID;
        String SectionName;
        int n = queneSection.size();
        for (int i = 0; i < n; i++) {
            iPos_InitPostTime = 0;
            section = queneSection.get(0);
            queneSection.remove(0);
            CourseID = SubString(section, "|", "|", "iPos_InitPostTime");
            CourseName = SubString(section, "|", "|", "iPos_InitPostTime");
            SectionID = SubString(section, "|", "|", "iPos_InitPostTime");
            SectionName = SubString(section, "|", "|", "iPos_InitPostTime");
            PostTime(CourseID, CourseName, SectionID, SectionName);
            if (Endthread == true) {
                endCourse();
                return;
            }
            RefreshCourseGrade(CourseID);
            sender.change("score", "", (String) classContentList.get(ClassModelBean.getIndex()).get("2"));
        }
    }

    /**
     * @param courseID
     * @param courseName
     * @param sectionID
     * @param sectionName
     */
    private void PostTime(String courseID, String courseName, String sectionID, String sectionName) {
        String result;
        int batchId = 1;
        String LastTime = "---";//上次剩余时间
        while (true) {
            if (Endthread == true) {
                endCourse();
                return;
            }
            result = GetSectionStatus(courseID, sectionID, batchId);
            if (result.equals("wrong")) {
                sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                Endthread = true;
                endCourse();
                return;
            }
            if (result.contains(
                    "OK</strong> \\r\\n                 <span class=\\\"explain_rate\\\"><a href=\\\"javascript:;\\\" onclick=\\\"atPage(\\\'时间说明")) {
                /*挂完了*/
                sender.change("", "", "\n该章节的时间挂完了：" + " " + sectionName + "\n");
                return;
            } else {
                String RemainTime = SubString(result, "已学时间\\\">", "</span>");
                RemainTime = RemainTime + "/" + SubString(result, "总学习时间\\\">", "</span>");
                if (!LastTime.equalsIgnoreCase(RemainTime)) {
                    LastTime = RemainTime;
                    sender.change("", "", "\n剩余时间：" + sectionName + " " + RemainTime + "\n");
                    batchId++;
                }
                goCountDown("等待提交时间", 15);
                for (int i = 0; i < 15; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                        Endthread = true;
                        e.printStackTrace();
                    }
                    if (Endthread == true) {
                        endCourse();
                        return;
                    }
                }

                httpInfo.url = getString(109);
                httpInfo.Header = getString(117);
                httpInfo.Header = httpInfo.Header.replace("CourseID", courseID);
                httpInfo.Header = httpInfo.Header.replace("SectionID", sectionID);
                httpInfo.PostData = getString(110) + GetScriptSessionId();
                httpInfo.PostData = httpInfo.PostData.replace("CourseID", courseID);
                httpInfo.PostData = httpInfo.PostData.replace("SectionID", sectionID);
                httpInfo.PostData = httpInfo.PostData.replace("BatchID", batchId + "");
                if (Endthread == true) {
                    endCourse();
                    return;
                }
                result = httpUtils.Post(httpInfo);
                if (result.equals("wrong")) {
                    sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                    Endthread = true;
                    endCourse();
                    return;
                }
                if (!result.contains("flag:1")) {
                    sender.change("挂时间出错", "挂时错误", "POSS时间出错：" + courseName + " " + sectionName + "\n");
                } else {
                    sender.change("", "", "挂了15秒：" + sectionName + "\n");
                }
                batchId++;
            }

        }

    }

    /**
     * 刷课总流程
     *
     * @return
     */
    private void ThreadNextCourse() {
        String courseID = ClassModelBean.getCourseId();//当前进行的课程代码
        String courseName = ClassModelBean.getCourseName(); //当前进行的课程的名称
        if (GetSection(courseID, courseName)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TK/";
            CourseAnswer = "";
            CourseAnswer = ReadAnswerFromFile(path + courseID + ".txt");//读取课程答案
            if (CourseAnswer.isEmpty()) {
                sender.change("未找到题库", "点击查看详情", "未找到题库->课程ID：" + courseID + " 课程名字：" + courseName + "\n");
                Endthread = true;
            }
            if (Endthread == true) {
                endCourse();
                return;
            }
            //开始当前课程
            ThreadNextSection(courseID, courseName);
            if (Endthread == true) {
                endCourse();
                return;
            }
        } else {
            sender.change("获取课程章节失败", "获取课程章节失败", courseID + " " + courseName + "获取课程章节失败\n");
            Endthread = true;
            return;
        }
    }

    /**
     * 开始当前课程的章节
     *
     * @param courseID
     * @param courseName
     */
    private void ThreadNextSection(String courseID, String courseName) {
        String SectionID;//当前进行节的ID
        String SectionName;//当前进行节的名字
        String user = PreferenceUtils.readString(App.getContext(), "login", "userName");
        int t = PreferenceUtils.readInt(App.getContext(), "saveProgress", user, 0);
        for (int i = t; i <= AllSectionID.size(); i++) {
            currentIndex = i;
            if (i == AllSectionID.size()) {
                RefreshCourseGrade(courseID);
                sender.change("score", "", (String) classContentList.get(ClassModelBean.getIndex()).get("2"));
                sender.change("", "", "您已经完成了该课程的题目和媒体评价,现在开始挂时间\n");
                if (Endthread == true) {
                    endCourse();
                    return;
                }
                InitQueneSection(courseID, courseName);
                InitPostTime();//挂时间启动
                return;
            } else {
                SectionID = AllSectionID.get(i);
                SectionName = AllSectionName.get(i);
            }
            sender.change("挂课中", "正在执行", "开始处理该节课程：\n" + SectionID + "." + SectionName + "\n");

            ThreadPostExes(courseID, courseName, SectionID, SectionName);
            RefreshCourseGrade(courseID);
            sender.change("score", "", (String) classContentList.get(ClassModelBean.getIndex()).get("2"));
            if (Endthread == true) {
                endCourse();
                return;
            }
        }
    }

    private void InitQueneSection(String courseID, String courseName) {
        if (queneSection.size() > 0) {
            return;
        }
        for (int i = 0; i < AllSectionID.size(); i++) {
            String SectionID = AllSectionID.get(i);
            String SectionName = AllSectionName.get(i);
            String status = GetSectionStatus(courseID, SectionID, 1);
            if (status.equals("wrong")) {
                sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                Endthread = true;
                endCourse();
                return;
            }

            if (!status.contains("OK</strong> \\r\\n                 <span class=\\\"explain_rate\\\"><a href=\\\"javascript:;\\\" onclick=\\\"atPage(\\\'时间说明")) {
            /*时间没挂完，加入待挂队列*/
                queneSection.add("|" + courseID + "|" + courseName + "|" + SectionID + "|"
                        + SectionName + "|");
            }

        }
    }

    /**
     * 将某章的题进行提交
     *
     * @param courseID
     * @param courseName
     * @param sectionID
     * @param sectionName
     */
    private void ThreadPostExes(String courseID, String courseName, String sectionID, String sectionName) {
        String Answer;//待提交的答案

        //本章课程学习的状态
        String status;
        boolean status_xiti = false;
        boolean status_meiti = false;

        status = GetSectionStatus(courseID, sectionID, 1);

        if (status.equals("wrong")) {
            sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
            Endthread = true;
            endCourse();
            return;
        }


        if (!status.contains("OK</strong> \\r\\n                 <span class=\\\"explain_rate\\\"><a href=\\\"javascript:;\\\" onclick=\\\"atPage(\\\'时间说明")) {
            /*时间没挂完，加入待挂队列*/
            queneSection.add("|" + courseID + "|" + courseName + "|" + sectionID + "|"
                    + sectionName + "|");
        }
        if (status.contains("OK</strong> \\r\\n                 <span class=\\\"explain_rate\\\"><a href=\\\"javascript:;\\\" onclick=\\\"atPage(\\\'习题说明")) {
            /*习题挂完了*/
            status_xiti = true;
        }

//        if (status.contains("OK</strong> \\r\\n             <span class=\\\"explain_rate\\\"><a href=\\\"javascript:;\\\" onclick=\\\"atPage(\\\'媒材说明")) {
//            /*媒体挂完了*/
//            status_meiti = true;
//        }
        if (status_xiti == true && status_meiti == true) {
            sender.change("", "", "本节已完成：" + sectionName + "\n\n");
            return;//全部挂完了就返回
        }

        /*开始获取本节的所有内容，包括媒体评价和习题*/
        httpInfo.url = getString(115);
        httpInfo.Header = getString(117) + getString(11);
        httpInfo.Header = httpInfo.Header.replace("CourseID", courseID);
        httpInfo.Header = httpInfo.Header.replace("SectionID", sectionID);
        httpInfo.PostData = getString(116) + GetScriptSessionId();
        httpInfo.PostData = httpInfo.PostData.replace("CourseID", courseID);
        httpInfo.PostData = httpInfo.PostData.replace("SectionID", sectionID);


        if (Endthread == true) {
            endCourse();
            return;
        }
        returnResult = httpUtils.Post(httpInfo);
        if (returnResult.equals("wrong")) {
            sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
            Endthread = true;
            endCourse();
            return;
        }


        if (status_meiti != true) {
            pos_MediaPj = 0;
            MediaId.clear();
            while (true) {
                String mediaId = SubString(returnResult, "parent.showMediaRight(", ")", "pos_MediaPj");
                MediaId.add(mediaId);
                if (mediaId.isEmpty()) {
                    break;
                }
            }
            String result = MediaPj(returnResult, courseName, sectionName);
            if (result.equals("netwrong")) {
                sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                Endthread = true;
                endCourse();
                return;
            } else if (result.equals("wrong")) {
                endCourse();
                return;
            }
        }

        returnResult = SubString(returnResult, "<span class=\\\"delNum", "</dd>");
        returnResult = Unicode2Chinese(returnResult);

        /*提交本章的题库*/
        Answer = SubString(CourseAnswer, "<" + sectionID + ">", "</" + sectionID + ">");
        if (returnResult.contains("正确率：100%") || status_xiti == true) {
            sender.change("", "", "本节题已做完：" + courseName + " " + sectionName + "\n");
            return;
        }

        //有题库,开始答题
        if (!Answer.isEmpty()) {
            httpInfo.url = getString(118);
            httpInfo.Header = getString(117);
            httpInfo.Header = httpInfo.Header.replace("CourseID", courseID);
            httpInfo.Header = httpInfo.Header.replace("SectionID", sectionID);
            httpInfo.PostData = getString(119) + GetScriptSessionId();
            //UFT-8 to URL
            String t = "";
            try {
                Answer = new String(Answer.getBytes(), "utf-8");
                t = java.net.URLEncoder.encode(Answer, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            httpInfo.PostData = httpInfo.PostData.replace("Answer", t);
            httpInfo.PostData = httpInfo.PostData.replace("CourseID", courseID);
            httpInfo.PostData = httpInfo.PostData.replace("SectionID", sectionID);

            if (isSleep) {
                int sleepTime = 120 + GetRandInt(1, 60);
                int sleepTimeSecond = sleepTime - 120;
                sender.change("", "", "模拟做题时间：等待" + sleepTimeSecond + "秒后提交题库（该时间系统随机生成）\n");
                goCountDown("等待题库提交", sleepTime);
                for (int i = 0; i < sleepTime; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        sender.change("网络不太好", "点击查看详情", "\n\n网络不太好，请在良好的网络环境下重试\n");
                        Endthread = true;
                        e.printStackTrace();
                    }
                    if (Endthread == true) {
                        endCourse();
                        sender.change("挂课停止", "挂课已手动停止", "\n挂课已手动停止。\n");
                        return;
                    }
                }
            } else {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    sender.change("网络不太好", "点击查看详情", "\n\n网络不太好，请在良好的网络环境下重试\n");
                    Endthread = true;
                    e.printStackTrace();
                }
                if (Endthread == true) {
                    endCourse();
                    sender.change("挂课停止", "挂课已手动停止", "\n挂课已手动停止。\n");
                    return;
                }
            }
            if (Endthread == true) {
                endCourse();
                return;
            }
            returnResult = httpUtils.Post(httpInfo);
            sender.change("", "", "该节题库已提交：" + courseName + " " + sectionName + "\n");

            if (returnResult.equals("wrong")) {
                sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                Endthread = true;
                endCourse();
                return;
            }
        } else {
            sender.change("该节无题库", "", "无题库：" + courseName + " " + sectionName + "\n");
        }

    }


    public void goCountDown(String countDownTitle, int sleepTime) {
        countDownTime = sleepTime;
        this.countDownTitle = countDownTitle;
        if (mThreadLogin == null) {
            mThreadLogin = new Thread(runnableCountDown);
            mThreadLogin.start();
        } else {
            mThreadLogin = null;
            mThreadLogin = new Thread(runnableCountDown);
            mThreadLogin.start();
        }
    }


    /**
     * 对媒体进行评价
     *
     * @param returnResult
     * @param courseName
     * @param sectionName
     * @return
     */
    private String MediaPj(String returnResult, String courseName, String sectionName) {
        String result;
        String MediaID;
        sender.change("正在提交媒体评价", "媒体评价", "提交媒体评价：" + " " + sectionName + "\n");
        for (int i = 0; i <= MediaId.size(); i++) {
            if (Endthread == true) {
                endCourse();
                return "wrong";
            }
            if (i == MediaId.size()) {
                sender.change("", "", "本节媒体已经评价完：" + " " + sectionName + "\n");
                break;
            }
            MediaID = MediaId.get(i);
            httpInfo.url = getString(118);
            httpInfo.Header = getString(122);
            httpInfo.Header = httpInfo.Header.replace("MediaID", MediaID);
            httpInfo.PostData = getString(121) + GetScriptSessionId();
            httpInfo.PostData = httpInfo.PostData.replace("MediaID", MediaID);

            sender.change("正在评价媒体", "媒体评价", "准备提交：" + " " + sectionName + " 媒体ID->" + MediaID + " \n");
            //媒体评价设置状态栏
            if (isSleep) {
                int time = GetRandInt(2, 5);
                sender.change("", "", "模拟提交时间：" + time + "秒后提交媒体评价（该时间系统随机生成）\n");
                //Thread.sleep(time* 1000);
                for (int j = 0; j < time; j++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                        Endthread = true;
                        e.printStackTrace();
                    }
                    if (Endthread == true) {
                        endCourse();
                        return "wrong";
                    }
                }
            } else {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                    Endthread = true;
                    e.printStackTrace();
                }
                if (Endthread == true) {
                    endCourse();
                    return "wrong";
                }
            }

            if (Endthread == true) {
                endCourse();
                return "wrong";
            }
            result = httpUtils.Post(httpInfo);//提交
            if (result.equals("wrong")) {
                sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
                Endthread = true;
                endCourse();
                return "netwrong";
            }
            sender.change("", "", "媒体评价提交成功\n\n");

        }
        return "true";
    }


    /**
     * 获取章节的学习状态,例如时间完成多少，题目完成多少，媒体评价完成多少
     *
     * @param CourseID
     * @param SectionID
     * @param batchId
     * @return
     */
    private String GetSectionStatus(String CourseID, String SectionID, int batchId) {
        String t;
        String result;
        httpInfo.url = getString(109);
        httpInfo.Header = getString(117);
        httpInfo.Header = httpInfo.Header.replace("CourseID", CourseID);
        httpInfo.Header = httpInfo.Header.replace("SectionID", SectionID);

        httpInfo.PostData = getString(112) + GetScriptSessionId();
        httpInfo.PostData = httpInfo.PostData.replace("CourseID", CourseID);
        httpInfo.PostData = httpInfo.PostData.replace("SectionID", SectionID);
        t = "" + batchId;
        httpInfo.PostData = httpInfo.PostData.replace("BatchID", t);
        if (Endthread == true) {
            endCourse();
            return "wrong";
        }

        result = httpUtils.Post(httpInfo);
        if (result.equals("wrong")) {
            sender.change("网络不太好", "点击查看详情", "\n\n网络不太好，请在良好的网络环境下重试\n");
            Endthread = true;
            endCourse();
            return "wrong";
        }
        result = Unicode2Chinese(result);
        return result;
    }


    /**
     * 更新成绩
     *
     * @param courseid
     * @return
     */
    public boolean RefreshCourseGrade(String courseid) {

        httpInfo.url = "http://www.attop.com/wk/index.htm?id=" + courseid;
        httpInfo.Header = "";
        if (Endthread == true) {
            endCourse();
            return false;
        }
        returnResult = httpUtils.Get(httpInfo);
        if (returnResult.equals("wrong")) {
            sender.change("网络不太好", "点击查看详情", "\n\n网络不太好，请在良好的网络环境下重试\n");
            Endthread = true;
            wrong = true; //网络错误
            endCourse();
            return false;
        }
        String Grade = SubString(returnResult, "class=\"markNum\">", "</strong>");
        if (Grade.isEmpty()) {
            return false;
        }
        int index = ClassModelBean.getIndex();
        if (index != -1) {
            Map<String, Object> map = classContentList.get(index);
            map.put("2", Grade);
            classContentList.set(index, map);//更新list中课程成绩
            return true;
        }


        return false;
    }

    /**
     * 读取文件必须无ROM UTF-8
     *
     * @param fileName
     * @return
     */
    public String ReadAnswerFromFile(String fileName) {
        String output = "";
        try {
            File urlFile = new File(fileName);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(urlFile), "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String mimeTypeLine = null;
            while ((mimeTypeLine = br.readLine()) != null) {
                output = output + mimeTypeLine;
            }
            br.close();
            isr.close();
            ;
        } catch (Exception e) {
            sender.change("", "", "读取答案失败");
            return "";
        }
        return output;
    }


    /**
     * 根据CourseID取出该课程中的所有章节
     *
     * @param Courseid
     * @param CourseName
     * @return
     */
    public boolean GetSection(String Courseid, String CourseName) {
        httpInfo.url = "http://www.attop.com/wk/learn.htm?id=" + Courseid;
        httpInfo.Header = "";
        if (Endthread == true) {
            endCourse();
            return false;
        }
        returnResult = httpUtils.Get(httpInfo);
        if (returnResult.equals("wrong")) {
            sender.change("网络不太好", "网络错误", "\n\n网络不太好，请在良好的网络环境下重试\n");
            Endthread = true;
            wrong = true; //网络错误
            endCourse();
            return false;
        }
        //页面错误
        if (!returnResult.contains("个人中心")) {
            sender.change("网络不太好", "网络错误", "\n\n不好意思，在挂下一门课的时候发生了未知错误，可能是你断网了，请重试！\n");
            Endthread = true;
            return false;
        }

        AllSectionID.clear();
        AllSectionName.clear();

        zhang_GetSection = 0;
        jie_GetSection = 0;
        String zhang;  //课程的第几章
        String jie;    //第几章的第几节
        sender.change("", "", "课程名称->" + CourseName + "\n");
        while (true) {
            zhang = SubString(returnResult, "<dt name=\"zj\"", "</dd>", "zhang_GetSection");
            if (zhang.isEmpty()) {
                break;
            }
            while (true) {
                jie = SubString(zhang, "<li", "</li>", "jie_GetSection");
                if (jie.isEmpty()) {
                    break;
                }
                String temp;
                temp = SubString(zhang, "span title=\"", "</span>");
                temp = temp.substring(0, 4);//取出第几章
                AllSectionID.add(SubString(jie, "id=\"j_", "\">"));
                AllSectionName.add(temp + " " + SubString(jie, "title=\"", "\""));
            }
            jie_GetSection = 0;

        }
        return true;
    }


    /**
     * 执行登录操作
     *
     * @return
     */
    public boolean login() {

        /**
         * 获取JSESSIONID
         */
        httpInfo.url = "http://www.attop.com/";
        if (Endthread == true) {
            sender.loginInfo("登陆失败,网络不太好,请在良好的网络环境下重试");
            return false;
        }

        String returnInfo = httpUtils.Get(httpInfo);
        if (returnInfo.equals("wrong")) {
            sender.loginInfo("登陆失败,网络不太好,请在良好的网络环境下重试");
            Endthread = true;
            wrong = true; //网络错误
            endCourse();
            return false;
        }
        /**
         * 获取DWRSESSIONID
         */
        httpInfo.url = getString(101);
        httpInfo.Header = "Referer: http://www.attop.com/index.htm";
        httpInfo.PostData = getString(102);

        if (Endthread == true) {
            sender.loginInfo("登陆失败,网络不太好,请在良好的网络环境下重试");
            return false;
        }

        String text = httpUtils.Post(httpInfo);
        if (text.equals("wrong")) {
            sender.loginInfo("登陆失败,网络不太好,请在良好的网络环境下重试");
            Endthread = true;
            wrong = true; //网络错误
            endCourse();
            return false;
        }

        //拼接cookie
        DWRSESSIONID = SubString(text, "r.handleCallback(\"0\",\"0\",\"", "\");");
        httpInfo.Cookie = "DWRSESSIONID=" + DWRSESSIONID + ";" + httpInfo.Cookie;

        /**
         * 开始登陆
         */
        httpInfo.url = getString(103);
        httpInfo.Header = getString(105);
        httpInfo.PostData = getString(104) + GetScriptSessionId();
        httpInfo.PostData = httpInfo.PostData.replace("Username", userName);
        httpInfo.PostData = httpInfo.PostData.replace("Password", passWord);
        httpInfo.Cookie = httpInfo.Cookie + ";rand=" + GetRandStr(1000, 9999);

        String str = httpUtils.Post(httpInfo);
        if (str.equals("wrong")) {
            sender.loginInfo("登陆失败,网络不太好,请在良好的网络环境下重试");
            Endthread = true;
            wrong = true; //网络错误
            endCourse();
            return false;
        }
        if (str.contains("flag:-4") || str.contains("flag:20")) {
            sender.loginInfo("登录失败,你已多次输入错误的账号或密码,请完全关闭APP后重新运行");
            Endthread = true;
            return false;
        }
        if (!str.contains("flag:1")) {
            sender.loginInfo("登录失败,用户名或密码错误");
            Endthread = true;
            return false;
        }
        return true;
    }


    public String GetScriptSessionId() {
        /*原js
         * _pageId = tokenify(new Date().getTime()) + "-" +
		 * tokenify(Math.random() * 1E16);
		 * ScriptSessionId=DWRSESSIONID+"/"+_pageId;
		 */
        String ScriptSessionId;
        ScriptSessionId = DWRSESSIONID + "/";
        ScriptSessionId = ScriptSessionId + tokenify(new Date().getTime()) + "-"
                + tokenify((long) (Math.random() * 1E16));
        return ScriptSessionId;
    }


    /**
     * 刷新课表
     *
     * @return
     */
    public void RefreshCourse() {
        httpInfo.url = getString(106);
        httpInfo.Header = getString(108);
        httpInfo.PostData = getString(107) + GetScriptSessionId();
        if (Endthread == true) {
            endCourse();
            return;
        }
        returnResult = httpUtils.Post(httpInfo);
        if (returnResult.equals("wrong")) {
            sender.updateClassContent("网络不太好，请在良好的网络环境下重试", "error");
            Endthread = true;
            wrong = true; //网络错误
            endCourse();
            return;
        }

        returnResult = Unicode2Chinese(returnResult);
        classContentList.clear();
        /**
         * 刷新所有课程
         */
        returnResult += "</a></td>\\r\\n              </tr>\\r\\n";
        if (!returnResult.contains("flag:0")) {
            return;
        }

        String TD = "";//一列
        TDpos = 0;

        String TR = "";//一行
        TRpos = 0;
        while (true) {
            Map<String, Object> map = new HashMap<String, Object>();
            TR = SubString(returnResult, "<tr>\\r\\n                <td>", "</tr>\\r\\n", "TRpos");//取出一行
            if (TR.equals(""))
                break;
            TD = SubString(TR, "", "</td>\\r\\n", "TDpos");
            map.put("0", TD);//课程代码
            TD = SubString(TR, "target=\\\"_blank\\\">", "</a></td>", "TDpos");
            map.put("1", TD);//课程名称
            TD = SubString(TR, "<td>", "</td>\\r\\n", "TDpos");
            map.put("2", TD);//课程成绩
            TD = SubString(TR, "<td>", "</td>\\r\\n", "TDpos");
            map.put("3", TD);//学习期限
            TD = SubString(TR, "<td>", "</td>\\r\\n", "TDpos");
            map.put("4", TD);//学习状态
            TDpos = 0;
            classContentList.add(map);
        }
        if (!classContentList.isEmpty()) {
            sender.updateClassContent("更新课程列表成功，现在您可以开始挂课了！：)", "success");
        }
    }


    /**
     * 将Unicode转换为中文
     *
     * @param utfString
     * @return
     */
    public String Unicode2Chinese(String utfString) {
        StringBuilder sb = new StringBuilder();
        int i = -1;
        int pos = 0;
        while ((i = utfString.indexOf("\\u", pos)) != -1) {
            sb.append(utfString.substring(pos, i));
            if (i + 5 < utfString.length()) {
                pos = i + 6;
                sb.append((char) Integer.parseInt(utfString.substring(i + 2, i + 6), 16));
            }
        }
        return sb.toString();
    }


    public String tokenify(long number) {

		/*原js
         * function(number) { var tokenbuf = []; var charmap =
		 * "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ*$";
		 * var remainder = number; while (remainder > 0) {
		 * tokenbuf.push(charmap.charAt(remainder & 0x3F)); remainder =
		 * Math.floor(remainder / 64); } return tokenbuf.join(""); }
		 */
        String tokenbuf = "";
        String charmap = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ*$";
        long remainder = number;
        while (remainder > 0) {
            tokenbuf = tokenbuf + charmap.charAt((int) (remainder) & 0x3F);
            remainder = (long) Math.floor(remainder / 64);
        }
        return tokenbuf;
    }


    /**
     * 随机生成m-n的数字
     *
     * @param m
     * @param n
     * @return
     */
    public String GetRandStr(int m, int n) {
        Random rand = new Random();
        return "" + (m + rand.nextInt(n - m));
    }


    /**
     * 结束刷课数据操作
     */
    public void endCourse() {
        httpInfo.url = "";
        httpInfo.Header = "";
        httpInfo.PostData = "";
        returnResult = "";

        CourseAnswer = "";
        queneSection.clear();
        AllSectionID.clear();//每个课程里面每节的ID
        AllSectionName.clear();
        DWRSESSIONID = "";

    }


    /**
     * 拼接的路由和post的数据
     *
     * @param i
     * @return
     */
    public String getString(int i) {
        switch (i) {
            case 101:
                return "http://www.attop.com/js/ajax/call/plaincall/__System.generateId.dwr";
            case 102:
                return "callCount=1\nc0-scriptName=__System\nc0-methodName=generateId\nc0-id=0\nbatchId=0\ninstanceId=0\npage=%2Findex.htm\nscriptSessionId=\nwindowName=\n";
            case 103:
                return "http://www.attop.com/js/ajax/call/plaincall/zsClass.coreAjax.dwr";
            case 104:
                return "callCount=1\nwindowName=\nc0-scriptName=zsClass\nc0-methodName=coreAjax\nc0-id=0\nc0-param0=string:login\nc0-e1=string:Username\nc0-e2=string:Password\nc0-e3=string:\nc0-e4=number:2\nc0-param1=Object_Object:{username:reference:c0-e1, password:reference:c0-e2, rand:reference:c0-e3, autoflag:reference:c0-e4}\nc0-param2=string:doLogin\nbatchId=1\ninstanceId=0\npage=%2Flogin_pop.htm\nscriptSessionId=";
            case 105:
                return "Referer: http@//www.attop.com/login_pop.htm";
            case 106:
                return "http://www.attop.com/js/ajax/call/plaincall/zsClass.commonAjax.dwr";
            case 107:
                return "callCount=1\nwindowName=\nc0-scriptName=zsClass\nc0-methodName=commonAjax\nc0-id=0\nc0-param0=string:getAjaxList\nc0-e1=string:\nc0-e2=string:study.htm\nc0-e3=number:1\nc0-e4=string:showajaxinfo\nc0-param1=Object_Object:{param:reference:c0-e1, pagename:reference:c0-e2, currentpage:reference:c0-e3, showmsg:reference:c0-e4}\nc0-param2=string:doGetAjaxList\nbatchId=2\ninstanceId=0\npage=%2Fuser%2Fstudy.htm\nscriptSessionId=";
            case 108:
                return "Referer: http@//www.attop.com/user/study.htm";
            case 109:
                return "http://www.attop.com/js/ajax/call/plaincall/zsClass.commonAjax.dwr";
            case 110:
                return "callCount=1\nwindowName=\nc0-scriptName=zsClass\nc0-methodName=commonAjax\nc0-id=0\nc0-param0=string:getWkOnlineNum\nc0-e1=number:CourseID\nc0-e2=number:SectionID\nc0-param1=Object_Object:{bid:reference:c0-e1, jid:reference:c0-e2}\nc0-param2=string:doGetWkOnlineNum\nbatchId=BatchID\ninstanceId=0\npage=%2Fwk%2Flearn.htm%3Fid%3DCourseID%26jid%3DSectionID\nscriptSessionId=";
            case 112:
                return "callCount=1\nwindowName=\nc0-scriptName=zsClass\nc0-methodName=commonAjax\nc0-id=0\nc0-param0=string:getAjaxList2\nc0-e1=string:id%3DCourseID%26jid%3DSectionID\nc0-e2=string:learn_1.htm\nc0-e3=number:1\nc0-e4=string:showajaxinfo2\nc0-param1=Object_Object:{param:reference:c0-e1, pagename:reference:c0-e2, currentpage:reference:c0-e3, id:reference:c0-e4}\nc0-param2=string:doShowAjaxList2\nbatchId=BatchID\ninstanceId=0\npage=%2Fwk%2Flearn.htm%3Fid%3DCourseID\nscriptSessionId=";
            case 115:
                return "http://www.attop.com/js/ajax/call/plaincall/zsClass.commonAjax.dwr";
            case 116:
                return "callCount=1\nwindowName=\nc0-scriptName=zsClass\nc0-methodName=commonAjax\nc0-id=0\nc0-param0=string:getAjaxList\nc0-e1=string:id%3DCourseID%26jid%3DSectionID\nc0-e2=string:learn.htm\nc0-e3=number:1\nc0-e4=string:showajaxinfo\nc0-param1=Object_Object:{param:reference:c0-e1, pagename:reference:c0-e2, currentpage:reference:c0-e3, showmsg:reference:c0-e4}\nc0-param2=string:doGetAjaxList\nbatchId=2\ninstanceId=0\npage=%2Fwk%2Flearn.htm%3Fid%3DCourseID\nscriptSessionId=";
            case 117:
                return "Referer: http@//www.attop.com/wk/learn.htm?id=CourseID&jid=SectionID";
            case 118:
                return "http://www.attop.com/js/ajax/call/plaincall/zsClass.dotAjax.dwr";
            case 119:
                return "callCount=1\nwindowName=\nc0-scriptName=zsClass\nc0-methodName=dotAjax\nc0-id=0\nc0-param0=string:doSubmitWkXtAll\nc0-e1=number:CourseID\nc0-e2=number:SectionID\nc0-e3=string:Answer\nc0-param1=Object_Object:{bid:reference:c0-e1, jid:reference:c0-e2, msg:reference:c0-e3}\nc0-param2=string:doCommonReturn\nbatchId=4\ninstanceId=0\npage=%2Fwk%2Flearn.htm%3Fid%3DCourseID\nscriptSessionId=";
            case 120:
                return "callCount=1\nwindowName=\nc0-scriptName=zsClass\nc0-methodName=dotAjax\nc0-id=0\nc0-param0=string:doResetWkXt\nc0-e1=number:CourseID\nc0-e2=number:SectionID\nc0-e3=number:AnswerID\nc0-param1=Object_Object:{bid:reference:c0-e1, jid:reference:c0-e2, pid:reference:c0-e3}\nc0-param2=string:doCommonReturn\nbatchId=3\ninstanceId=0\npage=%2Fwk%2Flearn.htm%3Fid%3DCourseID%26jid%3DSectionID\nscriptSessionId=";
            case 121:
                return "callCount=1\nwindowName=\nc0-scriptName=zsClass\nc0-methodName=dotAjax\nc0-id=0\nc0-param0=string:doWkMediaPj\nc0-e1=number:MediaID\nc0-e2=number:3\nc0-param1=Object_Object:{id:reference:c0-e1, type:reference:c0-e2}\nc0-param2=string:doWkMediaPj\nbatchId=1\ninstanceId=0\npage=%2Fwk%2Fmedia_pop.htm%3Fid%3DMediaID\nscriptSessionId=";
            case 122:
                return "Referer: http@//www.attop.com/wk/media_pop.htm?id=MediaID";
        }
        return "";
    }

    public int GetRandInt(int m, int n) {
        Random rand = new Random();
        return m + rand.nextInt(n - m);
    }


    public String SubString(String text, String a, String b) {
        int beginIndex = text.indexOf(a);
        int endIndex = text.indexOf(b, beginIndex + a.length());
        return text.substring(beginIndex + a.length(), endIndex);
    }


    public String SubString(String text, String a, String b, String type) {
        int beginIndex = 0;
        if (type.equals("TRpos")) {
            beginIndex = text.indexOf(a, TRpos);
        } else if (type.equals("TDpos")) {
            beginIndex = text.indexOf(a, TDpos);
        } else if (type.equals("zhang_GetSection")) {
            beginIndex = text.indexOf(a, zhang_GetSection);
        } else if (type.equals("jie_GetSection")) {
            beginIndex = text.indexOf(a, jie_GetSection);
        } else if (type.equals("pos_MediaPj")) {
            beginIndex = text.indexOf(a, pos_MediaPj);
        } else if (type.equals("iPos_InitPostTime")) {
            beginIndex = text.indexOf(a, iPos_InitPostTime);
        }

        if (beginIndex == -1) {
            return "";
        }
        int endIndex = text.indexOf(b, beginIndex + a.length());

        if (type.equals("TRpos")) {
            TRpos = endIndex;
        } else if (type.equals("TDpos")) {
            TDpos = endIndex;
        } else if (type.equals("zhang_GetSection")) {
            zhang_GetSection = endIndex;
        } else if (type.equals("jie_GetSection")) {
            jie_GetSection = endIndex;
        } else if (type.equals("pos_MediaPj")) {
            pos_MediaPj = endIndex;
        } else if (type.equals("iPos_InitPostTime")) {
            iPos_InitPostTime = endIndex;
        }
        if (endIndex == -1) {
            return "";
        }
        return text.substring(beginIndex + a.length(), endIndex);
    }


    public static List<Map<String, Object>> getClassContent() {
        return classContentList;
    }


}
