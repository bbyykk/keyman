/*
 * Copyright (C) 2019 SIL International. All rights reserved.
 */
package com.tavultesoft.kmea;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.FileProvider;

import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.tavultesoft.kmea.data.CloudRepository;
import com.tavultesoft.kmea.data.Dataset;
import com.tavultesoft.kmea.data.Keyboard;
import com.tavultesoft.kmea.util.FileUtils;
import com.tavultesoft.kmea.util.FileProviderUtils;
import com.tavultesoft.kmea.util.HelpFile;
import com.tavultesoft.kmea.util.MapCompat;
import com.tavultesoft.kmea.util.QRCodeUtil;

import static com.tavultesoft.kmea.ConfirmDialogFragment.DialogType.DIALOG_TYPE_DELETE_KEYBOARD;

// Public access is necessary to avoid IllegalAccessException
public final class KeyboardSettingsActivity extends AppCompatActivity {
  private static final String TAG = "KbSettingsActivity";
  private static ArrayList<HashMap<String, String>> infoList = null;
  private static Typeface titleFont = null;
  private static final String titleKey = "title";
  private static final String subtitleKey = "subtitle";
  private static final String iconKey = "icon";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    final Context context = this;
    final String authority = FileProviderUtils.getAuthority(context);

    setContentView(R.layout.activity_list_layout);
    final Toolbar toolbar = findViewById(R.id.list_toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    final ListView listView = findViewById(R.id.listView);

    final String packageID = getIntent().getStringExtra(KMManager.KMKey_PackageID);
    final String languageID = getIntent().getStringExtra(KMManager.KMKey_LanguageID);
    final String languageName = getIntent().getStringExtra(KMManager.KMKey_LanguageName);
    final String kbID = getIntent().getStringExtra(KMManager.KMKey_KeyboardID);
    final String kbName = getIntent().getStringExtra(KMManager.KMKey_KeyboardName);
    final String kbVersion = getIntent().getStringExtra(KMManager.KMKey_KeyboardVersion);
    String latestKbdCloudVersion = kbVersion;

    // Determine if keyboard update is available from the cloud
    Dataset dataset = CloudRepository.shared.fetchDataset(this);
    HashMap<String, String> kbInfo = new HashMap<>();
    kbInfo.put(KMManager.KMKey_PackageID, packageID);
    kbInfo.put(KMManager.KMKey_LanguageID, languageID);
    kbInfo.put(KMManager.KMKey_LanguageName, languageName);
    kbInfo.put(KMManager.KMKey_KeyboardID, kbID);
    kbInfo.put(KMManager.KMKey_KeyboardName, kbName);

    Keyboard kbdQuery = new Keyboard(kbInfo);
    final Keyboard latestKbd = dataset.keyboards.findMatch(kbdQuery);
    if (latestKbd != null) {
      latestKbdCloudVersion = latestKbd.getVersion();
    }

    final TextView textView = findViewById(R.id.bar_title);
    textView.setText(kbName);
    if (titleFont != null)
      textView.setTypeface(titleFont, Typeface.BOLD);

    infoList = new ArrayList<>();
    // Display keyboard version title
    final String noIcon = "0";
    String icon = noIcon;
    HashMap<String, String> hashMap = new HashMap<>();
    hashMap.put(titleKey, getString(R.string.keyboard_version));
    hashMap.put(subtitleKey, kbVersion);
    // Display notification to download update if latestKbdCloudVersion > kbVersion (installed)
    if (FileUtils.compareVersions(latestKbdCloudVersion, kbVersion) == FileUtils.VERSION_GREATER) {
      hashMap.put(subtitleKey, context.getString(R.string.update_available, kbVersion));
      icon = String.valueOf(R.drawable.ic_cloud_download);
    }
    hashMap.put(iconKey, icon);
    infoList.add(hashMap);

    // Display keyboard help link
    hashMap = new HashMap<>();
    final String helpUrlStr = getIntent().getStringExtra(KMManager.KMKey_HelpLink);
    final String customHelpLink = getIntent().getStringExtra(KMManager.KMKey_CustomHelpLink);
    // Check if app declared FileProvider
    icon = String.valueOf(R.drawable.ic_arrow_forward);
    // Don't show help link arrow if File Provider unavailable, or custom help doesn't exist
    if ( (customHelpLink != null && !FileProviderUtils.exists(context)) ||
         (customHelpLink == null && !packageID.equals(KMManager.KMDefault_UndefinedPackageID)) ) {
      icon = noIcon;
    }
    hashMap.put(titleKey, getString(R.string.help_link));
    hashMap.put(subtitleKey, "");
    hashMap.put(iconKey, icon);
    infoList.add(hashMap);

    // Display uninstall keyboard
    if (!packageID.equalsIgnoreCase(KMManager.KMDefault_UndefinedPackageID) ||
        !kbID.equalsIgnoreCase(KMManager.KMDefault_KeyboardID)) {
      hashMap = new HashMap<>();
      hashMap.put(titleKey, getString(R.string.uninstall_keyboard));
      hashMap.put(subtitleKey, "");
      hashMap.put(iconKey, noIcon);
      infoList.add(hashMap);
    }

    String[] from = new String[]{titleKey, subtitleKey, iconKey};
    int[] to = new int[]{R.id.text1, R.id.text2, R.id.image1};

    ListAdapter adapter = new SimpleAdapter(context, infoList, R.layout.list_row_layout2, from, to) {
      @Override
      public boolean isEnabled(int position) {
        HashMap<String, String> hashMap = infoList.get(position);
        String itemTitle = MapCompat.getOrDefault(hashMap, titleKey, "");
        String icon = MapCompat.getOrDefault(hashMap, iconKey, noIcon);
        if (itemTitle.equals(getString(R.string.keyboard_version)) && icon.equals(noIcon)) {
          // No point in 'clicking' on version info if no update available
          return false;
        // Visibly disables the help option when help isn't available
        } else if (itemTitle.equals(getString(R.string.help_link)) && icon.equals(noIcon)) {
          return false;
        }

        return super.isEnabled(position);
      }
    };
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        HashMap<String, String> hashMap = (HashMap<String, String>) parent.getItemAtPosition(position);
        String itemTitle = MapCompat.getOrDefault(hashMap, titleKey, "");

        // "Version" link clicked to download latest keyboard version from cloud
        if (itemTitle.equals(getString(R.string.keyboard_version))) {
          Bundle args = latestKbd.buildDownloadBundle();
          Intent i = new Intent(getApplicationContext(), KMKeyboardDownloaderActivity.class);
          i.putExtras(args);
          startActivity(i);

        // "Help" link clicked
        } else if (itemTitle.equals(getString(R.string.help_link))) {
          if (customHelpLink != null) {
            // Display local welcome.htm help file, including associated assets
            Intent i = HelpFile.toActionView(context, customHelpLink, packageID);

            if (FileProviderUtils.exists(context) || KMManager.isTestMode()) {
              startActivity(i);
            }
          } else {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(helpUrlStr));
            startActivity(i);
          }

        // "Uninstall Keyboard" clicked
        } else if (itemTitle.equals(getString(R.string.uninstall_keyboard))) {
          // Uninstall selected keyboard
          String title = String.format("%s: %s", languageName, kbName);
          String keyboardKey = String.format("%s_%s", languageID, kbID);
          DialogFragment dialog = ConfirmDialogFragment.newInstanceForItemKeyBasedAction(
            DIALOG_TYPE_DELETE_KEYBOARD, title, getString(R.string.confirm_delete_keyboard), keyboardKey);
          dialog.show(getFragmentManager(), "dialog");
        }
      }
    });

    // If QRGen library included, append the QR code View to the
    // scrollable listview for sharing keyboard
    View view = getLayoutInflater().inflate(R.layout.qr_layout, null);
    if (QRCodeUtil.libraryExists(context)) {
      LinearLayout qrLayout = (LinearLayout) view.findViewById(R.id.qrLayout);
      listView.addFooterView(qrLayout);

      String url = String.format("%s%s", QRCodeUtil.QR_BASE, kbID);
      Bitmap myBitmap = QRCodeUtil.toBitmap(url);
      ImageView imageView = (ImageView) findViewById(R.id.qrCode);
      imageView.setImageBitmap(myBitmap);

      TextView qrDescription = (TextView) findViewById(R.id.qrDescription);
      qrDescription.setText(Html.fromHtml(getString(R.string.keyboard_qr_code)));
    }
  }

  @Override
  public boolean onSupportNavigateUp() {
    super.onBackPressed();
    return true;
  }

  @Override
  public void onDestroy(){
    super.onDestroy();

  }

}
