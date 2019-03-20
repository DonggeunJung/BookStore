/**
 * Created by ART on 2017-06-14.
 * FileDownloader : Download image file class
 *                  URL 경로와 파일명, 요청ID를 reqDownloadFile() 함수에 전달하면
 *                  이미지 파일을 다운로드해서 앱데이터 폴더에 저장한다.
 *                  다운로드가 완료되면 이벤트 리스너에 Bitmap 이미지를 전달한다.
 *                  여러개의 파일을 지정하면 차례로 순차적으로 다운로드를 수행한다.
 *
 *  필요한 UsesPermission :
 *                  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 *                  <uses-permission android:name="android.permission.INTERNET" />
 *                  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
 *                  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 *
 * Made by : Jung-Donggeun (Dennis)
 */
package com.seller.bookstore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FileDownloader {
    public Context mContext;
    public boolean mIsDownloading = false;              // 다운로드중 여부
    public ArrayList<DownInfo> mArDown = new ArrayList<DownInfo>();

    // 파일 다운로드 요청 정보
    class DownInfo {
        public String url = null;                    // 서버 URL 주소
        public String fileName = null;                // 파일명
        public String fileId = null;                  // 파일ID
        public boolean isDownloading = false;
        public Bitmap bmpCopy = null;                   // 이전 항목과 일치하는 이미지라면 복사

        public DownInfo(String strUrl, String fileName, String fileId) {
            this.url = strUrl;
            this.fileName = fileName;
            this.fileId = fileId;
        }
    }

    // 생성자 함수
    public FileDownloader(Context context) {
        mContext = context;
    }

    // 파일 다운로드 완료 후에 처리하는 함수
    public void onDownloadSucceed(String result, String fileId, String filePath, Bitmap bmp) {
        // 다운요청 배열에서 ID로 위치를 구한다
        int index = getIndexDownArray(fileId);
        if( index >= 0 && index < mArDown.size() )
            // 다운 완료된 항목을 배열에서 삭제
            mArDown.remove(index);

        // 이벤트 리스너가 존재하지 않으면 함수 탈출
        if( mEventListener != null ) {
            // 파일 다운로드 성공했을때
            if (result.length() > 0) {
                mEventListener.onDownloadCompleted(fileId, filePath, bmp);
            }
            // 파일 다운로드 실패했을때
            else {
                mEventListener.onDownloadFailed(filePath);
            }
        }

        // 잠시후에 다음 파일 다운로드 시작
        startDownloadNext_Delay();
    }

    // 다음 파일 다운로드 시작
    public void startDownloadNextFile() {
        // 현재 다운로드 중이라면 함수 탈출
        if( mIsDownloading )
            return;
        // 모든 다운로드가 완료되었다면 함수 탈출
        if( mArDown.size() < 1 )
            return;
        for( DownInfo di : mArDown ) {
            if( di.isDownloading )
                continue;

            // 이미지가 이미 존재한다면 다운로드하지 않는다
            if( di.bmpCopy != null ) {
                String strFileName = di.fileName;
                // 파일명에 확장자명 추가
                if( strFileName.indexOf(".") < 0 )
                    strFileName += ".jpg";
                // 파일이 저장되는 경로를 구한다
                String filePath = mContext.getFilesDir() + "/" + strFileName;
                // 파일 다운로드 완료 후에 처리하는 함수
                onDownloadSucceed("True", di.fileId, filePath, di.bmpCopy);
                return;
            }

            di.isDownloading = true;
            // 파일 다운로드 시작
            startDownloadFile(di.url, di.fileName, di.fileId);
            return;
        }
    }

    // 다운요청 배열에서 ID로 위치를 구한다
    public int getIndexDownArray(String fileId) {
        for(int i=0; i < mArDown.size(); i ++) {
            DownInfo di = mArDown.get(i);
            if( fileId.equals(di.fileId) )
                return i;
        }
        return -1;
    }

    // 파일 다운로드 요청 추가
    public void reqDownloadFile(String strUrl, String fileName, String fileId, EventListener listener) {
        setListener(listener);
        reqDownloadFile(strUrl, fileName, fileId);
    }

    // 파일 다운로드 요청 추가
    public void reqDownloadFile(String strUrl, String fileName, String fileId) {
        DownInfo di = new DownInfo(strUrl, fileName, fileId);
        mArDown.add(di);
        // 잠시후에 다음 파일 다운로드 시작
        startDownloadNext_Delay();
    }

    // 잠시후에 다음 파일 다운로드 시작
    public void startDownloadNext_Delay() {
        // 타이머 시작
        mTimer_startDownloadNext.removeMessages(0);
        mTimer_startDownloadNext.sendEmptyMessageDelayed(0, 30);
        //mTimer_startDownloadNext.sendEmptyMessageDelayed(0, 1000);
    }

    // 다음 파일 다운로드 시작 타이머
    Handler mTimer_startDownloadNext = new Handler() {
        public void handleMessage(Message msg) {
            // 다음 파일 다운로드 시작
            startDownloadNextFile();
        }
    };

    // 파일 다운로드 시작
    public void startDownloadFile(String strUrl, String fileName, String fileId) {
        // 현재 다운로드 중이라면 함수 탈출
        if( mIsDownloading )
            return;
        // 서버에서 이미지 다운로드를 수행하는 스레드
        new HttpReqTask().execute(strUrl, fileName, fileId);
    }

    // 파일 다운로드 스레드
    private class HttpReqTask extends AsyncTask<String,String,String> {
        Bitmap mBmp = null;
        String mFilePath = null;
        String mFileId = null;
        String mFileName = null;

        @Override
        protected String doInBackground(String... arg) {
            if( arg.length < 3 )
                return "";
            // 다운로드중으로 상태 변경
            mIsDownloading = true;
            mFileId = arg[2];           // 파일 ID

            boolean result = true;
            mFileName = arg[1];
            String strFileName = arg[1];            // 파일명

            // 파일 경로에서 파일명만 추출
            strFileName = Utils.filterFileName(strFileName);
            // 파일명에 확장자명 추가
            if( strFileName.indexOf(".") < 0 )
                strFileName += ".jpg";
            // 파일이 저장되는 경로를 구한다
            mFilePath = mContext.getFilesDir() + "/" + strFileName;
            //Log.d("tag", "HttpReqTask-FilePath " + mFilePath);
            // 입력 파일 Open
            File file = new File( mFilePath );
            // 파일이 존재하지 않으면 다운로드 시작
            if( file.exists() == false ) {
                // 서버에서 다운로드 한 데이터를 파일로 저장
                result = downloadFile(arg[0], strFileName);
            }

            // 파일이 이미 존재하거나 다운로드를 완료했을때
            if (result) {
                // 파일을 로딩해서 Bitmap 객체로 생성
                mBmp = BitmapFactory.decodeFile( mFilePath );
                return "True";
            }
            return "";
        }

        // 스레드의 업무가 끝났을 때 결과를 처리하는 함수
        protected void onPostExecute(String result) {
            // 다운로드 종료로 상태 변경
            mIsDownloading = false;
            // 파일 다운로드 완료 후에 처리하는 함수
            onDownloadSucceed(result, mFileId, mFilePath, mBmp);

            // 다운로드 요청 리스트에서 동일한 파일은 복사한다
            copyBitmapSameReq(mFileId, mFileName, mBmp);
            /*// 다운요청 배열에서 ID로 위치를 구한다
            int index = getIndexDownArray(mFileId);
            if( index >= 0 && index < mArDown.size() )
                // 다운 완료된 항목을 배열에서 삭제
                mArDown.remove(index);

            // 이벤트 리스너가 존재하지 않으면 함수 탈출
            if( mEventListener != null ) {
                // 파일 다운로드 성공했을때
                if (result.length() > 0) {
                    mEventListener.onDownloadCompleted(mFileId, mFilePath, mBmp);
                }
                // 파일 다운로드 실패했을때
                else {
                    mEventListener.onDownloadFailed(mFileId);
                }
            }

            // 잠시후에 다음 파일 다운로드 시작
            startDownloadNext_Delay();*/
        }
    }

    ///*** 리스너 정의 Begin ***///

    // 이벤트 리스너 클래스 정의
    public interface EventListener {
        // 파일 다운로드 완료 이벤트 함수
        void onDownloadCompleted(String fileId, String filePath, Bitmap bmp);

        // 파일 다운로드 실패 이벤트 함수
        void onDownloadFailed(String fileId);
    }

    // 이벤트 리스너를 멤버 변수로 선언
    private EventListener mEventListener = null;

    // 이벤트 리스너 객체를 받아서 멤버변수에 저장하는 함수
    public void setListener(EventListener listener){
        mEventListener = listener;
    }

    ///*** 리스너 정의 End ***///

    // 서버에서 다운로드 한 데이터를 파일로 저장
    boolean downloadFile(String strUrl, String fileName) {
        try {
            URL url = new URL(strUrl);
            // 서버와 접속하는 클라이언트 객체 생성
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            // 입력 스트림을 구한다
            InputStream is = conn.getInputStream();
            // 파일 저장 스트림을 생성
            FileOutputStream fos = mContext.openFileOutput(fileName, 0);

            // 입력 스트림을 파일로 저장
            byte[] buf = new byte[1024];
            int count;
            while( (count = is.read(buf)) > 0 ) {
                fos.write(buf, 0, count);
            }
            // 접속 해제
            conn.disconnect();
            // 파일을 닫는다
            fos.close();
        } catch (Exception e) {
            Log.d("tag", "Image download error.");
            return false;
        }
        return true;
    }

    // 다운로드 요청 리스트에서 동일한 파일은 복사한다
    public void copyBitmapSameReq(String fileId, String fileName, Bitmap bmp) {
        for(DownInfo di:mArDown) {
            // 파일 아이디가 같다면 무시
            if( di.fileId.equals(fileId) )
                continue;
            // 이미 Bitmap 이미지가 존재한다면 무시
            if( di.bmpCopy != null )
                continue;
            // 파일명이 일치한다면 Bitmap 을 복사한다
            String itemFileName = di.fileName;
            if( itemFileName.equals(fileName) ) {
                di.bmpCopy = bmp;
                /*if (bmp.isRecycled())
                    di.bmpCopy = bmp;
                else
                    di.bmpCopy = bmp.copy(bmp.getConfig(), true);*/
            }
        }
    }

}
