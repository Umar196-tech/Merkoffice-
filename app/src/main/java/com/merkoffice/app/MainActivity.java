package com.merkoffice.app;

import android.app.Activity;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private WebView editor;
    private ValueCallback<Uri[]> fileCallback;

    private final ActivityResultLauncher<String> openFile =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) content.append(line);
                String encoded = android.util.Base64.encodeToString(
                        content.toString().getBytes(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
                editor.evaluateJavascript("window.loadDocumentFromBase64('" + encoded + "')", null);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open this file", Toast.LENGTH_SHORT).show();
            }
        });

    private final ActivityResultLauncher<String> createFile =
        registerForActivityResult(new ActivityResultContracts.CreateDocument("text/html"), uri -> {
            if (uri == null) return;
            editor.evaluateJavascript("window.getDocumentHtmlBase64()", value -> {
                try {
                    String encoded = value == null ? "" : value.replace("\"", "");
                    byte[] data = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT);
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out != null) out.write(data);
                    }
                    Toast.makeText(this, "Document saved", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Could not save document", Toast.LENGTH_SHORT).show();
                }
            });
        });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editor = findViewById(R.id.editor);
        editor.getSettings().setJavaScriptEnabled(true);
        editor.getSettings().setDomStorageEnabled(true);
        editor.setWebViewClient(new WebViewClient());
        editor.setWebChromeClient(new WebChromeClient());
        editor.loadUrl("file:///android_asset/index.html");
    }

    public void newDocument(android.view.View view) {
        editor.evaluateJavascript("window.newDocument()", null);
    }
    public void saveDocument(android.view.View view) {
        createFile.launch("MerkOffice-document.html");
    }
    public void openDocument(android.view.View view) {
        openFile.launch("text/*");
    }
    public void exportPdf(android.view.View view) {
        PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
        if (printManager != null) {
            printManager.print("MerkOffice document", editor.createPrintDocumentAdapter("MerkOffice"),
                    new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build());
        }
    }
}
