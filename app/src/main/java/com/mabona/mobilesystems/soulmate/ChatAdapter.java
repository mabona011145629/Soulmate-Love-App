package com.mabona.mobilesystems.soulmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CHAT = 0;
    private static final int TYPE_AD = 1;
    private static final int TYPE_EMPTY = 2;  // NEW: For empty state

    private List<ChatItem> chatList;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ChatItem chat);
    }

    public ChatAdapter(List<ChatItem> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // Check if this is an empty state item
        if (position < chatList.size() && chatList.get(position).isEmptyState()) {
            return TYPE_EMPTY;
        }
        // Show ad after every 5 chat items (but not for empty state)
        if (position > 0 && position % 5 == 0 && !chatList.get(position - (position / 5)).isEmptyState()) {
            return TYPE_AD;
        }
        return TYPE_CHAT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_AD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ad, parent, false);
            return new AdViewHolder(view);
        } else if (viewType == TYPE_EMPTY) {
            // Use the same chat layout but we'll modify it for empty state
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat, parent, false);
            return new EmptyViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat, parent, false);
            return new ChatViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ChatViewHolder) {
            int chatPosition = position - (position / 5);
            if (chatPosition < chatList.size() && !chatList.get(chatPosition).isAd()) {
                ChatItem chat = chatList.get(chatPosition);
                ((ChatViewHolder) holder).bind(chat, listener);
            }
        } else if (holder instanceof EmptyViewHolder) {
            int chatPosition = position - (position / 5);
            if (chatPosition < chatList.size()) {
                ChatItem chat = chatList.get(chatPosition);
                ((EmptyViewHolder) holder).bind(chat);
            }
        } else if (holder instanceof AdViewHolder) {
            ((AdViewHolder) holder).loadAd();
        }
    }

    @Override
    public int getItemCount() {
        int chatCount = chatList.size();
        // Don't add ads if there's only an empty state item
        if (chatCount == 1 && chatList.get(0).isEmptyState()) {
            return 1;
        }
        int adCount = chatCount / 5;
        return chatCount + adCount;
    }

    // NEW: Empty ViewHolder for friendly messages
    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText;
        TextView lastMessageText;
        TextView timeText;
        ImageView onlineIndicator;

        EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            lastMessageText = itemView.findViewById(R.id.lastMessageText);
            timeText = itemView.findViewById(R.id.timeText);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }

        void bind(final ChatItem chat) {
            // Show empty state friendly message
            nameText.setText(chat.getName());
            lastMessageText.setText(chat.getLastMessage());
            lastMessageText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            lastMessageText.setPadding(0, 20, 0, 20);

            // Hide unnecessary elements
            timeText.setVisibility(View.GONE);
            onlineIndicator.setVisibility(View.GONE);

            // Set empty state icon
            profileImage.setImageResource(R.drawable.ic_empty_chat);
            profileImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            // Make item non-clickable
            itemView.setClickable(false);
            itemView.setEnabled(false);
        }
    }

    // Chat ViewHolder
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText;
        TextView lastMessageText;
        TextView timeText;
        ImageView onlineIndicator;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            lastMessageText = itemView.findViewById(R.id.lastMessageText);
            timeText = itemView.findViewById(R.id.timeText);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }

        void bind(final ChatItem chat, final OnChatClickListener listener) {
            // Reset to normal display
            lastMessageText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            lastMessageText.setPadding(0, 0, 0, 0);
            timeText.setVisibility(View.VISIBLE);

            nameText.setText(chat.getName());
            lastMessageText.setText(chat.getLastMessage());
            timeText.setText(chat.getTime());

            // Load profile image
            if (chat.getProfileImage() != null && !chat.getProfileImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(chat.getProfileImage())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.default_profile);
            }

            if (chat.isOnline()) {
                onlineIndicator.setVisibility(View.VISIBLE);
            } else {
                onlineIndicator.setVisibility(View.GONE);
            }

            itemView.setClickable(true);
            itemView.setEnabled(true);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onChatClick(chat);
                }
            });
        }
    }

    // Ad ViewHolder
    static class AdViewHolder extends RecyclerView.ViewHolder {
        AdView adView;

        AdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = itemView.findViewById(R.id.adView);
        }

        void loadAd() {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    adView.setVisibility(View.GONE);
                }

                @Override
                public void onAdLoaded() {
                    adView.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}