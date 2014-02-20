package com.yeetrack.android.easypicture.tool;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Created by xuemeng on 14-2-12.
 */
public class HttpClientFactory
{
    private static DefaultHttpClient httpClient;

    private HttpClientFactory(){}

    public static DefaultHttpClient getInstance()
    {
        //初始化HttpClient
        if(httpClient == null)
        {
            httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        }
        return httpClient;
    }
}
