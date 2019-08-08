package com.transcend.otg.action.loader;

import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;

public class NullLoader extends AsyncTaskLoader<Boolean> {

    public NullLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean loadInBackground() {
        return true;
    }
}
