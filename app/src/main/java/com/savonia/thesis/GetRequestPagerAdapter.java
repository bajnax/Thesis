package com.savonia.thesis;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class GetRequestPagerAdapter extends FragmentPagerAdapter {

    private Context mContext;

    public GetRequestPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return GetRequestBuilder.newInstance("GetRequestBuilder");
        } else {
            return GetResponse.newInstance("GetResponse");
        }
    }

    @Override
    public int getCount() {
        return 2;
    }
}
