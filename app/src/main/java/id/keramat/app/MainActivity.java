package id.keramat.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
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

    // ===== Anti-curang =====
    private boolean isExamRunning = false;
    private MediaPlayer alarmPlayer;
private boolean isLocked = false;
private final String PASSWORD_PENGAWAS = "KERAMATJAYA";
private android.view.View lockScreenView;
private android.widget.EditText etPasswordPengawas;
private android.widget.TextView tvAlasanKecurangan;
    @Override
    public void onCreate(Bundle state) {
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
            @JavascriptInterface
            public void saveJson(String content, String filename) {
                saveTextFile(content, filename, "application/json");
            }

            @JavascriptInterface
            public void saveFile(String content, String filename, String mime) {
                saveTextFile(content, filename, mime);
            }

            @JavascriptInterface
            public void saveBase64File(String content, String filename, String mime) {
                runOnUiThread(() -> {
                    try {
                        pendingBytes = Base64.decode(content, Base64.DEFAULT);
                        pendingContent = null;
                        pendingMime = safeMime(mime);
                        createDocument(filename);
                    } catch (Exception ignored) { }
                });
            }

            // Dipanggil dari siswa.html saat ujian mulai
            @JavascriptInterface
            public void examStarted() {
                isExamRunning = true;
            }

            // Dipanggil dari siswa.html saat ujian selesai
            @JavascriptInterface
            public void examFinished() {
                isExamRunning = false;
                stopAlarm();
            }
        }, "AndroidBridge");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
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

    // Saat aplikasi kehilangan fokus (Home, Recent, dll)
    @Override
    protected void onPause() {
        super.onPause();
        if (isExamRunning) {
            startAlarm();
        }
    }

    // Saat kembali ke aplikasi
    @Override
    protected void onResume() {
        super.onResume();
        stopAlarm();
    }

    private void startAlarm() {
        stopAlarm(); // pastikan tidak double

        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            alarmPlayer = new MediaPlayer();
            alarmPlayer.setDataSource(this, alarmUri);
            alarmPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            alarmPlayer.setLooping(true);
            alarmPlayer.setVolume(1.0f, 1.0f);
            alarmPlayer.prepare();
            alarmPlayer.start();

            // Naikkan volume alarm ke maksimum
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0);
            }
        } catch (Exception e) {
            // fallback sederhana jika gagal
            try {
                Uri notif = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                alarmPlayer = MediaPlayer.create(this, notif);
                if (alarmPlayer != null) {
                    alarmPlayer.setLooping(true);
                    alarmPlayer.start();
                }
            } catch (Exception ignored) {}
        }
    }

    private void stopAlarm() {
        if (alarmPlayer != null) {
            try {
                if (alarmPlayer.isPlaying()) alarmPlayer.stop();
                alarmPlayer.release();
            } catch (Exception ignored) {}
            alarmPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }

    // ========== kode lama (file picker) ==========
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
// ... akhir isi metode onCreate atau metode lain di atasnya ...
    } // <-- Ini penutup metode onCreate (atau metode sebelumnya)

    // --- MULAI MENARUH METODE DI SINI (di luar onCreate, di dalam MainActivity) ---

    private void tampilkanHalamanLoginPengawas(String alasan) {
        // ... isi kode tampilkanHalamanLoginPengawas ...
    }

  @Override
    public void onBackPressed() {
        tampilkanHalamanLoginPengawas("Siswa menekan tombol BACK.");
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        isLocked = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLocked) {
            tampilkanHalamanLoginPengawas("Terdeteksi keluar aplikasi atau menekan tombol Home / Ringkasan.");
        }
    }

    private void tampilkanHalamanLoginPengawas(String alasan) {
        isLocked = true;

        if (lockScreenView == null) {
            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setGravity(android.view.Gravity.CENTER);
            layout.setPadding(60, 80, 60, 80);
            layout.setBackgroundColor(android.graphics.Color.parseColor("#0F172A"));
            layout.setClickable(true);
            layout.setFocusable(true);

            android.widget.TextView tvJudul = new android.widget.TextView(this);
            tvJudul.setText("LAYAR DIKUNCI - TERDETEKSI KECURANGAN");
            tvJudul.setTextColor(android.graphics.Color.parseColor("#F87171"));
            tvJudul.setTextSize(20f);
            tvJudul.setGravity(android.view.Gravity.CENTER);
            tvJudul.setTypeface(null, android.graphics.Typeface.BOLD);

            tvAlasanKecurangan = new android.widget.TextView(this);
            tvAlasanKecurangan.setTextColor(android.graphics.Color.parseColor("#E2E8F0"));
            tvAlasanKecurangan.setTextSize(14f);
            tvAlasanKecurangan.setGravity(android.view.Gravity.CENTER);
            tvAlasanKecurangan.setPadding(0, 20, 0, 40);

            android.widget.TextView tvLabel = new android.widget.TextView(this);
            tvLabel.setText("Login Pengawas untuk Membuka Ujian");
            tvLabel.setTextColor(android.graphics.Color.WHITE);
            tvLabel.setTextSize(15f);
            tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            tvLabel.setPadding(0, 0, 0, 12);

            etPasswordPengawas = new android.widget.EditText(this);
            etPasswordPengawas.setHint("Masukkan password pengawas");
            etPasswordPengawas.setHintTextColor(android.graphics.Color.parseColor("#94A3B8"));
            etPasswordPengawas.setTextColor(android.graphics.Color.WHITE);
            etPasswordPengawas.setBackgroundColor(android.graphics.Color.parseColor("#1E293B"));
            etPasswordPengawas.setPadding(30, 24, 30, 24);
            etPasswordPengawas.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            android.widget.TextView tvError = new android.widget.TextView(this);
            tvError.setTextColor(android.graphics.Color.parseColor("#FCA5A5"));
            tvError.setTextSize(13f);
            tvError.setPadding(0, 12, 0, 20);

            android.widget.Button btnBuka = new android.widget.Button(this);
            btnBuka.setText("Buka Kunci Layar");
            btnBuka.setBackgroundColor(android.graphics.Color.parseColor("#2563EB"));
            btnBuka.setTextColor(android.graphics.Color.WHITE);
            btnBuka.setPadding(30, 24, 30, 24);

            btnBuka.setOnClickListener(v -> {
                String inputPass = etPasswordPengawas.getText().toString().trim();
                if (inputPass.equals(PASSWORD_PENGAWAS)) {
                    isLocked = false;
                    etPasswordPengawas.setText("");
                    tvError.setText("");
                    lockScreenView.setVisibility(android.view.View.GONE);
                } else {
                    tvError.setText("Password pengawas salah! Ujian tetap terkunci.");
                }
            });

            layout.addView(tvJudul);
            layout.addView(tvAlasanKecurangan);
            layout.addView(tvLabel);
            layout.addView(etPasswordPengawas);
            layout.addView(tvError);
            layout.addView(btnBuka);

            lockScreenView = layout;
            addContentView(
                lockScreenView,
                new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            );
        }

        tvAlasanKecurangan.setText(alasan + "\nSilakan panggil pengawas untuk memasukkan password.");
        lockScreenView.setVisibility(android.view.View.VISIBLE);
        lockScreenView.bringToFront();
    }

} // <-- Penutup TERAKHIR class MainActivity
    }
}
