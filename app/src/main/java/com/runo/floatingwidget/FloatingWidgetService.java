package com.runo.floatingwidget;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


public class FloatingWidgetService extends Service implements View.OnClickListener {

    private WindowManager mWindowManager;
    private View mFloatingWidgetView, widgetView;
    private LinearLayout expandView,expand_Layout;
    private ImageView expand_btn_1,expand_btn_2;
    private ImageView remove_image_view;
    private ImageView floating_widget_view;
    private Point szWindow = new Point();
    private View removeFloatingWidgetView;

    private int x_init_cord, y_init_cord, x_init_margin, y_init_margin;

    // Variable to check if the Floating widget view is on left side or in right side
    // initially we are displaying Floating widget view to Left side so set it to true
    private boolean isLeft = true;
    private boolean isExpand = false;

    public FloatingWidgetService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //init WindowManager
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        getWindowManagerDefaultDisplay();

        //Init LayoutInflater
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        //add all view
        addRemoveView(inflater);
        addFloatingWidgetView(inflater);
        addExpandView(inflater);

        //implement input function
        implementClickListeners();
        implementTouchListenerToFloatingWidgetView();
    }


    /*  Add Remove View to Window Manager  */
    private View addRemoveView(LayoutInflater inflater) {
        //Inflate the removing view layout we created
        removeFloatingWidgetView = inflater.inflate(R.layout.remove_floating_widget_layout, null);

        //Add the view to the window.
        WindowManager.LayoutParams paramRemove = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        paramRemove.gravity = Gravity.TOP | Gravity.LEFT;
        paramRemove.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        //removeFloatingWidgetView設為Gone會沒辦法渲染寬高，所以初始化先設為Invisible，以免資料讀取道寬高=0
        removeFloatingWidgetView.setVisibility(View.INVISIBLE);
        remove_image_view = (ImageView) removeFloatingWidgetView.findViewById(R.id.remove_img);

        //Add the view to the window
        mWindowManager.addView(removeFloatingWidgetView, paramRemove);


        return remove_image_view;
    }

    /*  Add Floating Widget View to Window Manager  */
    private void addFloatingWidgetView(LayoutInflater inflater) {
        //Inflate the floating view layout we created
        mFloatingWidgetView = inflater.inflate(R.layout.floating_widget_layout, null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        //Initially view will be added to top-left corner, you change x-y coordinates according to your need
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager.addView(mFloatingWidgetView, params);

        //find id of collapsed view layout
        widgetView = mFloatingWidgetView.findViewById(R.id.floating_widget_view);
        floating_widget_view = widgetView.findViewById(R.id.floating_widget_button);

    }

    private void addExpandView(LayoutInflater inflater) {

        expandView = (LinearLayout)inflater.inflate(R.layout.expanded_floating_widget_layout, null);
        expand_Layout = expandView.findViewById(R.id.expand_Layout);
        //擴展的按鈕
        expand_btn_1 = expandView.findViewById(R.id.expand_button_1);
        expand_btn_2 = expandView.findViewById(R.id.expand_button_2);


        //Add the view to the window.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        //Initially view will be added to top-left corner, you change x-y coordinates according to your need
        params.x = 0;
        params.y = 100;

        expandView.setOrientation(LinearLayout.HORIZONTAL);//could set the orientation programmatically
        expandView.setVisibility(View.GONE);
        //Add the view to the window
        mWindowManager.addView(expandView, params);



    }



    private void getWindowManagerDefaultDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
            mWindowManager.getDefaultDisplay().getSize(szWindow);
        else {
            int w = mWindowManager.getDefaultDisplay().getWidth();
            int h = mWindowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }
    }

    /*  Implement Touch Listener to Floating Widget Root View  */
    private void implementTouchListenerToFloatingWidgetView() {
        //Drag and move floating view using user's touch action.
        mFloatingWidgetView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {

            long time_start = 0, time_end = 0;

            boolean isLongClick = false;//variable to judge if user click long press
            boolean inBounded = false;//variable to judge if floating view is bounded to remove view
            int remove_img_width = 0, remove_img_height = 0;

            Handler handler_longClick = new Handler();
            Runnable runnable_longClick = new Runnable() {
                @Override
                public void run() {
                    //On Floating Widget Long Click

                    //Set isLongClick as true
                    isLongClick = true;

                    widgetView.setVisibility(View.VISIBLE);
                   // expandedView.setVisibility(View.GONE);
                    //Set remove widget view visibility to VISIBLE
                    removeFloatingWidgetView.setVisibility(View.VISIBLE);

                    onFloatingWidgetLongClick();
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //Get Floating widget view params
                WindowManager.LayoutParams param_floatingWidgetView = (WindowManager.LayoutParams) mFloatingWidgetView.getLayoutParams();

                //get the touch location coordinates
                int x_cord = (int) event.getRawX();
                int y_cord = (int) event.getRawY();

                int x_cord_Destination, y_cord_Destination;
                //update expand view
                UpdateExpandView(x_cord);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        time_start = System.currentTimeMillis();
                        // set expandedView Gone
                        handler_longClick.postDelayed(runnable_longClick, 800);

                        remove_img_width = remove_image_view.getLayoutParams().width;
                        remove_img_height = remove_image_view.getLayoutParams().height;

                        x_init_cord = x_cord;
                        y_init_cord = y_cord;

                        //remember the initial position.
                        x_init_margin = param_floatingWidgetView.x;
                        y_init_margin = param_floatingWidgetView.y;

                        return true;
                    case MotionEvent.ACTION_UP:
                        isLongClick = false;
                        removeFloatingWidgetView.setVisibility(View.GONE);
                        remove_image_view.getLayoutParams().height = remove_img_height;
                        remove_image_view.getLayoutParams().width = remove_img_width;
                        handler_longClick.removeCallbacks(runnable_longClick);

                        //If user drag and drop the floating widget view into remove view then stop the service
                        if (inBounded) {
                            stopSelf();
                            inBounded = false;
                            break;
                        }


                        //Get the difference between initial coordinate and current coordinate
                        int x_diff = x_cord - x_init_cord;
                        int y_diff = y_cord - y_init_cord;

                        //The check for x_diff <5 && y_diff< 5 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Math.abs(x_diff) < 5 && Math.abs(y_diff) < 5) {
                            time_end = System.currentTimeMillis();

                            //Also check the difference between start time and end time should be less than 300ms
                            if ((time_end - time_start) < 300)
                                onFloatingWidgetClick();

                        }

                        y_cord_Destination = y_init_margin + y_diff;

                        int barHeight = getStatusBarHeight();
                        if (y_cord_Destination < 0) {
                            y_cord_Destination = 0;
                        } else if (y_cord_Destination + (mFloatingWidgetView.getHeight() + barHeight) > szWindow.y) {
                            y_cord_Destination = szWindow.y - (mFloatingWidgetView.getHeight() + barHeight);
                        }

                        Log.d("LayoutA","y_cord_Destination : "+y_cord_Destination);
                        param_floatingWidgetView.y = y_cord_Destination;

                        inBounded = false;



                        return true;
                    case MotionEvent.ACTION_MOVE:

                        LeftCheck(x_cord);
                        int x_diff_move = x_cord - x_init_cord;
                        int y_diff_move = y_cord - y_init_cord;

                        x_cord_Destination = x_init_margin + x_diff_move;
                        y_cord_Destination = y_init_margin + y_diff_move;


                        //If user long click the floating view, update remove view
                        if (isLongClick) {
                            int x_bound_left = szWindow.x / 2 - (int) (remove_img_width * 1.5);
                            int x_bound_right = szWindow.x / 2 + (int) (remove_img_width * 1.5);
                            int y_bound_top = szWindow.y - (int) (remove_img_height * 1.5);


                            //If Floating view comes under Remove View update Window Manager
                            if ((x_cord >= x_bound_left && x_cord <= x_bound_right) && y_cord >= y_bound_top) {
                                inBounded = true;

                                int x_cord_remove = (int) ((szWindow.x - (remove_img_height * 1.5)) / 2);
                                int y_cord_remove = (int) (szWindow.y - ((remove_img_width * 1.5) + getStatusBarHeight()));

                                if (remove_image_view.getLayoutParams().height == remove_img_height) {
                                    remove_image_view.getLayoutParams().height = (int) (remove_img_height * 1.5);
                                    remove_image_view.getLayoutParams().width = (int) (remove_img_width * 1.5);

                                    WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeFloatingWidgetView.getLayoutParams();
                                    param_remove.x = x_cord_remove;
                                    param_remove.y = y_cord_remove;

                                    mWindowManager.updateViewLayout(removeFloatingWidgetView, param_remove);
                                }

                                //讓widget符合移除圈的大小
                                param_floatingWidgetView.x = x_cord_remove + (Math.abs(removeFloatingWidgetView.getWidth() - mFloatingWidgetView.getWidth())) / 2;
                                param_floatingWidgetView.y = y_cord_remove + (Math.abs(removeFloatingWidgetView.getHeight() - mFloatingWidgetView.getHeight())) / 2;

                                //Update the layout with new X & Y coordinate
                                mWindowManager.updateViewLayout(mFloatingWidgetView, param_floatingWidgetView);
                                break;
                            } else {

                                //If Floating window gets out of the Remove view update Remove view again
                                inBounded = false;
                                //當沒有在移除圈內的時候，校正移除圈的大小

                                remove_image_view.getLayoutParams().height = remove_img_height;
                                remove_image_view.getLayoutParams().width = remove_img_width;


                                WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeFloatingWidgetView.getLayoutParams();
                                mWindowManager.updateViewLayout(removeFloatingWidgetView, param_remove);


                            }



                        }


                        //clamp x_cord_destination
                        // out of bounds
                        if(x_cord_Destination<=0)
                            x_cord_Destination=0;
                        else if(x_cord_Destination>szWindow.x - mFloatingWidgetView.getWidth())
                            x_cord_Destination=szWindow.x- mFloatingWidgetView.getWidth();

                        param_floatingWidgetView.x = x_cord_Destination;
                        param_floatingWidgetView.y = y_cord_Destination;
                        //Update the layout with new X & Y coordinate

                        mWindowManager.updateViewLayout(mFloatingWidgetView, param_floatingWidgetView);

                        return true;
                }
                return false;
            }

        });
    }

    //按下的時候會跑出來的按鍵
    private void implementClickListeners() {
        mFloatingWidgetView.findViewById(R.id.floating_widget_close).setOnClickListener(this);

        // expand view button
        expandView.findViewById(R.id.expand_button_1).setOnClickListener(this);
        expandView.findViewById(R.id.expand_button_2).setOnClickListener(this);

    }

    //當按下這些按鈕會跑出的事件
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.floating_widget_close:
                //close the service and remove the from from the window
                stopSelf();
                break;


            case R.id.expand_button_1:
                Toast.makeText(this,
                        "Hello World",
                        Toast.LENGTH_SHORT).show();

                break;
            case R.id.expand_button_2:
                //open the activity and stop service
                Intent intent = new Intent(FloatingWidgetService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                //close the service and remove view from the view hierarchy
                stopSelf();
                break;

        }

    }

    /*  on Floating Widget Long Click, increase the size of remove view as it look like taking focus */
    private void onFloatingWidgetLongClick() {
        //Get remove Floating view params
        WindowManager.LayoutParams removeParams = (WindowManager.LayoutParams) removeFloatingWidgetView.getLayoutParams();

        //get x and y coordinates of remove view
        int x_cord = (szWindow.x - removeFloatingWidgetView.getWidth()) / 2;
        int y_cord = szWindow.y - (removeFloatingWidgetView.getHeight() + getStatusBarHeight());


        removeParams.x = x_cord;
        removeParams.y = y_cord;



        //Update Remove view params
        mWindowManager.updateViewLayout(removeFloatingWidgetView, removeParams);
    }





    private void LeftCheck(int x_cord_now)
    {

        if (x_cord_now <= szWindow.x / 2) {
            isLeft = true;
        } else {
            isLeft = false;
        }
    }

    /*  Get Bounce value if you want to make bounce effect to your Floating Widget */
    private double bounceValue(long step, long scale) {
        double value = scale * java.lang.Math.exp(-0.055 * step) * java.lang.Math.cos(0.08 * step);
        return value;
    }



    /*  return status bar height on basis of device display metrics  */
    private int getStatusBarHeight() {
        return (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
    }


    /*  Update Floating Widget view coordinates on Configuration change  */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        getWindowManagerDefaultDisplay();

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mFloatingWidgetView.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {


            if (layoutParams.y + (mFloatingWidgetView.getHeight() + getStatusBarHeight()) > szWindow.y) {
                layoutParams.y = szWindow.y - (mFloatingWidgetView.getHeight() + getStatusBarHeight());
                mWindowManager.updateViewLayout(mFloatingWidgetView, layoutParams);
            }


            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
            //    resetPosition(szWindow.x);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            if (layoutParams.x > szWindow.x) {
              //  resetPosition(szWindow.x);
            }

        }

    }

    /*  on Floating widget click show expanded view  */
    private void onFloatingWidgetClick() {

        isExpand = !isExpand;
        if(isExpand)
        {

            floating_widget_view.setImageResource(R.drawable.ic_launcher_foreground_black);
            expandView.setVisibility(View.VISIBLE);
        }else {

            floating_widget_view.setImageResource(R.drawable.ic_launcher_foreground);
            expandView.setVisibility(View.GONE);
        }
    }

    private void UpdateExpandView(int x_cord)
    {


        WindowManager.LayoutParams param_floatingWidgetView = (WindowManager.LayoutParams) mFloatingWidgetView.getLayoutParams();
        WindowManager.LayoutParams param_expandView = (WindowManager.LayoutParams) expandView.getLayoutParams();

        expand_Layout.getLayoutParams().height = mFloatingWidgetView.getHeight();
        expand_Layout.getLayoutParams().width = szWindow.x /2;

        int LeftBound = mFloatingWidgetView.getWidth();
        int rightBound = szWindow.x - mFloatingWidgetView.getWidth();


        if(x_cord < LeftBound){

            param_expandView.x = param_floatingWidgetView.x + widgetView.getWidth();
            param_expandView.y = param_floatingWidgetView.y;
            expand_Layout.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }else if (x_cord > rightBound){

            param_expandView.x = param_floatingWidgetView.x - szWindow.x / 2;
            param_expandView.y = param_floatingWidgetView.y;

            expand_Layout.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        }else
        {

            if(isLeft){

                param_expandView.x = param_floatingWidgetView.x + widgetView.getWidth();
                param_expandView.y = param_floatingWidgetView.y;
                expand_Layout.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            }else{

                param_expandView.x = param_floatingWidgetView.x - szWindow.x / 2;
                param_expandView.y = param_floatingWidgetView.y;

                expand_Layout.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            }
        }


        if(isExpand)
            expandView.setVisibility(View.VISIBLE);

        mWindowManager.updateViewLayout(expandView, param_expandView);



    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        /*  on destroy remove both view from window manager */

        if (mFloatingWidgetView != null)
            mWindowManager.removeView(mFloatingWidgetView);

        if (removeFloatingWidgetView != null)
            mWindowManager.removeView(removeFloatingWidgetView);
        if (expandView != null)
            mWindowManager.removeView(expandView);
    }


}