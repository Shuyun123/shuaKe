package net.anumbrella.zhishan.http;

/**
 * author：anumbrella
 * Date:16/10/8 下午7:00
 */

public interface CallBackListener {
    void change(String title, String text, String str);
    void loginInfo(String info);
    void updateClassContent(String content,String result);
}
