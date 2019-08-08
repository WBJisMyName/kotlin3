package com.transcend.otg.external;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.transcend.otg.BuildConfig;

import java.io.File;

public abstract class AbstractExternalStorage {
    private Context mContext;

    public AbstractExternalStorage(Context context) {
        mContext = context;
    }

    public boolean isWritePermissionNotGranted() {
        return false;
    }

    public boolean isWritePermissionRequired(String... path) {
        return false;
    }

    public void handleWriteOperationFailed() {

    }

    public Uri getSDFileUri(String path) {
        Uri uri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(path));
        return uri;
    }

    protected Context getContext() {
        return mContext;
    }
}
