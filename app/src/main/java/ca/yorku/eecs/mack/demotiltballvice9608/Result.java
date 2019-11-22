package ca.yorku.eecs.mack.demotiltballvice9608;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.TextView;

public class Result extends Activity {

    int lap;
    int wallHit;
    float totalT;
    float inPath;
    float miss;
    float acc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);

        Bundle b = getIntent().getExtras();
       // lap = b.getInt("laps");
       // wallHit = b.getInt("hit");
        totalT = b.getFloat("time");
        miss = b.getFloat("miss");
        acc = b.getFloat("acc");
        //inPath = b.getFloat("inPath");

       /* TextView tx1 = (TextView) findViewById(R.id.lapsN);
        tx1.setText(Integer.toString(lap));


        TextView tx3 = (TextView) findViewById(R.id.wallhitsN);
        tx3.setText(Integer.toString(wallHit));

        TextView tx4 = (TextView) findViewById(R.id.inpathtimeN);
        tx4.setText(String.format("%.2f",100*inPath)+"%");*/

        TextView tx2 = (TextView) findViewById(R.id.laptimeN);
        tx2.setText(String.format("%.2f",totalT));
        TextView tx3 = (TextView) findViewById(R.id.wallhitsN);
        tx3.setText(String.format("%.2f",miss));
        TextView tx4 = (TextView) findViewById(R.id.inpathtimeN);
        tx4.setText(String.format("%.2f",100*acc)+"%");
    }











    public void clickSetup(View view)
    {
        Intent i = new Intent(getApplicationContext(),DemoTiltBallSetup.class);
        startActivity(i);

    }
    public void clickExit(View view)
    {
        super.onDestroy(); // cleanup
        this.finish(); // terminate
    }
}
