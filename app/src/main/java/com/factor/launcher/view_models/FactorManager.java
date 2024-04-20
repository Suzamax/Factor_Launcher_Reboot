package com.factor.launcher.view_models;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.factor.launcher.FactorApplication;
import com.factor.launcher.adapters.FactorsAdapter;
import com.factor.launcher.database.FactorsDatabase;
import com.factor.launcher.models.AppSettings;
import com.factor.launcher.models.AppShortcut;
import com.factor.launcher.models.Factor;
import com.factor.launcher.models.UserApp;
import com.factor.launcher.models.Payload;
import com.factor.launcher.util.IconPackManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FactorManager extends ViewModel
{
    private static final String TAG = FactorManager.class.getSimpleName();
    private final MutableLiveData<ArrayList<Factor>> factorMutableLiveData = new MutableLiveData<>();

    private final ArrayList<Factor> userFactors = new ArrayList<>();

    private final FactorsDatabase.FactorsDao daoReference;

    private final LauncherApps.ShortcutQuery shortcutQuery;

    public PackageManager packageManager;

    public FactorsAdapter adapter;

    public final LauncherApps launcherApps;

    private AppSettings appSettings;

    private final AppListManager appListManager;

    private final IconPackManager.IconPack iconPack;

    //constructor
    public FactorManager(Activity activity,
                         ViewGroup background,
                         AppListManager appListManager,
                         PackageManager pm,
                         LauncherApps launcherApps,
                         LauncherApps.ShortcutQuery shortcutQuery,
                         IconPackManager.IconPack iconPack,
                         Boolean isLiveWallpaper)
    {
        this.packageManager = pm;
        this.appListManager = appListManager;
        this.shortcutQuery = shortcutQuery;
        this.launcherApps = launcherApps;
        this.appSettings = AppSettingsManager.getInstance(activity.getApplication()).getAppSettings();
        this.iconPack = iconPack;

        adapter = new FactorsAdapter(this, appSettings, activity, isLiveWallpaper, userFactors, background);
        daoReference = FactorsDatabase.Companion.getInstance(activity.getApplicationContext()).factorsDao();
        loadFactors();

        factorMutableLiveData.setValue(userFactors);
    }

    public MutableLiveData<ArrayList<Factor>> getFactorMutableLiveData()
    {
        return this.factorMutableLiveData;
    }

    public void reloadFactors()
    {
        userFactors.clear();
        loadFactors();
    }

    //load factors from db
    private void loadFactors()
    {
        new Thread(()->
        {
            userFactors.addAll(daoReference.getAll());
            for (Factor f: userFactors)
            {
                if(!f.getLabelNew().equals("<WIDGET>"))
                    loadIcon(f);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                    f.setShortcuts(getShortcutsFromFactor(f));

            }
            SharedPreferences p = FactorApplication.context.getSharedPreferences("factor_widget", Context.MODE_PRIVATE);
            int appWidgetId = p.getInt("widget_key", -1);
            if (appWidgetId != -1 && false)
            {
                //add widget
                addWidgetView(appWidgetId);
            }
            adapter.activity.runOnUiThread(adapter::notifyDataSetChanged);
        }).start();
    }

    private void addWidgetView(int id)
    {


        Factor factor = new Factor();
        factor.setLabelNew("<WIDGET>");
        factor.setPackageName(String.valueOf(id));
        factor.setSize(Factor.Size.WIDGET);

        appListManager.getFactorManager().adapter.userFactors.add(factor);


        //binding.widgetBaseShadow.setVisibility(View.GONE);
    }

    //add recently used app
    public void addToRecent(UserApp app)
    {
        if (!app.isHidden())
           appListManager.recentAppsHost.add(app);
    }

    public UserApp findAppByPackage(String name)
    {
        return appListManager.findAppByPackage(name);
    }

    //add new app to home
    public void addToHome(UserApp app)
    {
        Factor factor = app.toFactor();
        userFactors.add(factor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            factor.setShortcuts(getShortcutsFromFactor(factor));

        factor.setOrder(userFactors.indexOf(factor));
        new Thread(() ->
        {
            daoReference.insert(factor);
            adapter.addFactorBroadcast(userFactors.indexOf(factor));
            adapter.activity.runOnUiThread(()-> adapter.notifyItemInserted(factor.getOrder()));
        }).start();
        updateOrders();
    }

    //remove factor from home
    public void removeFromHome(Factor factor)
    {
        new Thread(() ->
        {
            userFactors.remove(factor);
            daoReference.delete(factor);
            adapter.activity.runOnUiThread(adapter::notifyDataSetChanged);
            updateOrders();
        }).start();
    }

    //check if the app is added to home
    public boolean isAppPinned(UserApp app)
    {
        ArrayList<Factor> copyFactors = new ArrayList<>(userFactors);
        for (Factor f : copyFactors)
            if (f.getPackageName().equals(app.getPackageName())) return true;

        return false;
    }

    //resize a factor
    public boolean resizeFactor(Factor factor, int size)
    {
        factor.setSize(size);
        return updateFactor(factor);
    }

    //update factor info after editing
    private boolean updateFactor(Factor factor)
    {
        if (!userFactors.contains(factor))
            return false;
        else
        {
            int position = userFactors.indexOf(factor);
            factor.setOrder(position);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                factor.setShortcuts(getShortcutsFromFactor(factor));

            new Thread(()->
            {
                daoReference.updateFactorInfo(factor);
                adapter.activity.runOnUiThread(() -> adapter.notifyItemChanged(position));
            }).start();
        }
        return true;
    }

    //update factor info after its app has changed
    public void updateFactor(UserApp app)
    {
        ArrayList<Factor> factorsToUpdate = getFactorsByPackage(app);
        for (Factor f : factorsToUpdate)
        {
            if (userFactors.contains(f))
            {
                loadIcon(f);
                f.setLabelOld(app.getLabelOld());
                f.setLabelNew(app.getLabelNew());
                f.setUserApp(app);
                new Thread(()->
                {
                    daoReference.updateFactorInfo(f);
                    adapter.activity.runOnUiThread(() -> adapter.notifyItemChanged(userFactors.indexOf(f)));
                }).start();
            }
        }

    }

    //update the index of each factor after reordering
    public void updateOrders()
    {
        new Thread(() ->
        {
            for (Factor f: userFactors)
            {
                f.setOrder(userFactors.indexOf(f));
                daoReference.updateFactorOrder(f.getPackageName(), f.getOrder());
            }
        }).start();
    }

    //remove all related factors from home given an app
    public void remove(UserApp app)
    {
        new Thread(()->
        {
            ArrayList<Factor> factorsToRemove = getFactorsByPackage(app);
            for (Factor f : factorsToRemove)
            {
                if (userFactors.contains(f)) removeFromHome(f);
            }
        }).start();

    }

    //search for factors related to the same app (deprecate, as currently each app can only have one factor pinned)
    private ArrayList<Factor> getFactorsByPackage(UserApp app)
    {
        ArrayList<Factor> factorsToRemove = new ArrayList<>();

        for (Factor f : userFactors)
        {
            if (f.getPackageName().equals(app.getPackageName()))
                factorsToRemove.add(f);
        }

        return factorsToRemove;
    }



    //retrieve the icon for a given factor
    public void loadIcon(Factor factor)
    {
        Drawable icon = null;
        try
        {

            if (packageManager.getApplicationInfo(factor.getPackageName(), 0).enabled)
            {

                // todo: fallback to bitmap icon
                if (iconPack != null)
                {
                    icon = iconPack.getDrawableIconForPackage(factor.getPackageName(), packageManager.getApplicationIcon(factor.getPackageName()));
                }

                if (icon == null)
                    icon = appListManager.packageManager.getApplicationIcon(factor.getPackageName());


                factor.setIcon(icon);

            }
        }
        catch (PackageManager.NameNotFoundException | NullPointerException e)
        {
            Log.d(TAG, "failed to load icon for " + factor.getPackageName() + " " + e.getMessage());
            //removeFromHome(factor);
        }
    }


    //received notification
    public void onReceivedNotification(Intent intent, UserApp app, Payload payload)
    {
        adapter.onReceivedNotification(intent, app, payload);
    }

    //cleared notification
    public void onClearedNotification(Intent intent, UserApp app, Payload payload)
    {
        adapter.onClearedNotification(intent, app, payload);
    }


    //update UI given the app
    public void clearNotification(UserApp app)
    {
        adapter.clearNotification(app);
    }


    public void invalidate()
    {
        this.adapter.invalidate();
        this.packageManager = null;
        this.appSettings = null;
        this.adapter = null;
    }


    //find shortcuts related to a factor
    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public ArrayList<AppShortcut> getShortcutsFromFactor(Factor factor)
    {
        ArrayList<AppShortcut> shortcuts = new ArrayList<>();

        if (launcherApps == null || !launcherApps.hasShortcutHostPermission())
            return shortcuts;

        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC|
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST|
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);

        shortcutQuery.setPackage(factor.getPackageName());
        List<ShortcutInfo> s = launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle());
        if (s != null && !s.isEmpty())
        {
            for (ShortcutInfo info : s)
            {
                Drawable icon = launcherApps.getShortcutIconDrawable(info, adapter.activity.getResources().getDisplayMetrics().densityDpi);
                View.OnClickListener listener = v -> launcherApps.startShortcut(info.getPackage(), info.getId(), null, null, Process.myUserHandle());
                shortcuts.add(new AppShortcut(!info.isDynamic(), info.getRank(), info.getShortLabel(), icon, listener));
            }
        }
        Collections.sort(shortcuts);
        return shortcuts;

    }
}
