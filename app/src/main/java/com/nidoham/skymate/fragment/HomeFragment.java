package com.nidoham.skymate.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

import com.nidoham.skymate.R;

import com.nidoham.skymate.databinding.FragmentHomeBinding;
import com.nidoham.skymate.fragment.home.*;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeTabAdapter tabAdapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewPager();
        setupTabLayout();
    }
    
    private void setupViewPager() {
        tabAdapter = new HomeTabAdapter(getActivity());
        binding.contentPager.setAdapter(tabAdapter);
        binding.contentPager.setOffscreenPageLimit(1); // Keep both fragments in memory
        binding.contentPager.setUserInputEnabled(false);
    }
    
    private void setupTabLayout() {
        new TabLayoutMediator(binding.tabLayout, binding.contentPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setIcon(R.drawable.ic_live);
                    break;
                case 1:
                    tab.setIcon(R.drawable.ic_trending);
                    break;
                case 2:
                    tab.setIcon(R.drawable.ic_tv);
                    break;
                case 3:
                    tab.setIcon(R.drawable.ic_games);
                    break;
            }
        }).attach();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // ViewPager2 Adapter for tabs
    private static class HomeTabAdapter extends FragmentStateAdapter {
        
        public HomeTabAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new SubHomeFragment();
                case 1:
                    return new SubTrendingFragment();
                case 2:
                    return new SubHomeFragment();
                case 3:
                    return new SubHomeFragment();
                    
                default:
                    return new SubHomeFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 4; // Trending and Live tabs
        }
    }
}