package com.droidlogic.setupwizard;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.GuidedStepSupportFragment;

import com.droidlogic.setupwizard.fragment.BaseGuideStepFragment;
import com.droidlogic.setupwizard.fragment.DateTimeFragment;
import com.droidlogic.setupwizard.fragment.DisplaySettingsFragment;
import com.droidlogic.setupwizard.fragment.LocalFragment;
import com.droidlogic.setupwizard.fragment.NavigationFragment;
import com.droidlogic.setupwizard.fragment.NetworkFragment;
import com.droidlogic.setupwizard.utils.Backdoor;
import com.droidlogic.setupwizard.utils.SetupWizardUtils;

import java.util.List;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    private TextView runningInfo;
    private View viNextAction;
    private View viWifiFloat;
    private TextView tvWifiName;
    private View backgroundView;
    private int currentBackgroundColor;
    private ValueAnimator pulseAnimator;

    private final int[] pageColors = new int[]{
            0xFF1A237E, // Deep Blue (Local)
            0xFF004D40, // Deep Teal (Navigation)
            0xFF2E7D32, // Dark Green (Network)
            0xFF4A148C, // Dark Purple (DateTime)
            0xFF006064  // Dark Aqua (Display)
    };

    private final int[] forbiddenKey = new int[]{206, 243, 244, 245, 165, 246, 247, 248, 168, 85, 86, 130, 169, 88, 87, 89, 90, 183, 184, 185, 186};

    private static final String USER_SETUP_COMPLETE = "user_setup_complete";

    // Debug Escape Logic
    private int cornerClickCount = 0;
    private long lastCornerClickTime = 0;
    private final Handler debugHandler = new Handler(Looper.getMainLooper());
    private final Runnable longPressRunnable = this::showEscapeDialog;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setAppPermissions();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        runningInfo = findViewById(R.id.text);
        backgroundView = findViewById(R.id.background_view);

        currentBackgroundColor = pageColors[0];
        updateBackground(currentBackgroundColor);

        if (SetupWizardUtils.isSetupWizardDisabled() || Settings.Secure.getInt(getContentResolver(), USER_SETUP_COMPLETE, 0) == 1) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            finishSetup();
            return;
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this::updatePageVisuals);

        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(this, new LocalFragment(), android.R.id.content);
        }

        enableWifi();
        setHdmiCecComponentEnabled(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

        final FrameLayout contentGroup = findViewById(android.R.id.content);
        viWifiFloat = LayoutInflater.from(MainActivity.this).inflate(R.layout.view_wifi_float, contentGroup, false);
        tvWifiName = viWifiFloat.findViewById(R.id.tv_wifi_name);
        viNextAction = LayoutInflater.from(MainActivity.this).inflate(R.layout.view_next_action, contentGroup, false);
        viNextAction.findViewById(R.id.tv_next_action).setOnClickListener(view -> {
            BaseGuideStepFragment topFragment = getTopBaseGuideStepFragment();
            if (topFragment != null) {
                topFragment.onNextAction();
            }
        });
        contentGroup.post(() -> {
            contentGroup.addView(viNextAction);
            contentGroup.addView(viWifiFloat);
        });
        /*
        mainRoot.post(() -> {
            if (buttonContainer != null) {
                buttonContainer.removeAllViews();
                buttonContainer.addView(viNextAction);
                buttonContainer.bringToFront();
            }
            ((FrameLayout)findViewById(R.id.content_container)).addView(viWifiFloat);
            viWifiFloat.bringToFront();
        });
        */
        contentGroup.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float xThreshold = Math.max(v.getWidth() * 0.10f, 100f);
                float yThreshold = Math.max(v.getHeight() * 0.10f, 100f);

                float x = event.getX();
                float y = event.getY();
                if (x < xThreshold && y < yThreshold) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastCornerClickTime < 1000) {
                        cornerClickCount++;
                    } else {
                        cornerClickCount = 1;
                    }
                    lastCornerClickTime = currentTime;

                    if (cornerClickCount == 3) {
                        debugHandler.postDelayed(longPressRunnable, 5000);
                    }
                } else {
                    cornerClickCount = 0;
                    debugHandler.removeCallbacks(longPressRunnable);
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (cornerClickCount < 3) {
                    cornerClickCount = 0;
                }
                debugHandler.removeCallbacks(longPressRunnable);
            }
            return false;
        });
    }

    private void updatePageVisuals() {
        Fragment topFragment = getTopBaseGuideStepFragment();
        int colorIndex = 0;
        if (topFragment instanceof LocalFragment) colorIndex = 0;
        else if (topFragment instanceof NavigationFragment) colorIndex = 1;
        else if (topFragment instanceof NetworkFragment) colorIndex = 2;
        else if (topFragment instanceof DateTimeFragment) colorIndex = 3;
        else if (topFragment instanceof DisplaySettingsFragment) colorIndex = 4;

        animateBackgroundColor(pageColors[colorIndex % pageColors.length]);

        if (viNextAction != null) {
            viNextAction.bringToFront();
            viNextAction.setVisibility(View.VISIBLE);
        }
        if (viWifiFloat != null) viWifiFloat.bringToFront();
    }

    private void animateBackgroundColor(int targetColor) {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), currentBackgroundColor, targetColor);
        colorAnimation.setDuration(2000);
        colorAnimation.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            updateBackground(color);
        });

        colorAnimation.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                startPulsingAnimation(targetColor);
            }
        });

        colorAnimation.start();
        currentBackgroundColor = targetColor;
    }

    private void startPulsingAnimation(int baseColor) {
        float[] hsv = new float[3];
        Color.colorToHSV(baseColor, hsv);

        float originalValue = hsv[2];
        float targetValue = Math.min(originalValue + 0.15f, 1.0f);
        if (targetValue == originalValue) {
            targetValue = Math.max(originalValue - 0.15f, 0.0f);
        }

        pulseAnimator = ValueAnimator.ofFloat(originalValue, targetValue);
        pulseAnimator.setDuration(4000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);

        pulseAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            float[] currentHsv = new float[3];
            Color.colorToHSV(baseColor, currentHsv);
            currentHsv[2] = value;
            updateBackground(Color.HSVToColor(currentHsv));
        });

        pulseAnimator.start();
    }

    private void updateBackground(int color) {
        if (backgroundView != null) {
            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.BR_TL,
                    new int[]{color, Color.BLACK}
            );
            backgroundView.setBackground(gradient);
        }
    }

    public void showFragment(Fragment fragment) {
        if (fragment instanceof GuidedStepSupportFragment) {
            GuidedStepSupportFragment.add(getSupportFragmentManager(), (GuidedStepSupportFragment) fragment, android.R.id.content);
        }
    }

    public void enableWifi() {
        new Thread(() -> {
            try {
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null && !wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private BaseGuideStepFragment getTopBaseGuideStepFragment() {
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for (int i = fragmentList.size() - 1; i >= 0; i--) {
            Fragment fragment = fragmentList.get(i);
            if (fragment instanceof BaseGuideStepFragment) {
                return (BaseGuideStepFragment) fragment;
            }
        }
        return null;
    }

    private final ViewTreeObserver.OnGlobalFocusChangeListener globalFocusChangeListener = (oldFocus, newFocus) -> {
        BaseGuideStepFragment topFragment = getTopBaseGuideStepFragment();
        if (topFragment != null) {
            topFragment.onFocusChange(oldFocus, newFocus);
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(globalFocusChangeListener);
        } else {
            getWindow().getDecorView().getViewTreeObserver().removeOnGlobalFocusChangeListener(globalFocusChangeListener);
        }
    }

    public void setWifiName(String wifiName) {
        tvWifiName.setText(wifiName);
    }

    public void showWifiView(View anchorView) {
        if (anchorView == null || viWifiFloat == null) return;
        viWifiFloat.postDelayed(() -> {
            int[] location = new int[2];
            anchorView.getLocationInWindow(location);
            viWifiFloat.setVisibility(View.VISIBLE);
            viWifiFloat.bringToFront();

            if (viWifiFloat.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams pms = (FrameLayout.LayoutParams) viWifiFloat.getLayoutParams();
                pms.leftMargin = location[0];
                pms.topMargin = (int) (location[1] - viWifiFloat.getHeight() * 0.5);
                viWifiFloat.setLayoutParams(pms);
            }
        }, 60);
    }

    public void hideWifiView() {
        if (viWifiFloat != null && viWifiFloat.getVisibility() == View.VISIBLE) {
            viWifiFloat.setVisibility(View.INVISIBLE);
        }
    }

    public void actionNextVisible() {
        if (viNextAction != null) {
            viNextAction.setVisibility(View.VISIBLE);
            viNextAction.bringToFront();
        }
    }

    public void actionNextInvisible() {
        if (viNextAction != null) viNextAction.setVisibility(View.INVISIBLE);
    }

    public void nextActionBringToFront() {
        if (viNextAction != null) viNextAction.bringToFront();
    }

    public void setNextActionText(String text) {
        if (viNextAction != null) {
            TextView tvNext = viNextAction.findViewById(R.id.tv_next_action);
            if (tvNext != null) tvNext.setText(text);
        }
    }

    private final Backdoor backdoor = new Backdoor();

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (backdoor.input(event)) {
            showEscapeDialog();
            return true;
        }
        for (int keyCode : forbiddenKey) {
            if (keyCode == event.getKeyCode()) {
                return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (viNextAction != null) viNextAction.postDelayed(this::actionNextVisible, 50);
        }
        return super.dispatchKeyEvent(event);
    }

    private void showEscapeDialog() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_skip_title)
                    .setMessage(R.string.dialog_skip_notice)
                    .setPositiveButton(R.string.dialog_btn_confirm, (dialog, which) -> {
                        dialog.dismiss();
                        setHdmiCecComponentEnabled(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                        finishSetup();
                    })
                    .setNegativeButton(R.string.dialog_btn_cancel, (dialog, which) -> {
                        dialog.dismiss();
                        cornerClickCount = 0;
                    })
                    .create()
                    .show();
        });
    }

    private void setHdmiCecComponentEnabled(int state) {
        try {
            PackageManager pm = getPackageManager();
            ComponentName name = new ComponentName("com.android.tv.settings", "com.android.tv.settings.tvoption.HdmiCecActivity");
            try {
                pm.getActivityInfo(name, 0);
                pm.setComponentEnabledSetting(name, state, PackageManager.DONT_KILL_APP);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "HdmiCecActivity not found");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to set HdmiCec component state: " + e.getMessage());
        }
    }

    private void setAppPermissions() {
        String packageName = "cu.axel.smartdock";
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                String enabledAccessibilityServices = Settings.Secure.getString(
                        getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (enabledAccessibilityServices != null && !enabledAccessibilityServices.isEmpty()) {
                    enabledAccessibilityServices += ":cu.axel.smartdock/cu.axel.smartdock.services.DockService";
                } else {
                    enabledAccessibilityServices = "cu.axel.smartdock/cu.axel.smartdock.services.DockService";
                }
                Settings.Secure.putString(getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        enabledAccessibilityServices);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
    }

    @Override
    public void finish() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            setHdmiCecComponentEnabled(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            super.finish();
        }
    }

    public void finishSetup() {
        disableComponent(this);
        setAppPermissions();
        super.finish();
    }

    public static void disableComponent(Context context) {
        new Thread(() -> {
            try {
                ContentResolver contentResolver = context.getContentResolver();
                Settings.Global.putInt(contentResolver, Settings.Global.DEVICE_PROVISIONED, 1);
                Settings.Secure.putInt(contentResolver, "user_setup_complete", 1);
                PackageManager pm = context.getPackageManager();
                ComponentName name = new ComponentName(context, MainActivity.class);
                pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void enableComponent(Context context) {
        new Thread(() -> {
            try {
                ContentResolver contentResolver = context.getContentResolver();
                Settings.Global.putInt(contentResolver, Settings.Global.DEVICE_PROVISIONED, 0);
                Settings.Secure.putInt(contentResolver, "user_setup_complete", 0);
                PackageManager pm = context.getPackageManager();
                ComponentName name = new ComponentName(context, MainActivity.class);
                pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
