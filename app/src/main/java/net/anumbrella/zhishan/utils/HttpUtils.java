package net.anumbrella.zhishan.utils;

import net.anumbrella.zhishan.http.HttpInfo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * author：anumbrella
 * Date:16/10/8 下午6:57
 */

public class HttpUtils {


    /**
     * http封装请求方法
     *
     * @param httpInfo
     * @param type     type=0,为get方法;type=1,为post方法
     * @return
     */
    public String visit(HttpInfo httpInfo, int type) {
        String SetCookie = "";
        String resultData = "";
        URL url = null;
        try {
            url = new URL(httpInfo.url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (url != null) {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                if (type == 1) {
                    urlConnection.setRequestMethod("POST");
                } else {
                    urlConnection.setRequestMethod("GET");
                }
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(true);
                String strHeader = httpInfo.Header;

                //去除头部没用的信息
                strHeader = strHeader.replace(" ", "");
                strHeader = strHeader.replace("\r", "");
                String[] tem = strHeader.split("\n");

                for (int i = 0; i < tem.length; i++) {
                    if (tem[i].contains(":")) {
                        String[] tem2 = tem[i].split(":");
                        //因为要用:拆分，而Referer的网址的http中含有@，所以将@用:代替
                        if (tem2[1].contains("@")) {
                            tem2[1] = tem2[1].replace("@", ":");
                        }
                        urlConnection.setRequestProperty(tem2[0], tem2[1]);
                    }
                }

                if (!strHeader.contains("Referer:")) {
                    strHeader = strHeader + "\r\n" + "Referer:" + httpInfo.url;
                    urlConnection.setRequestProperty("Referer", httpInfo.url);
                }
                if (!strHeader.contains("Accept:")) {
                    strHeader = strHeader + "\r\n" + "Accept: */*";
                    urlConnection.setRequestProperty("Accept", "*/*");
                }
                if (!strHeader.contains("Accept-Language:")) {
                    strHeader = strHeader + "\r\n" + "Accept-Language: zh-cn";
                    urlConnection.setRequestProperty("Accept-Language", "zh-cn");
                }
                if (!strHeader.contains("Content-Type:")) {
                    strHeader = strHeader + "\r\n" + "Content-Type: application/x-www-form-urlencoded";
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                }
                if (!httpInfo.Cookie.isEmpty()) {
                    strHeader = strHeader + "\r\n" + "Cookie: " + httpInfo.Cookie;
                    urlConnection.setRequestProperty("Cookie", httpInfo.Cookie);
                }
                if ((!strHeader.contains("Content-Length:")) && type == 1) {
                    String tmp;
                    tmp = String.valueOf(httpInfo.PostData.length());
                    strHeader = strHeader + "\r\n" + "Content-Length: " + tmp;
                    urlConnection.setRequestProperty("Content-Length", tmp);
                }
                urlConnection.connect();
                DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
                if (type == 1) {
                    String content = httpInfo.PostData;
                    out.writeBytes(content);
                    out.flush();
                    out.close();
                }
                InputStreamReader in = new InputStreamReader(urlConnection.getInputStream(), "utf-8");
                BufferedReader buffer = new BufferedReader(in);
                String inputLine = null;
                while (((inputLine = buffer.readLine()) != null)) {
                    resultData += inputLine;
                }
                in.close();
                if (urlConnection.getHeaderField("Set-Cookie") != null) {
                    SetCookie = urlConnection.getHeaderField("Set-Cookie");
                    //将旧的cookie中的信息(新cookie没有的)加入到新的cookie中
                    httpInfo.Cookie = UpdateCookie(SetCookie, httpInfo.Cookie);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "wrong";
            }

        }

        return resultData;
    }

    public String UpdateCookie(String newCookie, String oldCookie) {

        String text = "";
        newCookie.replace(" ", "");
        oldCookie.replace(" ", "");

        if (oldCookie.contains("=")) {
            //将新cookie拆分
            String[] temNew = newCookie.split(";");
            String cookieNew[][] = new String[temNew.length][2];
            for (int i = 0; i < temNew.length; i++) {
                String[] temNew2 = temNew[i].split("=");
                cookieNew[i][0] = temNew2[0];
                cookieNew[i][1] = temNew2[1];
            }
            //将旧cookie拆分
            String[] temOld = oldCookie.split(";");
            String cookieOld[][] = new String[temOld.length][2];
            for (int i = 0; i < temOld.length; i++) {
                String[] temOld2 = temOld[i].split("=");
                cookieOld[i][0] = temOld2[0];
                cookieOld[i][1] = temOld2[1];
            }
            //将旧的cookie中的信息(新cookie没有的)加入到新的cookie中
            for (int i = 0; i < temOld.length; i++) {
                boolean exist = false;
                for (int j = 0; j < temNew.length; j++) {
                    if (cookieOld[i][0].equals(cookieNew[j][0])) {
                        exist = true;
                    }
                }
                if (!exist) {
                    text += cookieOld[i][0] + "=" + cookieOld[i][1] + ";";
                }
            }
            text += newCookie;
            return text;
        } else {
            //无旧的，直接返回新的
            return newCookie;
        }
    }


    /**
     * GET请求方法
     *
     * @param info
     * @return
     */
    public String Get(HttpInfo info) {
        return visit(info, 0);
    }


    /**
     * POST请求方法
     *
     * @param info
     * @return
     */
    public String Post(HttpInfo info) {
        return visit(info, 1);
    }


}
