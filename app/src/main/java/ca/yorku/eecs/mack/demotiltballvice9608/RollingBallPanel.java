package ca.yorku.eecs.mack.demotiltballvice9608;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;


import android.view.MotionEvent;
import android.view.View;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.util.Log;
import java.util.Locale;

import java.util.Locale;

public class RollingBallPanel extends View
{
    private final static String MYDEBUG = "MYDEBUG";
    final static float DEGREES_TO_RADIANS = 0.0174532925f;

    // the ball diameter will be min(width, height) / this_value
    final static float BALL_DIAMETER_ADJUST_FACTOR = 30;

    final static int DEFAULT_LABEL_TEXT_SIZE = 20; // tweak as necessary
    final static int DEFAULT_STATS_TEXT_SIZE = 10;
    final static int DEFAULT_GAP = 7; // between lines of text
    final static int DEFAULT_OFFSET = 10; // from bottom of display

    final static int MODE_NONE = 0;
    final static int PATH_TYPE_SQUARE = 1;
    final static int PATH_TYPE_CIRCLE = 2;

    final static float PATH_WIDTH_NARROW = 2f; // ... x ball diameter
    final static float PATH_WIDTH_MEDIUM = 4f; // ... x ball diameter
    final static float PATH_WIDTH_WIDE = 8f; // ... x ball diameter




    float radiusOuter, radiusInner;
    float moveSpeed;
    Bitmap ball, decodedBallBitmap;
    int ballDiameter;

    float dT; // time since last sensor event (seconds)

    float width, height, pixelDensity;
    int labelTextSize, statsTextSize, gap, offset;

    RectF innerRectangle, outerRectangle, innerShadowRectangle, outerShadowRectangle, ballNow, antiRectangle,lineRectangle,outerShadowRectangle1,ballOld, edgeRectangle;
    RectF edgeShadowRectangle;
    boolean bl;
    boolean touchFlag;
    boolean touchFlag1;
    boolean cheat;
    boolean inP;
    boolean started = false;
    Vibrator vib;
    int wallHits;
    int count=0;

    float xBall, yBall; // top-left of the ball (for painting)
    float oldx,oldy;
    float di;
    float miss = 0;
    float acc;
    float xBallCenter, yBallCenter; // center of the ball

    float pitch, roll;
    float tiltMagnitude;
    float tiltAngle=180;

    // parameters from Setup dialog
    String orderOfControl;
    float gain, pathWidth;
    int numberL;
    int pathType;
    boolean movement;
    boolean toLeft = false;
    boolean touched=false;
    boolean stay=false;

    int lap;

    float velocity; // in pixels/second (velocity = tiltMagnitude * tiltVelocityGain
    float dBall; // the amount to move the ball (in pixels): dBall = dT * velocity
    float xCenter, yCenter; // the center of the screen
    long now, lastT;
    long startT,endT,startE,endE,freshE;
    float totalT,errorT,inT;
    Paint statsPaint, labelPaint, linePaint, fillPaint, backgroundPaint;
    float[] updateY;




    public RollingBallPanel(Context contextArg)
    {
        super(contextArg);
        initialize(contextArg);
    }

    public RollingBallPanel(Context contextArg, AttributeSet attrs)
    {
        super(contextArg, attrs);
        initialize(contextArg);
    }

    public RollingBallPanel(Context contextArg, AttributeSet attrs, int defStyle)
    {
        super(contextArg, attrs, defStyle);
        initialize(contextArg);
    }

    // things that can be initialized from within this View
    private void initialize(Context c)
    {
        linePaint = new Paint();
        linePaint.setColor(Color.RED);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        linePaint.setAntiAlias(true);

        fillPaint = new Paint();
        fillPaint.setColor(0xffccbbbb);
        fillPaint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.LTGRAY);
        backgroundPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint();
        labelPaint.setColor(Color.BLACK);
        labelPaint.setTextSize(DEFAULT_LABEL_TEXT_SIZE);
        labelPaint.setAntiAlias(true);

        statsPaint = new Paint();
        statsPaint.setAntiAlias(true);
        statsPaint.setTextSize(DEFAULT_STATS_TEXT_SIZE);

        // NOTE: we'll create the actual bitmap in onWindowFocusChanged
        decodedBallBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ball);



        lastT = System.nanoTime();
        this.setBackgroundColor(Color.LTGRAY);
        touchFlag = false;
        touchFlag1 = false;
        outerRectangle = new RectF();
        innerRectangle = new RectF();
        innerShadowRectangle = new RectF();
        outerShadowRectangle = new RectF();
        outerShadowRectangle1 = new RectF();
        edgeRectangle = new RectF();
        edgeShadowRectangle = new RectF();
        antiRectangle = new RectF();
        lineRectangle = new RectF();
        ballNow = new RectF();
        ballOld= new RectF();
        wallHits = 0;
        lap = -1;
        errorT = 0;
        freshE=0;
        cheat = true;
        inP= false;

        vib = (Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);


    }

    /**
     * Called when the window hosting this view gains or looses focus.  Here we initialize things that depend on the
     * view's width and height.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if (!hasFocus)
            return;

        width = this.getWidth();
        height = this.getHeight();

        // the ball diameter is nominally 1/30th the smaller of the view's width or height
        ballDiameter = width < height ? (int)(width / BALL_DIAMETER_ADJUST_FACTOR)
                : (int)(height / BALL_DIAMETER_ADJUST_FACTOR);

        // now that we know the ball's diameter, get a bitmap for the ball
        ball = Bitmap.createScaledBitmap(decodedBallBitmap, ballDiameter, ballDiameter, true);

        // center of the view
        xCenter = width / 2f;
        yCenter = height / 2f;

        // top-left corner of the ball
        xBall = xCenter;
        yBall = yCenter+400;

        // center of the ball
        xBallCenter = xBall + ballDiameter / 2f;
        yBallCenter = yBall + ballDiameter / 2f;

        // configure outer rectangle of the path
        radiusOuter = width < height ? 0.40f * width : 0.40f * height;
       /* outerRectangle.left = xCenter - radiusOuter;
        outerRectangle.top = yCenter - radiusOuter;
        outerRectangle.right = xCenter + radiusOuter;
        outerRectangle.bottom = yCenter + radiusOuter;*/
        outerRectangle.left = xCenter - 65f;
        outerRectangle.top = yCenter - 300f;
        outerRectangle.right = xCenter + 65f;
        outerRectangle.bottom = yCenter - 210f;

        // configure inner rectangle of the path
        // NOTE: medium path width is 4 x ball diameter
        radiusInner = radiusOuter - pathWidth * ballDiameter;
        innerRectangle.left = xCenter - radiusInner;
        innerRectangle.top = yCenter - radiusInner;
        innerRectangle.right = xCenter + radiusInner;
        innerRectangle.bottom = yCenter + radiusInner;

        // configure outer shadow rectangle (needed to determine wall hits)
        // NOTE: line thickness (aka stroke width) is 2
        outerShadowRectangle.left = outerRectangle.left + ballDiameter - 2f;
        outerShadowRectangle.top = outerRectangle.top + ballDiameter - 2f;
        outerShadowRectangle.right = outerRectangle.right - ballDiameter + 2f;
        outerShadowRectangle.bottom = outerRectangle.bottom - ballDiameter + 2f;

        outerShadowRectangle1.left = outerRectangle.left + ballDiameter/2 - 2f;
        outerShadowRectangle1.top = outerRectangle.top + ballDiameter/2 - 2f;
        outerShadowRectangle1.right = outerRectangle.right - ballDiameter/2 + 2f;
        outerShadowRectangle1.bottom = outerRectangle.bottom - ballDiameter/2 + 2f;

        // configure inner shadow rectangle (needed to determine wall hits)
        innerShadowRectangle.left = innerRectangle.left + ballDiameter - 2f;
        innerShadowRectangle.top = innerRectangle.top + ballDiameter - 2f;
        innerShadowRectangle.right = innerRectangle.right - ballDiameter + 2f;
        innerShadowRectangle.bottom = innerRectangle.bottom - ballDiameter + 2f;

        antiRectangle.left = xCenter;
        antiRectangle.top = yCenter;
        antiRectangle.right = xCenter;
        antiRectangle.bottom = height;

        lineRectangle.left = outerRectangle.left;
        lineRectangle.right = innerRectangle.left;
        lineRectangle.top = yCenter;
        lineRectangle.bottom = yCenter;

        edgeRectangle.left = 8;
        edgeRectangle.top = 8;
        edgeRectangle.right = width-8;
        edgeRectangle.bottom = height-8;

        edgeShadowRectangle.left = edgeRectangle.left + ballDiameter - 2f;
        edgeShadowRectangle.top = edgeRectangle.top + ballDiameter - 2f;
        edgeShadowRectangle.right =  edgeRectangle.right - ballDiameter + 2f;
        edgeShadowRectangle.bottom = edgeRectangle.bottom - ballDiameter + 2f;
        // initialize a few things (e.g., paint and text size) that depend on the device's pixel density
        pixelDensity = this.getResources().getDisplayMetrics().density;
        labelTextSize = (int)(DEFAULT_LABEL_TEXT_SIZE * pixelDensity + 0.5f);
        labelPaint.setTextSize(labelTextSize);

        statsTextSize = (int)(DEFAULT_STATS_TEXT_SIZE * pixelDensity + 0.5f);
        statsPaint.setTextSize(statsTextSize);

        gap = (int)(DEFAULT_GAP * pixelDensity + 0.5f);
        offset = (int)(DEFAULT_OFFSET * pixelDensity + 0.5f);

        // compute y offsets for painting stats (bottom-left of display)
        updateY = new float[6]; // up to 6 lines of stats will appear
        for (int i = 0; i < updateY.length; ++i)
            updateY[i] = height - offset - i * (statsTextSize + gap);
    }

    /*
     * Do the heavy lifting here! Update the ball position based on the tilt angle, tilt
     * magnitude, order of control, etc.
     */
    public void updateBallPosition(boolean m, float rollArg, float tiltAngleArg, float tiltMagnitudeArg)
    {
        if(toLeft==false) {
            outerRectangle.left = outerRectangle.left + moveSpeed;
            outerRectangle.right = outerRectangle.right + moveSpeed;
        }
        else if(toLeft){
            outerRectangle.left = outerRectangle.left - moveSpeed;
            outerRectangle.right = outerRectangle.right - moveSpeed;

        }

        if(outerRectangle.right>=width-30)
            toLeft=true;
        if(outerRectangle.left<=30)
            toLeft=false;



        if(stay){
            if(tiltAngleArg!=tiltAngle)
                stay=false;

        }


        //pitch = pitchArg; // for information only (see onDraw)
        movement = m; //receive new  input
        if(m&&!started){
            startT = System.nanoTime();
            started=true;
        }
        roll = rollArg; // for information only (see onDraw)
        tiltAngle = tiltAngleArg;
        tiltMagnitude=0;

        if(movement&&!stay) {
            tiltMagnitude = 12;

        }




        // get current time and delta since last onDraw
        now = System.nanoTime();
        dT = (now - lastT) / 1000000000f; // seconds
        lastT = now;

        // don't allow tiltMagnitude to exceed 45 degrees
        final float MAX_MAGNITUDE = 45f;
        tiltMagnitude = tiltMagnitude > MAX_MAGNITUDE ? MAX_MAGNITUDE : tiltMagnitude;

        // This is the only code that distinguishes velocity-control from position-control

            // compute ball velocity (depends on the tilt of the device and the gain setting)
            velocity = tiltMagnitude * 200;

            // compute how far the ball should move (depends on the velocity and the elapsed time since last update)
            dBall = dT * velocity; // make the ball move this amount (pixels)

            // compute the ball's new coordinates (depends on the angle of the device and dBall, as just computed)



            float dx = (float)Math.sin(tiltAngle * DEGREES_TO_RADIANS) * dBall;
            float dy = -(float)Math.cos(tiltAngle * DEGREES_TO_RADIANS) * dBall;




            di = yBall - oldy;
            oldx = xBall;
            oldy = yBall;
            xBall += dx;
            yBall += dy;

        /*else
        // position control
        {
            // compute how far the ball should move (depends on the tilt of the device and the gain setting)
            dBall = tiltMagnitude * gain;

            // compute the ball's new coordinates (depends on the angle of the device and dBall, as just computed)
            float dx = (float)Math.sin(tiltAngle * DEGREES_TO_RADIANS) * dBall;
            float dy = -(float)Math.cos(tiltAngle * DEGREES_TO_RADIANS) * dBall;
            xBall = xCenter + dx;
            yBall = yCenter + dy;
        }*/

        // make an adjustment, if necessary, to keep the ball visible (also, restore if NaN)
        if (Float.isNaN(xBall) || xBall < 0)
            xBall = 0;
        else if (xBall > width - ballDiameter)
            xBall = width - ballDiameter;
        if (Float.isNaN(yBall) || yBall < 0)
            yBall = 0;
        else if (yBall > height - ballDiameter)
            yBall = height - ballDiameter;

        // oh yea, don't forget to update the coordinate of the center of the ball (needed to determine wall  hits)
        xBallCenter = xBall + ballDiameter / 2f;
        yBallCenter = yBall + ballDiameter / 2f;

        // if ball touches wall, vibrate and increment wallHits count
        // NOTE: We also use a boolean touchFlag so we only vibrate on the first touch
       /* if(lap!=-1 && inPathTime()){
            if(inP==false) {
                startE = System.nanoTime();
                inP=true;
            }else if(inP==true){

                endE = System.nanoTime();
                freshE += endE-startE;
                startE = System.nanoTime();

            }
        }else if(!inPathTime()){
            inP=false;
        }

        if(anticheated()){
            cheat = false;

        }*/

       /* if(ballBeginning()&& !touchFlag1){
            touchFlag1=true;

            if(di>0){

                if(lap==-1) {
                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                    Log.i(MYDEBUG, "111111");
                    startT = System.nanoTime();
                    lap++;
                    //ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    //toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,300);

                    //cheat = true;
                }
                else {
                    if(cheat==false) {
                        lap++;
                        //ToneGenerator toneGen2 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                        //toneGen2.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,300);
                        cheat = true;
                    }
                }
            }
             if(count==5) {

                endT = System.nanoTime();
                totalT = ((endT-startT)/ 1000000000f)/numberL;
                //inT = (freshE/ 1000000000f)/((endT-startT)/ 1000000000f);
                Intent i = new Intent(getContext(), Result.class);
                Bundle b = new Bundle();
              //  b.putInt("laps",numberL);
                b.putFloat("time",totalT);
               // b.putFloat("inPath",inT);
               // b.putInt("hit",wallHits);
                i.putExtras(b);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getContext().startActivity(i);
            }

        }
        else if (!ballBeginning()&& touchFlag1){
            touchFlag1 = false;

        }*/




        if (ballTouchingLine() ||ballTouchingEdge()) {
            //touchFlag = true; // the ball has *just* touched the line: set the touchFlag

            //vib.vibrate(20); // 20 ms vibrotactile pulse
            if(count==5) {

                endT = System.nanoTime();
                totalT = ((endT-startT)/ 1000000000f);
                acc = 5/(miss+5);
                //inT = (freshE/ 1000000000f)/((endT-startT)/ 1000000000f);
                Intent i = new Intent(getContext(), Result.class);
                Bundle b = new Bundle();
                //  b.putInt("laps",numberL);
                 b.putFloat("time",totalT);
                 b.putFloat("miss",miss);
                 b.putFloat("acc",acc);
                // b.putFloat("inPath",inT);
                // b.putInt("hit",wallHits);
                i.putExtras(b);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getContext().startActivity(i);
            }


            xBall = xCenter;
            yBall = yCenter+400;

            xBallCenter = xBall + ballDiameter / 2f;
            yBallCenter = yBall + ballDiameter / 2f;

            tiltMagnitude=0;
            stay=true;
            touched=false;

            /*ballOld.left = oldx;
            ballOld.right = oldx + ballDiameter;
            ballOld.top = oldy;
            ballOld.bottom = oldy + ballDiameter;*/

        }


         /*  if (pathType == PATH_TYPE_SQUARE)
            {
               // if((oldx>outerRectangle.left&&(oldx+ballDiameter)<outerRectangle.right&&oldy>outerRectangle.top&&(oldy+ballDiameter)<outerRectangle.bottom)&&
                       // (!(oldx>innerRectangle.left&&oldx+ballDiameter<innerRectangle.right&&oldy>innerRectangle.top&&oldy+ballDiameter<innerRectangle.bottom))) {
                if (RectF.intersects(ballOld, outerShadowRectangle1) && !RectF.intersects(ballOld, innerRectangle)){
                    ++wallHits;

                }


            }
            else if (pathType == PATH_TYPE_CIRCLE){
                final float ballDistance = (float)Math.sqrt((oldx+ballDiameter / 2f - xCenter) * (oldx+ballDiameter / 2f - xCenter)
                        + (oldy+ballDiameter / 2f - yCenter) * (oldy+ballDiameter / 2f - yCenter));

                if((ballDistance+ballDiameter / 2f)<radiusOuter&&(!((ballDistance+ballDiameter / 2f)<radiusInner))) {
                    ++wallHits;

                }

            }


        } else if (!ballTouchingLine() && touchFlag)
            touchFlag = false; // the ball is no longer touching the line: clear the touchFlag
        */
        invalidate(); // force onDraw to redraw the screen with the ball in its new position
    }

    protected void onDraw(Canvas canvas)
    {
        // draw the paths

            // draw fills
            canvas.drawRect(outerRectangle, fillPaint);
            //canvas.drawRect(innerRectangle, backgroundPaint);

            // draw lines
            canvas.drawRect(outerRectangle, linePaint);
          //  canvas.drawRect(innerRectangle, linePaint);
          //  canvas.drawLine(outerRectangle.left,yCenter,innerRectangle.left,yCenter,linePaint);
           // canvas.drawLine(outerRectangle.left - 40f,yCenter - 50f,outerRectangle.left - 40f,yCenter+50f,linePaint);
          //  canvas.drawLine(outerRectangle.left - 40f,yCenter + 50f,outerRectangle.left - 20f,yCenter+30f,linePaint);
          //  canvas.drawLine(outerRectangle.left - 40f,yCenter + 50f,outerRectangle.left - 60f,yCenter+30f,linePaint);

        // draw label
        canvas.drawText("Project", 6f, labelTextSize, labelPaint);

        // draw stats (pitch, roll, tilt angle, tilt magnitude)
      /*  if (pathType == PATH_TYPE_SQUARE || pathType == PATH_TYPE_CIRCLE)
        {
            canvas.drawText("Wall hits = " + wallHits, 6f, updateY[5], statsPaint);
            canvas.drawText("-----------------", 6f, updateY[4], statsPaint);
        }
        canvas.drawText(String.format(Locale.CANADA, "Tablet pitch (degrees) = %.2f", pitch), 6f, updateY[3],
                statsPaint);
        canvas.drawText(String.format(Locale.CANADA, "Tablet roll (degrees) = %.2f", roll), 6f, updateY[2], statsPaint);
        canvas.drawText(String.format(Locale.CANADA, "Ball x = %.2f", xBallCenter), 6f, updateY[1], statsPaint);
        canvas.drawText(String.format(Locale.CANADA, "Ball y = %.2f", yBallCenter), 6f, updateY[0], statsPaint);
        */
        // draw the ball in its new location
        canvas.drawText(String.format(Locale.CANADA, "Count = %d", count), 6f, updateY[3],
                statsPaint);
        canvas.drawBitmap(ball, xBall, yBall, null);

    } // end onDraw

    /*
     * Configure the rolling ball panel according to setup parameters
     */
    public void configure(String pathMode,String orderOfControlArg)
    {
        // square vs. circle
        /*if (pathMode.equals("Square"))
            pathType = PATH_TYPE_SQUARE;
        else if (pathMode.equals("Circle"))
            pathType = PATH_TYPE_CIRCLE;
        else*/

        if (pathMode.equals("Medium"))
            moveSpeed = 5;
        else if (pathMode.equals("Hard"))
            moveSpeed = 10;

            pathWidth = PATH_WIDTH_MEDIUM;

        //gain = gainArg;
      // orderOfControl = orderOfControlArg;
       // numberL = numberArg;
    }

    // returns true if the ball is touching (i.e., overlapping) the line of the inner or outer path border
    public boolean ballTouchingLine()
    {

            ballNow.left = xBall;
            ballNow.top = yBall;
            ballNow.right = xBall + ballDiameter;
            ballNow.bottom = yBall + ballDiameter;

           // if(ballNow.left>=outerRectangle.left&&ballNow.left<=inner)
            //if (RectF.intersects(ballNow, outerRectangle) && !RectF.intersects(ballNow, outerShadowRectangle))
            if (RectF.intersects(ballNow, outerRectangle)) {
                if (!touched) {
                    count++; // touching outside rectangular border
                    touched = true;
                }
                return true;
            }

          //  if (RectF.intersects(ballNow, innerRectangle) && !RectF.intersects(ballNow, innerShadowRectangle))
           //     return true; // touching inside rectangular border




        return false;
    }

    public boolean ballTouchingEdge(){
        ballNow.left = xBall;
        ballNow.top = yBall;
        ballNow.right = xBall + ballDiameter;
        ballNow.bottom = yBall + ballDiameter;
        if (RectF.intersects(ballNow, edgeRectangle) && !RectF.intersects(ballNow, edgeShadowRectangle)) {
            miss ++;
            return true;
        }
        return false;


    }

    public boolean ballBeginning(){
        ballNow.left = xBall;
        ballNow.top = yBall;
        ballNow.right = xBall + ballDiameter;
        ballNow.bottom = yBall + ballDiameter;

        if (RectF.intersects(ballNow, lineRectangle))
            return true;
        /*if (pathType == PATH_TYPE_SQUARE)
        {

            if(xBallCenter>=outerRectangle.left&&xBallCenter<=innerRectangle.left&&yBallCenter==yCenter)
                return true;

        } else if (pathType == PATH_TYPE_CIRCLE)
        {
            if(xBallCenter>=outerRectangle.left&&xBallCenter<=innerRectangle.left&&yBallCenter==yCenter)
                return true;

        }*/
            return false;

    }

    public boolean anticheated(){
        ballNow.left = xBall;
        ballNow.top = yBall;
        ballNow.right = xBall + ballDiameter;
        ballNow.bottom = yBall + ballDiameter;

        if (RectF.intersects(ballNow, antiRectangle)){
            return true;

        }
        return false;


    }





}
