package com.nomzit.snique;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SniqueWebViewClient extends WebViewClient {

	SniqueActivity activity;

	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		// If we get here, someone's followed a link in our WebView, and we need to parse the data.
		activity.new NetworkTask().execute(url);
		return true;
	}

	public SniqueWebViewClient(SniqueActivity act) {
		super();
		activity = act;
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		activity.pageLoading(this, favicon);
	}
}
