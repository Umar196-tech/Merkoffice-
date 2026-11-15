package com.merkoffice.app;

import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
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
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private WebView editor;

    private final ActivityResultLauncher<String[]> openDocx =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new java.io.IOException("Cannot open file");
                String html = DocxHelper.read(in);
                editor.evaluateJavascript("window.loadDocumentFromHtml(" +
                    org.json.JSONObject.quote(html) + ")", null);
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
                    String html = readJavascriptString(value);
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

    private final ActivityResultLauncher<String> saveHtml =
        registerForActivityResult(new ActivityResultContracts.CreateDocument("text/html"), uri -> {
            if (uri == null) return;
            editor.evaluateJavascript("window.getDocumentHtml()", value -> {
                try {
                    String body = readJavascriptString(value);
                    String document = "<!doctype html><html><head><meta charset=\"utf-8\">" +
                        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                        "<title>MerkOffice document</title><style>body{font-family:Arial,sans-serif;" +
                        "line-height:1.6;margin:40px;max-width:900px}img{max-width:100%}</style>" +
                        "</head><body>" + body + "</body></html>";
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out == null) throw new java.io.IOException("Unable to create file");
                        out.write(document.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    }
                    Toast.makeText(this, "Document exported successfully", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Export failed. Please try again.", Toast.LENGTH_LONG).show();
                }
            });
        });

    private static String readJavascriptString(String value) throws Exception {
        Object decoded = new org.json.JSONTokener(value).nextValue();
        return decoded instanceof String ? (String) decoded : "";
    }

    private class EditorBridge {
        @JavascriptInterface public void openWordFile() {
            runOnUiThread(() -> openDocx.launch(new String[]{
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            }));
        }
        @JavascriptInterface public void saveWordFile() {
            runOnUiThread(() -> saveDocx.launch("MerkOffice-document.docx"));
        }
        @JavascriptInterface public void saveHtmlFile() {
            runOnUiThread(() -> saveHtml.launch("MerkOffice-document.html"));
        }
        @JavascriptInterface public void printDocument() {
            runOnUiThread(() -> exportPdf(null));
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editor = findViewById(R.id.editor);
        editor.getSettings().setJavaScriptEnabled(true);
        editor.getSettings().setDomStorageEnabled(true);
        editor.getSettings().setAllowFileAccess(true);
        editor.addJavascriptInterface(new EditorBridge(), "MerkOffice");
        editor.setWebViewClient(new WebViewClient());
        editor.setWebChromeClient(new WebChromeClient());
        editor.loadUrl("file:///android_asset/index.html");
    }

    public void newDocument(android.view.View view) {
        editor.evaluateJavascript("window.newDocument()", null);
    }
    public void saveDocument(android.view.View view) {
        saveHtml.launch("MerkOffice-document.html");
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
        } else {
            Toast.makeText(this, "Printing is not available on this device", Toast.LENGTH_LONG).show();
        }
    }
}
