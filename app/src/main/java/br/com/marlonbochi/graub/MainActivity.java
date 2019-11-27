package br.com.marlonbochi.graub;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button btnGetAgenda, btnSaveFirebase;
    private Cursor cursor;
    ListView listViewAgenda, listViewFirebase;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGetAgenda = findViewById(R.id.btnGetAgenda);
        btnSaveFirebase = findViewById(R.id.btnSaveFirebase);
        listViewAgenda = findViewById(R.id.listViewAgenda);
        listViewFirebase = findViewById(R.id.listViewFirebase);

        Intent receiveData = getIntent();
        Gson gson = new Gson();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null && currentUser.getEmail() == "") {
            goToLogin();
        }

        ActivityCompat.requestPermissions(MainActivity.this,new String[]
                {Manifest.permission.READ_CONTACTS},1);


        btnGetAgenda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri caminho = ContactsContract.Contacts.CONTENT_URI;
                ContentResolver contentResolver = MainActivity.this.getContentResolver();
                cursor = contentResolver.query(caminho, null, null, null,ContactsContract.Contacts.DISPLAY_NAME);

                if (cursor.getCount() > 0)
                {
                    List<String> contacts = new ArrayList<>();
                    while (cursor.moveToNext())
                    {
                        String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        String has_phone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        if (!has_phone.endsWith("0")) {

                            String cellphone = "";
                            Cursor cp = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

                            if (cp != null && cp.moveToFirst()) {
                                cellphone = cp.getString(cp.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                cp.close();
                            }

                            name += " - " + cellphone;
                        }
                        contacts.add(name);
                    }

                    listViewAgenda.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                            android.R.layout.simple_list_item_1, contacts));
                }
            }
        });


        btnSaveFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                Map<String, Object> contact = new HashMap<>();

                for (int i = 0; i < listViewAgenda.getChildCount(); i++) {
                    // Get row's spinner
                    View listItem = listViewAgenda.getChildAt(i);

                    TextView item = (TextView)listItem;

                    contact.put("idUser", currentUser.getUid());
                    contact.put("contact", item.getText());

                    db.collection("agenda")
                            .add(contact)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("Added into firebase", "DocumentSnapshot added with ID: " + documentReference.getId());

                                    getInfoAgendaFirebase();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("ErrorAdd", "Error adding document", e);
                                }
                            });
                }
            }
        });
    }

    protected void goToLogin() {

        FirebaseAuth.getInstance().signOut();

        Intent sendData = new Intent(MainActivity.this, LoginActivity.class);

        startActivity(sendData);
    }

    protected void getInfoAgendaFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("agenda")
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {

                    List<String> contacts = new ArrayList<>();

                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> data = document.getData();
                            contacts.add(data.get("contact").toString());

                        }

                        listViewFirebase.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                                android.R.layout.simple_list_item_1, contacts));
                    } else {
                        Log.w("errorGetAgenda", "Error getting documents.", task.getException());
                    }
                }
            });
    }

}
