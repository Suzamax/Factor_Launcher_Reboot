package com.factor.launcher.util;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.factor.launcher.R;

public class CircleDrawableCreator {

    public static GradientDrawable createCircleDrawable(Context context) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.OVAL);

        // Get primary color from Material You theme
        gradientDrawable.setColor(context.getColor(R.color.launcher_icon_background));
        gradientDrawable.setBounds(0, 0, 70, 70);

        return gradientDrawable;
    }
}
