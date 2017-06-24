/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

/**
 * Utility class for performing HTTP GET and HTTP POST requests.
 * 
 * @author craignewton 
 *
 */
public class CustomHttpClient {

    /** The time it takes for our client to timeout */
    public static final int HTTP_TIMEOUT = 30 * 1000; // milliseconds

    /** Single instance of our HttpClient */
    private static HttpClient mHttpClient;

    /**
     * Get our single instance of our HttpClient object.
     * 
     * @return an HttpClient object with connection parameters set
     */
    private static HttpClient getHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
            final HttpParams params = mHttpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, HTTP_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, HTTP_TIMEOUT);
            ConnManagerParams.setTimeout(params, HTTP_TIMEOUT);
        }
        return mHttpClient;
    }

    /**
     * Performs an HTTP Post request to the specified url with the
     * specified parameters.
     * 
     * @param url The web address to post the request to
     * @param postParameters The parameters to send via the request
     * @return The result of the request
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static String executeHttpPost(String url, ArrayList postParameters) throws Exception {
        BufferedReader in = null;
        try {
            HttpClient client = getHttpClient();            
            HttpPost request = new HttpPost(url);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
            request.setEntity(formEntity);
            HttpResponse response = client.execute(request);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
                Log.w(CustomHttpClient.class.getName(), "Line:" + line + " + " + NL); 
            }
            in.close();

            String result = sb.toString();
            return result;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Performs an HTTP GET request to the specified url.
     * 
     * @param url The web address to post the request to
     * @return The result of the request
     * @throws Exception
     */
    public static String executeHttpGet(String url) throws Exception {
        BufferedReader in = null;
        
        try {
            HttpClient client = getHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(url));
            HttpResponse response = client.execute(request);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();

            String result = sb.toString();
            return result; 
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
     public static InputStream retrieveInputStreamFromHttpGet(String url) throws Exception {  
        InputStream is;
        HttpClient client = getHttpClient();
        HttpGet request = new HttpGet();
        try {
            request.setURI(new URI(url));
            HttpResponse response = client.execute(request);
            is = response.getEntity().getContent(); 
            return is;
        } catch(IOException e) {
            request.abort();
            
        }
        return null;
    }
     
     public static InputStream retrieveInputStreamFromHttpGetOrThrow(String url) throws IOException, URISyntaxException {  
         InputStream is;
         HttpClient client = getHttpClient();
         HttpGet request = new HttpGet();
         
         request.setURI(new URI(url));
         HttpResponse response = client.execute(request);
         is = response.getEntity().getContent(); 
         return is;
        
     }
     
     public static String executeHttpJsonPost(String url, String json) throws Exception {
    	 BufferedReader in = null;
    	 try { 
    		 HttpClient client = getHttpClient(); 
    		 HttpPost request = new HttpPost(url);
    		 request.setEntity(new StringEntity(json));
    		 request.setHeader("Accept", "application/json");
    		 request.setHeader("Content-Type", "application/json"); 
    		 HttpResponse response = client.execute(request);
    		 in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

             StringBuffer sb = new StringBuffer("");
             String line = "";
             String NL = System.getProperty("line.separator");
             while ((line = in.readLine()) != null) {
                 sb.append(line + NL);
                 Log.w(CustomHttpClient.class.getName(), "Line:" + line + " + " + NL); 
             }
             in.close();

             String result = sb.toString();
             return result;
         } finally {
             if (in != null) {
                 try {
                     in.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }
     }
}