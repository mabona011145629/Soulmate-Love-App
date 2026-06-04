package com.mabona.mobilesystems.soulmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private List<SearchItem> userList;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(SearchItem user);
        // REMOVED onSendRequestClick - not needed
    }

    public SearchAdapter(List<SearchItem> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_user, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        SearchItem user = userList.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText;
        TextView ageGenderText;
        TextView bioText;
        TextView locationText;
        ImageView onlineIndicator;

        SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            ageGenderText = itemView.findViewById(R.id.ageGenderText);
            bioText = itemView.findViewById(R.id.bioText);
            locationText = itemView.findViewById(R.id.locationText);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }

        void bind(final SearchItem user, final OnUserClickListener listener) {
            nameText.setText(user.getFullName());

            String ageGender = user.getAge() + " yrs • " +
                    user.getGender().substring(0, 1).toUpperCase() +
                    user.getGender().substring(1);
            ageGenderText.setText(ageGender);

            bioText.setText(user.getBio());
            locationText.setText("📍 " + user.getLocation());

            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getProfileImage())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.default_profile);
            }

            if (user.isOnline()) {
                onlineIndicator.setVisibility(View.VISIBLE);
            } else {
                onlineIndicator.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}