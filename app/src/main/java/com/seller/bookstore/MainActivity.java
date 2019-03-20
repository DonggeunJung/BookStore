package com.seller.bookstore;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {
    ListView mListBook;
    RelativeLayout mLayoutSendWatch;
    ArrayList<BookItem> mArBook = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayoutSendWatch = (RelativeLayout) findViewById(R.id.layout_send_watch);

        initListView();

        // Read Json Data and make array
        reqBookDataList();

        //reqDownloadImageFile();
    }

    public void getBookData_Array(String strJson) {
        final String TITLE = "title";
        final String IMAGE_URL = "imageURL";

        try {
            // parsing JSON code and make JSONArray object
            JSONArray jAr = new JSONArray(strJson);
            int count = jAr.length();
            for(int i=0; i < jAr.length(); i++) {
                JSONObject jObj = jAr.getJSONObject(i);
                String strItem = jObj.toString();
                String title = "", imageURL = "";
                if( strItem.indexOf(TITLE) > 0 )
                    title = jObj.getString(TITLE);
                if( strItem.indexOf(IMAGE_URL) > 0 )
                    imageURL = jObj.getString(IMAGE_URL);

                BookItem bi = new BookItem(title, imageURL);
                mArBook.add(bi);
            }
        } catch (JSONException e) {
            Log.d("tag", "Parse Error");
        }
    }

    protected void initListView() {
        // make  Book ArrayList Object
        mArBook = new ArrayList<BookItem>();
        // make Adapter object & set to ListView
        BookItemAdaptor bookListAdapter = new BookItemAdaptor(this, R.layout.book_list_item, mArBook);
        mListBook = (ListView)findViewById(R.id.listBooks);
        mListBook.setAdapter(bookListAdapter);
    }

    protected void reqDownloadImageFile() {
        for(int i=0; i < mArBook.size(); i++) {
            BookItem bi = mArBook.get(i);

            // loading image from local storage
            Bitmap bmp = loadThumbnamlFromLocal(bi.mImageUrl);

            // if there is Image file in local storage
            if( bmp != null ) {
                bi.mBmpBookImage = bmp;
            }
            // no image in local storage, download from server
            else {
                reqDownloadImageFile(bi.mImageUrl, Integer.toString(i), this);
            }
        }

        BookItemAdaptor bookListAdapter = (BookItemAdaptor) mListBook.getAdapter();
        bookListAdapter.notifyDataSetChanged();
    }

    // file download completed event
    @Override
    public void onDownloadCompleted(String fileId, String filePath, Bitmap bmp) {
        Log.d("tag", "onDownloadCompleted()-" + fileId + " / " + filePath);
        int index = Integer.parseInt(fileId);
        BookItem bi = mArBook.get(index);
        bi.mBmpBookImage = bmp;

        int first = mListBook.getFirstVisiblePosition();
        int last = mListBook.getLastVisiblePosition();
        if( index >= first && index <= last) {
            redrawListView();
        }
    }

    // Read Json Data and make array
    public void reqBookDataList() {

        String addr = "http://de-coding-test.s3.amazonaws.com/books.json";
        // Send url address & request data
        new HttpReqTask().execute(addr);
    }

    @Override
    protected void onRecv_BookListJson(String strJson) {
        getBookData_Array( strJson );
        mLayoutSendWatch.setVisibility(View.INVISIBLE);

        redrawListView();
        reqDownloadImageFile();
    }

    protected void redrawListView() {
        BookItemAdaptor bookListAdapter = (BookItemAdaptor) mListBook.getAdapter();
        bookListAdapter.notifyDataSetChanged();
    }

}
