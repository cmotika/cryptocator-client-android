/*
 * Copyright (c) 2015, Christian Motika.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * all contributors, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, an acknowledgement to all contributors, this list of conditions
 * and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * 3. Neither the name Delphino Cryptocator nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS “AS IS” AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.cryptocator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.util.Log;

public class HttpStringRequest {

	public interface OnResponseListener {
		void response(String response);
	}

	private OnResponseListener listener;
	
	// -------------------------------------------------------------------------
	
	public void setOnResponseListener(OnResponseListener responseListener) {
		listener = responseListener;
	}
	
	// -------------------------------------------------------------------------

	public HttpStringRequest(Context context, String url, OnResponseListener responseListener) {
		listener = responseListener;
		(new UrlThread(context, url)).start();
	}
	
	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------

	class UrlThread extends Thread {
		Context context;
		String url;

		// ------------------------------------------------

		public UrlThread(Context context, String url) {
			super();
			this.context = context;
			this.url = url;
		}

		// ------------------------------------------------

		public void run() {
			String response;
			try {
				response = getData(context, url);
				listener.response(response);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	
	public static String getData(Context context, String url) throws ClientProtocolException, IOException {
		// Create a new HttpClient
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet request = new HttpGet();
		String returnValue = "";
		try {
	        URI website = new URI(url);
	        request.setURI(website);
	        HttpResponse response = httpclient.execute(request);
	        returnValue = getContentAsString(response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return returnValue;
	}

	// -------------------------------------------------------------------------

	public static String getContentAsString(HttpResponse response) throws IOException {
		String returnString = "";
		HttpEntity httpEntity = response.getEntity();

		InputStream inputStream = httpEntity.getContent();
		InputStreamReader is = new InputStreamReader(inputStream, "ISO-8859-1"); //"UTF-8");

		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(is);
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}

			
			returnString = writer.toString().replace("\n", "");
			
//			returnString = Utility.reencodeISO(returnString);
//			
//			returnString = returnString.replace("Markt", "Mürkt");
//			
//			String dummyString = returnString.substring(returnString.indexOf("Meldorf"));
			
			Log.d("Notdienst", "### returnString=" + returnString);
//			Log.d("Notdienst", "### returnString2=" + dummyString);

		}

		return returnString;
	}

	// -------------------------------------------------------------------------
	// -------------------------------------------------------------------------
	
}
