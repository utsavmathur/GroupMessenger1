package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {


    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String[] ports = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static int seq_num=0;

    private Uri mUri;

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText editText = (EditText) findViewById(R.id.editText1);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e("my", "Can't create a ServerSocket:"+e.toString());
            return;
        }

        Button bsend=(Button)findViewById(R.id.button4);
        bsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(),"send button clicked",Toast.LENGTH_LONG).show();
                String msg = editText.getText().toString();
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg+"\n"); // This is one way to display a string.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                Log.i("my","send button clicked");
            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket= null;
            DataInputStream in= null;
            try {
                while(true) {
                    socket = serverSocket.accept();
                    in = new DataInputStream(socket.getInputStream());
                    String line = "";
                    try {
                        line = in.readUTF();
                        publishProgress(line);
                        //line = in.readUTF();
                    } catch (IOException i) {
                        Log.e("my", i.toString());
                    }
                    in.close();
                    socket.close();
                }
            }
            catch(IOException i){
                Log.e("my", i.toString());
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            //code to store the messages in DB  along with sequence number.

            //Toast.makeText(getApplicationContext(),"seq: "+seq_num,Toast.LENGTH_LONG).show();
            Log.i("db","seq:"+seq_num+"|"+strReceived);

            ContentValues cv = new ContentValues();
            cv.put("key", Integer.toString(seq_num));
            cv.put("value", strReceived);

            getContentResolver().insert(mUri, cv);

            seq_num++;
            return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                /*String remotePort = REMOTE_PORT0;
                if (msgs[1].equals(REMOTE_PORT0))
                    remotePort = REMOTE_PORT1;*/
                String msgToSend = msgs[0];
                for(String remotePort:  ports)
                {
                    //if (!remotePort.equals(msgs[1]))
                    //{
                        try{
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));


                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            try{
                                out.writeUTF(msgToSend);
                            }
                            catch (IOException i){
                                Log.e("my", i.toString());
                            }
                        }catch (Exception e)
                        {
                            Log.e("my",e.toString());
                        }
                    //}
                   /* else
                    {
                        Log.i("db","seq:"+seq_num+"|"+msgToSend);
                        //code to add to DB
                        ContentValues cv = new ContentValues();
                        cv.put("key", Integer.toString(seq_num));
                        cv.put("value", msgToSend);

                        getContentResolver().insert(mUri, cv);

                        seq_num++;
                    }*/
                }
            } catch (Exception e) {
                Log.e("my", "ClientTask socket IOException");
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
