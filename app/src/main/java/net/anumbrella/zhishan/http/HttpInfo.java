package net.anumbrella.zhishan.http;

/**
 * author：anumbrella
 * Date:16/10/8 下午6:53
 */

public class HttpInfo {
    public String url = "";//链接
    public String Header = "";//协议头
    public String PostData = "";//POST专用，提交信息（实际上Cookies也会加入到这里）
    public String Cookie = "";//提交Cookies,本参数传递变量时会自动回传返回的Cookies
}
