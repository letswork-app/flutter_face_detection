package plugins.letswork.letsface;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.common.api.CommonStatusCodes;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;
import plugins.letswork.letsface.face.FaceCaptureActivity;
import plugins.letswork.letsface.ui.CameraSource;
import plugins.letswork.letsface.util.AbstractCaptureActivity;

public class LetsFaceDelegate implements ActivityResultListener, RequestPermissionsResultListener {
    private static final int RC_FACE_DETECT = 7030;

    private static final int REQUEST_CAMERA_PERMISSION = 2345;

    private int callerId;
    private MethodChannel.Result pendingResult;
    private MethodCall methodCall;

    private final Activity activity;

    private boolean useFlash = false;
    private boolean autoFocus = true;
    private boolean multiple = false;
    private boolean waitTap = false;
    private boolean showText = false;
    private int previewWidth = 640;
    private int previewHeight = 480;
    private float fps = 15.0f;


    LetsFaceDelegate(final Activity activity) {
        this.activity = activity;
    }

    private boolean isCameraPermissionGranted() {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void askForCameraPermission() {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    private boolean needRequestCameraPermission() {
        return LetsFaceUtils.needRequestCameraPermission(activity);
    }

    void face(MethodCall methodCall, MethodChannel.Result result, int camera) {
        if (prepare(methodCall, result)) {
            launchIntent(camera);
        }
    }
    private boolean prepare(MethodCall methodCall, MethodChannel.Result result) {
        if (!setPendingMethodCallAndResult(methodCall, result)) {
            finishWithAlreadyActiveError(result);
            return false;
        }

        if (needRequestCameraPermission() && !isCameraPermissionGranted()) {
            askForCameraPermission();
            return false;
        }

        return true;
    }

    private void launchIntent(int camera) {
        Intent intent = new Intent(activity, FaceCaptureActivity.class);

        intent.putExtra(AbstractCaptureActivity.AUTO_FOCUS, autoFocus);
        intent.putExtra(AbstractCaptureActivity.USE_FLASH, useFlash);
        intent.putExtra(AbstractCaptureActivity.MULTIPLE, multiple);
        intent.putExtra(AbstractCaptureActivity.WAIT_TAP, waitTap);
        intent.putExtra(AbstractCaptureActivity.SHOW_TEXT, showText);
        intent.putExtra(AbstractCaptureActivity.PREVIEW_WIDTH, previewWidth);
        intent.putExtra(AbstractCaptureActivity.PREVIEW_HEIGHT, previewHeight);

        intent.putExtra(AbstractCaptureActivity.CAMERA, camera);

        activity.startActivityForResult(intent, callerId);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == CommonStatusCodes.SUCCESS) {
            if (intent != null) {
                Bundle extras = intent.getExtras();
                finishWithSuccess(extras.getString("imgBase64"));
                return true;
            }
            finishWithError("No face detected, intent data is null", null);
        }
        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION) {
            boolean permissionGranted = grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (permissionGranted) {

                launchIntent(CameraSource.CAMERA_FACING_FRONT);
                return true;
            }

            finishWithError("camera_access_denied", "The user did not allow camera access.");
        }
        return false;
    }

    private boolean setPendingMethodCallAndResult(MethodCall methodCall,
                                                  MethodChannel.Result result) {
        if (pendingResult != null) {
            return false;
        }

        this.callerId = LetsFaceDelegate.RC_FACE_DETECT;
        this.methodCall = methodCall;
        pendingResult = result;

        return true;
    }

    private void finishWithAlreadyActiveError(MethodChannel.Result result) {
        result.error("already_active",
                "Flutter Mobile Vision is already active.",
                null);
    }

    private void finishWithSuccess(String imgBase64) {
        pendingResult.success(imgBase64);
        clearMethodCallAndResult();
    }

    private void finishWithError(String errorCode, String errorMessage) {
        if (pendingResult == null) {
            // TODO - Return an error.
            return;
        }

        pendingResult.error(errorCode, errorMessage, null);
        clearMethodCallAndResult();
    }

    private void clearMethodCallAndResult() {
        callerId = 0;
        methodCall = null;
        pendingResult = null;
    }

}
