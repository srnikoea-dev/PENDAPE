package id.keramat.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER = 1001;
    private static final int CREATE_FILE = 1002;
    private String pendingContent;
    private byte[] pendingBytes;
    private String pendingMime = "application/json";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface public void saveJson(String content, String filename) {
                saveTextFile(content, filename, "application/json");
            }
            @JavascriptInterface public void saveFile(String content, String filename, String mime) {
                saveTextFile(content, filename, mime);
            }
            @JavascriptInterface public void saveBase64File(String content, String filename, String mime) {
                runOnUiThread(() -> {
                    try {
                        pendingBytes = Base64.decode(content, Base64.DEFAULT);
                        pendingContent = null;
                        pendingMime = safeMime(mime);
                        createDocument(filename);
                    } catch (Exception ignored) { }
                });
            }
        }, "AndroidBridge");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    String[] accept = params.getAcceptTypes();
                    String mime = chooseMime(accept);
                    intent.setType(mime);
                    if (accept != null && accept.length > 1 && "*/*".equals(mime)) {
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, accept);
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(Intent.createChooser(intent, "Pilih file"), FILE_CHOOSER);
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });
        webView.loadUrl("file:///android_asset/index.html");
    }

    private String chooseMime(String[] accept) {
        if (accept == null || accept.length == 0) return "*/*";
        for (String item : accept) {
            if (item == null) continue;
            String value = item.trim().toLowerCase();
            if (value.startsWith("image/")) return "image/*";
            if (value.contains("csv") || value.contains("text/")) return "text/*";
            if (value.contains("json")) return "application/json";
        }
        return "*/*";
    }

    private String safeMime(String mime) {
        if (mime == null || mime.trim().isEmpty()) return "application/octet-stream";
        return mime;
    }

    private void saveTextFile(String content, String filename, String mime) {
        runOnUiThread(() -> {
            pendingContent = content;
            pendingBytes = null;
            pendingMime = safeMime(mime);
            createDocument(filename);
        });
    }

    private void createDocument(String filename) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(pendingMime);
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, CREATE_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out != null) {
                        if (pendingBytes != null) out.write(pendingBytes);
                        else if (pendingContent != null) out.write(pendingContent.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Exception ignored) { }
            }
            pendingContent = null;
            pendingBytes = null;
        } else if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null
                    ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
