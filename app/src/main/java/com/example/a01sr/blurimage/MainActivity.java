package com.example.a01sr.blurimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TimeUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout root = this.findViewById(R.id.root);
        //这样获得的bitmap是不可修改的
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b);
        //复制一份可修改的
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        //原图
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        root.addView(imageView ,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        //修改后的图
        ImageView blurImageView = new ImageView(this);
        root.addView(blurImageView ,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        new BlurTask(blurImageView).execute(bitmap, mutableBitmap);
    }

    class Node{
        private final int x;
        private final int y;

        public Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash*31 + x;
            hash = hash*31 + y;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == this)
                return true;
            if(!(obj instanceof Node))
                return false;
            Node o = (Node) obj;
            return (o.getX()==this.getX()&&o.getY()==this.getY());
        }

        @Override
        public String toString() {
            return "Node{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    class BlurTask extends AsyncTask<Bitmap, Float, Bitmap>{
        private final ImageView imageView;
        public BlurTask(ImageView imageView){
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            int size = 5*5;
            int l = (int) Math.sqrt(size);
            Log.i("###","l="+l);
            long start = System.currentTimeMillis();
            Bitmap bitmap = bitmaps[0];
            Bitmap blurBitmap = bitmaps[1];
            //保存相对坐标
            /*
            以9个点为例
            -------------------------
            |(-1, 1)|( 0, 1)|( 1, 1)|
            -------------------------
            |(-1, 0)|( 0, 0)|( 1, 0)|
            -------------------------
            |(-1,-1)|( 0,-1)|( 1,-1)|
            -------------------------
             */
            Node[] c = new Node[size];
            //平方差
            double sigama = 1.5;
            double[] weights = new double[size];
            double weightSum = 0;
            int low = -(l-1)/2;
            int high = (l-1)/2;
            int k = 0;
            //添加相对坐标的，并计算每个点的权值
            for(int i = low; i <= high; i++ ){
                for(int j = low; j<= high; j++){
                    c[k] = new Node(i,j);
                    weights[k] = gauss(sigama, i, j);
                    Log.i("###","---weight="+weights[k]);
                    weightSum+=weights[k];
                    k++;
                }
            }
            //让权值和为1
            for(int i = 0; i < size;i++){
                weights[i] = weights[i]/weightSum;
                Log.i("###","weight = "+weights[i]+" node = "+c[i]);
            }

//            Map<Node,Integer> map = new HashMap<>();
            int widthL = blurBitmap.getWidth();
            int heightL = blurBitmap.getHeight();
            Log.i("####","width="+widthL+" height="+heightL);
            for(int i = 0; i < widthL; i++){
                for(int j = 0; j< heightL; j++){
                    int blurColor = 0;
                    double alpha = 0;
                    double r =0;
                    double g = 0;
                    double b = 0;
                    for(k = 0; k < size; k++ ){
                        Integer color = 0;
                        int x = i+c[k].getX();
                        int y = j+c[k].getY();
                        x = x<0?0:x;
                        x = x>=widthL?widthL-1:x;
                        y = y<0?0:y;
                        y = y>=heightL?heightL-1:y;
//                    if((color = map.get(new Node(x,y)))==null){
                        color = bitmap.getPixel(x,y);
//                        map.put(new Node(x,y),color);
//                    }

                        alpha+=Color.alpha(color)*weights[k];
                        r+=Color.red(color)*weights[k];
                        g+=Color.green(color)*weights[k];
                        b+=Color.blue(color)*weights[k];
                    }
                    int ocolor = bitmap.getPixel(i,j);
                    int oa = Color.alpha(ocolor);
                    int or = Color.red(ocolor);
                    int og = Color.green(ocolor);
                    int ob = Color.blue(ocolor);
                    blurColor = Color.argb((int)alpha,(int)r,(int)g,(int)b);
                    Log.i("####","color="+oa+" "+or+" "+og+" "+ob+" "+" ;blurColor="+(int)alpha+" "+(int)r+" "+(int)g+" "+(int)b);
                    blurBitmap.setPixel(i,j, blurColor);
                }
            }
            long end = System.currentTimeMillis();
            Log.i("###","---运行时间："+(end - start));
            return blurBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.i("###","complete");
            //模糊完成后把bitmap添加到imageview
            imageView.setImageBitmap(bitmap);
        }
    }

    //
    private double gauss(double sigma, int x, int y){
        double sigma2_2 = sigma*sigma*2;
        return (1/(Math.PI*sigma2_2))*Math.pow(Math.E,-(x*x+y*y)/sigma2_2);
    }

//    private int hash(int relativeX, int relativeY, int x, int y){
//        //中心对称
//        relativeX = Math.abs(relativeX);
//        relativeY = Math.abs(relativeY);
//        if(relativeX < relativeY){
//            int tmp = relativeX;
//            relativeX = relativeY;
//            relativeY = tmp;
//        }
//        int hash = 17;
//        hash = hash*31 + relativeX;
//        hash = hash*31 + relativeY;
//        hash = hash*31 + x;
//        hash = hash*31 + y;
//        return hash;
//    }

}
