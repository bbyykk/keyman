package com.tavultesoft.kmapro;

import android.annotation.SuppressLint;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import android.util.Log;
import android.widget.Toast;

import com.tavultesoft.kmea.KMManager;
import com.tavultesoft.kmea.KeyboardEventHandler;
import com.tavultesoft.kmea.packages.PackageProcessor;
import com.tavultesoft.kmea.packages.LexicalModelPackageProcessor;
import com.tavultesoft.kmea.util.FileUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PackageActivity extends AppCompatActivity {

  private Toolbar toolbar;
  private WebView webView;
  private AlertDialog alertDialog;
  private File kmpFile;
  private File tempPackagePath;
  private static ArrayList<KeyboardEventHandler.OnKeyboardDownloadEventListener> kbDownloadEventListeners = null;
  private PackageProcessor kmpProcessor;

  @SuppressLint({"SetJavaScriptEnabled", "InflateParams"})
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_package_installer);
    boolean silentInstall = false;
    String languageID = null;

    final Context context = this;
    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      kmpFile = new File(bundle.getString("kmpFile"));
      silentInstall = bundle.getBoolean("silentInstall", false);
      languageID = bundle.getString("language", null);
    }

    File resourceRoot =  new File(context.getDir("data", Context.MODE_PRIVATE).toString() + File.separator);
    kmpProcessor =  new PackageProcessor(resourceRoot);
    final String pkgId = kmpProcessor.getPackageID(kmpFile);
    final String pkgTarget = kmpProcessor.getPackageTarget(kmpFile);

    try {
      if (pkgTarget.equals(PackageProcessor.PP_TARGET_LEXICAL_MODELS)) {
        kmpProcessor = new LexicalModelPackageProcessor(resourceRoot);
      } else if (!pkgTarget.equals(PackageProcessor.PP_TARGET_KEYBOARDS)) {
        showErrorToast(context, getString(R.string.no_targets_to_install));
        return;
      }
      tempPackagePath = kmpProcessor.unzipKMP(kmpFile);

    } catch (Exception e) {
      showErrorToast(context, getString(R.string.failed_to_extract));
      return;
    }

    JSONObject pkgInfo = kmpProcessor.loadPackageInfo(tempPackagePath);
    if (pkgInfo == null) {
      showErrorToast(context, getString(R.string.invalid_metadata));
      return;
    }

    // Silent installation (skip displaying welcome.htm and user confirmation)
    if (silentInstall) {
      installPackage(context, pkgTarget, pkgId, languageID, true);
      return;
    }

    String pkgVersion = kmpProcessor.getPackageVersion(pkgInfo);
    String pkgName = kmpProcessor.getPackageName(pkgInfo);

    toolbar = (Toolbar) findViewById(R.id.titlebar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(null);
    getSupportActionBar().setDisplayUseLogoEnabled(false);
    getSupportActionBar().setDisplayShowHomeEnabled(false);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setDisplayShowCustomEnabled(true);
    getSupportActionBar().setBackgroundDrawable(MainActivity.getActionBarDrawable(this));

    TextView packageActivityTitle = new TextView(this);
    packageActivityTitle.setWidth((int) getResources().getDimension(R.dimen.package_label_width));
    packageActivityTitle.setTextSize(getResources().getDimension(R.dimen.titlebar_label_textsize));
    packageActivityTitle.setGravity(Gravity.CENTER);

    String pkgTargetTitle = pkgTarget.equals(PackageProcessor.PP_TARGET_KEYBOARDS) ? 
      getString(R.string.install_keyboard_package) : getString(R.string.install_predictive_text_package);
    String titleStr = String.format("%s %s", pkgTargetTitle, pkgVersion);
    packageActivityTitle.setText(titleStr);
    getSupportActionBar().setCustomView(packageActivityTitle);

    webView = (WebView) findViewById(R.id.packageWebView);
    webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setUseWideViewPort(true);
    webView.getSettings().setLoadWithOverviewMode(true);
    webView.getSettings().setBuiltInZoomControls(true);
    webView.getSettings().setSupportZoom(true);
    webView.getSettings().setTextZoom(200);
    webView.setVerticalScrollBarEnabled(true);
    webView.setHorizontalScrollBarEnabled(true);
    webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    webView.setWebChromeClient(new WebChromeClient() {
    });
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url != null && !url.toLowerCase().equals("about:blank"))
          view.loadUrl(url);

        return true;
      }

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
      }

      @Override
      public void onPageFinished(WebView view, String url) {
      }
    });

     // Determine if ad-hoc distributed KMP contains readme.htm (case-insensitive) to display
    FileFilter _readmeFilter = new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        if (pathname.isFile() && FileUtils.isReadmeFile(pathname.getName())) {
          return true;
        }
        return false;
      }
    };

    File[] files = tempPackagePath.listFiles(_readmeFilter);
    if (files.length > 0 && files[0].exists() && files[0].length() > 0) {
      webView.loadUrl("file:///" + files[0].getAbsolutePath());
    } else {
      // No readme.htm so display minimal package information
      String targetString = "";
      if (pkgTarget.equals(PackageProcessor.PP_TARGET_KEYBOARDS)) {
        targetString = pkgName != null && pkgName.toLowerCase().endsWith("keyboard")
          ? "" : String.format(" %s", getResources().getQuantityString(R.plurals.title_keyboards, 1));
      } else if (pkgTarget.equals(PackageProcessor.PP_TARGET_LEXICAL_MODELS)) {
        targetString = pkgName != null && pkgName.toLowerCase().endsWith("model")
          ? "" :String.format(" %s", "model");
      }
      String htmlString = String.format(
        "<body style=\"max-width:600px;\"><H1>The %s%s Package</H1></body>",
        pkgName, targetString);
      webView.loadData(htmlString, "text/html; charset=utf-8", "UTF-8");
    }

    initializeButtons(context, pkgId, languageID, pkgTarget);
  }

  /**
   * Initialize buttons of package installer.
   * @param context the context
   * @param pkgId the keyman package id
   * @param languageID the optional language id
   * @param pkgTarget  String: PackageProcessor.PP_TARGET_KEYBOARDS or PP_TARGET_LEXICAL_MODELS
   */
  private void initializeButtons(final Context context, final String pkgId, final String languageID, final String pkgTarget) {
    final Button installButton = (Button) findViewById(R.id.installButton);
    final Button cancelButton = (Button) findViewById(R.id.cancelButton);
    final Button finishButton = (Button) findViewById(R.id.finishButton);

    installButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {


        installPackage(context, pkgTarget, pkgId, languageID, false);
      }
    });

    OnClickListener _cleanup_action = new OnClickListener() {
      @Override
      public void onClick(View v) {
        cleanup();
      }
    };
    cancelButton.setOnClickListener(_cleanup_action);
    finishButton.setOnClickListener(_cleanup_action);

    updateButtonState(true);
  }

  @Override
  public void onDestroy(){
    super.onDestroy();
    if ( alertDialog !=null && alertDialog.isShowing() ){
      alertDialog.dismiss();
    }
  }

  private void cleanup() {
    try {
      if (kmpFile != null && kmpFile.exists()) {
        kmpFile.delete();
      }
      if (tempPackagePath != null && tempPackagePath.exists()) {
        FileUtils.deleteDirectory(tempPackagePath);
      }
    } catch (Exception e) {
      Log.e("PackageActivity", "cleanup() failed with error " + e);
    } finally {
      finish();
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
  }

  @Override
  public void onBackPressed() {
    finish();
    overridePendingTransition(0, android.R.anim.fade_out);
  }

  private void showErrorToast(Context context, String message) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    // Setting result to 1 so calling activity will finish too
    setResult(1);
    cleanup();
  }

  /**
   * switch button visibility for package installer.
   * before installation show Install and Cancel
   * after installation show OK button
   * @param anIsStartInstaller if true - before installation, false - after installation
   */
  private void updateButtonState(boolean anIsStartInstaller)
  {
    final Button installButton = (Button) findViewById(R.id.installButton);
    final Button cancelButton = (Button) findViewById(R.id.cancelButton);
    final Button closeButton = (Button) findViewById(R.id.finishButton);
    if(anIsStartInstaller)
    {
      installButton.setVisibility(View.VISIBLE);
      cancelButton.setVisibility(View.VISIBLE);
      closeButton.setVisibility(View.GONE);
    }
    else
    {
      installButton.setVisibility(View.GONE);
      cancelButton.setVisibility(View.GONE);
      closeButton.setVisibility(View.VISIBLE);
    }
    findViewById(R.id.buttonBar).requestLayout();
  }

  /**
   * show welcome page from installed keyboard.
   * @param theInstalledPackages the installed keyboards or lexical models
   * @return true if a welcomepage is available
   */
  private boolean loadWelcomePage(List<Map<String, String>> theInstalledPackages)
  {
    boolean _found=false;
     for(Map<String,String> _keyboard:theInstalledPackages) {
       String _customlink = _keyboard.get(KMManager.KMKey_CustomHelpLink);
       if (_customlink != null) {
         webView.loadUrl("file:///" + _customlink);
         _found=true;
         break;
       }
     }
     if(!_found)
       return false;

     updateButtonState(false);

      return true;

  }
  /**
   * Installs the keyboard or lexical model package, and then notifies the corresponding listeners
   * @param context Context   The activity context
   * @param pkgTarget String: PackageProcessor.PP_TARGET_KEYBOARDS or PP_TARGET_LEXICAL_MODELS
   * @param pkgId String      The Keyman package ID
   * @param languageID String The optional language ID
   * @param anSilentInstall boolean If true, don't display readme.htm/welcome.htm content during installation
   */
  private void installPackage(Context context, String pkgTarget, String pkgId, String languageID, boolean anSilentInstall) {
    try {
      if (pkgTarget.equals(PackageProcessor.PP_TARGET_KEYBOARDS)) {
        // processKMP will remove currently installed package and install

        //Dataset kmpDataset = new Dataset(context);
        //kmpDataset.keyboards.addAll(kbdsList);

        List<Map<String, String>> installedPackageKeyboards =
          kmpProcessor.processKMP(kmpFile, tempPackagePath, PackageProcessor.PP_KEYBOARDS_KEY, languageID);
        // Do the notifications!
        boolean success = installedPackageKeyboards.size() != 0;
        boolean _cleanup = true;
        if (success) {
          if(!anSilentInstall)
            _cleanup = !loadWelcomePage(installedPackageKeyboards);
          notifyPackageInstallListeners(KeyboardEventHandler.EventType.PACKAGE_INSTALLED,
            installedPackageKeyboards, 1);
          if (installedPackageKeyboards != null) {
            notifyPackageInstallListeners(KeyboardEventHandler.EventType.PACKAGE_INSTALLED,
              installedPackageKeyboards, 1);
          }
          if(_cleanup)
            cleanup();
        } else {
          showErrorDialog(context, pkgId, getString(R.string.no_new_touch_keyboards_to_install));
        }
      } else if (pkgTarget.equals(PackageProcessor.PP_TARGET_LEXICAL_MODELS)) {
        List<Map<String, String>> installedLexicalModels =
          kmpProcessor.processKMP(kmpFile, tempPackagePath, PackageProcessor.PP_LEXICAL_MODELS_KEY);
        // Do the notifications
        boolean success = installedLexicalModels.size() != 0;
        boolean _cleanup = true;
        if (success) {
          if(!anSilentInstall)
            _cleanup = !loadWelcomePage(installedLexicalModels);
          notifyLexicalModelInstallListeners(KeyboardEventHandler.EventType.LEXICAL_MODEL_INSTALLED,
            installedLexicalModels, 1);
          if (installedLexicalModels != null) {
            notifyLexicalModelInstallListeners(KeyboardEventHandler.EventType.LEXICAL_MODEL_INSTALLED,
              installedLexicalModels, 1);
          }
          if(_cleanup)
            cleanup();
        } else {
          showErrorDialog(context, pkgId, getString(R.string.no_new_predictive_text_to_install));
        }
      }
    } catch (Exception e) {
      Log.e("PackageActivity", "Error " + e);
      showErrorDialog(context, pkgId, getString(R.string.no_targets_to_install));
    }
  }

  private void showErrorDialog(Context context, String pkgId, String message) {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

    alertDialogBuilder.setTitle(String.format("%s %s %s",
      getString(R.string.title_package), pkgId, getString(R.string.title_failed_to_install)));
    alertDialogBuilder
      .setMessage(message)
      .setCancelable(false)
      .setPositiveButton(getString(R.string.label_close),new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog,int id) {
          if (dialog != null) {
            dialog.dismiss();
          }
          cleanup();
        }
      });

    alertDialog = alertDialogBuilder.create();
    alertDialog.show();
  }

  void notifyPackageInstallListeners(KeyboardEventHandler.EventType eventType,
                                     List<Map<String, String>> keyboards, int result) {
    if (kbDownloadEventListeners != null) {
      KeyboardEventHandler.notifyListeners(kbDownloadEventListeners, eventType, keyboards, result);
    }
  }

  void notifyLexicalModelInstallListeners(KeyboardEventHandler.EventType eventType,
                                          List<Map<String, String>> models, int result) {
    if (kbDownloadEventListeners != null) {
      KeyboardEventHandler.notifyListeners(kbDownloadEventListeners, eventType, models, result);
    }
  }

  public static void addKeyboardDownloadEventListener(KeyboardEventHandler.OnKeyboardDownloadEventListener listener) {
    if (kbDownloadEventListeners == null) {
      kbDownloadEventListeners = new ArrayList<KeyboardEventHandler.OnKeyboardDownloadEventListener>();
    }

    if (listener != null && !kbDownloadEventListeners.contains(listener)) {
      kbDownloadEventListeners.add(listener);
    }
  }

  public static void removeKeyboardDownloadEventListener(KeyboardEventHandler.OnKeyboardDownloadEventListener listener) {
    if (kbDownloadEventListeners != null) {
      kbDownloadEventListeners.remove(listener);
    }
  }

}
