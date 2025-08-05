package com.nidoham.skymate.fragment.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nidoham.skymate.adapter.VideoAdapter;

import com.nidoham.skymate.databinding.FragmentsTrendingBinding;
import com.nidoham.strivo.kiosk.KioskList;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.schabi.newpipe.util.ExtractorHelper;

public class SubTrendingFragment extends Fragment implements VideoAdapter.OnVideoItemClickListener {

    private FragmentsTrendingBinding binding;
    private final List<StreamInfoItem> videoList = new ArrayList<>();
    private VideoAdapter videoAdapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private Page nextPage;
    private String trendingUrl;
    private boolean isLoading = false;
    private boolean hasMorePages = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentsTrendingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupSwipeRefresh();
        initializeTrendingUrl();
        loadTrendingVideos();
    }

    private void setupRecyclerView() {
        videoAdapter = new VideoAdapter(videoList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerViewTrending.setLayoutManager(layoutManager);
        binding.recyclerViewTrending.setAdapter(videoAdapter);

        // Add scroll listener for pagination
        binding.recyclerViewTrending.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMorePages && 
                        (visibleItemCount + pastVisibleItems) >= totalItemCount - 5) {
                        loadMoreVideos();
                    }
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshTrendingVideos);
    }

    private void initializeTrendingUrl() {
        try {
            trendingUrl = NewPipe.getService(KioskList.YOUTUBE_SERVICE_ID)
                    .getKioskList()
                    .getDefaultKioskExtractor()
                    .getLinkHandler()
                    .getUrl();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error initializing trending: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void refreshTrendingVideos() {
        nextPage = null;
        hasMorePages = true;
        videoList.clear();
        videoAdapter.notifyDataSetChanged();
        loadTrendingVideos();
    }

    private void loadTrendingVideos() {
        if (trendingUrl == null || isLoading) {
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setRefreshing(false);

        Single<KioskInfo> trendingObservable = ExtractorHelper.getKioskInfo(
                KioskList.YOUTUBE_SERVICE_ID,
                trendingUrl,
                false
        );

        disposables.add(trendingObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(kioskInfo -> {
                    binding.progressBar.setVisibility(View.GONE);
                    isLoading = false;

                    // Filter out active live streams while keeping regular videos and completed live videos
                    for (InfoItem item : kioskInfo.getRelatedItems()) {
                        if (item instanceof StreamInfoItem) {
                            StreamInfoItem streamItem = (StreamInfoItem) item;
                            
                            // Exclude active live streams (duration -1 typically indicates live content)
                            // Allow videos with duration 0 or positive values (regular videos and completed live streams)
                            if (streamItem.getDuration() != -1) {
                                videoList.add(streamItem);
                            }
                        }
                    }
                    
                    videoAdapter.notifyDataSetChanged();

                    // Store next page information for pagination
                    nextPage = kioskInfo.getNextPage();
                    hasMorePages = nextPage != null;

                }, throwable -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    Toast.makeText(requireContext(), "Error loading trending videos: " + throwable.getMessage(), 
                            Toast.LENGTH_LONG).show();
                }));
    }

    private void loadMoreVideos() {
        if (trendingUrl == null || isLoading || !hasMorePages || nextPage == null) {
            return;
        }

        isLoading = true;

        Single<ListExtractor.InfoItemsPage<StreamInfoItem>> moreItemsObservable = 
                ExtractorHelper.getMoreKioskItems(KioskList.YOUTUBE_SERVICE_ID, trendingUrl, nextPage);

        disposables.add(moreItemsObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(itemsPage -> {
                    isLoading = false;

                    List<StreamInfoItem> newItems = itemsPage.getItems();
                    if (newItems != null && !newItems.isEmpty()) {
                        int oldSize = videoList.size();
                        
                        // Apply the same filtering logic for pagination
                        for (StreamInfoItem item : newItems) {
                            if (item.getDuration() != -1) {
                                videoList.add(item);
                            }
                        }
                        
                        videoAdapter.notifyItemRangeInserted(oldSize, videoList.size() - oldSize);
                    }

                    // Update pagination information
                    nextPage = itemsPage.getNextPage();
                    hasMorePages = nextPage != null;

                }, throwable -> {
                    isLoading = false;
                    Toast.makeText(requireContext(), "Error loading more videos: " + 
                            throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    @Override
    public void onVideoItemClick(StreamInfoItem videoItem) {
        if (videoItem != null && videoItem.getUrl() != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(videoItem.getUrl()));
                requireContext().startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Cannot open video: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMoreOptionsClick(StreamInfoItem videoItem, int position) {
        if (videoItem != null && videoItem.getName() != null) {
            Toast.makeText(requireContext(), "Options: " + videoItem.getName(), Toast.LENGTH_SHORT).show();
            // TODO: Implement context menu with options like share, download, etc.
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}