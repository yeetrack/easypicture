package com.yeetrack.android.easypicture.tool;

import android.os.Environment;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by xuemeng on 14-2-13.
 * 图片操作类，包含图片扫描，图片上传
 */
public class ImageTool
{
    /**
     * sd卡中的图片文件夹路径集合
     */
    private static List<String> folderList;
    /**
     * 取每个图片文件夹中的第一个图片组成的集合，用来展示
     */
    private static List<String> folderImageList;


    /**
     * 扫描手机sd卡中的图片文件夹
     * @param isRefresh 是否重新扫描
     * @return
     */
    public static List<String> getAllImageFolderPath(boolean isRefresh , int depth)
    {
        //首先获取手机的存储空间
        //sdcard已经挂载
        if(android.os.Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            //扫描sdcard
            File rootDir = android.os.Environment.getExternalStorageDirectory();
            if(folderList == null)
            {
                folderList = new ArrayList<String>();
                folderImageList = new ArrayList<String>();
                imageFolderScan(rootDir.getAbsolutePath(), depth);
            }
            if(isRefresh)
            {
                folderList.clear();
                folderImageList.clear();
                imageFolderScan(rootDir.getAbsolutePath(), depth);
            }

        }

        return folderList;
    }

    /**
     * 根据folderSet 获得每个图片文件夹的第一个图片组成的集合
     */
    public static List<String> getFolderImageName()
    {
        return folderImageList;
    }

    //递归扫描图片文件夹
    private static void imageFolderScan(String fileName, int depth)
    {
        if(fileName==null || "".equals(fileName))
            return;
        File currentFile = new File(fileName);
        if(!currentFile.exists())
            return;
        if(currentFile.isFile() && isImage(fileName))
        {
            folderList.add(fileName);
            return;
        }
        if(depth<=0)
            return;

        //遍历当前文件夹下的文件
        File[] files = currentFile.listFiles();

        if(files==null)
            return;

        for(File index : files)
        {
            String indexName = index.getAbsolutePath();
            //如果是文件夹
            if(index.isDirectory())
                imageFolderScan(indexName, depth-1);
            else if(index.isFile())//如果是图片文件就将图片路径保存到List中，将图片路径保存到folderImageList，注意不要重复添加
            {
                if(isImage(index.getAbsolutePath()))
                {
                    folderList.add(fileName);
                    folderImageList.add("file://"+index.getAbsolutePath());
                    break;
                }
            }
        }
    }

    /**
     * 判断文件是否是图片
     */
    public static boolean isImage(String fileName)
    {
        if(fileName==null || "".equals(fileName))
            return false;
        String[] imageFormats = {".bmp", ".png", ".jpeg", ".jpg", ".gif", ".tiff"};
        for(String format : imageFormats)
        {
            if(fileName.toLowerCase().endsWith(format))
                return true;
        }
        return false;
    }

    /**
     * 上传指定的图片到服务器
     * @return 0:未登录 1:上传成功 2:其他错误
     */
    public static int image2Server(String fileName, String uuid)
    {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        //将uuid添加到cookie里
        BasicClientCookie cookie = new BasicClientCookie("PHPSESSID", uuid);
        cookie.setVersion(0);
        cookie.setDomain("."+Constant.SERVERADDRESS);
        cookie.setPath("/");
        httpClient.getCookieStore().addCookie(cookie);


        HttpPost post = new HttpPost("http://"+Constant.SERVERADDRESS+":"+Constant.SERVERPORT+"/imageUpload.php");
        //去掉路径前的file://
        File userImage = new File(fileName.substring(7));
        FileEntity fileEntity = new FileEntity(userImage, "image/jpeg");
        post.setEntity(fileEntity);

        //把图片路径写到header中，方便服务器使用
        //只要文件名，去掉filename中的路径
        post.addHeader("imageName", userImage.getName());
        int resultCode = 2;
        //上传
        try
        {
            HttpResponse response = httpClient.execute(post);
            String result = EntityUtils.toString(response.getEntity(), "utf-8");
            Log.d("debug", result);
            //服务器响应0：未登录 1：上传完成  2：未知错误
            if(result==null || "".equals(result))
                resultCode = 2;
            if(result.startsWith("0"))
                resultCode = 0;
            else if(result.startsWith("1"))
                resultCode = 1;
            else
                resultCode = 2;

        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            httpClient.getConnectionManager().shutdown();
        }
        return resultCode;
    }
}
