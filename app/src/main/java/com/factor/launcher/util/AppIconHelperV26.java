package com.factor.launcher.util;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.factor.launcher.FactorApplication;
import com.factor.launcher.R;
import com.factor.launcher.models.AppSettings;
import com.factor.launcher.view_models.AppSettingsManager;

public class AppIconHelperV26 {

    public static Drawable getAppIcon(PackageManager mPackageManager, String packageName) {

        try {
            Drawable drawable = mPackageManager.getApplicationIcon(packageName);
            if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !AppSettingsManager.getInstance(FactorApplication.application).getAppSettings().getAdaptiveIcons()) {
                return drawable;
            }

            boolean isMonochrome = false;

            Drawable foregroundDr = drawable;

            if (drawable instanceof AdaptiveIconDrawable) {
                int color = ContextCompat.getColor(FactorApplication.context, R.color.launcher_icon);
                Drawable monochrome = ((AdaptiveIconDrawable) drawable).getMonochrome();
                if(monochrome != null){
                    foregroundDr = ((AdaptiveIconDrawable) drawable).getMonochrome();
                    foregroundDr.setTint(color);
                    isMonochrome = true;
                }

            }

            Drawable[] drr = new Drawable[2];
            drr[0] = CircleDrawableCreator.createCircleDrawable(FactorApplication.context);
            drr[1] = foregroundDr;

            LayerDrawable layerDrawable = new LayerDrawable(drr);

            if(isMonochrome){
                int zoomFactor = -120;
                layerDrawable.setLayerInset(1, zoomFactor, zoomFactor, zoomFactor, zoomFactor);
            }


            int width = foregroundDr.getIntrinsicWidth() +40;
            int height = foregroundDr.getIntrinsicHeight() +40;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);

            layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            layerDrawable.draw(canvas);

            BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
            bitmapDrawable.setBounds(0, 0, width, height);

            return bitmapDrawable;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}