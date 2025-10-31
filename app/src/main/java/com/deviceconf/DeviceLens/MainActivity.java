package com.deviceconf.DeviceLens;
// This App is build by MD SIRAJUL ISLAM GitHub: @MSI-Sirajul
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask; // For background loading
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends android.app.Activity {

    private static final String TAG = "UserX_MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001; 

    // UI Components
    private EditText searchEditText;
    private FrameLayout contentFrame;
    private LinearLayout tabUserApps, tabSystemApps, tabDeviceInfo;
    private ImageView iconUserApps, iconSystemApps, iconDeviceInfo;
    private ImageView profileIcon; // Added profile icon reference

    // Data Holders
    private List<AppItem> allInstalledApps;
    private List<AppItem> userApps;
    private List<AppItem> systemApps;
    private AppListAdapter appListAdapter;

    // Current Tab
    private int currentTab = R.id.tab_user_apps; // Default to User Apps

    // Website URL for profile icon
    private static final String PROFILE_WEBSITE_URL = "https://md-sirajul-islam.vercel.app/"; // Replace with your actual website

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupTabListeners();
        setupSearchFunctionality();
        setupProfileIconListener(); // Setup profile icon click listener

        showLoadingAnimation(); // Show loading animation immediately
        new LoadAppsTask().execute(); // Load apps in background
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        contentFrame = findViewById(R.id.content_frame);

        tabUserApps = findViewById(R.id.tab_user_apps);
        tabSystemApps = findViewById(R.id.tab_system_apps);
        tabDeviceInfo = findViewById(R.id.tab_device_info);

        iconUserApps = findViewById(R.id.icon_user_apps);
        iconSystemApps = findViewById(R.id.icon_system_apps);
        iconDeviceInfo = findViewById(R.id.icon_device_info);

        profileIcon = findViewById(R.id.profile_icon); // Initialize profile icon
    }

    private void setupTabListeners() {
        tabUserApps.setOnClickListener(v -> selectTab(R.id.tab_user_apps));
        tabSystemApps.setOnClickListener(v -> selectTab(R.id.tab_system_apps));
        tabDeviceInfo.setOnClickListener(v -> selectTab(R.id.tab_device_info));
    }

    private void setupProfileIconListener() {
        profileIcon.setOnClickListener(v -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PROFILE_WEBSITE_URL));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Could not open website: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error opening website", e);
            }
        });
    }

    private void selectTab(int tabId) {
        // Only trigger animations and content change if a new tab is selected
        if (currentTab != tabId) {
            // Reset all tab backgrounds and icon tints
            resetTabStyles();

            // Set selected tab style
            LinearLayout selectedTab = findViewById(tabId);
            ImageView selectedIcon = null;

            if (tabId == R.id.tab_user_apps) {
                selectedIcon = iconUserApps;
            } else if (tabId == R.id.tab_system_apps) {
                selectedIcon = iconSystemApps;
            } else if (tabId == R.id.tab_device_info) {
                selectedIcon = iconDeviceInfo;
            }

            if (selectedTab != null) {
                selectedTab.setSelected(true); // This triggers the selector drawable
                if (selectedIcon != null) {
                    selectedIcon.setColorFilter(getResources().getColor(android.R.color.white)); // Ensure icon color is white
                }
                // Add a subtle animation for tab selection
                Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                selectedTab.startAnimation(anim);
            }

            // Load content for the selected tab with animation
            loadContentForTab(tabId);
            currentTab = tabId;
        } else {
            // If the same tab is clicked, just ensure its content is visible (useful after loading)
            loadContentForTab(tabId);
        }
    }

    private void resetTabStyles() {
        tabUserApps.setSelected(false);
        tabSystemApps.setSelected(false);
        tabDeviceInfo.setSelected(false);

        // Reset icon tints to default (white as per your layout)
        iconUserApps.setColorFilter(getResources().getColor(android.R.color.white));
        iconSystemApps.setColorFilter(getResources().getColor(android.R.color.white));
        iconDeviceInfo.setColorFilter(getResources().getColor(android.R.color.white));
    }

    private void showLoadingAnimation() {
        contentFrame.removeAllViews();
        View loadingView = LayoutInflater.from(this).inflate(R.layout.loading_spinner, contentFrame, false);
        contentFrame.addView(loadingView);
        // Optionally, add a fade in animation for the loading view
        Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        loadingView.startAnimation(anim);
    }

    private void loadContentForTab(int tabId) {
        // Ensure app data is loaded before trying to display lists
        if (allInstalledApps == null) {
            showLoadingAnimation(); // Show loading if data is not ready
            return;
        }

        contentFrame.removeAllViews(); // Clear previous content

        Animation contentAnim = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        contentAnim.setDuration(200); // Short duration for a snappier feel

        View tabContentView = null;

        if (tabId == R.id.tab_user_apps) {
            tabContentView = createAppsListView(userApps);
            if (appListAdapter != null) appListAdapter.updateApps(userApps); // Ensure adapter is updated
        } else if (tabId == R.id.tab_system_apps) {
            tabContentView = createAppsListView(systemApps);
            if (appListAdapter != null) appListAdapter.updateApps(systemApps); // Ensure adapter is updated
        } else if (tabId == R.id.tab_device_info) {
            tabContentView = createDeviceInfoView();
        }

        if (tabContentView != null) {
            contentFrame.addView(tabContentView);
            tabContentView.startAnimation(contentAnim);
        }
    }

    // AsyncTask to load apps in background
    private class LoadAppsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoadingAnimation(); // Ensure loading animation is visible before starting
        }

        @Override
        protected Void doInBackground(Void... voids) {
            PackageManager pm = getPackageManager();
            allInstalledApps = new ArrayList<>();
            userApps = new ArrayList<>();
            systemApps = new ArrayList<>();

            List<ApplicationInfo> applications;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { 
                applications = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            } else {
                applications = pm.getInstalledApplications(0); 
            }

            for (ApplicationInfo appInfo : applications) {
                if (appInfo.packageName.equals(getPackageName())) {
                    continue;
                }

                AppItem item = new AppItem();
                item.packageName = appInfo.packageName;
                item.appName = appInfo.loadLabel(pm).toString();
                item.appIcon = appInfo.loadIcon(pm);
                item.uid = appInfo.uid;

                try {
                    PackageInfo packageInfo;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { 
                        packageInfo = pm.getPackageInfo(appInfo.packageName, PackageManager.GET_META_DATA);
                    } else {
                        packageInfo = pm.getPackageInfo(appInfo.packageName, 0); 
                    }

                    item.versionName = packageInfo.versionName;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        item.versionCode = String.valueOf(packageInfo.getLongVersionCode());
                    } else {
                        item.versionCode = String.valueOf(packageInfo.versionCode);
                    }
                    item.firstInstallTime = packageInfo.firstInstallTime;
                    item.lastUpdateTime = packageInfo.lastUpdateTime;

                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Package not found: " + appInfo.packageName, e);
                }

                allInstalledApps.add(item);

                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    userApps.add(item);
                } else {
                    systemApps.add(item);
                }
            }

            Comparator<AppItem> appNameComparator = (app1, app2) -> app1.appName.toLowerCase(Locale.getDefault()).compareTo(app2.appName.toLowerCase(Locale.getDefault()));
            Collections.sort(userApps, appNameComparator);
            Collections.sort(systemApps, appNameComparator);
            Collections.sort(allInstalledApps, appNameComparator);
            
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Once apps are loaded, display the default tab content (User Apps)
            selectTab(currentTab); // This will now correctly show User Apps
        }
    }

    private View createAppsListView(List<AppItem> apps) {
        ListView listView = new ListView(this);
        listView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        listView.setDivider(null); 
        listView.setDividerHeight(0); 
        listView.setSelector(android.R.color.transparent); 

        // Initialize adapter here, or update if already initialized
        if (appListAdapter == null) {
            appListAdapter = new AppListAdapter(this, apps);
        } else {
            appListAdapter.updateApps(apps); // Update with the correct list
        }
        listView.setAdapter(appListAdapter);
        return listView;
    }

    private View createDeviceInfoView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        layout.setPadding(16, 16, 16, 16); 

        LayoutInflater inflater = LayoutInflater.from(this);

        addDeviceInfoRow(inflater, layout, getString(R.string.device_brand_label), Build.BRAND);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_model_label), Build.MODEL);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_android_version_label), Build.VERSION.RELEASE);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_sdk_version_label), String.valueOf(Build.VERSION.SDK_INT));
        addDeviceInfoRow(inflater, layout, getString(R.string.device_product_label), Build.PRODUCT);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_hardware_label), Build.HARDWARE);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_manufacturer_label), Build.MANUFACTURER);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_fingerprint_label), Build.FINGERPRINT);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_board_label), Build.BOARD);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_host_label), Build.HOST);
        
        String deviceSerial = "N/A";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { 
            try {
                deviceSerial = Build.getSerial();
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException getting device serial: " + e.getMessage());
                deviceSerial = "Permission Denied (SDK " + Build.VERSION.SDK_INT + ")";
            }
        } else {
            deviceSerial = Build.SERIAL; 
        }
        addDeviceInfoRow(inflater, layout, getString(R.string.device_id_label), deviceSerial); 

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        addDeviceInfoRow(inflater, layout, "Android ID:", androidId);

        addDeviceInfoRow(inflater, layout, getString(R.string.device_bootloader_label), Build.BOOTLOADER);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_type_label), Build.TYPE);
        addDeviceInfoRow(inflater, layout, getString(R.string.device_user_label), Build.USER);
        
        return layout;
    }

    private void addDeviceInfoRow(LayoutInflater inflater, LinearLayout parentLayout, String label, String value) {
        View rowView = inflater.inflate(R.layout.item_device_info, parentLayout, false);
        TextView labelTv = rowView.findViewById(R.id.device_info_label);
        TextView valueTv = rowView.findViewById(R.id.device_info_value);

        labelTv.setText(label);
        valueTv.setText(value != null && !value.isEmpty() ? value : "N/A"); 

        parentLayout.addView(rowView);
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (appListAdapter != null) {
                    // Pass the complete list (userApps or systemApps) to the adapter for filtering
                    // based on the currently active tab.
                    List<AppItem> currentList = new ArrayList<>();
                    if (currentTab == R.id.tab_user_apps && userApps != null) {
                        currentList.addAll(userApps);
                    } else if (currentTab == R.id.tab_system_apps && systemApps != null) {
                        currentList.addAll(systemApps);
                    }
                    appListAdapter.updateApps(currentList); // Re-set and filter
                    appListAdapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    // --- Inner Classes for App List ---

    private static class AppItem {
        String appName;
        String packageName;
        Drawable appIcon;
        int uid;
        String versionName;
        String versionCode;
        long firstInstallTime;
        long lastUpdateTime;
    }

    private class AppListAdapter extends BaseAdapter {
        private Context context;
        private List<AppItem> originalList;
        private List<AppItem> filteredList;
        private LayoutInflater inflater;

        public AppListAdapter(Context context, List<AppItem> apps) {
            this.context = context;
            this.originalList = new ArrayList<>(apps); 
            this.filteredList = new ArrayList<>(apps); 
            this.inflater = LayoutInflater.from(context);
        }

        public void updateApps(List<AppItem> newApps) {
            this.originalList.clear();
            this.originalList.addAll(newApps);
            filter(searchEditText.getText().toString()); 
        }

        public void filter(String charText) {
            charText = charText.toLowerCase(Locale.getDefault());
            filteredList.clear();
            if (charText.length() == 0) {
                filteredList.addAll(originalList);
            } else {
                for (AppItem item : originalList) {
                    if (item.appName.toLowerCase(Locale.getDefault()).contains(charText)) {
                        filteredList.add(item);
                    }
                }
            }
            Collections.sort(filteredList, (app1, app2) -> app1.appName.toLowerCase(Locale.getDefault()).compareTo(app2.appName.toLowerCase(Locale.getDefault())));
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_app, parent, false);
                holder = new ViewHolder();
                holder.appIcon = convertView.findViewById(R.id.app_icon);
                holder.appName = convertView.findViewById(R.id.app_name);
                holder.appUid = convertView.findViewById(R.id.app_uid);
                holder.launchButton = convertView.findViewById(R.id.launch_app_button);
                holder.infoButton = convertView.findViewById(R.id.app_info_button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final AppItem app = filteredList.get(position);

            holder.appIcon.setImageDrawable(app.appIcon);
            holder.appName.setText(app.appName);
            holder.appUid.setText(getString(R.string.app_uid_label) + " " + app.uid);

            holder.launchButton.setOnClickListener(v -> {
                Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (intent != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(context, "Could not launch " + app.appName, Toast.LENGTH_SHORT).show();
                }
            });

            holder.infoButton.setOnClickListener(v -> {
                showAppDetailsDialog(app); 
            });
            
            convertView.setOnClickListener(v -> {
                Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (intent != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(context, "Could not launch " + app.appName, Toast.LENGTH_SHORT).show();
                }
            });


            return convertView;
        }

        private class ViewHolder {
            ImageView appIcon;
            TextView appName;
            TextView appUid;
            LinearLayout launchButton; 
            ImageView infoButton;
        }
    }

    // --- Helper Methods & Dialogs ---

    private void showAppDetailsDialog(AppItem app) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_app_details, null);
        builder.setView(dialogView);

        ImageView iconView = dialogView.findViewById(R.id.dialog_app_icon);
        TextView nameView = dialogView.findViewById(R.id.dialog_app_name);
        TextView packageView = dialogView.findViewById(R.id.dialog_app_package);
        TextView uidView = dialogView.findViewById(R.id.dialog_app_uid);
        TextView versionView = dialogView.findViewById(R.id.dialog_app_version);
        TextView installTimeView = dialogView.findViewById(R.id.dialog_app_install_time);
        TextView updateTimeView = dialogView.findViewById(R.id.dialog_app_update_time);

        iconView.setImageDrawable(app.appIcon);
        nameView.setText(app.appName);
        packageView.setText(getString(R.string.app_package_label) + " " + app.packageName);
        uidView.setText(getString(R.string.app_uid_label) + " " + app.uid);
        versionView.setText(getString(R.string.app_version_label) + " " + (app.versionName != null ? app.versionName : "N/A") + " (" + (app.versionCode != null ? app.versionCode : "N/A") + ")");
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        installTimeView.setText(getString(R.string.app_install_date_label) + " " + sdf.format(new Date(app.firstInstallTime)));
        updateTimeView.setText(getString(R.string.app_update_date_label) + " " + sdf.format(new Date(app.lastUpdateTime)));

        builder.setPositiveButton("Open App Info", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", app.packageName, null));
            startActivity(intent);
        });
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        builder.show();
    }
}