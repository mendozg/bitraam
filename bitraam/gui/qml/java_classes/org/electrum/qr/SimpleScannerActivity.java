package org.electrum.qr;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.content.Intent;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.Arrays;

import de.markusfisch.android.barcodescannerview.widget.BarcodeScannerView;
import de.markusfisch.android.zxingcpp.ZxingCpp.Result;
import de.markusfisch.android.zxingcpp.ZxingCpp.ContentType;


import org.electrum.electrum.res.R; // package set in build.gradle

public class SimpleScannerActivity extends Activity {
    private static final int MY_PERMISSIONS_CAMERA = 1002;

    private BarcodeScannerView mScannerView = null;
    final String TAG = "org.electrum.qr.SimpleScannerActivity";

    private boolean mAlreadyRequestedPermissions = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanner_layout);

        // change top text
        Intent intent = getIntent();
        String text = intent.getStringExtra(intent.EXTRA_TEXT);
        TextView hintTextView = (TextView) findViewById(R.id.hint);
        hintTextView.setText(text);

        // bind "paste" button
        Button btn = (Button) findViewById(R.id.paste_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard.hasPrimaryClip()
                        && (clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            || clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    String clipboardText = item.getText().toString();
                    // limit size of content. avoid https://developer.android.com/reference/android/os/TransactionTooLargeException.html
                    if (clipboardText.length() >  512 * 1024) {
                        Toast.makeText(SimpleScannerActivity.this, "Clipboard contents too large.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SimpleScannerActivity.this.setResultAndClose(null, clipboardText);
                } else {
                    Toast.makeText(SimpleScannerActivity.this, "Clipboard is empty.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        setupEdgeToEdge();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.hasPermission()) {
            this.startCamera();
        } else if (!mAlreadyRequestedPermissions) {
            mAlreadyRequestedPermissions = true;
            this.requestPermission();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != mScannerView) {
            mScannerView.close();  // Stop camera on pause
        }
    }

    private void startCamera() {
        if (mScannerView == null) {
            mScannerView = new BarcodeScannerView(this);
            mScannerView.setCropRatio(0.75f); // Set crop ratio to 75% (this defines the square area shown in the scanner view)
            // by default only Format.QR_CODE is set
            ViewGroup contentFrame = (ViewGroup) findViewById(R.id.content_frame);
            contentFrame.addView(mScannerView);
            mScannerView.setOnBarcodeListener(result -> {
                // Handle the scan result
                this.setResultAndClose(result, null);
                // Return false to stop scanning after first result
                return false;
            });
        }
        mScannerView.openAsync();  // Start camera on resume
    }

    private void setResultAndClose(Result scanResult, String textOnly) {
        Intent resultIntent = new Intent();
        if (textOnly != null) {
            Log.v(TAG, "clipboard contentType TEXT");
            resultIntent.putExtra("text", textOnly);
        } else if (scanResult != null) {
            if (scanResult.getContentType() == ContentType.TEXT) {
                Log.v(TAG, "scanResult contentType TEXT");
                resultIntent.putExtra("text", scanResult.getText());
            } else if (scanResult.getContentType() == ContentType.BINARY) {
                Log.v(TAG, "scanResult contentType BINARY");
                resultIntent.putExtra("binary", scanResult.getRawBytes());
            } else {
                Log.v(TAG, "scanresult contenttype unknown");
            }
        }
        setResult(Activity.RESULT_OK, resultIntent);
        this.finish();
    }

    private boolean hasPermission() {
        return (ActivityCompat.checkSelfPermission(this,
                                                   Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_CAMERA: {
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    this.startCamera();
                } else {
                    // permission denied
                    //this.finish();
                }
                return;
            }
        }
    }

    private boolean enforcesEdgeToEdge() {
        // if true the UI needs to be padded to be e2e compatible
        return Build.VERSION.SDK_INT >= 35;
    }

    private void setupEdgeToEdge() {
        if (!enforcesEdgeToEdge()) {
            return;
        }

        // Get the root view and set up insets listener
        getWindow().getDecorView().setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
            
            // Apply padding to content frame to keep scanner focus area centered
            ViewGroup contentFrame = findViewById(R.id.content_frame);
            if (contentFrame != null) {
                contentFrame.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                );
            }

            // Apply top padding to hint text for status bar
            TextView hintTextView = findViewById(R.id.hint);
            if (hintTextView != null) {
                hintTextView.setPadding(
                    hintTextView.getPaddingLeft(),
                    systemBars.top,
                    hintTextView.getPaddingRight(),
                    hintTextView.getPaddingBottom()
                );
            }

            // Apply bottom margin to paste button for navigation bar  
            Button pasteButton = findViewById(R.id.paste_btn);
            if (pasteButton != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) pasteButton.getLayoutParams();
                params.bottomMargin = systemBars.bottom;
                pasteButton.setLayoutParams(params);
            }

            return insets;
        });
    }
}
