package com.gclue.CameraSample;

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

		// Notification Bar������
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Title Bar������
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// SensorManager
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		// LocationManager��GPS�̒l���擾���邽�߂̐ݒ�
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// �l���ω������ۂɌĂяo����郊�X�i�[�̒ǉ�
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				0, 0, this);

		// Sensor�̎擾�ƃ��X�i�[�ւ̓o�^
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
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		// Log.i("SURFACE", "SensorChanged()");
		if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			// Log.i("SURFACE", "yaw:" + sensorEvent.values[0]);
			// Log.i("SURFACE", "picth:" + sensorEvent.values[1]);
			// Log.i("SURFACE", "roll:" + sensorEvent.values[2]);
			mView.setOrientation("" + sensorEvent.values[0], ""
					+ sensorEvent.values[1], "" + sensorEvent.values[2]);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		Log.i("GPS", "lat=" + location.getLatitude());
		Log.i("GPS", "lon=" + location.getLongitude());
		mView.setGps("" + location.getLatitude(), "" + location.getLongitude());

		// �������W
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
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

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
	/**
	 * Camera�̃C���X�^���X���i�[����ϐ�
	 */
	private Camera mCamera;

	/**
	 * MyView�̃C���X�^���X���i�[����ϐ�
	 */
	private View mView;
	private boolean mInProgress;

	public CameraView(Context context) {
		super(context);
		getHolder().addCallback(this);
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	/**
	 * MyView���󂯓n��
	 * 
	 * @param mView
	 *            MyView
	 */
	public void setView(View mView) {
		this.mView = mView;
	}

	/**
	 * Surface�ɕω����������ꍇ�ɌĂ΂��
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i("CAMERA", "surfaceChaged");

		// ��ʐݒ�
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

		// �v���r���[�\�����J�n
		mCamera.startPreview();
	}

	/**
	 * Surface���������ꂽ�ۂɌĂ΂��
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i("CAMERA", "surfaceCreated");

		// �J������Open
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (Exception e) {
		}
	}

	/**
	 * Surface���j�����ꂽ�ꍇ�ɌĂ΂��
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i("CAMERA", "surfaceDestroyed");

		// �J������Close
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
					Log.i("DEBUG", "onTouch");

					// �v���r���[�B�e��L���ɂ���
					mView.setDrawingCacheEnabled(true);
					Bitmap viewBitmap = Bitmap.createBitmap(mView
							.getDrawingCache());
					// �v���r���[�B�e�𖳌��ɂ���
					mView.setDrawingCacheEnabled(false);

					// �X�P�[�����擾
					int scale = getScale(data, viewBitmap.getWidth(),
							viewBitmap.getHeight());

					// Bitmap�������̃I�v�V�������쐬
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = scale;

					// �J�����摜����X�P�[���̒l��ݒ肵����ԂŃJ�����摜��Bitmap�`���Ŋi�[
					Bitmap myBitmap = BitmapFactory.decodeByteArray(data, 0,
							data.length, options);

					// �擾�����摜�T�C�Y�ɂ����T�C�Y��Bitmap���쐬(�����`�悳��Ă��Ȃ�)
					Bitmap tmpBitmap = Bitmap.createBitmap(myBitmap.getWidth(),
							myBitmap.getHeight(), Bitmap.Config.ARGB_8888);

					// tmpBitmap����L�����o�X���쐬
					Canvas canvas = new Canvas(tmpBitmap);

					// �쐬����Canvas��MyView�̃v���r���[�ƃJ�����̉摜���͂������
					canvas.drawBitmap(
							myBitmap,
							null,
							new Rect(0, 0, myBitmap.getWidth(), myBitmap
									.getHeight()), null);
					canvas.drawBitmap(viewBitmap, null, new Rect(0, 0,
							viewBitmap.getWidth(), viewBitmap.getHeight()),
							null);

					// sd�J�[�h�ɕۑ�
					saveBitmapToSd(tmpBitmap);

					// �v���r���[�\�����ĊJ
					mCamera.startPreview();

				}
			});

		}

		setDrawingCacheEnabled(false);
		return false;
	}

	/**
	 * �X�P�[�����擾����
	 * 
	 * @param data
	 *            �k�ڂ𒲍�����摜�f�[�^
	 * @param width
	 *            �ύX���鉡�̃T�C�Y
	 * @param height
	 *            �ύX����c�̃T�C�Y
	 * @return �ύX�������T�C�Y�̃X�P�[���̒l
	 */
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

	/**
	 * Bitmap�摜��sd�J�[�h�ɕۑ�
	 * 
	 * @param mBitmap
	 *            Bitmap�f�[�^
	 */
	public void saveBitmapToSd(Bitmap mBitmap) {
		try {
			// sdcard�t�H���_���w��
			File root = new File(Environment.getExternalStorageDirectory()
					.getPath() + "/DCIM/");

			// ���t�Ńt�@�C�������쐬�@
			Date mDate = new Date();
			SimpleDateFormat fileName = new SimpleDateFormat("yyyyMMdd_HHmmss");

			// �ۑ������J�n
			FileOutputStream fos = null;
			fos = new FileOutputStream(new File(root, fileName.format(mDate)
					+ ".jpg"));

			// jpeg�ŕۑ�
			mBitmap.compress(CompressFormat.JPEG, 100, fos);

			// �ۑ������I��
			fos.close();
		} catch (Exception e) {
			Log.e("Error", "" + e.toString());
		}
	}

}

/**
 * �I�[�o�[���C�`��p�̃N���X
 */
class MyView extends View {

	/**
	 * x���W
	 */
	private int x;

	/**
	 * y���W
	 */
	private int y;

	/**
	 * Roll
	 */
	private String roll;

	/**
	 * Yaw
	 */
	private String yaw;

	/**
	 * Pitch
	 */
	private String pitch;

	/**
	 * Lat
	 */
	private String lat;

	/**
	 * Lon
	 */
	private String lon;

	/**
	 * distance
	 */
	private String distance;

	/**
	 * direction
	 */
	private String direction;

	/**
	 * �R���X�g���N�^
	 * 
	 * @param context
	 */
	public MyView(Context context) {
		super(context);
	}

	/**
	 * �l��n��
	 */
	public void setOrientation(String yaw, String pitch, String roll) {
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		invalidate();
	}

	/**
	 * �l��n��(GPS)
	 */
	public void setGps(String lat, String lon) {
		this.lat = lat;
		this.lon = lon;
		invalidate();
	}

	/**
	 * �l��n��(Distance)
	 */
	public void setDistance(String distance) {
		this.distance = distance;
		invalidate();
	}

	/**
	 * �l��n��(Direction)
	 */
	public void setDirection(String direction) {
		this.direction = direction;
		invalidate();
	}

	/**
	 * �`�揈��
	 */
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// �w�i�F��ݒ�
		canvas.drawColor(Color.TRANSPARENT);

		// �`�悷�邽�߂̐��̐F��ݒ�
		Paint mainPaint = new Paint();
		mainPaint.setStyle(Paint.Style.FILL);
		mainPaint.setARGB(255, 255, 255, 100);

		// ������`��
		canvas.drawText("" + yaw, 10, 10, mainPaint);
		canvas.drawText("" + roll, 10, 30, mainPaint);
		canvas.drawText("" + pitch, 10, 50, mainPaint);

		canvas.drawText("" + lat, 10, 100, mainPaint);
		canvas.drawText("" + lon, 10, 120, mainPaint);

		canvas.drawText("����:" + distance, 10, 160, mainPaint);
		canvas.drawText("���p:" + direction, 10, 180, mainPaint);
	}

}