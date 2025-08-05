#### URL to details
    private void testDownloader() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
                StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl);

                // ✅ Update UI after building is complete
                runOnUiThread(() -> {
                    String result = "✅ Title: " + info.getName() + "\nUploader: " + info.getUploaderUrl();
                    statusTextView.setText(result);
                });

            } catch (Exception e) {
                runOnUiThread(() -> statusTextView.setText("❌ Error fetching video info"));
            }
        });
    }
    
    
    ## Fetch trending videos
    
    /* disposables.add(
            ExtractorHelper.getTrendingVideos(YOUTUBE_SERVICE_ID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(kioskInfo -> {
                    // Extract the list of items from KioskInfo
                    // Cast to InfoItem list since getRelatedItems() returns List<StreamInfoItem>
                    List<InfoItem> items = new ArrayList<>(kioskInfo.getRelatedItems());
                    
                    videoList.clear();
                    if (items != null && !items.isEmpty()) {
                        videoList.addAll(items);
                    }
                    videoAdapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);
                    
                    // Optional: Show message if no videos found
                    if (videoList.isEmpty()) {
                        Toast.makeText(requireContext(), "No trending videos found", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    
                    String errorMessage = error.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "Failed to load trending videos";
                    }
                    
                    Toast.makeText(requireContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    
                    // Log the error for debugging
                    if (error instanceof Exception) {
                        error.printStackTrace();
                    }
                })
        ); */