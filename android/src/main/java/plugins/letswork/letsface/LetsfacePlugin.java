package plugins.letswork.letsface;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import plugins.letswork.letsface.ui.CameraSource;

/** LetsfacePlugin */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class LetsfacePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

  private static final String CHANNEL = "letsface";

  private MethodChannel channel;
  private LetsFaceDelegate delegate;
  private FlutterPluginBinding pluginBinding;
  private ActivityPluginBinding activityBinding;
  private Activity activity;

  @SuppressWarnings("unused")
  public static void registerWith(Registrar registrar) {
    Activity activity = registrar.activity();
    LetsfacePlugin plugin = new LetsfacePlugin();
    plugin.setup(registrar.messenger(), activity, registrar, null);
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    pluginBinding = flutterPluginBinding;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    pluginBinding = null;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activityBinding = binding;
    setup(pluginBinding.getBinaryMessenger(),
            activityBinding.getActivity(),
            null,
            activityBinding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    tearDown();
  }

  private void setup(
          final BinaryMessenger messenger,
          final Activity activity,
          final PluginRegistry.Registrar registrar,
          final ActivityPluginBinding activityBinding) {
    this.activity = activity;
    this.delegate = new LetsFaceDelegate(activity);
    channel = new MethodChannel(messenger, CHANNEL);
    channel.setMethodCallHandler(this);
    if (registrar != null) {
      // V1 embedding setup for activity listeners.
      registrar.addActivityResultListener(delegate);
      registrar.addRequestPermissionsResultListener(delegate);
    } else {
      // V2 embedding setup for activity listeners.
      if(pluginBinding != null) {
        activityBinding.addActivityResultListener(delegate);
        activityBinding.addRequestPermissionsResultListener(delegate);
      }
    }
  }

  private void tearDown() {
    activityBinding.removeActivityResultListener(delegate);
    activityBinding.removeRequestPermissionsResultListener(delegate);
    activityBinding = null;
    delegate = null;
    channel.setMethodCallHandler(null);
    channel = null;
  }

  // MethodChannel.Result wrapper that responds on the platform thread.
  private static class MethodResultWrapper implements MethodChannel.Result {
    private MethodChannel.Result methodResult;
    private Handler handler;

    MethodResultWrapper(MethodChannel.Result result) {
      methodResult = result;
      handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void success(final Object result) {
      handler.post(
              new Runnable() {
                @Override
                public void run() {
                  methodResult.success(result);
                }
              });
    }

    @Override
    public void error(
            final String errorCode, final String errorMessage, final Object errorDetails) {
      handler.post(
              new Runnable() {
                @Override
                public void run() {
                  methodResult.error(errorCode, errorMessage, errorDetails);
                }
              });
    }

    @Override
    public void notImplemented() {
      handler.post(
              new Runnable() {
                @Override
                public void run() {
                  methodResult.notImplemented();
                }
              });
    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result rawResult) {
    if (activity == null) {
      rawResult.error("no_activity",
              "LetsFace plugin requires a foreground activity.",
              null);
      return;
    }

    Result result = new MethodResultWrapper(rawResult);


    if (call.method.equals("detectFace")) {
      String[] cameraNames = new String[0];
      int camera = CameraSource.CAMERA_FACING_FRONT;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
          cameraNames = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
          e.printStackTrace();
        }

        if(cameraNames.length == 1){
          camera = CameraSource.CAMERA_FACING_BACK;
        }
      }
      delegate.face(call, result, camera);
      //result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

}
