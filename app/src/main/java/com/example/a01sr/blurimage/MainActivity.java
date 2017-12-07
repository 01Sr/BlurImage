package com.example.a01sr.blurimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    private ImageView blurImageView;
    private SeekBar seekBar;

    Bitmap originBitmap = null;
    Bitmap blurBitmpa = null;

    int max = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        blurImageView = findViewById(R.id.iv);

        //这样获得的bitmap是不可修改的
        originBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.a);
        blurBitmpa = originBitmap.copy(originBitmap.getConfig(),true);
        blurImageView.setImageBitmap(blurBitmpa);

        seekBar = findViewById(R.id.sb);
        seekBar.setMax(max);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int preRadius = 0;
            private final Object lock = new Object();
            long preTime = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.i("###","current progress= "+progress);
                if(preTime == 0)
                    preTime = System.currentTimeMillis();
                else{
                    long current = System.currentTimeMillis();
                    if(current - preTime < 100) return;
                    preTime = current;
                }
                if(originBitmap==null)return;
                int radius = seekBar.getProgress();
                if(radius==preRadius) return;
                int tmp = radius;
                if(radius>preRadius){
                    radius = radius - preRadius;
                }else
                    blurBitmpa = originBitmap.copy(originBitmap.getConfig(),true);

                new BlurTask(MainActivity.this, blurImageView, blurBitmpa, radius, tmp>preRadius, lock).execute();
                preRadius = tmp;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i("###","progress = "+seekBar.getProgress());
                if(originBitmap==null)return;
                int radius = seekBar.getProgress();
                if(radius==preRadius) return;
                int tmp = radius;
                if(radius>preRadius){
                    radius = radius - preRadius;
                }else
                    blurBitmpa = originBitmap.copy(originBitmap.getConfig(),true);

                new BlurTask(MainActivity.this, blurImageView, blurBitmpa, radius, tmp>preRadius, lock).execute();
                preRadius = tmp;
            }
        });
    }

    public class BlurTask extends AsyncTask<Object,Integer,Bitmap> {
        private final Context context;
        private final ImageView imageView;
        private  Bitmap blurBitmpa;
        private final int radius;
        private final boolean more;
        private final Object lock;
        public BlurTask(Context context, ImageView imageView, Bitmap blurBitmpa, int radius, boolean more, Object lock){
            this.context = context;
            this.imageView = imageView;
            this.blurBitmpa = blurBitmpa;
            this.radius = radius;
            this.more = more;
            this.lock = lock;
        }

        @Override
        protected Bitmap doInBackground(Object[] objects) {
            synchronized (lock){
                blur2(context, blurBitmpa, radius);
                return blurBitmpa;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(!more)
                imageView.setImageBitmap(bitmap);
            else
                imageView.invalidate();
        }

    }

    private Bitmap blur(@NonNull Context context, @NonNull Bitmap bitmap, int radius){
        long startTime = System.currentTimeMillis();
        RenderScript mRS = RenderScript.create(context);
        Allocation mInAllocation = Allocation.createFromBitmap(mRS, bitmap, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
        Allocation mOutAllocation = Allocation.createTyped(mRS, mInAllocation.getType());
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
        int r0 = radius%25;
        boolean i2o = true;
        if(r0!=0){
            scriptIntrinsicBlur.setInput(mInAllocation);
            scriptIntrinsicBlur.setRadius(r0);
            scriptIntrinsicBlur.forEach(mOutAllocation);
            i2o = false;
        }
        int n = radius/25;
        while((n--)!=0){
            scriptIntrinsicBlur.setInput(i2o?mInAllocation:mOutAllocation);
            scriptIntrinsicBlur.setRadius(25);
            scriptIntrinsicBlur.forEach(i2o?mOutAllocation:mInAllocation);
            i2o = i2o?false:true;
        }
        if(i2o)
            mInAllocation.copyTo(bitmap);
        else
            mOutAllocation.copyTo(bitmap);
        long endTime = System.currentTimeMillis();
        Log.i("###","blur time: "+(endTime-startTime)+"ms");
        return bitmap;
    }

    private Bitmap blur2(@NonNull Context context,@NonNull Bitmap bitmap, int radius){
        long startTime = System.currentTimeMillis();
        RenderScript mRS = RenderScript.create(context);
        Allocation mInAllocation = Allocation.createFromBitmap(mRS, bitmap, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
        int r0 = radius%25;
        if(r0!=0){
            scriptIntrinsicBlur.setInput(mInAllocation);
            scriptIntrinsicBlur.setRadius(r0);
            scriptIntrinsicBlur.forEach(mInAllocation);
        }
        int n = radius/25;
        while((n--)!=0){
            scriptIntrinsicBlur.setInput(mInAllocation);
            scriptIntrinsicBlur.setRadius(25);
            scriptIntrinsicBlur.forEach(mInAllocation);
        }
        mInAllocation.copyTo(bitmap);
        long endTime = System.currentTimeMillis();
        Log.i("###","blur2 time: "+(endTime-startTime)+"ms");
        return bitmap;
    }

    private void copyPixels(Bitmap sourceBitmap, Bitmap targetBitmap){
        int width = sourceBitmap.getWidth();
        int heigh = sourceBitmap.getHeight();
        int[] pixels = new int[width*heigh];
        sourceBitmap.getPixels(pixels,0,width,0,0,width,heigh);
        targetBitmap.setPixels(pixels,0,width,0,0,width,heigh);
    }

}
