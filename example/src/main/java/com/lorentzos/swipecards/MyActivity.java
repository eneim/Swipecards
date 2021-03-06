package com.lorentzos.swipecards;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import java.util.ArrayList;


public class MyActivity extends Activity {

    @InjectView(R.id.frame)
    SwipeFlingAdapterView mFlingView;
    private ArrayList<String> al;
    private ArrayAdapter<String> arrayAdapter;
    private int i;

    static void makeToast(Context ctx, String s) {
        Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        ButterKnife.inject(this);

        al = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            al.add((i + 1) + "");
        }
        i = al.size();

        arrayAdapter = new ArrayAdapter<>(this, R.layout.item, R.id.helloText, al);

        mFlingView.setAdapter(arrayAdapter);
        mFlingView.setFlingListener(new SwipeFlingAdapterView.OnSwipeListener() {
            @Override
            public void onTopExited() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                Log.d("LIST", "removed object!");
                if (al.size() > 0) {
                    al.remove(0);
                    arrayAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onExitToLeft(View dataObject) {
                //Do something on the left!
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject
                makeToast(MyActivity.this, "Left!");
            }

            @Override
            public void onExitToRight(View dataObject) {
                makeToast(MyActivity.this, "Right!");
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                // Ask for more data here
//                al.add((i + 1) + "");
//                arrayAdapter.notifyDataSetChanged();
                Log.d("LIST", "size: " + itemsInAdapter);
                i++;
            }

            @Override
            public void onFlingTopView(float offset) {
                View view = mFlingView.getTopView();
                if (view != null) {
                    view.findViewById(R.id.item_swipe_right_indicator).setAlpha(offset < 0 ? -offset : 0);

                    view.findViewById(R.id.item_swipe_left_indicator).setAlpha(offset > 0 ? offset : 0);
                }
            }
        });

        // Optionally add an OnItemClickListener
        mFlingView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                makeToast(MyActivity.this, "Clicked!");
            }
        });

    }

    @OnClick(R.id.right)
    public void right() {
        /**
         * Trigger the right event manually.
         */
        mFlingView.getTopCardListener().swipeToRight();
        mFlingView.getSelectedView();
    }

    @OnClick(R.id.left)
    public void left() {
        mFlingView.getTopCardListener().swipeToLeft();
    }


}
