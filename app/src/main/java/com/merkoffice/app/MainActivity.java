package com.merkoffice.app;

import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private WebView editor;

    private final ActivityResultLauncher<String[]> openDocx =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                String html = DocxHelper.read(in);
                editor.evaluateJavascript("window.loadDocumentFromHtml(" + org.json.JSONObject.quote(html) + ")", null);
                Toast.makeText(this, "Word document opened", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not open this Word document", Toast.LENGTH_LONG).show();
            }
        });

    private final ActivityResultLauncher<String> saveDocx =
        registerForActivityResult(new ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"), uri -> {
            if (uri == null) return;
            editor.evaluateJavascript("window.getDocumentHtml()", value -> {
                try {
                    Object decoded = new org.json.JSONTokener(value).nextValue();
                    String html = decoded instanceof String ? (String) decoded : "";
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out == null) throw new java.io.IOException("Unable to create file");
                        DocxHelper.write(html, out);
                    }
                    Toast.makeText(this, "Word document saved", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Could not save Word document", Toast.LENGTH_LONG).show();
                }
            });
        });

    private class EditorBridge {
        @JavascriptInterface public void openWordFile() {
            runOnUiThread(() -> openDocx.launch(new String[]{
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            }));
        }
        @JavascriptInterface public void saveWordFile() {
            runOnUiThread(() -> saveDocx.launch("MerkOffice-document.docx"));
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editor = findViewById(R.id.editor);
        editor.getSettings().setJavaScriptEnabled(true);
        editor.getSettings().setDomStorageEnabled(true);
        editor.addJavascriptInterface(new EditorBridge(), "MerkOffice");
        editor.setWebViewClient(new WebViewClient());
        editor.setWebChromeClient(new WebChromeClient());
        editor.loadUrl("file:///android_asset/index.html");
    }

    public void newDocument(android.view.View view) {
        editor.evaluateJavascript("window.newDocument()", null);
    }
    public void saveDocument(android.view.View view) {
        editor.evaluateJavascript("window.getDocumentHtmlBase64()", value -> {
            Toast.makeText(this, "Use Save Word to create a .docx file", Toast.LENGTH_SHORT).show();
        });
    }
    public void openDocument(android.view.View view) {
        openDocx.launch(new String[]{"application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
    }
    public void saveWordDocument(android.view.View view) {
        saveDocx.launch("MerkOffice-document.docx");
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