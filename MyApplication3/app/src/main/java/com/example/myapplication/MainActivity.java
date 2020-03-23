package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnCapture;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    // Second Camera
    private Button btnCapture2;
    private TextureView textureView2;
    private String cameraId2;
    private CameraDevice cameraDevice2;
    private CameraCaptureSession cameraCaptureSessions2;
    private CaptureRequest.Builder captureRequestBuilder2;
    private Size imageDimension2;
    private ImageReader imageReader2;
    private File file2;
    private boolean mFlashSupported2;
    private Handler mBackgroundHandler2;
    private HandlerThread mBackgroundThread2;

    CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview(true);

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (cameraDevice == null) {
                return;
            }
            cameraDevice.close();
            cameraDevice = null;

        }
    };
    CameraDevice.StateCallback stateCallBack2 = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice2 = camera;
            createCameraPreview(false);
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice2.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (cameraDevice2 == null) {
                return;
            }
            cameraDevice2.close();
            cameraDevice2 = null;
        }
    };
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = (TextureView) findViewById(R.id.textureView);
        textureView2 = (TextureView) findViewById(R.id.textureView2);
        assert textureView != null;
        assert textureView2 != null;
        textureView.setSurfaceTextureListener(textureListener);
        textureView2.setSurfaceTextureListener(textureListener2);

        btnCapture = (Button)findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture(true);
            }
        });
        btnCapture2 = (Button)findViewById(R.id.btnCapture2);
        btnCapture2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture(false);
            }
        });
    }

    private void openCamera(Boolean which)  {
        if (which) {
            CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
            try {
                cameraId = manager.getCameraIdList()[0];
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_CAMERA_PERMISSION);
                    return;
                }
                manager.openCamera(cameraId, stateCallBack, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else  {
            CameraManager manager2 = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
            try {
                cameraId2 = manager2.getCameraIdList()[1];
                CameraCharacteristics characteristics2 = manager2.getCameraCharacteristics(cameraId2);
                StreamConfigurationMap map2 = characteristics2.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map2 != null;
                imageDimension2 = map2.getOutputSizes(SurfaceTexture.class)[0];
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_CAMERA_PERMISSION);
                    return;
                }
                manager2.openCamera(cameraId2, stateCallBack2, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void createCameraPreview(Boolean which) {
        if (which) {
            try {
                SurfaceTexture texture = textureView.getSurfaceTexture();
                assert texture != null;
                texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
                Surface surface = new Surface(texture);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(surface);
                cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        if (cameraDevice == null) {
                            return;
                        }
                        cameraCaptureSessions = cameraCaptureSession;
                        updatePreview(true);

                    }
                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Toast.makeText(MainActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                    }
                }, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            try {
            SurfaceTexture texture2 = textureView2.getSurfaceTexture();
            assert texture2 != null;
            texture2.setDefaultBufferSize(imageDimension2.getWidth(), imageDimension2.getHeight());
            Surface surface2 = new Surface(texture2);
            captureRequestBuilder2 = cameraDevice2.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder2.addTarget(surface2);
            cameraDevice2.createCaptureSession(Arrays.asList(surface2), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice2 == null) {
                        return;
                    }
                    cameraCaptureSessions2 = cameraCaptureSession;
                    updatePreview(false);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        }
    }

    private void updatePreview(Boolean which) {
        if (which) {
            if (cameraDevice == null) {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            }
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            try {
                cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            if (cameraDevice2 == null) {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            }
            captureRequestBuilder2.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            try {
                cameraCaptureSessions2.setRepeatingRequest(captureRequestBuilder2.build(), null, mBackgroundHandler2);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void takePicture(Boolean which) {
        if (which) {
            if (cameraDevice == null) {
                return;
            }
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
                Size[] jpegSizes = null;
                if (characteristics != null) {
                    jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                }
                int width = 640;
                int height = 480;
                if (jpegSizes != null && jpegSizes.length > 0) {
                    width = jpegSizes[0].getWidth();
                    height = jpegSizes[0].getHeight();
                }
                final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                List<Surface> outputSurface = new ArrayList<>(2);
                outputSurface.add(reader.getSurface());
                outputSurface.add(new Surface(textureView.getSurfaceTexture()));
                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(reader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

                file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/FixAR.jpeg");
                ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        Image image = null;
                        try {
                            image = reader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            save(bytes);


                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null) {
                                image.close();
                            }
                        }

                    }

                    private void save(byte[] bytes) throws IOException {
                        OutputStream outputStream = null;
                        try {
                            outputStream = new FileOutputStream(file);
                            outputStream.write(bytes);
                        } finally {
                            if (outputStream != null) {
                                outputStream.close();
                            }
                        }

                    }
                };

                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
                final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Toast.makeText(MainActivity.this, "Saved " + file, Toast.LENGTH_SHORT).show();
                        createCameraPreview(true);
                    }
                };

                cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        try {
                            cameraCaptureSession.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    }
                }, mBackgroundHandler);


            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            if (cameraDevice2 == null) {
                return;
            }
            CameraManager manager2 = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
            try {
                CameraCharacteristics characteristics2 = manager2.getCameraCharacteristics(cameraDevice2.getId());
                Size[] jpegSizes2 = null;
                if (characteristics2 != null) {
                    jpegSizes2 = characteristics2.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                }
                int width2 = 640;
                int height2 = 480;
                if (jpegSizes2 != null && jpegSizes2.length > 0) {
                    width2 = jpegSizes2[0].getWidth();
                    height2 = jpegSizes2[0].getHeight();
                }
                final ImageReader reader2 = ImageReader.newInstance(width2, height2, ImageFormat.JPEG, 1);
                List<Surface> outputSurface2 = new ArrayList<>(2);
                outputSurface2.add(reader2.getSurface());
                outputSurface2.add(new Surface(textureView2.getSurfaceTexture()));
                final CaptureRequest.Builder captureBuilder2 = cameraDevice2.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder2.addTarget(reader2.getSurface());
                captureBuilder2.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                int rotation2 = getWindowManager().getDefaultDisplay().getRotation();
                captureBuilder2.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation2));

                file2 = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/FixAR2.jpeg");
                ImageReader.OnImageAvailableListener readerListener2 = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader2) {
                        Image image2 = null;
                        try {
                            image2 = reader2.acquireLatestImage();
                            ByteBuffer buffer2 = image2.getPlanes()[0].getBuffer();
                            byte[] bytes2 = new byte[buffer2.capacity()];
                            buffer2.get(bytes2);
                            save(bytes2);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image2 != null) {
                                image2.close();
                            }
                        }
                    }

                    private void save(byte[] bytes2) throws IOException {
                        OutputStream outputStream2 = null;
                        try {
                            outputStream2 = new FileOutputStream(file2);
                            outputStream2.write(bytes2);
                        } finally {
                            if (outputStream2 != null) {
                                outputStream2.close();
                            }
                        }

                    }
                };

                reader2.setOnImageAvailableListener(readerListener2, mBackgroundHandler2);
                final CameraCaptureSession.CaptureCallback captureListener2 = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Toast.makeText(MainActivity.this, "Saved " + file, Toast.LENGTH_SHORT).show();
                        createCameraPreview(false);
                    }
                };

                cameraDevice2.createCaptureSession(outputSurface2, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession2) {
                        try {
                            cameraCaptureSession2.capture(captureBuilder2.build(), captureListener2, mBackgroundHandler2);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    }
                }, mBackgroundHandler2);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(true);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    TextureView.SurfaceTextureListener textureListener2 = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(false);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You cant use camera without permission.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(true);
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
        if (textureView2.isAvailable()) {
            openCamera(false);
        } else {
            textureView2.setSurfaceTextureListener(textureListener2);
        }
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBackgroundThread2.quitSafely();
        try {
            mBackgroundThread2.join();
            mBackgroundThread2 = null;
            mBackgroundHandler2 = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundThread2 = new HandlerThread("Camera Background2");
        mBackgroundThread2.start();
        mBackgroundHandler2 = new Handler(mBackgroundThread2.getLooper());
    }
}
