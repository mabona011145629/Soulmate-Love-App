package com.mabona.mobilesystems.soulmate;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class RepliesAdapter extends RecyclerView.Adapter<RepliesAdapter.ReplyViewHolder> {

    private List<ReplyItem> replyList;
    private OnReplyClickListener listener;
    private String authToken;
    private int myUserId;
    private String myUserName;
    private String myUserEmail;
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    public interface OnReplyClickListener {
        void onChatClick(ReplyItem reply);
        void onProfileViewClick(ReplyItem reply);
    }

    public RepliesAdapter(List<ReplyItem> replyList, OnReplyClickListener listener,
                          String authToken, int myUserId, String myUserName, String myUserEmail) {
        this.replyList = replyList;
        this.listener = listener;
        this.authToken = authToken;
        this.myUserId = myUserId;
        this.myUserName = myUserName;
        this.myUserEmail = myUserEmail;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        ReplyItem reply = replyList.get(position);
        holder.bind(reply, listener, authToken, myUserId, myUserName, myUserEmail);
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText;
        TextView statusText;
        TextView ageGenderText;
        TextView bioText;
        TextView responseTimeText;
        ImageView onlineIndicator;
        CardView itemCard;

        ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            statusText = itemView.findViewById(R.id.statusText);
            ageGenderText = itemView.findViewById(R.id.ageGenderText);
            bioText = itemView.findViewById(R.id.bioText);
            responseTimeText = itemView.findViewById(R.id.responseTimeText);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            itemCard = (CardView) itemView;
        }

        void bind(final ReplyItem reply, final OnReplyClickListener listener,
                  String authToken, int myUserId, String myUserName, String myUserEmail) {

            nameText.setText(reply.getFullName());

            // Set status badge color and text
            if ("approved".equals(reply.getRequestStatus())) {
                if ("love".equals(reply.getRequestType())) {
                    statusText.setText("✅ Love Accepted");
                    statusText.setBackgroundColor(itemView.getContext().getColor(R.color.green));
                } else {
                    statusText.setText("✅ Profile Access Granted");
                    statusText.setBackgroundColor(itemView.getContext().getColor(R.color.green));
                }
            } else {
                if ("love".equals(reply.getRequestType())) {
                    statusText.setText("❌ Love Declined");
                    statusText.setBackgroundColor(itemView.getContext().getColor(R.color.red));
                } else {
                    statusText.setText("❌ Profile Access Denied");
                    statusText.setBackgroundColor(itemView.getContext().getColor(R.color.red));
                }
            }

            String ageGender = reply.getAge() + " yrs • " +
                    reply.getGender().substring(0, 1).toUpperCase() +
                    reply.getGender().substring(1);
            ageGenderText.setText(ageGender);
            bioText.setText(reply.getBio());

            // Format response time
            responseTimeText.setText("Responded: " + formatDate(reply.getRespondedAt()));

            // Load profile image if available
            String imagePath = reply.getProfileImage();
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

            onlineIndicator.setVisibility(reply.isOnline() ? View.VISIBLE : View.GONE);

            // Show dialog on click
            itemView.setOnClickListener(v -> showReplyDialog(reply, listener, myUserId, myUserName, myUserEmail, authToken));
        }

        private void showReplyDialog(ReplyItem reply, OnReplyClickListener listener,
                                     int myUserId, String myUserName, String myUserEmail, String authToken) {
            Dialog dialog = new Dialog(itemView.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_reply_detail);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            TextView titleText = dialog.findViewById(R.id.titleText);
            TextView messageText = dialog.findViewById(R.id.messageText);
            CardView chatButton = dialog.findViewById(R.id.chatButton);
            CardView profileButton = dialog.findViewById(R.id.profileButton);
            TextView profileButtonText = dialog.findViewById(R.id.profileButtonText);
            CardView closeButton = dialog.findViewById(R.id.closeButton);

            // Set dialog content based on status
            if ("approved".equals(reply.getRequestStatus())) {
                if ("love".equals(reply.getRequestType())) {
                    titleText.setText("💕 Love Request Accepted");
                    messageText.setText("You accepted " + reply.getFullName() + "'s love request! You can now chat and view their full profile.");
                } else {
                    titleText.setText("👁️ Profile Access Granted");
                    messageText.setText("You granted " + reply.getFullName() + " access to view your profile. They can now see your full profile.");
                }
                profileButton.setEnabled(true);
                profileButton.setAlpha(1f);
                if (profileButtonText != null) {
                    profileButtonText.setText("View Profile");
                }
            } else {
                if ("love".equals(reply.getRequestType())) {
                    titleText.setText("💔 Love Request Declined");
                    messageText.setText("You declined " + reply.getFullName() + "'s love request. You can still chat, but profile access is limited.");
                } else {
                    titleText.setText("👁️ Profile Access Denied");
                    messageText.setText("You denied " + reply.getFullName() + " access to view your profile.");
                }
                profileButton.setEnabled(false);
                profileButton.setAlpha(0.5f);
                if (profileButtonText != null) {
                    profileButtonText.setText("Profile Locked");
                }
            }

            // Chat button always works
            chatButton.setOnClickListener(v -> {
                dialog.dismiss();
                listener.onChatClick(reply);
            });

            // Profile view button (only if approved or can view)
            profileButton.setOnClickListener(v -> {
                dialog.dismiss();
                if (reply.canViewProfile() || "approved".equals(reply.getRequestStatus())) {
                    listener.onProfileViewClick(reply);
                }
            });

            closeButton.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }

        private String formatDate(String timestamp) {
            try {
                java.text.SimpleDateFormat input = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                java.text.SimpleDateFormat output = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                java.util.Date date = input.parse(timestamp);
                return output.format(date);
            } catch (Exception e) {
                return timestamp;
            }
        }
    }
}