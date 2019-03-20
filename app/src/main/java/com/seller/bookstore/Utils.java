package com.seller.bookstore;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by JDG on 2018-06-08.
 */

public class Utils {
    public static final String TAG = "Utils";
    public static App mApp = null;

    // load image file and change Bitmap
    public static Bitmap loadImage(String filePath, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = null;
        // load image file
        try {
            options.inSampleSize = sampleSize;
            bitmap = BitmapFactory.decodeFile(filePath, options);

        }
        // if image size is too big, reduce size and try again
        catch( java.lang.OutOfMemoryError e) {
            bitmap = loadImage(filePath, sampleSize * 2);
        }
        return bitmap;
    }

    // get file name from file path
    public static String filterFileName(String filePath) {
        int pos = -1;
        if( (pos = filePath.lastIndexOf("/")) >= 0 ) {
            filePath = filePath.substring(pos+1);
        }
        return filePath;
    }

}
