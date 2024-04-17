package com.factor.launcher.util;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.factor.launcher.R;

public class CircleDrawableCreator {

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static GradientDrawable createCircleDrawable(Context context) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.OVAL);

        // Get primary color from Material You theme
        gradientDrawable.setColor(context.getColor(android.R.color.system_accent1_500));
        gradientDrawable.setBounds(0, 0, 100, 100);

        return gradientDrawable;
    }
}
