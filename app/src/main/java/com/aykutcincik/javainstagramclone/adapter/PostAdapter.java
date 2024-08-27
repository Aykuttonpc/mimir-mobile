package com.aykutcincik.javainstagramclone.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aykutcincik.javainstagramclone.databinding.RecyclerRowBinding;
import com.aykutcincik.javainstagramclone.model.Post;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostHolder> {

    private ArrayList<Post> postArrayList;

    public PostAdapter(ArrayList<Post> postArrayList) {
        this.postArrayList = postArrayList;
    }

    class PostHolder extends RecyclerView.ViewHolder {
        RecyclerRowBinding recyclerRowBinding;

        public PostHolder(@NonNull RecyclerRowBinding recyclerRowBinding) {
            super(recyclerRowBinding.getRoot());
            this.recyclerRowBinding = recyclerRowBinding;

        }
    }

    @NonNull
    @Override
    public PostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerRowBinding recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new PostHolder(recyclerRowBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostHolder holder, int position) {

        holder.recyclerRowBinding.recylerViewUserEmailText.setText(postArrayList.get(position).email);
        holder.recyclerRowBinding.recylerViewCommentText.setText(postArrayList.get(position).comment);
        Picasso.get().load(postArrayList.get(position).donwloadUrl).into(holder.recyclerRowBinding.recylerViewImageView);
    }

    @Override
    public int getItemCount() {
        return postArrayList.size();
    }


}