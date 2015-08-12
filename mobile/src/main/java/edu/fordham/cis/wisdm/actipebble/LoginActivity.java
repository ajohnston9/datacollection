package edu.fordham.cis.wisdm.actipebble;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.io.File;

/**
 * Allows users to set their name and choose an activity to collect data for
 * @author Andrew H. Johnston <a href="mailto:ajohnston9@fordham.edu">ajohnston9@fordham.edu</a>
 * @version 1.0STABLE
 */
public class LoginActivity extends Activity {

    /**
     * The field for setting a user's name
     */
    private EditText mName;

    /**
     * The button that takes a user to the training activity
     */
    private Button mStartTraining;


    /**
     * A button to force data to be sent to an email
     */
    private Button mForceDataSend;

    /**
     * The radio group holding the two sex buttons
     */
    private RadioGroup mSexRadioGroup;

    /**
     * The EditText for entering the users email
     */
    private EditText mEmail;

    /**
     * The EditText for entering the users age
     * @param savedInstanceState
     */
    private EditText mAge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mName = (EditText)findViewById(R.id.name);
        mEmail = (EditText) findViewById(R.id.email);
        mAge = (EditText) findViewById(R.id.age);
        mSexRadioGroup = (RadioGroup) findViewById(R.id.radioGrpSex);


        mForceDataSend = (Button) findViewById(R.id.pushDataButton);
        mForceDataSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(LoginActivity.this);
                alert.setTitle("Force send data");
                alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File[] dirfiles = getFilesDir().listFiles();
                        for (File file : dirfiles) {
                            if (!file.isDirectory() && file.getName().endsWith(".txt")) {
                                new Thread(new DataSender(getBaseContext(), file.getName())).start();
                            }
                        }
                        //Do work in new thread so UI doesn't get clogged up
                    }
                });
                alert.setNegativeButton("Forget it", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing
                    }
                });
                alert.show();
            }
        });
        mStartTraining = (Button)findViewById(R.id.login);

        mStartTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = mName.getText().toString().toLowerCase().trim().replace(" ", "_");
                char sex = (mSexRadioGroup.getCheckedRadioButtonId() == R.id.isFemale)? 'F' : 'M';
                int age = Integer.parseInt(mAge.getText().toString());
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                i.putExtra("SEX", sex);
                i.putExtra("AGE", age);
                i.putExtra("EMAIL", mEmail.getText().toString().trim());
                i.putExtra("NAME", name);
                startActivity(i);

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
