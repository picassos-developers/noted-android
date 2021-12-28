package com.picassos.noted.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.picassos.noted.R;

import java.util.Objects;

public class Toasto {
    /**
     * use @show_toast to show a custom toast with
     * text, duration and style. for example:
     * show_toast(context, title, duration, type);
     * we assigned null to the layout inflater instead
     * of assigning @custom_toast to root as it will always
     * return null. Toast design is designed to be clean.
     * @param context for context
     * @param title for toast title
     * @param duration for duration, you can whether
     * choose 0 for short period or 1 for long period
     * @param type for toast type. currently support designs
     *             is (0) #primary, (1) #alert, (2) #warning
     */
    @SuppressLint("InflateParams")
    public static void show_toast(Context context, String title, int duration, int type) {
        // layout inflater
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = null;
        if (type == 0) {
            layout = Objects.requireNonNull(inflater).inflate(R.layout.toast_layout_primary, null);
        } else if (type == 1) {
            layout = Objects.requireNonNull(inflater).inflate(R.layout.toast_layout_alert, null);
        } else if (type == 2) {
            layout = Objects.requireNonNull(inflater).inflate(R.layout.toast_layout_warning, null);
        }

        // initialize toast text and set text from the method
        TextView toast_title = Objects.requireNonNull(layout).findViewById(R.id.toast_text);
        toast_title.setText(title);

        // create a new toast and set gravity as @fill_horizontal
        // and @gravity.top to make the toast stick on the top.
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(duration);
        toast.setView(layout);
        // show the toast
        toast.show();
    }
}
