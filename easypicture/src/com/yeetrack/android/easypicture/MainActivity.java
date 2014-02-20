package com.yeetrack.android.easypicture;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.yeetrack.android.easypicture.barcode.core.CaptureActivity;
import com.yeetrack.android.easypicture.tool.Constant;
import com.yeetrack.android.easypicture.tool.HttpClientFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @author xuemeng
 * @date 2014/2/11
 * 程序入口
 */
public class MainActivity extends Activity
{
    private String serverMatchUrl = "http://"+Constant.SERVERADDRESS+":"+ Constant.SERVERPORT+"/qrcodeMatch.php";
    private static final String TAG = "com.yeetrack.android.easypicture.Activity";
    private TextView textView;
    //扫描二维码按钮
    private Button scanButton;
    //客户端id
    private String uuid;

    private DefaultHttpClient httpClient;

    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            String result = msg.getData().getString("match_result");
            if(msg.what == 1)
            {
                Log.d("debug", result);
                if(result.startsWith("1"))
                {
                    //跳转到图片选择Activity
                    Toast.makeText(MainActivity.this, "配对成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, ImageFolderActivity.class);
                    intent.putExtra("uuid", uuid);
                    Toast.makeText(MainActivity.this, "扫描本地图片。。。", Toast.LENGTH_LONG).show();
                    startActivity(intent);
                }
                else if(result.startsWith("0"))
                {
                    Toast.makeText(MainActivity.this, "配对失败，请重试", Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(MainActivity.this, "程序异常，请检查网络是否连接。", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView)findViewById(R.id.textViewId);
        scanButton = (Button)findViewById(R.id.scanButtonId);

        //scanButton的监听器
        scanButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivityForResult(intent, 1);
            }
        });
    }

    /**
     * 为了得到传回的数据，必须在前面的Activity中（指MainActivity类）重写onActivityResult方法
     * requestCode 请求码，即调用startActivityForResult()传递过去的值
     * resultCode 结果码，结果码用于标识返回数据来自哪个新Activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        String result = null;
        try
        {
            result = data.getExtras().getString("qr_result");//得到新Activity 关闭后返回的数据
        }catch (Exception e) { e.printStackTrace(); }

        if("".equals(result) || null == result)
            return;
        Log.d(TAG, result);
        uuid = result;
        if(httpClient == null)
            httpClient = new DefaultHttpClient();

        //开启线程，发送配对请求
        final String uuid = result;
        new Thread()
        {
            @Override
            public void run()
            {
                //将id放入header中
                HttpGet get = new HttpGet(serverMatchUrl);
                get.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.107 Safari/537.36");
                //将二维码扫描到的sessionid，添加到httpclient中
                BasicClientCookie cookie = new BasicClientCookie("PHPSESSID", uuid);
                cookie.setVersion(0);
                cookie.setDomain("."+Constant.SERVERADDRESS);
                cookie.setPath("/");
                httpClient.getCookieStore().addCookie(cookie);
                try
                {
                    HttpResponse response = httpClient.execute(get);
                    //1表示匹配成功，0表示匹配失败
                    String responseResult = EntityUtils.toString(response.getEntity(), "utf-8");
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString("match_result", responseResult);
                    msg.setData(bundle);
                    msg.what = 1;
                    handler.sendMessage(msg);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }


}

