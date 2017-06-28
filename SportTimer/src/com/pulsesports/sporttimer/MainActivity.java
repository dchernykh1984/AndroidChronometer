package com.pulsesports.sporttimer;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.security.*;
import java.util.*;
import java.io.*;
import org.apache.commons.net.ftp.*;
import java.net.*;

public class MainActivity extends Activity implements View.OnKeyListener, Runnable
{

	public void run()
	{
		TextView operationStatus = (TextView)findViewById(R.id.editTextStatus);
		operationStatus.setText("Current status: " + currentStatus);
		Handler handler = new Handler();
		handler.postDelayed(this,1000);
	}
	
String ftpServer;
String ftpFolder;
String ftpLogin;
String ftpPassword;
String pointNumber;
boolean writeNext = true;
String defaultRootDirectory;
String currentBackupFile;
String currentStatus = "none";

	public boolean onKey(View thisView, int keyCode, KeyEvent eventInfo)
	{
		if((thisView.getId()==R.id.editTextNextNumber ||
		thisView.getId()==R.id.editTextEventDescription ||
		thisView.getId()==R.id.editTextPenalty) && 
		keyCode == 66) {
			if(writeNext) {
				EditText nextNumber = (EditText)findViewById(R.id.editTextNextNumber);
				EditText textResults = (EditText)findViewById(R.id.editTextResults);
				EditText textEvent = (EditText)findViewById(R.id.editTextEventDescription);
				EditText textPenalty = (EditText)findViewById(R.id.editTextPenalty);
				long currentTime = System.currentTimeMillis()+TimeZone.getDefault().getRawOffset();
				String backupFileName = currentTime+".txt";
				long days = currentTime/24000/3600;
				currentTime -= days*24000*3600;
				long hours = currentTime/3600000;
				currentTime -= hours*3600000;
				long minutes = currentTime/60000;
				currentTime -= minutes*60000;
				long seconds = currentTime/1000;
				currentTime -= seconds*1000;
				long msFirst = currentTime/100;
				long msSecond = currentTime/10%10;
				long msThird = currentTime%10;
				
				textResults.setText(
					nextNumber.getText().toString() + 
					"#"+ days + " " + hours+
					":"+minutes+":"+
					seconds+"."+msFirst+msSecond+msThird+
					"#"+textEvent.getText().toString()+"#"
					+textPenalty.getText().toString()+"#\n" +
					textResults.getText().toString());
				nextNumber.setText("");
				writeFile(defaultRootDirectory,
				"results.txt",textResults.getText().toString());
				writeFile(defaultRootDirectory+"/timerBackup",backupFileName,textResults.getText().toString());
				if(openFTPConfig(defaultRootDirectory)) {
					currentBackupFile = defaultRootDirectory+"/timerBackup/"+backupFileName;
					currentStatus = "uploading " + currentBackupFile;
					Thread upl = new Thread(uploader);
					upl.start();
				}
				writeNext = false;
			} else {
				writeNext = true;
			}
			return true;
		}
		return false;
	}

	public Runnable uploader = new Runnable() {
	@Override
	public void run()
	{
		String uploadFileName = (pointNumber.equals("0"))?"results.txt":"results_"+pointNumber+".txt";
		String backupFileName = currentBackupFile;
		FTPClient ftpClient = new FTPClient();
		try
		{

/*			if (android.os.Build.VERSION.SDK_INT > 9) {
				StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
				StrictMode.setThreadPolicy(policy);
			}*/
			ftpClient.connect(InetAddress.getByName(ftpServer));
			ftpClient.login(ftpLogin,ftpPassword);
			if(!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
				ftpClient.disconnect();
				throw new Exception("login failed:"+ftpClient.getReplyCode());
			}
			ftpClient.enterLocalPassiveMode();
			ftpClient.changeWorkingDirectory(ftpFolder);
			ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
			ftpClient.storeFile(uploadFileName,new FileInputStream(new File(backupFileName)));
			ftpClient.logout();
			ftpClient.disconnect();
			currentStatus = "upload sucseeded " +
									ftpLogin+"@"+ftpServer+ftpFolder+uploadFileName;
		}
		catch (Exception e)
		{
			currentStatus = ftpLogin + "@" + ftpServer + ftpFolder + uploadFileName + " failed with output:" + e.getStackTrace()[0].toString()
			+"\n"+e.getMessage();
	
	/*		operationStatus.setText("Operation status: ");
			for(int i =0; i<e.getStackTrace().length&&i<10;i++) {
				operationStatus.setText(operationStatus.getText()+"\n"+e.getStackTrace()[i].toString());
			}*/
		}
	}
	};

	private boolean openFTPConfig(String defaultRootDirectory)
	{
		try {
			File configFile = new File(defaultRootDirectory+"/ftpConfig.txt");
			if(!configFile.exists()) {
				currentStatus = "no ftp config-no upload " + defaultRootDirectory+"/ftpConfig.txt";
				return false;
			}
			BufferedReader br = new BufferedReader(new FileReader(configFile.getAbsolutePath()));
			ftpServer = br.readLine();
			ftpFolder = br.readLine();
			ftpLogin = br.readLine();
			ftpPassword = br.readLine();
			pointNumber = br.readLine();
			br.close();
			
		} catch (Exception e) {
			currentStatus = e.getMessage();
			return false;
		}
		return true;
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
		Handler handler = new Handler();
		handler.postDelayed(this,1000);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		((EditText)findViewById(R.id.editTextNextNumber)).setOnKeyListener(this);
		((EditText)findViewById(R.id.editTextEventDescription)).setOnKeyListener(this);
		((EditText)findViewById(R.id.editTextPenalty)).setOnKeyListener(this);
		defaultRootDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + 
			"/DCIM/TTStarter";
		((EditText)findViewById(R.id.editTextResults)).setText(readFile(defaultRootDirectory,
				  "results.txt"));
    }
		@Override
	public boolean onCreateOptionsMenu(Menu main_menu) {
		getMenuInflater().inflate(R.menu.main_menu,main_menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menuReset:
				((EditText)findViewById(R.id.editTextResults)).setText("");
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	public void writeFile(String filePath,String fileName, String value) {
		try
		{
			File locationDirs = new File(filePath);
			if(!locationDirs.exists()) {
				locationDirs.mkdirs();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(filePath+"/"+fileName));
			bw.write(value);
			bw.close();
		}
		catch (Exception e)
		{
			currentStatus = e.getMessage();
		}
	}
	public String readFile(String filePath,String fileName) {
		try {
			File inputFile = new File(filePath+"/"+fileName);
			if(!inputFile.exists()) {
				return new String();
			}
			BufferedReader br = new BufferedReader(new FileReader(filePath+"/"+fileName));
			String line = br.readLine();
			String result = new String();
			while( (line != null) && (!line.isEmpty()) ){
				result = result + line + "\n";
				line = br.readLine();
			}
			br.close();
			return result;
		}
		catch (Exception e)
		{
			currentStatus = e.getMessage();
			return e.getMessage();
		}
	}
}
