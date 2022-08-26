package plugins.letswork.letsface.face;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;

import android.util.Base64;
import android.util.DisplayMetrics;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Objects;

import plugins.letswork.letsface.ui.CameraSource;
import plugins.letswork.letsface.util.AbstractCaptureActivity;
import plugins.letswork.letsface.util.LetsFaceException;


public final class FaceCaptureActivity extends AbstractCaptureActivity<FaceGraphic> {
    public String pictureBase64 = "";
    ArrayList<Object> list = new ArrayList<>();
    private boolean mFaceDetector;
    int imgQuality = 90;


    @SuppressLint("InlinedApi")
    protected void createCameraSource() throws LetsFaceException {
        Context context = getApplicationContext();

        int orientation = context.getResources().getConfiguration().orientation;


        FaceDetector faceDetector = new FaceDetector.Builder(context)
                .setMinFaceSize((orientation == Configuration.ORIENTATION_LANDSCAPE ? 0.6f : 0.9f))
                //.setLandmarkType(ALL_LANDMARKS)
                //.setMode(ACCURATE_MODE)
                .setProminentFaceOnly(true)
                //.setClassificationType(ACCURATE_MODE)
                .build();
        try {
            FaceTrackerFactory faceTrackerFactory = new FaceTrackerFactory(graphicOverlay, showText);

            faceDetector.setProcessor(
                new MultiProcessor.Builder<>(faceTrackerFactory).build());

            mFaceDetector = faceDetector.isOperational();
            //mFaceDetector = false;
            if (!faceDetector.isOperational()) {
                IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

                if (hasLowStorage) {
                    throw new LetsFaceException("Low Storage.");
                }
            }
        } catch (Exception e){
            mFaceDetector = false;
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                throw new LetsFaceException("Low Storage.");
            }
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        cameraSource = new CameraSource
            .Builder(getApplicationContext(), faceDetector)
            .setFacing(camera)
            .setRequestedPreviewSize(metrics.heightPixels, metrics.widthPixels)
            .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
            .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
            .setRequestedFps(15.0f)
            .build();


        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;
        //Log.d(TAG, String.valueOf(availableMegs));

        if(availableMegs < 400){
            imgQuality = 85;
        }
        if(availableMegs < 250){
            imgQuality = 80;
        }

        if(mFaceDetector) {
            new android.os.Handler().postDelayed(
                    this::screenShot,
                    300);
        } else {
            if(availableMegs < 150) {
                AlertDialog.Builder builderAlert = new AlertDialog.Builder(this);
                builderAlert.setTitle("Atenção:");
                builderAlert.setMessage("Seu celular está utilizando quase toda a memória de seu aparelho e isso pode causar lentidão ou travamento do aplicativo. Feche outros aplicativos para fazer um reconhecimento facial bem sucedido.");
                builderAlert.setPositiveButton("OK", (dialog, which) -> finish());
                builderAlert.create();
                builderAlert.show();
            } else {
                graphicOverlay.setShowText("Fique bem próximo da câmera.");
                new android.os.Handler().postDelayed(
                        this::screenShotRaw,
                    4000);
            }
        }

    }

    public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.NO_WRAP);
    }

    private void takePictureHandle(byte[] bytes){
        try {
            int orientation = Exif.getOrientation(bytes);

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            switch(orientation) {
                case 90:
                    bitmap= rotateImage(bitmap, 90);
                    break;
                case 180:
                    bitmap= rotateImage(bitmap, 180);
                    break;
                case 270:
                    bitmap= rotateImage(bitmap, 270);
                    break;
                case 0:
                    // if orientation is zero we don't need to rotate this

                default:
                    break;
            }

            int maxSize = 720;

            int outWidth;
            int outHeight;
            int inWidth = bitmap.getWidth();
            int inHeight = bitmap.getHeight();
            if(inWidth > inHeight){
                outWidth = maxSize;
                outHeight = (inHeight * maxSize) / inWidth;
            } else {
                outHeight = maxSize;
                outWidth = (inWidth * maxSize) / inHeight;
            }


            if(outHeight < 600){
                imgQuality = 95;
            }

            Bitmap imgResized = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);
            pictureBase64 = encodeToBase64(imgResized, Bitmap.CompressFormat.JPEG, imgQuality);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void screenShotRaw(){

        graphicOverlay.setShowText(null);
        cameraSource.takePicture(null, this::takePictureHandle);

        checkImg();
    }

    private void screenShot(){
        for (FaceGraphic graphic : graphicOverlay.getGraphics()) {
            list.add(new MyFace(graphic.getFace()));
        }

        if (!list.isEmpty() || !mFaceDetector) {
            graphicOverlay.setShowText(null);
            cameraSource.takePicture(null, this::takePictureHandle);

            checkImg();


        } else {

            new android.os.Handler().postDelayed(
                    () -> {
                        graphicOverlay.setShowText("Se aproxime um pouco mais.");
                        screenShot();
                    },
                1500);
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(),   source.getHeight(), matrix,
            true);
    }

    private void checkImg() {

        if(!Objects.equals(pictureBase64, "")){
            Intent data = new Intent();
            data.putExtra("imgBase64", pictureBase64);
            setResult(CommonStatusCodes.SUCCESS, data);
            finish();
        } else {
            new android.os.Handler().postDelayed(
                    this::checkImg,
                300);
        }
    }


    protected boolean onTap(float rawX, float rawY) {
        return false;
    }
}

