package edu.uncc.inclass09;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import edu.uncc.inclass09.databinding.FragmentPostsBinding;
import edu.uncc.inclass09.databinding.PagingRowItemBinding;
import edu.uncc.inclass09.databinding.PostRowItemBinding;
import edu.uncc.inclass09.models.Post;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PostsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PostsFragment extends Fragment {

    final String TAG = "test";
    private OkHttpClient client = new OkHttpClient();

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM_TOKEN = "token";

    private String token;

    public PostsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param token Parameter 1.
     * @return A new instance of fragment PostsFragment.
     */
    public static PostsFragment newInstance(String token) {
        PostsFragment fragment = new PostsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TOKEN, token);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            token = getArguments().getString(ARG_PARAM_TOKEN);
        }
    }

    FragmentPostsBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPostsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        binding.textViewTitle.setText("Hello " + sharedPref.getString("user_fullname", null));

        binding.buttonCreatePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.createPost();
            }
        });

        binding.buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.logout();
            }
        });

        getPosts(1);

        binding.recyclerViewPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postsAdapter = new PostsAdapter();
        binding.recyclerViewPosts.setAdapter(postsAdapter);

        binding.recyclerViewPaging.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        pagingAdapter = new PagingAdapter();
        binding.recyclerViewPaging.setAdapter(pagingAdapter);

        binding.textViewPaging.setText("Loading ...");

        getActivity().setTitle(R.string.posts_label);
    }

    PostsAdapter postsAdapter;
    PagingAdapter pagingAdapter;
    ArrayList<Post> mPosts = new ArrayList<>();
    ArrayList<String> mPages = new ArrayList<>();

    void getPosts(int page) {
        String token;
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        token = sharedPref.getString("token", null);

        HttpUrl postsUrl = HttpUrl.parse("https://www.theappsdr.com/posts").newBuilder()
                .addQueryParameter("page", String.valueOf(page))
                .build();

        Request request = new Request.Builder()
                .url(postsUrl)
                .addHeader("Authorization", "BEARER " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());

                        int totalCount = json.getInt("totalCount");
                        int remainder = totalCount%10;
                        if (remainder > 0){
                            remainder = 1;
                        }
                        int pages = totalCount/10 + remainder;

                        mPages.clear();
                        for (int j = 1; j <= pages; j++) {
                            mPages.add(String.valueOf(j));
                        }

                        JSONArray jsonPosts = json.getJSONArray("posts");
                        mPosts.clear();
                        for (int i = 0; i < jsonPosts.length(); i++){
                            JSONObject jsonPostObject = jsonPosts.getJSONObject(i);

                            Post post = new Post();
                            post.created_by_name = jsonPostObject.getString("created_by_name");
                            post.post_id = jsonPostObject.getString("post_id");
                            post.created_by_uid = jsonPostObject.getString("created_by_uid");
                            post.post_text = jsonPostObject.getString("post_text");
                            post.created_at = jsonPostObject.getString("created_at");

                            mPosts.add(post);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    postsAdapter.notifyDataSetChanged();
                                    pagingAdapter.notifyDataSetChanged();
                                    binding.textViewPaging.setText("Showing Page " + page + " out of " + pages);
                                }
                            });
                        }

                    } catch (JSONException e) {
                        Log.d(TAG, "onResponse: " + e.getMessage());
                    }
                } else {
                    Log.d(TAG, "onResponse: " + response.body().string());
                }
            }
        });
    }

    class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostsViewHolder> {
        @NonNull
        @Override
        public PostsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            PostRowItemBinding binding = PostRowItemBinding.inflate(getLayoutInflater(), parent, false);
            return new PostsViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull PostsViewHolder holder, int position) {
            Post post = mPosts.get(position);
            holder.setupUI(post);
        }

        @Override
        public int getItemCount() {
            return mPosts.size();
        }

        class PostsViewHolder extends RecyclerView.ViewHolder {
            PostRowItemBinding mBinding;
            Post mPost;
            public PostsViewHolder(PostRowItemBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
            }

            public void setupUI(Post post){
                mPost = post;
                mBinding.textViewPost.setText(post.getPost_text());
                mBinding.textViewCreatedBy.setText(post.getCreated_by_name());
                mBinding.textViewCreatedAt.setText(post.getCreated_at());
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                String user_fullname = sharedPref.getString("user_fullname", null);
                if (post.getCreated_by_name().equals(user_fullname)){
                    mBinding.imageViewDelete.setVisibility(View.VISIBLE);
                } else {
                    mBinding.imageViewDelete.setVisibility(View.INVISIBLE);
                }
                mBinding.imageViewDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: " + mPost.getCreated_by_name());
                    }
                });
            }
        }

    }

    class PagingAdapter extends RecyclerView.Adapter<PagingAdapter.PagingViewHolder> {
        @NonNull
        @Override
        public PagingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            PagingRowItemBinding binding = PagingRowItemBinding.inflate(getLayoutInflater(), parent, false);
            return new PagingViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull PagingViewHolder holder, int position) {
            String page = mPages.get(position);
            holder.setupUI(page);
        }

        @Override
        public int getItemCount() {
            return mPages.size();
        }

        class PagingViewHolder extends RecyclerView.ViewHolder {
            PagingRowItemBinding mBinding;
            String mPage;
            public PagingViewHolder(PagingRowItemBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
            }

            public void setupUI(String page){
                mPage = page;
                mBinding.textViewPageNumber.setText(page);
                mBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getPosts(getAdapterPosition() + 1);
                    }
                });
            }
        }

    }

    PostsListener mListener;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (PostsListener) context;
    }

    interface PostsListener{
        void logout();
        void createPost();
    }
}