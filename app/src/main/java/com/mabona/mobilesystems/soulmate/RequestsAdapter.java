package com.mabona.mobilesystems.soulmate;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {

    private List<RequestItem> requestList;
    private OnRequestClickListener listener;
    private String authToken;
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    public interface OnRequestClickListener {
        void onRequestClick(RequestItem request);
    }

    public RequestsAdapter(List<RequestItem> requestList, OnRequestClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RequestItem request = requestList.get(position);
        holder.bind(request, listener, authToken);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText;
        TextView typeText;
        TextView ageGenderText;
        TextView bioText;
        ImageView onlineIndicator;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            typeText = itemView.findViewById(R.id.typeText);
            ageGenderText = itemView.findViewById(R.id.ageGenderText);
            bioText = itemView.findViewById(R.id.bioText);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }

        void bind(final RequestItem request, final OnRequestClickListener listener, String authToken) {
            nameText.setText(request.getFullName());

            // Set request type text with icon
            if ("love".equals(request.getRequestType())) {
                typeText.setText("❤️ Love Request");
                typeText.setTextColor(itemView.getContext().getColor(R.color.pink));
            } else {
                typeText.setText("👁️ Profile View Request");
                typeText.setTextColor(itemView.getContext().getColor(R.color.purple));
            }

            String ageGender = request.getAge() + " yrs • " +
                    request.getGender().substring(0, 1).toUpperCase() +
                    request.getGender().substring(1);
            ageGenderText.setText(ageGender);
            bioText.setText(request.getBio());

            // Load profile image using the path with get_image.php gateway
            String imagePath = request.getProfileImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                String imageUrl = BASE_URL + "get_image.php?path=" + Uri.encode(imagePath) + "&token=" + authToken;
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.default_profile);
            }

            onlineIndicator.setVisibility(request.isOnline() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onRequestClick(request));
        }
    }
}