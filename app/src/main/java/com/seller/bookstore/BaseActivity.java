package com.seller.bookstore;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.AdapterView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by JDG on 2018-06-08.
 */

public class BaseActivity extends AppCompatActivity
        implements FileDownloader.EventListener {

    protected App mApp = null;          // Application Class instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init member variable
        initVariable();
    }

    // init member variable
    public void initVariable() {
        // get Application class instance
        mApp = (App) getApplication();
    }

    // download image file
    protected void reqDownloadImageFile(String imageId1, String eventId, FileDownloader.EventListener listener) {
        // if there is no file, return
        if( imageId1 == null || imageId1.length() < 1 )
            return;
        // server image url address
        String imageUrl = imageId1;

        // get file name from url address
        String fileName = Utils.filterFileName(imageUrl);
        // request file download
        mApp.mFileDownloader.reqDownloadFile(imageUrl, fileName, eventId, listener);
    }

    // file download completed event
    @Override
    public void onDownloadCompleted(String fileId, String filePath, Bitmap bmp) {
    }

    // file download failed event
    @Override
    public void onDownloadFailed(String fileId) {
        Log.d("tag", "onDownloadFailed() - " + fileId);
    }

    /*// return contents of Text File in assets Folder
    public String ReadTextAssets(String strFileName) {
        String text = null;
        try {
            // Get InputStream of file
            InputStream is = getAssets().open(strFileName);
            // make byte array
            int size = is.available();
            byte[] buffer = new byte[size];
            // Read data from InputStream
            is.read(buffer);
            // Close InputStream
            is.close();
            // Change InputStream to String type
            text = new String(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // return contents of Text File
        return text;
    }*/

    // loading image from local storage
    protected Bitmap loadThumbnamlFromLocal(String imageId) {
        // get file name from url address
        imageId = Utils.filterFileName(imageId);

        // add extension to file name
        if( imageId.indexOf(".") < 0 )
            imageId += ".jpg";
        // get storage path
        String filePath = this.getFilesDir() + "/" + imageId;

        // Open file
        File file = new File( filePath );
        // if image file not exist, return
        if( file.exists() == false ) {
            return null;
        }
        // load image file and change to Bitmap
        Bitmap bmp = Utils.loadImage(filePath, 1);
        return bmp;
    }

    // return result of HTTP request
    public String getHttpConnResult(String strUrl) {
        String line, result = new String();

        try {
            // make Http client
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection)
                    url.openConnection();
            // set connect information
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // start connect
            conn.connect();

            // get data
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));

            while((line = reader.readLine()) != null) {
                result += line + '\n';
                if( result.length() > 1000000 ) break;
            }
            // close connection
            reader.close();
            conn.disconnect();
        }
        catch(Exception e) {
            Log.d("tag", "HttpURLConnection error");
        }
        return result;
    }

    // define thread class
    protected class HttpReqTask extends AsyncTask<String,String,String> {
        @Override // Running thread
        protected String doInBackground(String... arg) {
            String response = "";
            // request data to server
            if( arg.length == 1 ) {
                return (String)getHttpConnResult(arg[0]);
            }
            return response;
        }

        // after thread completed
        protected void onPostExecute(String result) {
            onRecv_BookListJson(result);
        }
    }

    protected void onRecv_BookListJson(String strJson) {

    }

}
