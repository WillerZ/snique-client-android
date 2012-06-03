package com.nomzit.snique;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class SniqueActivity extends Activity
{
	WebView wv;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        wv = (WebView)findViewById(R.id.webView1);
        wv.getSettings().setJavaScriptEnabled(true);
        SniqueWebViewClient wvc = new SniqueWebViewClient(this);
        wv.setWebViewClient(wvc);
        wv.loadUrl("http://blog.nomzit.com/snique/");
    }
    
    void didDecodeMessage(SniqueWebViewClient wvc, String message)
    {
    	this.setTitle(message);
    }
    
    void pageLoading(SniqueWebViewClient wvc, Bitmap favicon)
    {
    	this.setTitle(R.string.app_name);
    	try
		{
			Method getActionBarMethod = this.getClass().getMethod("getActionBar",(Class<?>)null);
			Object actionBar = getActionBarMethod.invoke(this, (Object)null);
			Method setIconMethod = actionBar.getClass().getMethod("setIcon", Drawable.class);
			setIconMethod.invoke(actionBar, new BitmapDrawable(favicon));
		}
    	catch (SecurityException e)
		{
    		Log.e("SniqueActivity", "Security exception getting getActionBar() method", e);
		}
    	catch (NoSuchMethodException e)
		{
		}
    	catch (IllegalArgumentException e)
		{
    		Log.e("SniqueActivity", "Illegal argument exception calling getActionBar() method", e);
		}
    	catch (IllegalAccessException e)
		{
    		Log.e("SniqueActivity", "Illegal access exception calling getActionBar() method", e);
		}
    	catch (InvocationTargetException e)
		{
    		Log.e("SniqueActivity", "Invocation target exception calling getActionBar() method", e);
		} 
    }
}