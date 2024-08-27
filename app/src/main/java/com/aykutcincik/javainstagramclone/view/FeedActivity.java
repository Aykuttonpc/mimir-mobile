package com.aykutcincik.javainstagramclone.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aykutcincik.javainstagramclone.R;
import com.aykutcincik.javainstagramclone.adapter.PostAdapter;
import com.aykutcincik.javainstagramclone.databinding.ActivityFeedBinding;
import com.aykutcincik.javainstagramclone.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class FeedActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firebaseFirestore;

    ArrayList<Post> postArrayList;
    private ActivityFeedBinding binding;
    PostAdapter postAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        postArrayList = new ArrayList<>();


        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        getData();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(postArrayList);
        binding.recyclerView.setAdapter(postAdapter);








    }
    private void  getData(){



        firebaseFirestore.collection("Posts").orderBy("date", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                if (error!= null){
                    Toast.makeText(FeedActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                 }

                if (value!=null){

                 for (DocumentSnapshot snapshot : value.getDocuments()){
                     Map<String  , Object> data  = snapshot.getData();
                     String userEmail = (String) data.get("useremail");
                     String comment = (String) data.get("comment");
                     String downloadUrl = (String) data.get("downloadurl");

                     Post post = new Post(userEmail,comment,downloadUrl);
                     postArrayList.add(post);

                 }

                 postAdapter.notifyDataSetChanged();

                }
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add_post){
            //upload activity

            Intent intentToUpload = new Intent(FeedActivity.this , UploadActivity.class);
            startActivity(intentToUpload);
        }else if (item.getItemId() == R.id.signout) {

            //sign out
            auth.signOut();
            Intent intentToMain = new Intent(FeedActivity.this , MainActivity.class);
            startActivity(intentToMain);
            finish();
        }




        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.option_menu, menu);



        return super.onCreateOptionsMenu(menu);
    }
}