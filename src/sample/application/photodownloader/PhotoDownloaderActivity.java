package sample.application.photodownloader;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import java.net.Authenticator;
import java.net.PasswordAuthentication;




public class PhotoDownloaderActivity extends Activity {

	Context mContext = this;
	Object[] objects;
	static String strUrl=
			"https://api.datamarket.azure.com/Bing/Search/Image?$format=json&$top=10";
	char[] accountKey="<hVvx0p7DBlJJ4uofsHLCoEALMPWLpeFjWW5DZnFu/nE=>".toCharArray();
//p186リスト10の3～4行目の内容を修正後の内容に置き換えます。「$top」で指定する数値は検索件数です。デフォルトでは最大値の50件が指定されています。「<アカウントキー>」には自分のアカウントキーを入力しておきましょう
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_downloader);
        
        TextView tv =(TextView)findViewById(R.id.textView1);
        tv.setText(Html.fromHtml(getResources().getString(R.string.label_branding)));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        
        View button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new OnClickListener(){
        	
        	@Override
        	public void onClick(View arg0) {
        		doSearch();
        	}
        });
        
        View editText1 = findViewById(R.id.editText1);
        editText1.setOnKeyListener(new OnKeyListener() {
        	
        	@Override
        	public boolean onKey(View v, int keyCode, KeyEvent event) {
        		
        		if(event.getAction() == KeyEvent.ACTION_UP &&
        				keyCode == KeyEvent.KEYCODE_ENTER) {
        			doSearch();
        			return true;
        		}
        		return false;
        	}
        });
        Authenticator.setDefault(
        		new Authenticator() {
        			protected PasswordAuthentication getPasswordAuthentication() {
        				return new PasswordAuthentication("", accountKey);
        			}
        		}
        	);
    }
    public void doSearch() {
    	
    	URL url;
    	String q=null;
    	EditText et =(EditText)this.findViewById(R.id.editText1);
    	
    	try {
    		q = URLEncoder.encode(et.getText().toString(), "UTF-8");
    		url =new URL(strUrl + "&Query=" + q +"'");
    		new jsonTask().execute(url);
    	} catch (UnsupportedEncodingException e) {
    		e.printStackTrace();
    	} catch (MalformedURLException e) {
    		e.printStackTrace();
    	}
    }
    
    //プライベートクラスの実装
    private class jsonTask extends AsyncTask<URL, Integer, String> {

		@Override
		protected String doInBackground(URL... params) {
			
			HttpURLConnection connection=null;
			try {
				connection =(HttpURLConnection) params[0].openConnection();
				connection.setDoInput(true);
				connection.connect();
				
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(),"UTF-8"));
				String jsonText = reader.readLine();
				reader.close();
				return(jsonText);

			} catch (Exception e) {
				e.printStackTrace();
			}
			finally{
				if(connection != null) {
					connection.disconnect();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if(result != null){
				
				try {
					JSONObject jo = new JSONObject(result).
							getJSONObject("d");
					JSONArray jsonArray = null;
					jsonArray = jo.getJSONArray("results");
					
					objects = new Object[jsonArray.length()];
					URL[] thumbUrls = new URL[jsonArray.length()];
					
					for(int i = 0; i < jsonArray.length(); i++){
						objects[i] = jsonArray.getJSONObject(i);
						thumbUrls[i] = new URL(((JSONObject)objects[i]).
								getJSONObject("Thumbnail").getString("MediaUrl"));
					}
					
					new getThumbTask().execute(thumbUrls);
					
				} catch (JSONException e1) {
					e1.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
    }
    private class getThumbTask extends AsyncTask<URL, Integer, Bitmap[]> {

		@Override
		protected Bitmap[] doInBackground(URL... params) {
			
			HttpURLConnection connection = null;
			Bitmap[] bm = new Bitmap[params.length];
			
			try {
				for(int i=0; i<params.length; i++){
					connection = (HttpURLConnection) params[i].openConnection();
					connection.setDoInput(true);
					connection.connect();
					InputStream si = connection.getInputStream();
					bm[i] = BitmapFactory.decodeStream(si);
					si.close();
				}
				return bm;
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(connection != null)connection.disconnect();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap[] result) {
			super.onPostExecute(result);
		
			final ThumbnailAdapter adapter = new ThumbnailAdapter(mContext, objects, result);
			GridView lv = (GridView)findViewById(android.R.id.list);
			lv.setAdapter(adapter);
			lv.setOnItemClickListener(new OnItemClickListener() {
				
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					try {
						Uri downloadUri = Uri.parse(
								((JSONObject)adapter.getItem(position)).getString("MediaUrl"));
						DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
						DownloadManager.Request dr = new DownloadManager.Request(downloadUri); //Uriってなんですか？
						String fn = URLDecoder.decode(downloadUri.getLastPathSegment(),"UTF-8");
						
						dr.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fn);
						dm.enqueue(dr);
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			});
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_photo_downloader, menu);
        return true;
    }
}
