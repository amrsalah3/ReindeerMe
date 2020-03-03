package com.amr.mineapps.reindeerme;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;


import androidx.viewpager.widget.PagerAdapter;



public class ViewPagerAdapter extends PagerAdapter {

    private Context mContext;
    private String pageTitle;
    private int pageId;
    public int chatNum = 0;
    public int reqNum = 0;

    public ViewPagerAdapter(Context context){
        mContext = context;
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        switch (position){
            case 0:
                pageId = R.id.page_one;
                break;
            case 1:
                pageId = R.id.page_two;
                break;
        }
        return container.findViewById(pageId);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0:
                pageTitle = "CHATS"+"("+chatNum+")";
                break;
            case 1:
                pageTitle = "REQUESTS"+"("+reqNum+")";
                break;
        }
        return pageTitle;
    }
}
