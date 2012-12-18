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

		// Notification Barを消す
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Title Barを消す
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// SensorManager
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		// LocationManagerでGPSの値を取得するための設定
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// 値が変化した際に呼び出されるリスナーの追加
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				0, 0, this);

		// Sensorの取得とリスナーへの登録
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

		// 原発座標
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
	 * Cameraのインスタンスを格納する変数
	 */
	private Camera mCamera;

	/**
	 * MyViewのインスタンスを格納する変数
	 */
	private View mView;
	private boolean mInProgress;

	public CameraView(Context context) {
		super(context);
		getHolder().addCallback(this);
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	/**
	 * MyViewを受け渡す
	 * 
	 * @param mView
	 *            MyView
	 */
	public void setView(View mView) {
		this.mView = mView;
	}

	/**
	 * Surfaceに変化があった場合に呼ばれる
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i("CAMERA", "surfaceChaged");

		// 画面設定
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

		// プレビュー表示を開始
		mCamera.startPreview();
	}

	/**
	 * Surfaceが生成された際に呼ばれる
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i("CAMERA", "surfaceCreated");

		// カメラをOpen
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (Exception e) {
		}
	}

	/**
	 * Surfaceが破棄された場合に呼ばれる
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i("CAMERA", "surfaceDestroyed");

		// カメラをClose
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

					// プレビュー撮影を有効にする
					mView.setDrawingCacheEnabled(true);
					Bitmap viewBitmap = Bitmap.createBitmap(mView
							.getDrawingCache());
					// プレビュー撮影を無効にする
					mView.setDrawingCacheEnabled(false);

					// スケールを取得
					int scale = getScale(data, viewBitmap.getWidth(),
							viewBitmap.getHeight());

					// Bitmap生成時のオプションを作成
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = scale;

					// カメラ画像からスケールの値を設定した状態でカメラ画像をBitmap形式で格納
					Bitmap myBitmap = BitmapFactory.decodeByteArray(data, 0,
							data.length, options);

					// 取得した画像サイズにあうサイズのBitmapを作成(何も描画されていない)
					Bitmap tmpBitmap = Bitmap.createBitmap(myBitmap.getWidth(),
							myBitmap.getHeight(), Bitmap.Config.ARGB_8888);

					// tmpBitmapからキャンバスを作成
					Canvas canvas = new Canvas(tmpBitmap);

					// 作成したCanvasにMyViewのプレビューとカメラの画像をはりつけ合成
					canvas.drawBitmap(
							myBitmap,
							null,
							new Rect(0, 0, myBitmap.getWidth(), myBitmap
									.getHeight()), null);
					canvas.drawBitmap(viewBitmap, null, new Rect(0, 0,
							viewBitmap.getWidth(), viewBitmap.getHeight()),
							null);

					// sdカードに保存
					saveBitmapToSd(tmpBitmap);

					// プレビュー表示を再開
					mCamera.startPreview();

				}
			});

		}

		setDrawingCacheEnabled(false);
		return false;
	}

	/**
	 * スケールを取得する
	 * 
	 * @param data
	 *            縮尺を調査する画像データ
	 * @param width
	 *            変更する横のサイズ
	 * @param height
	 *            変更する縦のサイズ
	 * @return 変更したいサイズのスケールの値
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
	 * Bitmap画像をsdカードに保存
	 * 
	 * @param mBitmap
	 *            Bitmapデータ
	 */
	public void saveBitmapToSd(Bitmap mBitmap) {
		try {
			// sdcardフォルダを指定
			File root = new File(Environment.getExternalStorageDirectory()
					.getPath() + "/DCIM/");

			// 日付でファイル名を作成　
			Date mDate = new Date();
			SimpleDateFormat fileName = new SimpleDateFormat("yyyyMMdd_HHmmss");

			// 保存処理開始
			FileOutputStream fos = null;
			fos = new FileOutputStream(new File(root, fileName.format(mDate)
					+ ".jpg"));

			// jpegで保存
			mBitmap.compress(CompressFormat.JPEG, 100, fos);

			// 保存処理終了
			fos.close();
		} catch (Exception e) {
			Log.e("Error", "" + e.toString());
		}
	}

}

/**
 * オーバーレイ描画用のクラス
 */
class MyView extends View {

	/**
	 * x座標
	 */
	private int x;

	/**
	 * y座標
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
	 * コンストラクタ
	 * 
	 * @param context
	 */
	public MyView(Context context) {
		super(context);
	}

	/**
	 * 値を渡す
	 */
	public void setOrientation(String yaw, String pitch, String roll) {
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		invalidate();
	}

	/**
	 * 値を渡す(GPS)
	 */
	public void setGps(String lat, String lon) {
		this.lat = lat;
		this.lon = lon;
		invalidate();
	}

	/**
	 * 値を渡す(Distance)
	 */
	public void setDistance(String distance) {
		this.distance = distance;
		invalidate();
	}

	/**
	 * 値を渡す(Direction)
	 */
	public void setDirection(String direction) {
		this.direction = direction;
		invalidate();
	}

	/**
	 * 描画処理
	 */
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// 背景色を設定
		canvas.drawColor(Color.TRANSPARENT);

		// 描画するための線の色を設定
		Paint mainPaint = new Paint();
		mainPaint.setStyle(Paint.Style.FILL);
		mainPaint.setARGB(255, 255, 255, 100);

		// 文字を描画
		canvas.drawText("" + yaw, 10, 10, mainPaint);
		canvas.drawText("" + roll, 10, 30, mainPaint);
		canvas.drawText("" + pitch, 10, 50, mainPaint);

		canvas.drawText("" + lat, 10, 100, mainPaint);
		canvas.drawText("" + lon, 10, 120, mainPaint);

		canvas.drawText("距離:" + distance, 10, 160, mainPaint);
		canvas.drawText("方角:" + direction, 10, 180, mainPaint);
	}

}