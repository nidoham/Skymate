package com.nidoham.skymate.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nidoham.skymate.databinding.ItemVideoBinding;
import com.nidoham.skymate.R;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.util.image.PicassoHelper;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<StreamInfoItem> videoList;
    private final OnVideoItemClickListener clickListener;
    private Context context;

    public interface OnVideoItemClickListener {
        void onVideoItemClick(StreamInfoItem videoItem);
        void onMoreOptionsClick(StreamInfoItem videoItem, int position);
    }

    public VideoAdapter(List<StreamInfoItem> videoList, OnVideoItemClickListener clickListener) {
        this.videoList = videoList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        ItemVideoBinding binding = ItemVideoBinding.inflate(inflater, parent, false);
        return new VideoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        StreamInfoItem item = videoList.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return videoList != null ? videoList.size() : 0;
    }

    public void updateVideoList(List<StreamInfoItem> newVideoList) {
        this.videoList = newVideoList;
        notifyDataSetChanged();
    }

    public void addVideos(List<StreamInfoItem> newVideos) {
        if (newVideos != null && !newVideos.isEmpty()) {
            int startPosition = videoList.size();
            videoList.addAll(newVideos);
            notifyItemRangeInserted(startPosition, newVideos.size());
        }
    }

    public void clearVideos() {
        if (videoList != null) {
            int itemCount = videoList.size();
            videoList.clear();
            notifyItemRangeRemoved(0, itemCount);
        }
    }

    public void removeVideo(int position) {
        if (videoList != null && position >= 0 && position < videoList.size()) {
            videoList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, videoList.size() - position);
        }
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {

        private final ItemVideoBinding binding;

        public VideoViewHolder(@NonNull ItemVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(StreamInfoItem videoItem, int position) {
            if (videoItem == null) return;

            // Set video title
            binding.txtTitle.setText(videoItem.getName());

            // Build and set channel info string
            String channelInfo = buildChannelInfoString(videoItem);
            binding.txtInfo.setText(channelInfo);

            // Set duration or LIVE status
            setDurationOrLiveStatus(videoItem);

            // Load thumbnail using PicassoHelper
            loadThumbnail(videoItem.getThumbnails());

            // Load channel avatar using PicassoHelper
            loadChannelAvatar(videoItem.getUploaderAvatars());

            // Handle click events
            setupClickListeners(videoItem, position);
        }

        private void setDurationOrLiveStatus(StreamInfoItem videoItem) {
            if (isCurrentlyLive(videoItem)) {
                binding.txtDuration.setBackgroundColor(Color.RED);
                binding.txtDuration.setTextColor(Color.WHITE);
                binding.txtDuration.setText("LIVE");
            } else {
                binding.txtDuration.setBackgroundResource(R.drawable.duration_background);
                binding.txtDuration.setTextColor(Color.WHITE);
                binding.txtDuration.setText(formatDuration(videoItem.getDuration()));
            }
        }

        private boolean isCurrentlyLive(StreamInfoItem videoItem) {
            StreamType streamType = videoItem.getStreamType();
            if (streamType != StreamType.LIVE_STREAM) {
                return false;
            }

            // If duration is -1 or 0, it's likely currently live
            long duration = videoItem.getDuration();
            if (duration <= 0) {
                return true;
            }

            // Check upload date to distinguish current live vs past live sessions
            DateWrapper uploadDate = videoItem.getUploadDate();
            if (uploadDate != null) {
                Calendar calendar = uploadDate.date();
                if (calendar != null) {
                    long uploadTimeMs = calendar.getTimeInMillis();
                    long currentTimeMs = System.currentTimeMillis();
                    long diffMs = currentTimeMs - uploadTimeMs;
                    
                    // If uploaded more than 24 hours ago and has duration, likely past live session
                    return !(diffMs > 24 * 60 * 60 * 1000 && duration > 0);
                }
            }

            return true;
        }

        private void loadThumbnail(List<Image> thumbnails) {
            // Use PicassoHelper for consistent image loading with proper caching and transformations
            PicassoHelper.loadThumbnail(thumbnails)
                    .fit()
                    .centerCrop()
                    .tag(this) // Use ViewHolder as tag for proper cancellation
                    .into(binding.imgThumb);
        }

        private void loadChannelAvatar(List<Image> avatars) {
            // Use PicassoHelper for consistent avatar loading
            PicassoHelper.loadAvatar(avatars)
                    .fit()
                    .centerCrop()
                    .tag(this) // Use ViewHolder as tag for proper cancellation
                    .into(binding.imgAvatar);
        }

        private void setupClickListeners(StreamInfoItem videoItem, int position) {
            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onVideoItemClick(videoItem);
                }
            });

            binding.btnMore.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMoreOptionsClick(videoItem, position);
                }
            });
        }

        private String buildChannelInfoString(StreamInfoItem videoItem) {
            StringBuilder info = new StringBuilder();

            // Channel name
            String uploader = videoItem.getUploaderName();
            info.append(!TextUtils.isEmpty(uploader) ? uploader : "Unknown Channel");

            // View count
            long viewCount = videoItem.getViewCount();
            if (viewCount >= 0) {
                info.append(" • ").append(formatViewCount(viewCount));
            }

            // Upload date
            DateWrapper uploadDate = videoItem.getUploadDate();
            if (uploadDate != null) {
                Calendar calendar = uploadDate.date();
                if (calendar != null) {
                    long uploadTimeMs = calendar.getTimeInMillis();
                    info.append(" • ").append(formatTimeAgo(uploadTimeMs));
                }
            }

            return info.toString();
        }

        private String formatTimeAgo(long uploadTimeMs) {
            long currentTimeMs = System.currentTimeMillis();
            long diffMs = currentTimeMs - uploadTimeMs;
            long diffSeconds = diffMs / 1000;

            // Handle future dates or very recent uploads
            if (diffSeconds < 0) return "just now";
            if (diffSeconds < 60) return diffSeconds <= 5 ? "just now" : diffSeconds + " seconds ago";

            long diffMinutes = diffSeconds / 60;
            if (diffMinutes < 60) return diffMinutes == 1 ? "1 minute ago" : diffMinutes + " minutes ago";

            long diffHours = diffMinutes / 60;
            if (diffHours < 24) return diffHours == 1 ? "1 hour ago" : diffHours + " hours ago";

            long diffDays = diffHours / 24;
            if (diffDays < 7) return diffDays == 1 ? "1 day ago" : diffDays + " days ago";

            long diffWeeks = diffDays / 7;
            if (diffDays < 31) return diffWeeks == 1 ? "1 week ago" : diffWeeks + " weeks ago";

            long diffMonths = diffDays / 30;
            if (diffDays < 365) return diffMonths == 1 ? "1 month ago" : diffMonths + " months ago";

            long diffYears = diffDays / 365;
            if (diffYears < 2) {
                return "1 year ago";
            } else {
                // For very old videos, show the actual date
                return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        .format(new Date(uploadTimeMs));
            }
        }

        private String formatDuration(long durationInSeconds) {
            if (durationInSeconds <= 0) return "0:00";

            long hours = durationInSeconds / 3600;
            long minutes = (durationInSeconds % 3600) / 60;
            long seconds = durationInSeconds % 60;

            if (hours > 0) {
                return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
            }
        }

        private String formatViewCount(long viewCount) {
            if (viewCount <= 0) return "No views";
            if (viewCount == 1) return "1 view";
            if (viewCount < 1_000) return viewCount + " views";
            
            if (viewCount < 1_000_000) {
                double thousands = viewCount / 1_000.0;
                if (thousands < 10) {
                    return String.format(Locale.getDefault(), "%.1fK views", thousands);
                } else {
                    return String.format(Locale.getDefault(), "%.0fK views", thousands);
                }
            }
            
            if (viewCount < 1_000_000_000) {
                double millions = viewCount / 1_000_000.0;
                return String.format(Locale.getDefault(), "%.1fM views", millions);
            }
            
            double billions = viewCount / 1_000_000_000.0;
            return String.format(Locale.getDefault(), "%.1fB views", billions);
        }
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        // Cancel any pending image loads for this ViewHolder to prevent memory leaks
        PicassoHelper.cancelTag(holder);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Clean up any pending requests when adapter is detached
        if (context != null) {
            PicassoHelper.cancelTag(context);
        }
    }
}