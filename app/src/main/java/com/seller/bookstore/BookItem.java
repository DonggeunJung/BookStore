package com.seller.bookstore;

import android.graphics.Bitmap;

/**
 * Created by JDG on 2018-06-08.
 * Book Item data storage class
 */

public class BookItem {

    protected String mTitle;
    protected String mImageUrl;
    protected Bitmap mBmpBookImage = null;

    // Init member variable
    BookItem(String title, String imageUrl) {
        mTitle = title;
        mImageUrl = imageUrl;

    }

}
