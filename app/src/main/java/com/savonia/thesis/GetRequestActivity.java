package com.savonia.thesis;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.savonia.thesis.viewmodels.SaMiViewModel;
import com.savonia.thesis.webclient.measuremetsmodels.DataModel;

import java.text.SimpleDateFormat;
import java.util.List;

public class GetRequestActivity extends AppCompatActivity {

    private static final String TAG = GetRequestActivity.class.getSimpleName();
    private Toolbar toolBar;

    // used for tabs and viewPager
    private NonSwipeableViewPager viewPager;
    private GetRequestPagerAdapter pagerAdapter;
    private TabLayout tabLayout;

    // replace graph icons with material ones
    private int[] imageResId = {
            R.drawable.services, R.drawable.chart
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_response);

        toolBar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolBar);

        // Setting up the tabs and viewPager
        viewPager = (NonSwipeableViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(1);

        // Create an adapter that knows which fragment should be shown on each page
        pagerAdapter = new GetRequestPagerAdapter(GetRequestActivity.this, getSupportFragmentManager());

        // Set the adapter onto the view pager
        viewPager.setAdapter(pagerAdapter);

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        try {
            for (int i = 0; i < imageResId.length; i++) {
                tabLayout.getTabAt(i).setIcon(imageResId[i]);
            }
        } catch(NullPointerException ex) {
            ex.printStackTrace();
        }
    }

}
