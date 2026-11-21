package id.keramat.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private final String PASSWORD_PENGAWAS = "PENGAWAS2026"; // Ganti sesuai password pengawas
    private View lockScreenView;
    private EditText etPasswordPengawas;
    private TextView tvAlasanKecurangan;
    private TextView tvErrorPengawas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(
            webView,
            new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        );

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams
            ) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        isExamRunning = true;
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER) {
            if (fileCallback != null) {
                Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                fileCallback.onReceiveValue(results);
                fileCallback = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        tampilkanHalamanLoginPengawas("Siswa menekan tombol BACK.");
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isExamRunning) {
            isLocked = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isExamRunning && isLocked) {
            tampilkanHalamanLoginPengawas("Terdeteksi keluar aplikasi atau menekan tombol Home / Ringkasan.");
        }
    }

    @Override
    protected void onDestroy() {
        if (alarmPlayer != null) {
            try {
                if (alarmPlayer.isPlaying()) {
                    alarmPlayer.stop();
                }
                alarmPlayer.release();
            } catch (Exception ignored) {
            }
            alarmPlayer = null;
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private void tampilkanHalamanLoginPengawas(String alasan) {
        isLocked = true;

        if (lockScreenView == null) {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(60, 80, 60, 80);
            layout.setBackgroundColor(Color.parseColor("#0F172A"));
            layout.setClickable(true);
            layout.setFocusable(true);

            TextView tvJudul = new TextView(this);
            tvJudul.setText("LAYAR DIKUNCI - TERDETEKSI KECURANGAN");
            tvJudul.setTextColor(Color.parseColor("#F87171"));
            tvJudul.setTextSize(20f);
            tvJudul.setGravity(Gravity.CENTER);
            tvJudul.setTypeface(null, Typeface.BOLD);

            tvAlasanKecurangan = new TextView(this);
            tvAlasanKecurangan.setTextColor(Color.parseColor("#E2E8F0"));
            tvAlasanKecurangan.setTextSize(14f);
            tvAlasanKecurangan.setGravity(Gravity.CENTER);
            tvAlasanKecurangan.setPadding(0, 20, 0, 40);

            TextView tvLabel = new TextView(this);
            tvLabel.setText("Login Pengawas untuk Membuka Ujian");
            tvLabel.setTextColor(Color.WHITE);
            tvLabel.setTextSize(15f);
            tvLabel.setTypeface(null, Typeface.BOLD);
            tvLabel.setPadding(0, 0, 0, 12);

            etPasswordPengawas = new EditText(this);
            etPasswordPengawas.setHint("Masukkan password pengawas");
            etPasswordPengawas.setHintTextColor(Color.parseColor("#94A3B8"));
            etPasswordPengawas.setTextColor(Color.WHITE);
            etPasswordPengawas.setBackgroundColor(Color.parseColor("#1E293B"));
            etPasswordPengawas.setPadding(30, 24, 30, 24);
            etPasswordPengawas.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            tvErrorPengawas = new TextView(this);
            tvErrorPengawas.setTextColor(Color.parseColor("#FCA5A5"));
            tvErrorPengawas.setTextSize(13f);
            tvErrorPengawas.setPadding(0, 12, 0, 20);

            Button btnBuka = new Button(this);
            btnBuka.setText("Buka Kunci Layar");
            btnBuka.setBackgroundColor(Color.parseColor("#2563EB"));
            btnBuka.setTextColor(Color.WHITE);
            btnBuka.setPadding(30, 24, 30, 24);

            btnBuka.setOnClickListener(v -> {
                String inputPass = etPasswordPengawas.getText().toString().trim();
                if (inputPass.equals(PASSWORD_PENGAWAS)) {
                    isLocked = false;
                    etPasswordPengawas.setText("");
                    tvErrorPengawas.setText("");
                    lockScreenView.setVisibility(View.GONE);
                } else {
                    tvErrorPengawas.setText("Password pengawas salah! Ujian tetap terkunci.");
                }
            });

            layout.addView(tvJudul);
            layout.addView(tvAlasanKecurangan);
            layout.addView(tvLabel);
            layout.addView(etPasswordPengawas);
            layout.addView(tvErrorPengawas);
            layout.addView(btnBuka);

            lockScreenView = layout;
            addContentView(
                lockScreenView,
                new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            );
        }

        tvAlasanKecurangan.setText(alasan + "\nSilakan panggil pengawas untuk memasukkan password.");
        lockScreenView.setVisibility(View.VISIBLE);
        lockScreenView.bringToFront();
    }
}
