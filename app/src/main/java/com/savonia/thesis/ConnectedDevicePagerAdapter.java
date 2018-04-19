package com.savonia.thesis;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

public class ConnectedDevicePagerAdapter extends FragmentPagerAdapter {

    private Context mContext;
    private String servicesFragmentTag;
    private String temperatureFragmentTag;
    private String gasFragmentTag;

    public ConnectedDevicePagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.mContext = context;
    }

    // This determines the fragment for each tab
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return ServicesFragment.newInstance("Services");
        } else if (position == 1){
            return TemperatureFragment.newInstance("Temperature");
        } else {
            return GasFragment.newInstance("Gas");
        }

    }

    // saving the returned Fragment from
    // super.instantiateItem() into an appropriate reference depending
    // on the ViewPager position.
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
        // save the appropriate reference depending on position
        if(position == 0) {
            servicesFragmentTag = createdFragment.getTag();
        } else if(position == 1) {
            temperatureFragmentTag = createdFragment.getTag();
        } else if(position == 2) {
            gasFragmentTag = createdFragment.getTag();
        }
        return createdFragment;
    }

    // This determines the number of tabs
    @Override
    public int getCount() {
        return 3;
    }

    public String getFragmentTag(int position) {
        switch(position) {
            case 0: return servicesFragmentTag;
            case 1: return temperatureFragmentTag;
            case 2: return  gasFragmentTag;
            default: return null;
        }
    }
}
