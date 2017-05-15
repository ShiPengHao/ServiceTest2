package com.yimeng.servicetestt;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.yimeng.servicetest.service.CountAIDL;
import com.yimeng.servicetest.service.OnChangeListenerAIDL;
import com.yimeng.servicetestt.utils.MyToast;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TYPE_INTENT = "TYPE_INTENT";
    public static final int TYPE_BINDER = 100;
    public static final int TYPE_MESSENGER = 101;
    public static final int TYPE_AIDL = 102;

    public static final int WHAT_COUNT = 100;
    public static final int WHAT_FRESH = 101;
    public static final int WHAT_REMOVE_CLIENT = 102;
    public static final int WHAT_ADD_CLIENT = 103;

    private TextView tv_bs_messenger;
    private TextView tv_bs_messenger_count;
    private TextView tv_bs_aidl;
    private TextView tv_bs_aidl_count;
    private Intent mAIDLIntent;
    private ServiceConnection mAIDLConn;
    private CountAIDL mAIDLService;
    private OnChangeListenerAIDL mChangeListenerAIDL;
    private Intent mMessengerIntent;
    private ServiceConnection mMessengerConn;
    private Messenger mMessengerService;
    private Messenger mMessengerClient;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        setListener();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountService();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 初始化view
     */
    private void initView() {
        tv_bs_messenger = (TextView) findViewById(R.id.tv_bs_messenger);
        tv_bs_messenger_count = (TextView) findViewById(R.id.tv_bs_messenger_count);
        tv_bs_aidl = (TextView) findViewById(R.id.tv_bs_aidl);
        tv_bs_aidl_count = (TextView) findViewById(R.id.tv_bs_aidl_count);
    }

    /**
     * 准备数据
     */
    private void initData() {
        mMessengerIntent = new Intent("com.yimeng.servicetest.service.CountIntentService").putExtra(TYPE_INTENT, TYPE_MESSENGER);
        mAIDLIntent = new Intent("com.yimeng.servicetest.service.CountIntentService").putExtra(TYPE_INTENT, TYPE_AIDL);
    }

    /**
     * 设置监听
     */
    private void setListener() {
        tv_bs_messenger.setOnClickListener(this);
        tv_bs_messenger_count.setOnClickListener(this);
        tv_bs_aidl.setOnClickListener(this);
        tv_bs_aidl_count.setOnClickListener(this);
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case WHAT_FRESH:// 服务维护的数据有更新
                        MyToast.showLog(MainActivity.this, String.format("新值是:%s", msg.getData().getInt("number")));
                        return true;
                }
                return false;
            }
        });
        mMessengerClient = new Messenger(mHandler);
        mChangeListenerAIDL = new OnChangeListenerAIDL.Stub() {

            @Override
            public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

            }

            @Override
            public void onChange(int number) throws RemoteException {
                MyToast.showLog(MainActivity.this, "onchange:" + number);
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.tv_bs_messenger:
                String str = tv_bs_messenger.getText().toString();
                if (getString(R.string.bind_service_messenger).equalsIgnoreCase(str)) {
                    bindServiceMessenger();
                } else if (getString(R.string.unbind_service_messenger).equalsIgnoreCase(str)) {
                    unbindServiceMessenger();
                }
                break;

            case R.id.tv_bs_messenger_count:
                CountMessenger();
                break;

            case R.id.tv_bs_aidl:
                str = tv_bs_aidl.getText().toString();
                if (getString(R.string.bind_service_aidl).equalsIgnoreCase(str)) {
                    bindServiceAIDL();
                } else if (getString(R.string.unbind_service_aidl).equalsIgnoreCase(str)) {
                    unbindServiceAIDL();
                }
                break;

            case R.id.tv_bs_aidl_count:
                countAIDL();
                break;
        }
    }

    /**
     * 计数+1
     */
    private void countAIDL() {
        if (mAIDLService == null) {
            MyToast.showLog(this, R.string.service_not_bound);
            return;
        }
        try {
            mAIDLService.add();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 以messenger的方式绑定CountService
     */
    private void bindServiceAIDL() {
        if (mAIDLConn == null) {
            mAIDLConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {
                    if (iBinder == null) {
                        unbindServiceAIDL();
                        return;
                    }
                    MyToast.showLog(MainActivity.this, "onServiceConnected");
                    mAIDLService = CountAIDL.Stub.asInterface(iBinder);
                    try {

                        iBinder.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                MyToast.showLog(MainActivity.this, "binderDied");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        unbindServiceAIDL();
                                    }
                                });
                            }
                        }, 0);
                        mAIDLService.addListener(mChangeListenerAIDL);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                }
            };
        }
        try {
            if (bindService(mAIDLIntent, mAIDLConn, BIND_AUTO_CREATE)) {
                tv_bs_aidl.setText(R.string.unbind_service_aidl);
                tv_bs_messenger.setEnabled(false);
            } else {
                MyToast.showLog(MainActivity.this, "bind failure");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MyToast.showLog(MainActivity.this, "bind failure");
        }
    }

    /**
     * 解绑messenger service，重置messenger引用和连接对象
     */
    private void unbindServiceAIDL() {
        if (mAIDLService != null && mAIDLService.asBinder().pingBinder()) {
            try {
                mAIDLService.removeListener(mChangeListenerAIDL);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mAIDLService = null;
        if (mAIDLConn != null) {
            unbindService(mAIDLConn);
            mAIDLConn = null;
        }
        tv_bs_aidl.setText(R.string.bind_service_aidl);
        tv_bs_messenger.setEnabled(true);
    }

    /**
     * 计数+1
     */
    private void CountMessenger() {
        if (mMessengerService == null) {
            MyToast.showLog(this, R.string.service_not_bound);
            return;
        }
        Message message = Message.obtain();
        message.what = WHAT_COUNT;
        try {
            mMessengerService.send(message);
        } catch (RemoteException e) {
            MyToast.showLog(this, "RemoteException");
            e.printStackTrace();
        }
    }

    /**
     * 以messenger的方式绑定CountService
     */
    private void bindServiceMessenger() {
        if (mMessengerConn == null) {
            mMessengerConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    if (iBinder == null) {
                        unbindServiceMessenger();
                        return;
                    }
                    MyToast.showLog(MainActivity.this, "onServiceConnected");
                    mMessengerService = new Messenger(iBinder);
                    try {
                        iBinder.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                MyToast.showLog(MainActivity.this, "binderDied");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        unbindServiceMessenger();
                                    }
                                });
                            }
                        }, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Message message = Message.obtain();
                    message.what = WHAT_ADD_CLIENT;
                    message.replyTo = mMessengerClient;
                    try {
                        mMessengerService.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                }
            };
        }
        try {
            if (bindService(mMessengerIntent, mMessengerConn, BIND_AUTO_CREATE)) {
                tv_bs_messenger.setText(R.string.unbind_service_messenger);
                tv_bs_aidl.setEnabled(false);
            } else {
                MyToast.showLog(MainActivity.this, "bind failure");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MyToast.showLog(MainActivity.this, "bind failure");
        }
    }

    /**
     * 解绑messenger service，重置messenger引用和连接对象
     */
    private void unbindServiceMessenger() {
        if (mMessengerService != null && mMessengerService.getBinder().pingBinder()) {
            Message message = Message.obtain();
            message.what = WHAT_REMOVE_CLIENT;
            message.replyTo = mMessengerClient;
            try {
                mMessengerService.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mMessengerService = null;
        if (mMessengerConn != null) {
            unbindService(mMessengerConn);
            mMessengerConn = null;
        }
        tv_bs_messenger.setText(R.string.bind_service_messenger);
        tv_bs_aidl.setEnabled(true);
    }


    /**
     * 停止普通service
     */
    private void stopCountService() {
        unbindServiceMessenger();
        unbindServiceAIDL();
    }
}
