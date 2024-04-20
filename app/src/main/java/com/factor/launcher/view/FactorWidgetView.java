package com.factor.launcher.view;

import static androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.factor.launcher.FactorApplication;
import com.factor.launcher.R;
import com.factor.launcher.models.AppSettings;
import com.factor.launcher.models.Factor;
import com.factor.launcher.ui.ElevationImageView;
import com.factor.launcher.ui.ViewKt;
import com.factor.launcher.ui.wave_animation.WaveView;
import com.factor.launcher.ui.wave_animation.Waves;
import com.factor.launcher.util.Util;
import com.google.android.material.card.MaterialCardView;

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/**
 * Small tile
 */
public class FactorWidgetView extends ConstraintLayout implements LifecycleOwner
{
    private BlurView trans;

    private MaterialCardView card;

    private ConstraintLayout base;
    public FactorWidgetView(Context context) {
        super(context);
        init();
    }

    public FactorWidgetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FactorWidgetView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        View.inflate(getContext(), R.layout.factor_widget2_view, this);
        trans = findViewById(R.id.trans);
        card = findViewById(R.id.card);
        base = findViewById(R.id.base);
    }

    public void setupTile(AppSettings appSettings, boolean isLiveWallpaper, ViewGroup background)
    {

        //initialize blur and color
        if (isLiveWallpaper || !appSettings.isBlurred() || appSettings.getStaticBlur())
        {
            trans.setVisibility(INVISIBLE);
            card.setCardBackgroundColor(Color.parseColor("#" + appSettings.getTileThemeColor()));
        }
        else
        {
            //blur enabled, non static blur
            trans.setVisibility(VISIBLE);
            card.setCardBackgroundColor(Color.TRANSPARENT);

            BlurAlgorithm algorithm;

            algorithm = new RenderScriptBlur(getContext());

            trans.setupWith(background, algorithm)
                    .setOverlayColor(Color.parseColor("#" + appSettings.getTileThemeColor()))
                    .setBlurRadius(appSettings.getBlurRadius())
                    .setBlurAutoUpdate(false);
        }

        card.setRadius(Util.dpToPx(appSettings.getCornerRadius(), getContext()));
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle()
    {
        return ViewKt.getLifecycle(this);
    }

    public void setupContent(Factor factorToChange) {

        int id = Integer.parseInt(factorToChange.getPackageName());
        AppWidgetProviderInfo appWidgetInfo = FactorApplication.getAppWidgetManager().getAppWidgetInfo(id);
        AppWidgetHostView hostView = FactorApplication
                .getAppWidgetHost()
                .createView(FactorApplication.context, id, appWidgetInfo);
        hostView.setAppWidget(id, appWidgetInfo);
        // add widget
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT);

        params.setMargins(
                (int) Util.dpToPx(10, getContext()),
                (int) Util.dpToPx(30, getContext()),
                (int) Util.dpToPx(10, getContext()),
                (int) Util.dpToPx(10, getContext()));
        hostView.setLayoutParams(params);
        base.addView(hostView);
    }
}
