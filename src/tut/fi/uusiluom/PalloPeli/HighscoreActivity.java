package tut.fi.uusiluom.PalloPeli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class HighscoreActivity extends Activity {

	private boolean newHighscoreFlag;
	private boolean highscoreGenFlag;
	private int curPoints;
	private int newRecordInd;
	private String rawHighscore;
	private String results[] = new String[10];
	private String curName;
	private int points[] = new int[5];
	private EditText name;
	private Button enterButton;
	private TextView nameText;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.highscore_layout);
        
        //asetetaan nimikenttä ja enter-button pois näkyvistä
        name = (EditText) findViewById(R.id.editTextName);
		name.setVisibility(View.INVISIBLE);
		enterButton = (Button) findViewById(R.id.buttonEnter);
		enterButton.setVisibility(View.INVISIBLE);
		nameText = (TextView) findViewById(R.id.nameTextView);
		nameText.setVisibility(View.INVISIBLE);
		
		newHighscoreFlag = false;	//flagista päätellään onko tehty uusi ennätys
		highscoreGenFlag = false;	//flagista päätellään pitääkö generoida uusi Highscore.txt tiedosto
		rawHighscore = null;
        curPoints = DrawCanvas.getPoints();		//otetaan pelissä saadut pisteet curPoints muuttujaan
		DrawCanvas.setPoints(0);	//nollataan pisteet
        
        ReadHighscore();	//luetaan Highscore.txt
        
        // jos rawHighscore null, niin Highscore.txt tiedostoa ei ole vielä luotu
        if(rawHighscore == null){	
        	highscoreGenFlag = true;	//EnterName metodi päättelee tämän perusteella, mitä tehdään kun nappia
        								//painetaan, tässä tapauksessa true --> luodaan uusi tiedosto
        	//asetetaan näkyviksi
        	name = (EditText) findViewById(R.id.editTextName);
    		name.setVisibility(View.VISIBLE);
    		enterButton = (Button) findViewById(R.id.buttonEnter);
    		enterButton.setVisibility(View.VISIBLE);
    		nameText = (TextView) findViewById(R.id.nameTextView);
    		nameText.setVisibility(View.VISIBLE);	
        }
        
        else{	//muuten tutkitaan, että riittävätkö pisteet highscoreen
        	newRecordInd = 0;	//mahdollisen uuden ennätyksen indeksi
        	while( (!newHighscoreFlag) && (newRecordInd < 5) ){
        		if(curPoints > points[newRecordInd]){
        			newHighscoreFlag = true;
        			break;
        		}
        		newRecordInd++;
        	}
        	
        	if(newHighscoreFlag == true){
        		//asetetaan näkyviksi 
            	name = (EditText) findViewById(R.id.editTextName);
        		name.setVisibility(View.VISIBLE);
        		enterButton = (Button) findViewById(R.id.buttonEnter);
        		enterButton.setVisibility(View.VISIBLE);
        		nameText = (TextView) findViewById(R.id.nameTextView);
        		nameText.setVisibility(View.VISIBLE);	
        	}
        }
        
        ShowHighScore();	//näytetään highscore
	}
	
	  @Override
	  protected void onPause() {
	    super.onPause();
	    finish();
	  }
	  
	
	//************************ Käynnistetään uusi peli *****************************
	public void PlayAgain(View view){
		//käynnistettään StartActivity
		Intent intent = new Intent(this, StartActivity.class);
		this.startActivity(intent);
		
	}
	
	
	//********************  näytetään highscore *************************************
	private void ShowHighScore(){
		//1.sija
	   TextView place1Name = (TextView) findViewById(R.id.place1NameTextView);
	   place1Name.setText(results[0]);
	   TextView place1Points = (TextView) findViewById(R.id.place1PointsTextView);
	   place1Points.setText(""+points[0]);
	   //2.sija
	   TextView place2Name = (TextView) findViewById(R.id.place2NameTextView);
	   place2Name.setText(results[1]);
	   TextView place2Points = (TextView) findViewById(R.id.place2PointsTextView);
	   place2Points.setText(""+points[1]);
	   //3.sija
	   TextView place3Name = (TextView) findViewById(R.id.place3NameTextView);
	   place3Name.setText(results[2]);
	   TextView place3Points = (TextView) findViewById(R.id.place3PointsTextView);
	   place3Points.setText(""+points[2]);
	   //4.sija
	   TextView place4Name = (TextView) findViewById(R.id.place4NameTextView);
	   place4Name.setText(results[3]);
	   TextView place4Points = (TextView) findViewById(R.id.place4PointsTextView);
	   place4Points.setText(""+points[3]);
	   //5.sija
	   TextView place5Name = (TextView) findViewById(R.id.place5NameTextView);
	   place5Name.setText(results[4]);
	   TextView place5Points = (TextView) findViewById(R.id.place5PointsTextView);
	   place5Points.setText(""+points[4]);
	}
	
	
	// ************* enter -napin clikkaus -metodi **********************
	public void EnterName(View view){
        //asetetaan nimikenttä ja enter-button pois näkyvistä
        name = (EditText) findViewById(R.id.editTextName);
		name.setVisibility(View.INVISIBLE);
		enterButton = (Button) findViewById(R.id.buttonEnter);
		enterButton.setVisibility(View.INVISIBLE);
		nameText = (TextView) findViewById(R.id.nameTextView);
		nameText.setVisibility(View.INVISIBLE);
		
		//piilotetaan näppäimistö
		InputMethodManager inputMgr = (InputMethodManager)getSystemService(this.INPUT_METHOD_SERVICE);
		inputMgr.hideSoftInputFromWindow(nameText.getWindowToken(), 0);
		
		//poimitaan syötetty nimi
		name = (EditText) findViewById(R.id.editTextName);
		curName = name.getText().toString();
		
		if (name.getText().length() == 0){
			curName = "empty";
		}
		
		if(highscoreGenFlag == true){
			GenNewHighscore();		//luodaan uusi Highscore.txt -tiedosto jos tiedostoa ei olemassa
		}
		else if(newHighscoreFlag == true){
			//ylikirjoitetaan vanhat ennätykset
			String nameSwap, nameSwap2;
			int pointSwap, pointSwap2;
			
			for (int i = 4; i > newRecordInd; i--) {
				results[i] = results[i-1];
				points[i] = points[i-1];
			}
			
			results[newRecordInd] = curName;
			points[newRecordInd] = curPoints;
					
			WriteNewRecord();		//kirjoitetaan uusi ennätys highscoreen
		}
		ReadHighscore();	//luetaan Highscore.txt
        ShowHighScore();	//näytetään highscore
	}
	
	
	// ************* luetaan highscore -tiedosto **********************
	private void ReadHighscore(){ 
		try {
	        BufferedReader bufferedReader = new BufferedReader(new FileReader(new 
	                File(getFilesDir()+File.separator+"Highscore.txt")));
			String read;
			StringBuilder builder = new StringBuilder("");
		while((read = bufferedReader.readLine()) != null){
			builder.append(read);
		}
		rawHighscore = builder.toString();
		bufferedReader.close();
        } 
        catch (Exception e1) {
            e1.printStackTrace();	//jos tiedostoa ei olemassa niin tapahtuu poikkeus
            						// --> rawHighscore jää nulliksi
        }
        
        if(rawHighscore != null){	//jos ei ole null, niin luetaan tiedosto
        	// poimitaan rawHighscore:sta nimet ja pisteet
        	// indeksit 0-4 nimet, 5-9 pisteet
        	StringTokenizer st = new StringTokenizer(rawHighscore, "#", false); 
        	
        	int i = 0;
        	while ( st.hasMoreTokens() && i < 10 ){ 
        		results[i] = st.nextToken();
        		i++;
        	}
        	
        	// poimit results:sta pisteet omaksi taulukokseen
        	//result:iin jää vain nimet
        	for (int j = 5; j < 10; j++) {
				 points[j-5] = Integer.parseInt(results[j]);
			}
        }
	}
	
	
	// ************* luodaan uusi highscore -tiedosto **********************
	private void GenNewHighscore(){
        try {
	        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new 
	                File(getFilesDir()+File.separator+"Highscore.txt")));
	        //kirjoitetaan uudet pisteet tiedostoon (loppuihin tyhjää)
	        bufferedWriter.write(curName + "#");
	        bufferedWriter.write("empty#");
	        bufferedWriter.write("empty#");
	        bufferedWriter.write("empty#");
	        bufferedWriter.write("empty#");
	        bufferedWriter.write(curPoints + "#");
	        bufferedWriter.write("0#");
	        bufferedWriter.write("0#");
	        bufferedWriter.write("0#");
	        bufferedWriter.write("0#");
	        bufferedWriter.close();
        } 
        catch (Exception e1) {
            e1.printStackTrace();
        }
	}
	
	
	// ************* kirjoitetaan highscore -tiedostoon **********************
	// (jos uusi ennätys)
	private void WriteNewRecord(){
        try {
	        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new 
	                File(getFilesDir()+File.separator+"Highscore.txt")));
        	// kirjoitetaan nimet ja pisteet highscoreen
        	// indeksit 0-4 nimet, 5-9 pisteet
	        for (int i = 0; i < 5; i++) {
	        	bufferedWriter.write(results[i] +"#");
			}
	        for (int i = 0; i < 5; i++) {
	        	bufferedWriter.write(points[i] +"#");
			}
	        //bufferedWriter.write("lalit poptani#");
	        bufferedWriter.close();
        } 
        catch (Exception e1) {
            e1.printStackTrace();
        }
	}
}
