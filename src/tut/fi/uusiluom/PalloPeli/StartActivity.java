package tut.fi.uusiluom.PalloPeli;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

public class StartActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	public static float xAcc, yAcc;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new DrawCanvas(this));
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);	//pidet‰‰n aktiivisena
	    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}
	
	  // event trigger
	  @Override
	  public void onSensorChanged(SensorEvent event) {
	    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		    float[] values = event.values;
		    // kiihtyvyysarvot
		    xAcc = values[0];
		    yAcc = values[1];
	    }
	  }

	  @Override
	  public void onAccuracyChanged(Sensor sensor, int accuracy) {
	  }
	  
	  @Override
	  protected void onResume() {
	    super.onResume();
	    // accelerometer listener
	    sensorManager.registerListener(this,
	        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	  }

	  @Override
	  protected void onPause() {
	    super.onPause();
	    sensorManager.unregisterListener(this);
	  }
	  
	  @Override
	  protected void onStop() {
		  super.onStop();
		  finish();
	  }
}


class DrawCanvas extends View {
	static private final int LIMIT = 7;		//alueen rajat (dp)
	static private final int T_INCR = 3;		//laskennassa k‰ytetty aikainkrementti 3 ms
	static private final double BOUNCE_FACTOR = 0.6;	//sein‰st‰ kimpoamiskerroin
	static private final double HIDASTUS = 0.1;		// isompi -> nopeampi	
	static private final float FRUIT_OFFSET = 13;		// hedelm‰n kosketusoffset (dp)
	static private final double ENEMY_SPEED = 0.08;		//vihollisen nopeus
	static private final double ENEMY_BRAKE = 0.998;	//vihollisen hidastuvuus (isompi =v‰hemm‰n jarrua)
	static private final float ENEMY_OFFSET = 15;		//vihollisen kosketusoffset
	
    private double pos_Xcur, pos_Ycur;			//nykyisen ajanhetken paikka
    private double pos_Xprev, pos_Yprev;		//edellisen ajanhetken paikka
    private double veloc_Xcur, veloc_Ycur;		//nykyisen ajanhetken nopeudet
    private double veloc_Xprev, veloc_Yprev;		//edellisen ajanhetken nopeudet
    private int draw_CoordX, draw_CoordY;		//pallon piirtokoordinaatit
    
    private int enemy_posX = 0, enemy_posY = 0;
    private int enemy_drawCoord_X, enemy_drawCoord_Y;
    private double enemy_velocX, enemy_velocY;
    private double target_X, target_Y;
    private double targetDist = 1;
    private static boolean enemyFlag;
    
    private int screenWidth, screenHeight;
    private int ballW, ballH;
    private int enemyW, enemyH;
    private int fruitW, fruitH;
    private Bitmap ball, bgr, fruit, enemy;
    private Paint paint;
    private Random rndGen;
    
    private static int points = 0;		//hedelm‰pisteet
    private boolean fruitFlag = true;
    private int fruitCoord_X = 400, fruitCoord_Y = 400;
    private int fruitEat, fail, hitWall, hitWallEnemy;
    private SoundPool sp;
    

    // konstruktori
    public DrawCanvas(Context context) {
        super(context);
        sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        fruitEat = sp.load(context, R.raw.fruiteat, 1);
        fail = sp.load(context, R.raw.failbuzzer, 1);
        hitWall = sp.load(context, R.raw.hitwall, 1);
        hitWallEnemy = sp.load(context, R.raw.hitwallenemy, 1);
        ball = BitmapFactory.decodeResource(getResources(),R.drawable.hahmo2); 		//pallo
        bgr = BitmapFactory.decodeResource(getResources(),R.drawable.background); 	//taustakuva
        fruit = BitmapFactory.decodeResource(getResources(),R.drawable.mansikka2); 	//hedelm‰
        enemy = BitmapFactory.decodeResource(getResources(),R.drawable.enemy2); 	//vihollinen
        ballW = ball.getWidth();
        ballH = ball.getHeight();
        fruitW = fruit.getWidth();
        fruitH = fruit.getHeight();
        enemyW = enemy.getWidth();
        enemyH = enemy.getHeight();
        veloc_Xcur = 0; veloc_Ycur = 0;
        veloc_Xprev = 0; veloc_Yprev = 0;
        paint = new Paint();
    	rndGen = new Random(); 
    	enemyFlag = false;
    	points = 0;
    }

    @Override
    public void onSizeChanged (int width, int height, int oldWidth, int oldhHeight) {
        super.onSizeChanged(width, height, oldWidth, oldhHeight);
        screenWidth = width;
        screenHeight = height;
        bgr = Bitmap.createScaledBitmap(bgr, width, height, true); //sovitetaan taustakuva n‰yttˆˆn
        pos_Xprev = (int) (screenWidth /2); // keskitet‰‰n pallo n‰ytˆn keskelle
        pos_Yprev = (int) (screenHeight /2); 
        pos_Xcur = 0; pos_Ycur = 0;
        enemy_posX = enemyW;
        enemy_posY = enemyH;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // piirret‰‰n rajat ja taustakuva
        canvas.drawBitmap(bgr, 0, 0, null);
        paint.setStrokeWidth(Dp2Pixel(4));
        paint.setColor(Color.CYAN);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(Dp2Pixel(LIMIT), Dp2Pixel(LIMIT), screenWidth-Dp2Pixel(LIMIT), screenHeight-Dp2Pixel(LIMIT), paint );
        
        // piirret‰‰n hedelm‰, jos hedelm‰ on syˆty, niin generoidaan uusi
        // fruitFlag kertoo onko hedelm‰ syˆty
        if(fruitFlag == true){
        	canvas.drawBitmap(fruit, fruitCoord_X-(fruitW/2), fruitCoord_Y-(fruitH/2), null);
        }
        else{
        	fruitCoord_X = rndGen.nextInt(screenWidth-(int)Dp2Pixel(140)) + 
        			(int)Dp2Pixel(70);
        	fruitCoord_Y = rndGen.nextInt(screenHeight-(int)Dp2Pixel(140)) + 
        			(int)Dp2Pixel(70);
        	canvas.drawBitmap(fruit, fruitCoord_X-(fruitW/2), fruitCoord_Y-(fruitH/2), null);
        	fruitFlag = true;
        }

        // pieni viive, jotta eri tehoiset laitteet k‰ytt‰ytyisiv‰t suunilleen samalla tavalla
        try {
            Thread.sleep(T_INCR);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        EnemyPosition();		//lasketaan vihollisen sijainti
        EnemyOutLimits();		//onko vihollinen rajojen yli?
        CalcPosAndVeloc();		//lasketaan pallon nopeudet ja sijainnit
        IsOutLimits();		// onko pallo rajojen yli?
        DoesTouchFruit();	// koskeeko pallo hedelm‰‰n?
        EnemyCatch();		//onko vihollinen saanut kiinni?
        
        
        // piirret‰‰n pistetilanne oikeaan yl‰kulmaan
        paint.setColor(Color.RED); 
        paint.setTextSize(Dp2Pixel(50)); 
        canvas.drawText(Integer.toString(points), screenWidth-Dp2Pixel(90), 
        		Dp2Pixel(65), paint); 
        
        // piirret‰‰n pallo, k‰ytet‰‰n keskipistekoordinaattia
        canvas.drawBitmap(ball, draw_CoordX, draw_CoordY, null);
        
        // piirret‰‰n vihollinen, k‰ytet‰‰n keskipistekoordinaattia
        canvas.drawBitmap(enemy, enemy_drawCoord_X, enemy_drawCoord_Y, paint);
        
        pos_Xprev = pos_Xcur;
        pos_Yprev = pos_Ycur;
        veloc_Xprev = veloc_Xcur;
        veloc_Yprev = veloc_Ycur;
        
        invalidate();	// next frame
    }
    
    
    //*********************** Lasketaan vihollisen sijainti ***************************
    public void EnemyPosition(){
    	// lasketaan miss‰ suunnassa kohde on
    	targetDist = Math.sqrt( Math.pow((pos_Xcur - enemy_posX), 2) + Math.pow((pos_Ycur - enemy_posY), 2) );
    	target_X = (pos_Xcur - enemy_posX) / targetDist;
    	target_Y = (pos_Ycur - enemy_posY) / targetDist;
    	
    	// lasketaan x- ja y-suuntaiset nopeudet viholliselle
    	enemy_velocX = enemy_velocX*ENEMY_BRAKE + target_X*ENEMY_SPEED;
    	enemy_velocY = enemy_velocY*ENEMY_BRAKE + target_Y*ENEMY_SPEED;
    	
    	// lasketaan sijainnit viholliselle
    	enemy_posX = (enemy_posX + (int)enemy_velocX); 
    	enemy_posY = (enemy_posY + (int)enemy_velocY);
    	
        //lasketaan vihollisen piirtokoordinaatit, k‰ytet‰‰n keskipistekoordinaattia
    	enemy_drawCoord_X = enemy_posX - (enemyW/2);
    	enemy_drawCoord_Y = enemy_posY - (enemyH/2);
    }
    
    
    // ************* Tutkitaan onko vihollinen rajojen yli *****************
    public void EnemyOutLimits(){
    	if( enemy_posX < Dp2Pixel(LIMIT)+(enemyW/2)){
    		sp.play(hitWallEnemy, 1, 1, 0, 0, 1);
    		enemy_posX = (int)(Dp2Pixel(LIMIT)+(enemyW/2)+5);
    		enemy_velocX = enemy_velocX*(-BOUNCE_FACTOR);
        }
        else if( enemy_posX > screenWidth-Dp2Pixel(LIMIT)-(enemyW/2) ){
        	sp.play(hitWallEnemy, 1, 1, 0, 0, 1);
        	enemy_posX = (int)(screenWidth-Dp2Pixel(LIMIT)-(enemyW/2)-5);
        	enemy_velocX = enemy_velocX*(-BOUNCE_FACTOR);
        }
        if( enemy_posY < Dp2Pixel(LIMIT)+(enemyH/2) ){
        	sp.play(hitWallEnemy, 1, 1, 0, 0, 1);
        	enemy_posY = (int)(Dp2Pixel(LIMIT)+(enemyH/2)+5);
        	enemy_velocY = enemy_velocY*(-BOUNCE_FACTOR);
        }
        else if( enemy_posY > screenHeight-Dp2Pixel(LIMIT)-(enemyH/2) ){
        	sp.play(hitWallEnemy, 1, 1, 0, 0, 1);
        	enemy_posY = (int)(screenHeight-Dp2Pixel(LIMIT)-(enemyH/2)-5);
        	enemy_velocY = enemy_velocY*(-BOUNCE_FACTOR);
        }
    }
    
    // ************* Tutkitaan onko vihollinen saanut kiinni *****************
    public void EnemyCatch(){
    	if( (enemyFlag == false) && pos_Xcur > (enemy_posX-(enemyW/2)-Dp2Pixel(ENEMY_OFFSET) ) && 
    			( pos_Xcur < (enemy_posX+(enemyW/2)+Dp2Pixel(ENEMY_OFFSET)) ) && 
    			( pos_Ycur > (enemy_posY-(enemyH/2)-Dp2Pixel(ENEMY_OFFSET)) ) &&
    			( pos_Ycur < (enemy_posY+(enemyH/2)+Dp2Pixel(ENEMY_OFFSET)) ) ){
    		enemyFlag = true;
    		
    		sp.play(fail, 1, 1, 0, 0, 1);
    		
    		//k‰ynnistett‰‰n HighscoreActivity
    		Intent intent = new Intent(getContext(), HighscoreActivity.class);
    		getContext().startActivity(intent);
    		
    		//viive 500 ms
    		try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

    	}
    }
    
    
    // ************* Tutkitaan osutaanko hedelm‰‰n *****************
    public void DoesTouchFruit(){
    	if( (fruitFlag == true) && (enemyFlag == false) && 
    			( pos_Xcur > (fruitCoord_X-(fruitW/2)-Dp2Pixel(FRUIT_OFFSET)) ) && 
    			( pos_Xcur < (fruitCoord_X+(fruitW/2)+Dp2Pixel(FRUIT_OFFSET)) ) && 
    			( pos_Ycur > (fruitCoord_Y-(fruitH/2)-Dp2Pixel(FRUIT_OFFSET)) ) &&
    			( pos_Ycur < (fruitCoord_Y+(fruitH/2)+Dp2Pixel(FRUIT_OFFSET)) ) ){
    		fruitFlag = false;
    		sp.play(fruitEat, 1, 1, 0, 0, 1);
    		points++;
    	}
    }
    
 
    // *************** Tutkitaan onko pallo rajojen yli *******'******
    public void IsOutLimits(){
    	if( pos_Xcur < Dp2Pixel(LIMIT)+(ballW/2)){
    		sp.play(hitWall, 1, 1, 0, 0, 1);
    		pos_Xcur = Dp2Pixel(LIMIT)+(ballW/2);
        	veloc_Xcur = veloc_Xcur*(-BOUNCE_FACTOR);
        }
        else if( pos_Xcur > screenWidth-Dp2Pixel(LIMIT)-(ballW/2) ){
        	sp.play(hitWall, 1, 1, 0, 0, 1);
        	pos_Xcur = screenWidth-Dp2Pixel(LIMIT)-(ballW/2);
        	veloc_Xcur = veloc_Xcur*(-BOUNCE_FACTOR);
        }
        if( pos_Ycur < Dp2Pixel(LIMIT)+(ballH/2) ){
        	sp.play(hitWall, 1, 1, 0, 0, 1);
        	pos_Ycur = Dp2Pixel(LIMIT)+(ballH/2);
        	veloc_Ycur = veloc_Ycur*(-BOUNCE_FACTOR);
        }
        else if( pos_Ycur > screenHeight-Dp2Pixel(LIMIT)-(ballH/2) ){
        	sp.play(hitWall, 1, 1, 0, 0, 1);
        	pos_Ycur = screenHeight-Dp2Pixel(LIMIT)-(ballH/2);
        	veloc_Ycur = veloc_Ycur*(-BOUNCE_FACTOR);
        }
    }

    // ************** lasketaan pallon nopeudet ja sijainnit ******************
    public void CalcPosAndVeloc(){
    	// (kiihtyvyyden 1.derivaatta nopeus, 2.derivaatta sijainti)
        // pallon nopeus
        veloc_Xcur = veloc_Xprev + ((-1)*StartActivity.xAcc*T_INCR*HIDASTUS);
        veloc_Ycur = veloc_Yprev + (StartActivity.yAcc*T_INCR*HIDASTUS);
        // pallon sijainti
        pos_Xcur = pos_Xprev + (veloc_Xprev*T_INCR*HIDASTUS) + ((veloc_Xcur-veloc_Xprev)*(T_INCR*HIDASTUS)*(0.5));
        pos_Ycur = pos_Yprev + (veloc_Yprev*T_INCR*HIDASTUS) + ((veloc_Ycur-veloc_Yprev)*(T_INCR*HIDASTUS)*(0.5));
        
        //lasketaan pallon piirtokoordinaatit, k‰ytet‰‰n keskipistekoordinaattia
        draw_CoordX = (int)Math.round(pos_Xcur)-(ballW/2);
        draw_CoordY = (int)Math.round(pos_Ycur)-(ballH/2);
    }
    
    
    // **************** muunnetaan dp -> pixel ******************
    public float Dp2Pixel(float dp){
	  DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
	  float px = dp * (metrics.densityDpi/160f);
	  return px;
	}
    
    public static void setEnemyFlag(boolean state){
    	enemyFlag = state;
    }
    
    public static void setPoints(int points2){
    	points = points2;
    }
    
    public static int getPoints(){
    	return points;
    }
}