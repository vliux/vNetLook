package org.vliux.netlook;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.vliux.netlook.biz.UserMobileDataAction;
import org.vliux.netlook.db.DbManager;
import org.vliux.netlook.model.AppNetUse;
import org.vliux.netlook.model.TotalNetUse;
import org.vliux.netlook.util.NetUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private ListView mNetUseListView;
    private ImageButton mRefreshBtn;
    private TextView mSummaryTextView;
    private ToggleButton mMobileDataSwitch;

    SimpleAdapter mAdapter;
    List<Map<String, Object>> mDataSource = new ArrayList<Map<String, Object>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNetUseListView = (ListView)findViewById(R.id.main_netuse_listview);
        mRefreshBtn = (ImageButton)findViewById(R.id.main_refresh);
        mRefreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new LoadNetUseAsyncTask(new DbManager(MainActivity.this), getPackageManager(), mHandler).execute();
            }
        });
        mSummaryTextView = (TextView)findViewById(R.id.main_summary);
        mMobileDataSwitch = (ToggleButton)findViewById(R.id.main_3g_switch);

        UserMobileDataAction umda = new UserMobileDataAction(this);
        boolean isMonitoring = umda.getMonitoring();
        umda.close();
        mMobileDataSwitch.setChecked(isMonitoring);
        mMobileDataSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isClicked = ((ToggleButton)view).isChecked();
                NetUtil.setMobileDataEnabled(MainActivity.this, isClicked);
                UserMobileDataAction umda = null;
                try{
                    umda = new UserMobileDataAction(MainActivity.this);
                    umda.setMonitoring(isClicked);
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    if(null != umda)
                        umda.close();
                }

                if(isClicked){
                    Toast.makeText(MainActivity.this, getString(R.string.start_monitor_3g), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this, getString(R.string.stop_monitor_3g), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadNetUseAsyncTask(new DbManager(this), getPackageManager(), mHandler).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void updateDataSource(List<AppNetUse> appUses){
        if(null == appUses){
            return;
        }else{
            mDataSource.clear();
            for(AppNetUse au : appUses){
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("name", au.getmPackageName());
                long rx = au.getmRxBytes();
                long tx = au.getmTxBytes();
                long total = au.getTotalBytes();
                map.put("use", String.format(Locale.US, "%.2fk", (double)total/1024));

                map.put("icon", au.getmIcon());
                mDataSource.add(map);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case LoadNetUseAsyncTask.MSG_COMPLETED:
                    TotalNetUse totalUse = (TotalNetUse)msg.obj;
                    mAdapter = new SimpleAdapter(MainActivity.this,
                            mDataSource,
                            R.layout.item_netuse,
                            new String[]{"icon", "name", "use"},
                            new int[]{R.id.item_netuse_icon, R.id.item_netuse_name, R.id.item_netuse_use}
                    );
                    mAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                        @Override
                        public boolean setViewValue(View view, Object o, String s) {
                            if(view instanceof ImageView && o instanceof Drawable){
                                ImageView iv = (ImageView)view;
                                Drawable icon = (Drawable)o;
                                iv.setImageDrawable(icon);
                                return true;
                            }else{
                                return false;
                            }
                        }
                    });
                    mNetUseListView.setAdapter(mAdapter);
                    updateDataSource(totalUse.getmAppNetUses());
                    String totalMsg = String.format(getString(R.string.netuse_summary_total), ((double)totalUse.getBytes()/1024));
                    String mobileMsg = String.format(getString(R.string.netuse_summary_mobile), ((double)totalUse.getMobileBytes())/1024);

                    mSummaryTextView.setText(totalMsg + "\n" + mobileMsg);
                    break;
            }
        }


    };
    
}
