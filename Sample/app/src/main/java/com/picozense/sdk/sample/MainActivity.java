package com.picozense.sdk.sample;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;

import android.widget.Spinner;
import android.widget.Toast;
import com.picozense.sdk.IFrameCallback;
import com.picozense.sdk.PsFrame;
import com.picozense.sdk.PsCamera;
import com.picozense.sdk.PsFrame.DataType;
import android.content.pm.PackageManager;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import com.picozense.sdk.PsCamera.OnPicoCameraConnectListener;

public class MainActivity extends AppCompatActivity {
	private static final boolean DEBUG = true;
	private static final String TAG = "Activity";
	private final Object mSync = new Object();
	private PsCamera mPicoCamera;
	private FrameCallback mFrameCallback = null;
	ByteBuffer mdepthBmpBuf;
    ByteBuffer mIrBmpBuf;
	ByteBuffer mRgbBmpBuf;
	MyRenderer myrender;
	private GLSurfaceView mGlview;
	private Spinner sp_mView = null;
	private Spinner sp_mData = null;
	private Spinner sp_mResolution = null;
	private Spinner sp_mPara = null;
	private EditText mEditPara = null;
	private CheckBox ck_MapRgb;
	private CheckBox ck_MapDepth;
	private CheckBox ck_MapIr;
	boolean showDepth = true;
	boolean showIr = false;
	boolean showRgb = false;
	boolean isSpDataFirst = true;
	boolean isSpViewFirst = true;
	boolean isSpResolutionFirst = true;
	boolean isSpParaFirst = true;
	Bitmap bmpDepth;
    Bitmap bmpIr;
	Bitmap bmpRgb;
	private int resolutionIndex = 0;
	private int currentParaType = 0;
	private DataType  dataType = DataType.DATA_TYPE_DEPTH_RGB_30;
	private boolean bMapDepthToRgbEnabled = false;
	private boolean bMapRgbToDepthEnabled = false;
	private boolean bMapRgbToIrEnabled = false;
	String[] viewType = { "DEPTH","IR", "RGB"};
	String[] resolutionString = { "640*480","640*360", "1280*720","1920*1080"};
	String[] paraType = { "DepthRange","PulseCount", "Threshold"};
	String[] dataType_sp = { "DEPTH_RGB_30","DEPTH_30","IR_30", "RGB_30","DEPTH_IR_30","IR_RGB_30","DEPTH_IR_RGB_30","DEPTH_60","IR_60","DATA_TYPE_DEPTH_IR_15_RGB_30"};
	int startTime = 0;
	int endTime = 0;
	int fps = 0;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        String android_version = android.os.Build.VERSION.RELEASE;
        int version = Integer.parseInt(android_version.substring(0,1));
        if(version >= 6 ) {
            int permission = this.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mGlview = (GLSurfaceView)findViewById(R.id.glv_main);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		lp.height = dm.heightPixels;
		lp.width = lp.height*4/3;
		lp.gravity = Gravity.CENTER;
	    mGlview.setLayoutParams(lp);
		myrender = new MyRenderer(this);
		mGlview.setEGLContextClientVersion(2);
		mGlview.setRenderer(myrender);
		mGlview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		ck_MapRgb = (CheckBox) findViewById(R.id.map_rgb);
		ck_MapRgb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					mPicoCamera.setMapperEnabledDepthToRGB(true);
					bMapDepthToRgbEnabled = true;

					mPicoCamera.setMapperEnabledRGBToDepth(false);
					bMapRgbToDepthEnabled = false;
					ck_MapDepth.setChecked(false);
					mPicoCamera.setMapperEnabledRGBToIR(false);
					bMapRgbToIrEnabled = false;
					ck_MapIr.setChecked(false);

				}else{
					mPicoCamera.setMapperEnabledDepthToRGB(false);
					bMapDepthToRgbEnabled = false;
				}
			}
		});

		ck_MapDepth = (CheckBox) findViewById(R.id.map_depth);
		ck_MapDepth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if(isChecked){
				mPicoCamera.setMapperEnabledRGBToDepth(true);
				bMapRgbToDepthEnabled = true;

				mPicoCamera.setMapperEnabledDepthToRGB(false);
				bMapDepthToRgbEnabled = false;
				ck_MapRgb.setChecked(false);

			}else{
				mPicoCamera.setMapperEnabledRGBToDepth(false);
				bMapRgbToDepthEnabled = false;

			}
			}
		});

		ck_MapIr = (CheckBox) findViewById(R.id.map_ir);
		ck_MapIr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					mPicoCamera.setMapperEnabledRGBToIR(true);
					bMapRgbToIrEnabled = true;

					mPicoCamera.setMapperEnabledDepthToRGB(false);
					bMapDepthToRgbEnabled = false;
					ck_MapRgb.setChecked(false);
				}else{
					mPicoCamera.setMapperEnabledRGBToIR(false);
					bMapRgbToIrEnabled = false;
				}
			}
		});

		mEditPara= (EditText) findViewById(R.id.paraValue);
		mEditPara.setInputType( InputType.TYPE_CLASS_NUMBER);
		Button bSetPara = (Button) findViewById(R.id.setPara);
		bSetPara.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mPicoCamera != null){
					if(mEditPara != null){
						String paraValue= mEditPara.getText().toString();
						int parameterValue = Integer.parseInt(paraValue);
						if(0 == currentParaType) {
							//set depthrange
							if(parameterValue >= 0 && parameterValue <= 8){
								mPicoCamera.setDepthRange(parameterValue);
							} else{
								Toast.makeText(MainActivity.this, "depth range is 0-8",Toast.LENGTH_SHORT).show();
							}
						}else if(1 == currentParaType){
							//set Pulsecount
							if(parameterValue >= 0 && parameterValue <= 600){
								mPicoCamera.setPulseCount(parameterValue);
							} else{
								Toast.makeText(MainActivity.this, "pulseCount range must 0~600",Toast.LENGTH_SHORT).show();
							}
						}else if(2 == currentParaType){
							//set Threshold
							if(parameterValue >= 0 && parameterValue <= 200){
								mPicoCamera.setBGThresdhold(parameterValue);
							} else{
								Toast.makeText(MainActivity.this, "threshold range must be 0~200",Toast.LENGTH_SHORT).show();
							}
						}
					}

				}
			}
		});

		sp_mPara = (Spinner) findViewById(R.id.spinner_paratype);
		ArrayAdapter<String> mAdapterPara=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, paraType);
		sp_mPara.setAdapter(mAdapterPara);
		sp_mPara.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				if(isSpParaFirst){
					isSpParaFirst = false;
					return;
				}
				String str=parent.getItemAtPosition(position).toString();
				if(str.equals("DepthRange")){
					currentParaType = 0;
				}else if(str.equals("PulseCount")) {
					currentParaType = 1;
				}else if(str.equals("Threshold")){
					currentParaType = 2;
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		sp_mView = (Spinner) findViewById(R.id.spinner_viewtype);
        ArrayAdapter<String> mAdapterView=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, viewType);
		sp_mView.setAdapter(mAdapterView);
		sp_mView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
				if(isSpViewFirst){
					isSpViewFirst = false;
					return;
				}
                String str=parent.getItemAtPosition(position).toString();
                if(str.equals("DEPTH")){
                    showDepth = true;
                    showIr = false;
                    showRgb = false;
                }else if(str.equals("IR")) {
                    showIr = true;
                    showDepth = false;
                    showRgb = false;
                }else if(str.equals("RGB")){
                    showIr = false;
                    showDepth = false;
                    showRgb = true;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

		sp_mResolution = (Spinner) findViewById(R.id.spinner_resolution);
		ArrayAdapter<String> mAdapterResolution=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, resolutionString);
		sp_mResolution.setAdapter(mAdapterResolution);
		sp_mResolution.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				if (isSpResolutionFirst) {
					isSpResolutionFirst = false;
					return;
				}
				String str = parent.getItemAtPosition(position).toString();
				if (str.equals("640*360")) {
					mPicoCamera.setRgbResolution(3);
					resolutionIndex = 3;
				} else if (str.equals("640*480")) {
					mPicoCamera.setRgbResolution(2);
					resolutionIndex = 2;
				} else if (str.equals("1280*720")) {
					mPicoCamera.setRgbResolution(1);
					resolutionIndex = 1;
				} else if (str.equals("1920*1080")) {
					mPicoCamera.setRgbResolution(0);
					resolutionIndex = 0;
				}
				mPicoCamera.setMapperEnabledDepthToRGB(false);
				bMapDepthToRgbEnabled = false;
				ck_MapRgb.setChecked(false);
				mPicoCamera.setMapperEnabledRGBToDepth(false);
				bMapRgbToDepthEnabled = false;
				ck_MapDepth.setChecked(false);
				mPicoCamera.setMapperEnabledRGBToIR(false);
				bMapRgbToIrEnabled = false;
				ck_MapIr.setChecked(false);

			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		sp_mData = (Spinner) findViewById(R.id.spinner_datatype);
        ArrayAdapter<String> mAdapterData=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, dataType_sp);
		sp_mData.setAdapter(mAdapterData);
		sp_mData.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
            	if(isSpDataFirst){
					isSpDataFirst = false;
            		return;
				}
                String str = parent.getItemAtPosition(position).toString();
                if(str.equals("DEPTH_30")){
                    if(mPicoCamera != null){
                        dataType = DataType.DATA_TYPE_DEPTH_30;
                        mPicoCamera.setDataType(dataType.ordinal());
                        if(ck_MapRgb != null){
							ck_MapRgb.setClickable(false);
                        }
						if(ck_MapDepth != null){
							ck_MapDepth.setClickable(false);
						}
						if(ck_MapIr != null){
							ck_MapIr.setClickable(false);
						}
                    }
                }else if(str.equals("IR_30")) {
					dataType = DataType.DATA_TYPE_IR_30;
                    mPicoCamera.setDataType(dataType.ordinal());
                    if(ck_MapRgb != null){
						ck_MapRgb.setClickable(false);
                    }
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(false);
					}
					if(ck_MapIr != null){
						ck_MapIr.setClickable(false);
					}
                }else if(str.equals("RGB_30")){
					if(mPicoCamera != null) {
						dataType = DataType.DATA_TYPE_RGB_30;
						mPicoCamera.setDataType(dataType.ordinal());
						if (ck_MapRgb != null) {
							ck_MapRgb.setClickable(false);
						}
						if(ck_MapDepth != null){
							ck_MapDepth.setClickable(false);
						}
						if(ck_MapIr != null){
							ck_MapIr.setClickable(false);
						}
					}
                }else if(str.equals("DEPTH_IR_30")){
					if(mPicoCamera != null) {
						dataType = DataType.DATA_TYPE_DEPTH_IR_30;
						mPicoCamera.setDataType(dataType.ordinal());
						if (ck_MapRgb != null) {
							ck_MapRgb.setClickable(false);
						}
						if(ck_MapDepth != null){
							ck_MapDepth.setClickable(false);
						}
						if(ck_MapIr != null){
							ck_MapIr.setClickable(false);
						}
					}
					try{
						Thread.sleep(20);
					}catch(Exception e){

					}
                }else if(str.equals("DEPTH_RGB_30")){
					if(mPicoCamera != null) {
						dataType = DataType.DATA_TYPE_DEPTH_RGB_30;
						mPicoCamera.setDataType(dataType.ordinal());
					}
                    if(ck_MapRgb != null){
						ck_MapRgb.setClickable(true);
                    }
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(true);
					}
					if(ck_MapIr != null){
						ck_MapIr.setClickable(false);
					}
                }else if(str.equals("IR_RGB_30")){
					if(mPicoCamera != null) {
						dataType = DataType.DATA_TYPE_IR_RGB_30;
						mPicoCamera.setDataType(dataType.ordinal());
					}
                    if(ck_MapRgb != null){
						ck_MapRgb.setClickable(false);
                    }
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(false);
					}
					if(ck_MapIr != null){
						ck_MapIr.setClickable(false);
					}
                }else if(str.equals("DEPTH_IR_RGB_30")){
					if(mPicoCamera != null) {
						dataType = DataType.DATA_TYPE_DEPTH_IR_RGB_30;
						mPicoCamera.setDataType(dataType.ordinal());
					}
                    if(ck_MapRgb != null){
						ck_MapRgb.setClickable(true);
                    }
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(true);
					}
					if(ck_MapIr != null){
						ck_MapIr.setClickable(true);
					}
					try{
						Thread.sleep(20);
					}catch(Exception e){

					}
                }else if(str.equals("DEPTH_60")){
					if(mPicoCamera != null) {
						dataType = DataType.DATA_TYPE_DEPTH_60;
						mPicoCamera.setDataType(dataType.ordinal());
					}
					if(ck_MapRgb != null){
						ck_MapRgb.setClickable(false);
					}
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(false);
					}
					if(ck_MapIr != null){
						ck_MapIr.setClickable(false);
					}
				}else if(str.equals("IR_60")){
					if(mPicoCamera != null) {
						dataType = DataType.DATA_TYPE_IR_60;
						mPicoCamera.setDataType(dataType.ordinal());
					}
					if(ck_MapRgb != null){
						ck_MapRgb.setClickable(false);
					}
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(false);
					}
					if(ck_MapIr != null){
						ck_MapIr.setClickable(false);
					}
				}else if(str.equals("DATA_TYPE_DEPTH_IR_15_RGB_30")){
					if(mPicoCamera != null) {
						dataType = DataType.DATA_TYPE_DEPTH_IR_15_RGB_30;
						mPicoCamera.setDataType(dataType.ordinal());
					}
					if(ck_MapRgb != null){
						ck_MapRgb.setClickable(false);
					}
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(true);
					}
					if(ck_MapIr != null){
						ck_MapIr.setClickable(true);
					}
				}
				mPicoCamera.setMapperEnabledDepthToRGB(false);
				bMapDepthToRgbEnabled = false;
				mPicoCamera.setMapperEnabledRGBToDepth(false);
				bMapRgbToDepthEnabled = false;
				mPicoCamera.setMapperEnabledRGBToIR(false);
				bMapRgbToIrEnabled = false;
				ck_MapRgb.setChecked(false);
				ck_MapDepth.setChecked(false);
				ck_MapIr.setChecked(false);

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mPicoCamera = new PsCamera();
		if (mPicoCamera != null) {
			mPicoCamera.init(this,mOnPicoCameraConnectListener);
		}
		mFrameCallback = new FrameCallback();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.v(TAG, "onStart:");
		if (mPicoCamera != null) {
			mPicoCamera.setFrameCallback(mFrameCallback);
			mPicoCamera.setDepthRange(0);   //set depth range value  only support 0-8
			dataType = DataType.DATA_TYPE_DEPTH_RGB_30;
			mPicoCamera.setDataType(dataType.ordinal());
			mPicoCamera.setRgbResolution(2);//0 :1920x1080 1:1280x720 2:640x480 3:640x360
			resolutionIndex = 2;
			mPicoCamera.start(this);
		}
		mdepthBmpBuf = ByteBuffer.allocateDirect(1920 * 1080 * 4);
		mIrBmpBuf = ByteBuffer.allocateDirect(1920 * 1080 * 4);
		mRgbBmpBuf = ByteBuffer.allocateDirect(1920 * 1080 * 4);
	}

	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "onStop:");
		synchronized (mSync) {
			if (mPicoCamera != null) {
				mPicoCamera.stop();
			}
		}
		if(mdepthBmpBuf != null){
			mdepthBmpBuf = null;
		}
		if(mIrBmpBuf != null){
			mIrBmpBuf = null;
		}
		if(mRgbBmpBuf != null){
			mRgbBmpBuf = null;
		}
		super.onStop();

	}

	@Override
	protected void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		if (mPicoCamera != null) {
			mPicoCamera.destroy();
			mPicoCamera = null;
		}
		if (mGlview != null) {
			mGlview = null;
		}
		super.onDestroy();
	}

	private final OnPicoCameraConnectListener mOnPicoCameraConnectListener = new OnPicoCameraConnectListener() {
		@Override
		public void onAttach() {
			if (DEBUG) Log.e(TAG, "onAttach:");
		}

		@Override
		public void onConnect() {
			if (DEBUG) Log.e(TAG, "onConnect");
			if(mPicoCamera != null) {
				String sn = mPicoCamera.getSn();
				Log.i(TAG, "SN =  " + sn);
				String fwVer = mPicoCamera.getFWVerion();
				Log.i(TAG, "fwVer =  " + fwVer);
				String hwVer = mPicoCamera.getHWVerion();
				Log.i(TAG, "hwVer =  " + hwVer);
				String sdkVersion = mPicoCamera.getSDKVerion();
				Log.i(TAG, "SDKVersion  =  " + sdkVersion);
				String deviceName = mPicoCamera.getDeviceName();
				Log.i(TAG, "deviceName  =  " + deviceName);
			}
		}

		@Override
		public void onDisconnect() {
			if (DEBUG) Log.e(TAG, "onDisconnect");
		}

		@Override
		public void onDettach() {
			if (DEBUG) Log.e(TAG, "onDettach");
		}

		@Override
		public void onCancel() {
			if (DEBUG) Log.e(TAG, "onCancel");
		}
	};

	public class FrameCallback implements IFrameCallback {
		@Override
		public void onFrame(PsFrame DepthFrame,PsFrame IrFrame,PsFrame RgbFrame) {
			fps++;
			final 	Calendar mCalendar = Calendar.getInstance();
			endTime = mCalendar.get(Calendar.SECOND);
			if(endTime != startTime ){
				Log.i(TAG,"current fps  "+ fps);
				fps = 0;
				startTime = endTime;
			}
			if(showDepth && null != DepthFrame) {
			    if(bmpDepth == null || bmpDepth.getWidth() != DepthFrame.width || bmpDepth.getHeight() != DepthFrame.height) {
                    bmpDepth = Bitmap.createBitmap(DepthFrame.width, DepthFrame.height, Bitmap.Config.ARGB_8888);
                }
                byte[] mByteBuffer_depth;
				short depthValue = 0;
                mByteBuffer_depth = new byte[DepthFrame.frameData.remaining()];
				DepthFrame.frameData.rewind();
				DepthFrame.frameData.get(mByteBuffer_depth);
				if(!bMapRgbToDepthEnabled) {
					int count = mByteBuffer_depth.length >> 1;
					short[] depthBuf = new short[count];
					ByteBuffer.wrap(mByteBuffer_depth).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(depthBuf);
					depthValue = depthBuf[DepthFrame.width * DepthFrame.height/2 + DepthFrame.width/2];
				}
				mPicoCamera.Y16ToRgba(mByteBuffer_depth, mdepthBmpBuf,DepthFrame.width,DepthFrame.height);
				byte[] bImgBuf_depth = mdepthBmpBuf.array();
				bmpDepth.copyPixelsFromBuffer(ByteBuffer.wrap(bImgBuf_depth));
				if(!bMapRgbToDepthEnabled) {
					Canvas canvas = new Canvas(bmpDepth);
					Paint paint = new Paint();
					paint.setAntiAlias(true);
					paint.setDither(true);
					paint.setTextSize(40);
					paint.setColor(Color.parseColor("#ff0000"));
					canvas.drawText(String.valueOf(depthValue), (bmpDepth.getWidth() / 2) - 20, (bmpDepth.getHeight() / 2), paint);
					canvas.drawText(".", (bmpDepth.getWidth() / 2), (bmpDepth.getHeight() / 2 + 20), paint);
				}
				myrender.setBuf(bmpDepth);
				mGlview.requestRender();
			}
            if(showIr && null != IrFrame){
                if(bmpIr == null || bmpIr.getWidth() != IrFrame.width || bmpIr.getHeight() != IrFrame.height) {
                    bmpIr = Bitmap.createBitmap(IrFrame.width, IrFrame.height, Bitmap.Config.ARGB_8888);
                }
                byte[] mByteBuffer_ir;
                mByteBuffer_ir = new byte[IrFrame.frameData.remaining()];
				IrFrame.frameData.rewind();
				IrFrame.frameData.get(mByteBuffer_ir);
                mPicoCamera.Y16ToRgba(mByteBuffer_ir, mIrBmpBuf,IrFrame.width,IrFrame.height);
                byte[] bImgBuf_ir = mIrBmpBuf.array();
                bmpIr.copyPixelsFromBuffer(ByteBuffer.wrap(bImgBuf_ir));
                myrender.setBuf(bmpIr);
                mGlview.requestRender();
            }
			if(showRgb && null != RgbFrame){
                if(bmpRgb == null || bmpRgb.getWidth() != RgbFrame.width || bmpRgb.getHeight() != RgbFrame.height) {
                    bmpRgb = Bitmap.createBitmap(RgbFrame.width, RgbFrame.height, Bitmap.Config.ARGB_8888);
                }
				byte[] mByteBuffer_rgb;
				mByteBuffer_rgb = new byte[RgbFrame.frameData.remaining()];
				RgbFrame.frameData.rewind();
				RgbFrame.frameData.get(mByteBuffer_rgb);
				mPicoCamera.RgbToRgba(mByteBuffer_rgb, mRgbBmpBuf,RgbFrame.width,RgbFrame.height);
				byte[] bImgBuf_rgb = mRgbBmpBuf.array();
				bmpRgb.copyPixelsFromBuffer(ByteBuffer.wrap(bImgBuf_rgb));
				myrender.setBuf(bmpRgb);
				mGlview.requestRender();
			}
		}
	}
}
