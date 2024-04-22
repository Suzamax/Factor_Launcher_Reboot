package com.factor.launcher.util;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.factor.launcher.FactorApplication;

public class AppIconHelperV26 {

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static Drawable getAppIcon(PackageManager mPackageManager, String packageName) {

        try {
            Drawable drawable = mPackageManager.getApplicationIcon(packageName);

            if (drawable instanceof BitmapDrawable) {
                return drawable;
            } else if (drawable instanceof AdaptiveIconDrawable) {
                Drawable backgroundDr = ((AdaptiveIconDrawable) drawable).getBackground();
                Drawable foregroundDr = ((AdaptiveIconDrawable) drawable).getForeground();

                Drawable[] drr = new Drawable[2];
                drr[0] = CircleDrawableCreator.createCircleDrawable(FactorApplication.context);
                drr[1] = foregroundDr;

                LayerDrawable layerDrawable = new LayerDrawable(drr);

                int width = foregroundDr.getIntrinsicWidth() + 30;
                int height = foregroundDr.getIntrinsicHeight() + 30;

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);

                layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                layerDrawable.draw(canvas);

                BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
                bitmapDrawable.setBounds(0, 0, width, height);

                return bitmapDrawable;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}