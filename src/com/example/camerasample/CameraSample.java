package com.example.camerasample;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

public class CameraSample extends Activity implements SensorEventListener,
		LocationListener {

	private MyView mView;
	private CameraView mCamera;
	/**
	 * Sensor Manager
	 */
	private SensorManager mSensorManager = null;
	/**
	 * Location Manager
	 */
	private LocationManager mLocationManager = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				0, 0, this);
		List<Sensor> sensors = mSensorManager
				.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sensors.size() > 0) {
			Sensor sensor = sensors.get(0);
			mSensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_FASTEST);
		}

		mCamera = new CameraView(this);
		setContentView(mCamera);

		mView = new MyView(this);
		addContentView(mView, new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		mCamera.setView(mView);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			mView.setOrientation("" + sensorEvent.values[0], ""
					+ sensorEvent.values[1], "" + sensorEvent.values[2]);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		mView.setGps("" + location.getLatitude(), "" + location.getLongitude());
		Location genpatsuLocation = new Location("genpatsu");
		genpatsuLocation.setLatitude(37.428524);
		genpatsuLocation.setLongitude(141.032867);

		float distance = location.distanceTo(genpatsuLocation);
		mView.setDistance("" + distance);

		float direction = location.bearingTo(genpatsuLocation);
		mView.setDirection("" + direction);
	}

	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}

	public void onDestroy() {
		super.onDestroy();
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
		}
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);
		}
	}
}

/**
 * CameraView
 */
class CameraView extends SurfaceView implements SurfaceHolder.Callback {
	private Camera mCamera;

	private View mView;
	private boolean mInProgress;

	public CameraView(Context context) {
		super(context);
		getHolder().addCallback(this);
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setView(View mView) {
		this.mView = mView;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Camera.Parameters parameters = mCamera.getParameters();
		List<Size> params = parameters.getSupportedPictureSizes();
		int wid = params.get(0).width;
		int hei = params.get(0).height;
		for (Size s : params) {
			if (wid < s.width) {
				wid = s.width;
				hei = s.height;
			}
		}
		List<Size> params2 = parameters.getSupportedPreviewSizes();
		int wid2 = params2.get(0).width;
		int hei2 = params2.get(0).height;
		for (Size s : params2) {
			if (wid2 < s.width) {
				wid2 = s.width;
				hei2 = s.height;
			}
		}
		parameters.setPictureSize(wid, hei);
		parameters.setPreviewSize(wid2, hei2);
		mCamera.setParameters(parameters);

		mCamera.startPreview();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (Exception e) {
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i("CAMERA", "surfaceDestroyed");

		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			setDrawingCacheEnabled(false);
			setDrawingCacheEnabled(true);

			mCamera.takePicture(new Camera.ShutterCallback() {

				@Override
				public void onShutter() {
				}

			}, null, new Camera.PictureCallback() {
				@Override
				public void onPictureTaken(byte[] data, Camera camera) {
					mView.setDrawingCacheEnabled(true);
					Bitmap viewBitmap = Bitmap.createBitmap(mView
							.getDrawingCache());
					mView.setDrawingCacheEnabled(false);

					int scale = getScale(data, viewBitmap.getWidth(),
							viewBitmap.getHeight());

					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = scale;

					Bitmap myBitmap = BitmapFactory.decodeByteArray(data, 0,
							data.length, options);

					Bitmap tmpBitmap = Bitmap.createBitmap(myBitmap.getWidth(),
							myBitmap.getHeight(), Bitmap.Config.ARGB_8888);

					Canvas canvas = new Canvas(tmpBitmap);

					canvas.drawBitmap(
							myBitmap,
							null,
							new Rect(0, 0, myBitmap.getWidth(), myBitmap
									.getHeight()), null);
					canvas.drawBitmap(viewBitmap, null, new Rect(0, 0,
							viewBitmap.getWidth(), viewBitmap.getHeight()),
							null);
					saveBitmapToSd(tmpBitmap);

					mCamera.startPreview();

				}
			});

		}

		setDrawingCacheEnabled(false);
		return false;
	}

	public int getScale(byte[] data, int width, int height) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, options);

		int scaleW = options.outWidth / width + 1;
		int scaleH = options.outHeight / height + 1;

		int scale = Math.max(scaleW, scaleH);
		options.inJustDecodeBounds = false;

		return scale;
	}

	public void saveBitmapToSd(Bitmap mBitmap) {
		try {
			File root = new File(Environment.getExternalStorageDirectory()
					.getPath() + "/DCIM/");

			Date mDate = new Date();
			SimpleDateFormat fileName = new SimpleDateFormat("yyyyMMdd_HHmmss");

			FileOutputStream fos = null;
			fos = new FileOutputStream(new File(root, fileName.format(mDate)
					+ ".jpg"));

			mBitmap.compress(CompressFormat.JPEG, 100, fos);

			fos.close();
		} catch (Exception e) {
			Log.e("Error", "" + e.toString());
		}
	}

}

class MyView extends View {

	private int x;

	private int y;

	private String roll;

	private String yaw;

	private String pitch;

	private String lat;

	private String lon;

	private String distance;

	private String direction;

	public MyView(Context context) {
		super(context);
	}

	public void setOrientation(String yaw, String pitch, String roll) {
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		invalidate();
	}

	public void setGps(String lat, String lon) {
		this.lat = lat;
		this.lon = lon;
		invalidate();
	}

	public void setDistance(String distance) {
		this.distance = distance;
		invalidate();
	}

	public void setDirection(String direction) {
		this.direction = direction;
		invalidate();
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawColor(Color.TRANSPARENT);

		Paint mainPaint = new Paint();
		mainPaint.setStyle(Paint.Style.FILL);
		mainPaint.setARGB(255, 255, 255, 100);

		canvas.drawText("" + yaw, 10, 10, mainPaint);
		canvas.drawText("" + roll, 10, 30, mainPaint);
		canvas.drawText("" + pitch, 10, 50, mainPaint);

		canvas.drawText("" + lat, 10, 100, mainPaint);
		canvas.drawText("" + lon, 10, 120, mainPaint);

		canvas.drawText("����:" + distance, 10, 160, mainPaint);
		canvas.drawText("��p:" + direction, 10, 180, mainPaint);
	}

}