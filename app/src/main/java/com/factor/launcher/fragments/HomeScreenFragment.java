package com.factor.launcher.fragments;

import static com.factor.launcher.util.Constants.REQUEST_CREATE_WIDGET;
import static com.factor.launcher.util.Constants.REQUEST_PICK_WIDGET;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.ViewDataBinding;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.factor.chips.chipslayoutmanager.ChipsLayoutManager;
import com.factor.indicator_fast_scroll.FastScrollItemIndicator;
import com.factor.launcher.FactorApplication;
import com.factor.launcher.R;
import com.factor.launcher.activities.SettingsActivity;
import com.factor.launcher.adapters.FactorsAdapter;
import com.factor.launcher.database.FactorsDatabase;
import com.factor.launcher.databinding.FactorLargeBinding;
import com.factor.launcher.databinding.FactorMediumBinding;
import com.factor.launcher.databinding.FactorSmallBinding;
import com.factor.launcher.databinding.FragmentHomeScreenBinding;
import com.factor.launcher.models.AppSettings;
import com.factor.launcher.models.Factor;
import com.factor.launcher.models.UserApp;
import com.factor.launcher.receivers.AppActionReceiver;
import com.factor.launcher.receivers.NotificationBroadcastReceiver;
import com.factor.launcher.receivers.PackageActionsReceiver;
import com.factor.launcher.services.FactorNotificationListener;
import com.factor.launcher.ui.FixedLinearLayoutManager;
import com.factor.launcher.util.ChineseHelper;
import com.factor.launcher.util.Constants;
import com.factor.launcher.util.OnSystemActionsCallBack;
import com.factor.launcher.util.Util;
import com.factor.launcher.util.WidgetActivityResultContract;
import com.factor.launcher.view_models.AppListManager;
import com.factor.launcher.view_models.AppSettingsManager;
import com.google.android.renderscript.Toolkit;

import java.util.ArrayList;

import eightbitlab.com.blurview.RenderScriptBlur;


public class HomeScreenFragment extends Fragment implements OnSystemActionsCallBack, LifecycleOwner
{
    private boolean isLiveWallpaper = false;

    private FragmentHomeScreenBinding binding;

    private WallpaperManager wm;

    private AppListManager appListManager;

    private AppActionReceiver appActionReceiver;

    private PackageActionsReceiver packageActionsReceiver;

    private NotificationBroadcastReceiver notificationBroadcastReceiver;

    private AppSettings appSettings;

    private RenderScriptBlur blurAlg;

     private Intent notificationListenerIntent;

    private boolean isWidgetExpanded = false;

    private ObjectAnimator animatorExpand;

    private ObjectAnimator animatorCollapse;

    private final WidgetActivityResultContract widgetActivityResultContract = new WidgetActivityResultContract();

    private ActivityResultLauncher<Intent> widgetResultLauncher;

    private int appWidgetId = -1;

    public HomeScreenFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        wm = WallpaperManager.getInstance(getContext());
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentHomeScreenBinding.inflate(getLayoutInflater());
        binding.setLifecycleOwner(getViewLifecycleOwner());
        initializeComponents();
        return binding.getRoot();
    }


    // handle back button press
    @Override
    public boolean onBackPressed()
    {

        if (getContext() != null && isWidgetExpanded && !animatorCollapse.isStarted())
        {
            animatorCollapse.start();
            return true;
        }

        if (binding.homePager.getCurrentItem() == 1)
        {
            if (appListManager.isDisplayingHidden())
                binding.appsList.setAdapter(appListManager.setDisplayHidden(false));
            else
            {
                if (!binding.appsList.canScrollVertically(-1))
                    binding.homePager.setCurrentItem(0, true);
                else
                {
                    if (binding.appsList.getLayoutManager() != null)
                        (binding.appsList.getLayoutManager()).smoothScrollToPosition(binding.appsList, new RecyclerView.State(), 0);
                }
            }
            return true;
        }
        else if (binding.homePager.getCurrentItem() == 0)
        {
            if (getContext() == null)
                return true;

            if (binding.tilesList.getLayoutManager() != null)
                binding.tilesList.getLayoutManager().smoothScrollToPosition(binding.tilesList, new RecyclerView.State(), 0);
            return true;
        }
        else
            return true;
    }


    // home button press
    @Override
    public boolean onNewIntent()
    {
        if (getContext() != null)
        {
            if (isWidgetExpanded && !animatorCollapse.isStarted())
                animatorCollapse.start();

            if (binding.homePager.getCurrentItem() == 1)
            {
                if (appListManager.isDisplayingHidden())
                    binding.appsList.setAdapter(appListManager.setDisplayHidden(false));

                if (binding.appsList.getLayoutManager() != null)
                    (binding.appsList.getLayoutManager()).smoothScrollToPosition(binding.appsList, new RecyclerView.State(), 0);


                binding.homePager.setCurrentItem(0, true);
            }

            if (binding.tilesList.getLayoutManager() != null)
                binding.tilesList.getLayoutManager().smoothScrollToPosition(binding.tilesList, new RecyclerView.State(), 0);

        }
        return true;
    }


    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        if (getContext() != null)
        {
            if (appActionReceiver != null)
            {
                appActionReceiver.invalidate();
                getContext().unregisterReceiver(appActionReceiver);
            }

            if (notificationBroadcastReceiver != null)
            {
                notificationBroadcastReceiver.invalidate();
                getContext().unregisterReceiver(notificationBroadcastReceiver);
            }

            if (packageActionsReceiver != null)
            {
                packageActionsReceiver.invalidate();
                getContext().unregisterReceiver(packageActionsReceiver);
            }

            try
            {
                FactorApplication.getAppWidgetHost().stopListening();
            }
            catch (NullPointerException e)
            {
                removeWidget(appWidgetId);
                e.printStackTrace();
            }

            // stop wave animations
            for (int x = binding.tilesList.getChildCount(), i = 0; i < x; ++i)
            {
                FactorsAdapter.FactorsViewHolder holder = (FactorsAdapter.FactorsViewHolder) binding.tilesList.getChildViewHolder(binding.tilesList.getChildAt(i));
                ViewDataBinding holderBinding = holder.binding;
                if (holderBinding instanceof FactorSmallBinding)
                {
                    ((FactorSmallBinding) holderBinding).tile.getWaveView().stopAnimation();
                }
                if (holderBinding instanceof FactorMediumBinding)
                {
                    ((FactorMediumBinding) holderBinding).tile.getWaveView().stopAnimation();
                }
                if (holderBinding instanceof FactorLargeBinding)
                {
                    ((FactorLargeBinding) holderBinding).tile.getWaveView().stopAnimation();
                }
            }

            blurAlg.destroy();
            blurAlg = null;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (getContext()!= null)
        {

            forceNotificationListener();
            binding.recentAppsList.smoothScrollToPosition(0);

            appListManager.updateShortcuts();

        }
    }


    @Override
    public void onStop()
    {
        super.onStop();
        appListManager.saveRecentApps();
        try
        {
            FactorApplication.getAppWidgetHost().stopListening();
        }
        catch (NullPointerException e)
        {
            removeWidget(appWidgetId);
            e.printStackTrace();
        }

    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (isWidgetExpanded)
            FactorApplication.getAppWidgetHost().startListening();
    }



    private void forceNotificationListener()
    {

        appListManager.clearAllNotifications();
        if (notificationListenerIntent != null && getContext() != null)
        {
            try
            {
                getContext().startService(notificationListenerIntent);
            }
            catch (IllegalStateException e)
            {
                e.printStackTrace();
            }


        }
    }


    //initialize views and listeners
    @SuppressLint("ClickableViewAccessibility")
    private void initializeComponents()
    {

        if (getActivity() == null || getContext() == null)
            return;


        //initialize resources
        //***************************************************************************************************************************************************
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        int height = Resources.getSystem().getDisplayMetrics().heightPixels;


        int paddingTop105 = (int) Util.dpToPx(105, getContext());
        int dp4 = (int) Util.dpToPx(4, getContext());
        int dp20 = (int) Util.dpToPx(20, getContext());

        int paddingTop;

        if (((float)height)/8 > paddingTop105)
            paddingTop = paddingTop105;
        else
            paddingTop = height/8;

        int paddingBottom300 = (int) Util.dpToPx(300, getContext());
        int paddingBottom150 = (int) Util.dpToPx(150, getContext());
        int paddingBottomOnSearch = (int) Util.dpToPx(2000, getContext());
        int appListPaddingTop100 = (int) Util.dpToPx(100, getContext());


        blurAlg = new RenderScriptBlur(getContext());

        notificationListenerIntent = new Intent(getActivity(), FactorNotificationListener.class);

        //initialize saved user settings
        appSettings = AppSettingsManager.getInstance(getActivity().getApplication()).getAppSettings();



        //tile list guideline position

        binding.guideline.setGuidelinePercent(appSettings.getTileListScale());

        //get system wallpaper
        //***************************************************************************************************************************************************
        checkLiveWallpaper();

        //initialize data manager
        //***************************************************************************************************************************************************
        appListManager = new AppListManager(this, binding.backgroundHost, isLiveWallpaper, appSettings);

        //register broadcast receivers
        //***************************************************************************************************************************************************
        registerBroadcastReceivers(appListManager, binding);


        //home pager
        //***************************************************************************************************************************************************
        binding.homePager.addView(binding.tilesPage, 0);
        binding.homePager.addView(binding.drawerPage, 1);




        //arrow button guideline
        if (paddingTop == paddingTop105)
            binding.guidelineArrowHorizontal.setGuidelinePercent((appListPaddingTop100 + dp20 - .5f) /height);
        else
            binding.guidelineArrowHorizontal.setGuidelinePercent((paddingTop + dp20 - .5f) /height);



        //app drawer
        //***************************************************************************************************************************************************
        if (paddingTop == paddingTop105)
            binding.appsList.setPadding(0, appListPaddingTop100, 0, paddingBottom150);
        else
            binding.appsList.setPadding(0, paddingTop + dp4, 0, paddingBottom150);


        Observer<ArrayList<UserApp>> appsObserver = userArrayList ->
        {
            binding.appsList.setLayoutManager(new FixedLinearLayoutManager(getContext()));
            binding.appsList.setAdapter(appListManager.adapter);
        };

        appListManager.getAppsMutableLiveData().observe(getViewLifecycleOwner(), appsObserver);

        binding.appsList.setHasFixedSize(true);
        binding.appsList.setItemViewCacheSize(appListManager.getListSize()*2);




        LinearLayoutManager recentManager = new LinearLayoutManager(getContext());
        recentManager.setReverseLayout(true);
        binding.recentAppsList.setLayoutManager(recentManager);
        binding.recentAppsList.setAdapter(appListManager.recentAppsHost.getAdapter());



        binding.emptyBase.setOnClickListener(v ->
        {
            if (getContext() != null && isWidgetExpanded && !animatorCollapse.isStarted())
            {
                animatorCollapse.start();
            }
        });

        binding.homePager.setOnTouchListener((v, event) ->
        {
            if (getContext() != null && isWidgetExpanded && !animatorCollapse.isStarted())
            {
                animatorCollapse.start();
            }
            return false;
        });

        //home pager on scroll
        binding.homePager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
                float xOffset = position + positionOffset;
                binding.arrowButton.setRotation(180 * xOffset - 180);
                binding.searchBase.setTranslationY(-500f + 500 * xOffset);
                binding.searchView.clearFocus();

                if (paddingTop == paddingTop105)
                    binding.appsList.setPadding(0, appListPaddingTop100, 0, paddingBottom150);
                else
                    binding.appsList.setPadding(0, paddingTop + dp4, 0, paddingBottom150);
            }

            @Override
            public void onPageSelected(int position)
            {
                if (position == 0)
                {
                    binding.arrowButton.setRotation(180);
                    binding.blur.animate().alpha(0f);
                    binding.dim.animate().alpha(0f);
                }
                if (position == 1)
                {
                    binding.blur.animate().alpha(1f);
                    binding.dim.animate().alpha(1f);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}

        });

        binding.scrollBar.setupWithRecyclerView(
                binding.appsList,
                (position) ->
                {
                    UserApp item = appListManager.getUserApp(position);
                    char cap = '#';
                    if (item.getPackageName().isEmpty())
                        return new FastScrollItemIndicator.Text("");
                    if (!item.getLabelNew().isEmpty())
                        cap =  item.getLabelNew().toUpperCase().charAt(0);
                    String capString = "";
                    try
                    {
                        // if first letter is a number, return #
                        Integer.parseInt(String.valueOf(cap));
                        capString = "#";
                    }
                    catch (NumberFormatException ignored)
                    {
                        // not number
                        if (!item.getLabelNew().isEmpty())
                            capString = "" + ChineseHelper.INSTANCE.getStringPinYin(item.getLabelNew()).toUpperCase().charAt(0);
                    }
                    return new FastScrollItemIndicator.Text(capString);
                }
        );
        binding.thumb.setupWithFastScroller(binding.scrollBar);
        binding.scrollBar.setUseDefaultScroller(false);

        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getContext())
        {
            @Override protected int getVerticalSnapPreference()
            {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };

        binding.scrollBar.getItemIndicatorSelectedCallbacks().add(
                (indicator, indicatorCenterY, itemPosition) ->
                {
                    binding.appsList.stopScroll();
                    smoothScroller.setTargetPosition(itemPosition);
                    if (binding.appsList.getLayoutManager() != null)
                        binding.appsList.getLayoutManager().startSmoothScroll(smoothScroller);

                    // String selectedLetter = indicator.toString();

                    //todo: add touch event callback here
                    //key down while selectedLetter is not empty -> show list of apps
                    //key up clears selectedLetter, hide list of apps
                    //if key up on top of an app, launch the app
                }
        );


        //tile list
        //***************************************************************************************************************************************************
        binding.widgetBase.setTranslationY(Util.dpToPx(-500, getContext()));
        int left = (int)(width*(((0.8 - appSettings.getTileListScale())) * 100 * 0.005 + 0.05)) - 1;
        int right = (int)(width*(((0.8 - appSettings.getTileListScale())) * 100 * 0.005 + 0.2)) - 1;
        binding.tilesList.setPadding(left, paddingTop, right, paddingBottom300);

        Observer<ArrayList<Factor>> factorObserver = userArrayList ->
        {
            binding.tilesList
                    .setLayoutManager(ChipsLayoutManager.newBuilder(getContext())
                            .setOrientation(ChipsLayoutManager.HORIZONTAL)
                            .setChildGravity(Gravity.CENTER)
                            .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                            .setMaxViewsInRow(8)
                            .setScrollingEnabled(true)
                            .build());

            binding.tilesList.setAdapter(appListManager.getFactorManager().adapter);
        };
        // debug
        binding.tilesList.setItemViewCacheSize(40);
        binding.tilesList.getRecycledViewPool().setMaxRecycledViews(1, 20);
        binding.tilesList.getRecycledViewPool().setMaxRecycledViews(2, 20);
        binding.tilesList.getRecycledViewPool().setMaxRecycledViews(3, 20);

        appListManager.getFactorManager().getFactorMutableLiveData().observe(getViewLifecycleOwner(), factorObserver);
        binding.tilesList.setItemViewCacheSize(appListManager.getFactorManager().adapter.getItemCount());
        binding.tilesList.setOrientation(1);
        binding.tilesList.setConnectedView(binding.arrowButton);
        //binding.tilesList.setViewToAnimate(binding.swipeRefreshLayout);
        binding.tilesList.setConnectedSpringBottom(
                new SpringAnimation(binding.arrowButton, SpringAnimation.TRANSLATION_X)
                        .setSpring(new SpringForce()
                                        .setFinalPosition(0f)
                                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                                        .setStiffness(SpringForce.STIFFNESS_LOW)));

        binding.tilesList.setConnectedSpringTop(
                new SpringAnimation(binding.arrowButton, SpringAnimation.TRANSLATION_Y)
                        .setSpring(new SpringForce()
                                .setFinalPosition(0f)
                                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                                .setStiffness(SpringForce.STIFFNESS_LOW)));


        //search bar
        //***************************************************************************************************************************************************
        binding.searchBase.setTranslationY(-500f);
        binding.searchCard.setRadius(Util.dpToPx(appSettings.getCornerRadius(), getContext()));

        EditText searchBar = binding.searchView.findViewById(R.id.search_src_text);

        searchBar.setTextColor(appSettings.isDarkIcon()?Color.BLACK:Color.WHITE);
        searchBar.setHintTextColor(appSettings.isDarkIcon()?Color.DKGRAY:Color.LTGRAY);

        //select all text when the user clicks on the search bar
        //instead of clearing the search input
        ((EditText)(binding.searchView.findViewById(R.id.search_src_text))).setSelectAllOnFocus(true);

        binding.searchView.setOnCloseListener(() ->
        {
            binding.appsList.setPadding(0, appListPaddingTop100, 0, paddingBottom150);
            return false;
        });
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                binding.appsList.setPadding(0, appListPaddingTop100, 0, paddingBottomOnSearch);
                String queryText = newText.toLowerCase().trim();
                appListManager.findPosition(binding.appsList, queryText);
                return true;
            }
        });
        binding.searchView.setOnQueryTextFocusChangeListener((v, hasFocus) ->
        {
            if (paddingTop == paddingTop105)
                binding.appsList.setPadding(0, appListPaddingTop100, 0, paddingBottom150);
            else
                binding.appsList.setPadding(0, paddingTop + dp4, 0, paddingBottom150);
        });




        //menu button
        //***************************************************************************************************************************************************
        binding.menuButton.setImageResource(appSettings.isDarkIcon()? R.drawable.icon_menu_black : R.drawable.icon_menu);
        binding.menuButton.setOnClickListener(view ->
        {
            boolean isDisplayingHidden = appListManager.isDisplayingHidden();

            PopupMenu popup = new PopupMenu(getContext(), binding.menuButton);
            popup.getMenuInflater().inflate(R.menu.app_menu, popup.getMenu());

            MenuItem displayMode = popup.getMenu().getItem(0);
            MenuItem options = popup.getMenu().getItem(1);
            MenuItem wallpaperOption = popup.getMenu().getItem(2);

            //show hidden apps
            displayMode.setTitle(isDisplayingHidden ? "My apps" : "Hidden apps");
            displayMode.setOnMenuItemClickListener(item ->
            {
                binding.appsList.setAdapter(isDisplayingHidden?
                        appListManager.setDisplayHidden(false) : appListManager.setDisplayHidden(true));
                return true;
            });

            //launch settings
            options.setOnMenuItemClickListener(item ->
            {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
            });
            popup.show();

            //change system wallpaper
            wallpaperOption.setOnMenuItemClickListener(item ->
            {
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, getString(R.string.select_wallpaper)));
                return true;
            });

            popup.getMenu().add("Remove Widget").setOnMenuItemClickListener(item ->
            {
                removeWidget(appWidgetId);
                return true;
            });
        });


        binding.widgetBaseShadow.setTranslationY(-9999);

        // widget base animators
        animatorExpand = ObjectAnimator.ofFloat(binding.widgetBase, View.TRANSLATION_Y, Util.dpToPx(0, getContext()));
        animatorExpand.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                binding.arrowButton.animate().rotation(90);
                isWidgetExpanded = true;
                binding.emptyBase.setClickable(true);
                binding.widgetBaseShadow.setTranslationY(0);
                FactorApplication.getAppWidgetHost().startListening();
            }
        });
        animatorCollapse = ObjectAnimator.ofFloat(binding.widgetBase, View.TRANSLATION_Y, Util.dpToPx(-500, getContext()));
        animatorCollapse.setDuration(80);
        animatorCollapse.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);
                binding.arrowButton.animate().rotation(-180);
                isWidgetExpanded = false;
                binding.emptyBase.setClickable(false);
                binding.widgetBaseShadow.setTranslationY(-9999);
                try
                {
                    FactorApplication.getAppWidgetHost().stopListening();
                }
                catch (NullPointerException e)
                {
                    removeWidget(appWidgetId);
                    e.printStackTrace();
                }

            }

            @Override
            public void onAnimationStart(Animator animation)
            {
                super.onAnimationStart(animation);
                binding.tilesList.release();
                binding.emptyBase.setClickable(false);
            }
        });

        binding.emptyBase.setClickable(false);
        //go to app drawer on click, if widget base is expanded, collapse it instead
        binding.arrowButton.setOnClickListener(v ->
        {
            if (isWidgetExpanded && !animatorCollapse.isStarted())
            {
                animatorCollapse.start();
            }
            else
                binding.homePager.setCurrentItem(1, true);
        });

        //pull to expand widget base
        binding.swipeRefreshLayout.setDistanceToTriggerSync(paddingTop*2);
        binding.swipeRefreshLayout.setOnRefreshListener(() ->
        {
            binding.swipeRefreshLayout.setRefreshing(false);
            binding.emptyBase.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            animatorExpand.start();

            if (getContext() != null)
                binding.tilesList.springTranslateTo(Util.dpToPx(400 - paddingTop/4f, getContext()));


            //Util.INSTANCE.setExpandNotificationDrawer(getContext(), true);

        });


        //pull to open search
        binding.drawerSwipeRefreshLayout.setDistanceToTriggerSync(appListPaddingTop100*2);
        binding.drawerSwipeRefreshLayout.setOnRefreshListener(() ->
        {
            binding.drawerSwipeRefreshLayout.setRefreshing(false);
            binding.searchBase.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            searchBar.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchBar, 0);
            }
        });



        widgetResultLauncher = registerForActivityResult(widgetActivityResultContract, this::handleWidgetResult);



        binding.addWidgetText.setOnClickListener(v -> launchPickWidgetIntent());
        binding.arrowButton.setOnLongClickListener(v ->
        {

            removeWidget(appWidgetId);
            return true;
        });
    }


    //setup wallpaper
    @SuppressLint("MissingPermission")
    private void checkLiveWallpaper()
    {

        if (getContext() == null || getActivity() == null)
            return;

        binding.searchCard.setCardBackgroundColor(Color.parseColor("#" + appSettings.getSearchBarColor()));

        //static wallpaper
        if (Environment.isExternalStorageManager() &&
                wm.getWallpaperInfo() == null && appSettings.isBlurred())
        {
            isLiveWallpaper = false;


            new Thread(() ->
            {
                Bitmap m = Util.drawableToBitmap(wm.getFastDrawable());

                Bitmap temp = m;
                Bitmap blurredM5;
                Bitmap blurredM10;

                for (int i = 0; i < 10; i++)
                {
                    temp = Toolkit.INSTANCE.blur(temp, 25);
                }

                blurredM10 = temp;

                for (int i = 0; i < 10; i++)
                {
                    m = Toolkit.INSTANCE.blur(m, appSettings.getBlurRadius());
                }
                blurredM5 = m;

                getActivity().runOnUiThread(() ->
                {
                    binding.blur.setImageBitmap(blurredM10);
                    binding.blurTileStatic.setImageBitmap(blurredM5);
                    if (appSettings.getStaticBlur())
                        binding.blurTileStatic.setAlpha(1f);
                    else
                        binding.blurTileStatic.setAlpha(0f);
                });

            }).start();


            binding.backgroundImage.setImageDrawable(wm.getDrawable());

            binding.searchBlur.setupWith(binding.rootContent, blurAlg)
                    .setOverlayColor(Color.parseColor("#" + appSettings.getSearchBarColor()))
                    .setFrameClearDrawable(wm.getDrawable())
                    .setBlurRadius(25f)
                    .setBlurAutoUpdate(true)
                    .setBlurEnabled(true);
        }
        else //live wallpaper
        {
            isLiveWallpaper = true;
            //binding.blur.setBlurEnabled(false);
            binding.searchBlur.setBlurEnabled(false);
        }
    }


    //setup all broadcast receivers and notify FactorNotificationListener when all receivers are ready
    private void registerBroadcastReceivers(AppListManager appListManager, FragmentHomeScreenBinding binding)
    {

        if (getContext() == null)
            return;

        appActionReceiver = new AppActionReceiver(appListManager, binding);
        IntentFilter filterAppAction = new IntentFilter();
        filterAppAction.addAction(Constants.BROADCAST_ACTION_REMOVE);
        filterAppAction.addAction(Constants.BROADCAST_ACTION_ADD);
        filterAppAction.addAction(Constants.BROADCAST_ACTION_RENAME);
        filterAppAction.addAction(Constants.SETTINGS_CHANGED);
        //filterAppAction.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(appActionReceiver, filterAppAction, Context.RECEIVER_EXPORTED);
        }
        else
            getContext().registerReceiver(appActionReceiver, filterAppAction);
        packageActionsReceiver = new PackageActionsReceiver(appListManager);
        IntentFilter filterPackageAction = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filterPackageAction.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filterPackageAction.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filterPackageAction.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filterPackageAction.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filterPackageAction.addDataScheme("package");
        getContext().registerReceiver(packageActionsReceiver, filterPackageAction);


        notificationBroadcastReceiver = new NotificationBroadcastReceiver(appListManager);
        IntentFilter filterNotification = new IntentFilter();
        filterNotification.addAction(Constants.NOTIFICATION_INTENT_ACTION_CLEAR);
        filterNotification.addAction(Constants.NOTIFICATION_INTENT_ACTION_POST);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(notificationBroadcastReceiver, filterNotification, Context.RECEIVER_EXPORTED);
        }
        else
        {
            getContext().registerReceiver(notificationBroadcastReceiver, filterNotification);
        }

        if (notificationListenerIntent != null)
        {
            try
            {
                getContext().startService(notificationListenerIntent);

            }
            catch (IllegalStateException e)
            {
                e.printStackTrace();
            }
        }
    }



    //receive activity result from widget intent
    private void handleWidgetResult(Intent intent)
    {
        if (intent.getIntExtra(Constants.WIDGET_RESULT_KEY, -1) == Activity.RESULT_OK)
        {
            Log.d("widget", "result: ok");
            if (intent.getIntExtra(Constants.WIDGET_KEY, -1) == REQUEST_PICK_WIDGET)
            {
                try
                {
                    conFigureWidget(intent);
                }
                catch (SecurityException e)
                {
                    e.printStackTrace();
                }

            }
            else if (intent.getIntExtra(Constants.WIDGET_KEY, -1) == REQUEST_CREATE_WIDGET)
                createWidget(intent);
        }
        else if (intent.getIntExtra(Constants.WIDGET_RESULT_KEY, -1) == Activity.RESULT_CANCELED)
        {
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1)
            {
                FactorApplication.getAppWidgetHost().deleteAppWidgetId(appWidgetId);
            }
        }
    }

    //request to configure app widget
    private void conFigureWidget(Intent data)
    {
        if (getContext() != null)
        {
            Bundle extras = data.getExtras();
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            AppWidgetProviderInfo appWidgetInfo = FactorApplication.getAppWidgetManager().getAppWidgetInfo(appWidgetId);
            //requestBindWidget(appWidgetId, appWidgetInfo);
            if (appWidgetInfo.configure != null)
            {
                Log.d("widget", "configure");
                Intent createIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                createIntent.setComponent(appWidgetInfo.configure);
                createIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                createIntent.putExtra(Constants.WIDGET_KEY, Constants.REQUEST_CREATE_WIDGET);
                widgetResultLauncher.launch(widgetActivityResultContract.createIntent(getContext(), createIntent));
            }
            else
            {
                createWidget(data);
            }
        }
    }

    //create appWidgetView
    private void createWidget(Intent data)
    {
        Log.d("widget", "create");
        Bundle extras = data.getExtras();
        appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        addWidgetView(appWidgetId);
        SharedPreferences p = requireActivity().getSharedPreferences("factor_widget", Context.MODE_PRIVATE);
        p.edit().putInt("widget_key", appWidgetId).apply();
    }

    private void addWidgetView(int id)
    {

        FactorsDatabase.FactorsDao daoReference = FactorsDatabase.Companion.getInstance(FactorApplication.context).factorsDao();
        Factor factor = new Factor();
        factor.setLabelNew("<WIDGET>");
        factor.setPackageName(String.valueOf(id));
        factor.setSize(Factor.Size.WIDGET);


        appListManager.getFactorManager().insertFactor(factor);


        //binding.widgetBaseShadow.setVisibility(View.GONE);
    }

    //todo: this does not affect the binding process
    private void requestBindWidget(int appWidgetId, AppWidgetProviderInfo info)
    {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
        intent.putExtra("ACTION_BIND", AppWidgetManager.ACTION_APPWIDGET_BIND);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, info.configure);
        widgetResultLauncher.launch(intent);
    }


    public void launchPickWidgetIntent()
    {
        if (getContext() != null)
        {
            appWidgetId = FactorApplication.getAppWidgetHost().allocateAppWidgetId();
            Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            pickIntent.putExtra(Constants.WIDGET_KEY, Constants.REQUEST_PICK_WIDGET);
            widgetResultLauncher.launch(widgetActivityResultContract.createIntent(getContext(), pickIntent));
        }
    }

    public void removeWidget(int id)
    {
        try
        {
            FactorApplication.getAppWidgetHost().deleteAppWidgetId(id);
            binding.widgetBaseShadow.setVisibility(View.VISIBLE);
            binding.widgetBase.removeAllViews();
        }
        catch (NullPointerException ne)
        {
            ne.printStackTrace();
        }
        appWidgetId = -1;
        SharedPreferences p = requireActivity().getSharedPreferences("factor_widget", Context.MODE_PRIVATE);
        p.edit().putInt("widget_key", appWidgetId).apply();
    }
}