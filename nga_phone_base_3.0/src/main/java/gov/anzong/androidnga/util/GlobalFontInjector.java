package gov.anzong.androidnga.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import gov.anzong.androidnga.R;

/**
 * 全局字体注入（自用改造）
 * <p>
 * 主题里的 android:textViewStyle 方案在 AppCompat 上不生效，改为运行时强制注入：
 * 1. 每个 Activity 创建后，递归遍历已存在的 View 树，给所有 TextView 换字体；
 * 2. 包装该 Activity 的 LayoutInflater.Factory2，之后所有新 inflate 出来的
 * TextView（列表项、弹窗内容等）在创建瞬间就被换掉。
 */
public final class GlobalFontInjector {

    private static final String TAG = "GlobalFontInjector";

    private static Typeface sTypeface;

    private static boolean sLoadFailed;

    private GlobalFontInjector() {
    }

    public static void install(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                try {
                    wrapInflater(activity);
                    applyToTree(activity.getWindow().getDecorView());
                } catch (Throwable t) {
                    Log.e(TAG, "apply font failed", t);
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    private static Typeface typeface(Context context) {
        if (sTypeface == null && !sLoadFailed) {
            try {
                sTypeface = ResourcesCompat.getFont(context, R.font.honorsans_bold);
                Log.i(TAG, "custom typeface loaded: " + sTypeface);
            } catch (Throwable t) {
                sLoadFailed = true;
                Log.e(TAG, "load font failed", t);
            }
        }
        return sTypeface;
    }

    private static void wrapInflater(Activity activity) {
        final LayoutInflater inflater = activity.getLayoutInflater();
        final LayoutInflater.Factory2 origin = inflater.getFactory2();
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(@Nullable View parent, @NonNull String name,
                                     @NonNull Context context, @NonNull AttributeSet attrs) {
                View view = origin == null ? null : origin.onCreateView(parent, name, context, attrs);
                if (view != null) {
                    apply(view, context);
                }
                return view;
            }

            @Override
            public View onCreateView(@NonNull String name, @NonNull Context context,
                                     @NonNull AttributeSet attrs) {
                return onCreateView(null, name, context, attrs);
            }
        });
    }

    private static void applyToTree(View view) {
        if (view == null) {
            return;
        }
        apply(view, view.getContext());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                applyToTree(group.getChildAt(i));
            }
        }
    }

    private static void apply(View view, Context context) {
        if (!(view instanceof TextView)) {
            return;
        }
        Typeface typeface = typeface(context);
        if (typeface == null) {
            return;
        }
        TextView textView = (TextView) view;
        Typeface current = textView.getTypeface();
        if (current == typeface) {
            return;
        }
        int style = current == null ? Typeface.NORMAL : current.getStyle();
        textView.setTypeface(typeface, style);
    }
}
