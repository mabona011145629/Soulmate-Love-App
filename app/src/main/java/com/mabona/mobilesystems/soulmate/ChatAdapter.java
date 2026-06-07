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
    private static final int TYPE_EMPTY = 2;
    private static final int AD_INTERVAL = 5;

    private List<ChatItem> chatList;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ChatItem chat);           // Click on whole item -> opens ChatActivity
        void onProfileImageClick(ChatItem chat);   // Click on profile image -> opens ProfileViewActivity
    }

    public ChatAdapter(List<ChatItem> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (chatList.size() == 1 && chatList.get(0).isEmptyState()) {
            return TYPE_EMPTY;
        }

        // Ad logic: Every 5 items AND at the very bottom
        if ((position + 1) % 6 == 0 || position == getItemCount() - 1) {
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
            // Calculate actual chat index by subtracting number of ads before this position
            int chatIndex = position - (position / 6);
            if (chatIndex < chatList.size()) {
                ChatItem chat = chatList.get(chatIndex);
                ((ChatViewHolder) holder).bind(chat, listener);
            }
        } else if (holder instanceof EmptyViewHolder) {
            if (!chatList.isEmpty()) {
                ((EmptyViewHolder) holder).bind(chatList.get(0));
            }
        } else if (holder instanceof AdViewHolder) {
            ((AdViewHolder) holder).loadAd();
        }
    }

    @Override
    public int getItemCount() {
        int chatCount = chatList.size();
        if (chatCount == 0) return 0;
        if (chatCount == 1 && chatList.get(0).isEmptyState()) return 1;

        // Total items = chats + ads (one after every 5 + one at the end)
        int adCount = chatCount / AD_INTERVAL;
        int total = chatCount + adCount;
        
        // If the last item isn't already an ad from the "every 5" logic, add a bottom ad
        if (chatCount % AD_INTERVAL != 0) {
            total++;
        }
        
        return total;
    }

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
            nameText.setText(chat.getName());
            lastMessageText.setText(chat.getLastMessage());
            lastMessageText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            lastMessageText.setPadding(0, 20, 0, 20);
            timeText.setVisibility(View.GONE);
            onlineIndicator.setVisibility(View.GONE);
            profileImage.setImageResource(R.drawable.ic_empty_chat);
            profileImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            itemView.setClickable(false);
            itemView.setEnabled(false);
        }
    }

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
            // Reset display
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

            // Online indicator
            if (chat.isOnline()) {
                onlineIndicator.setVisibility(View.VISIBLE);
            } else {
                onlineIndicator.setVisibility(View.GONE);
            }

            // Make item clickable
            itemView.setClickable(true);
            itemView.setEnabled(true);

            // Whole item click -> opens ChatActivity
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(chat);
                }
            });

            // Profile image click -> opens ProfileViewActivity
            profileImage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProfileImageClick(chat);
                }
            });
        }
    }

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