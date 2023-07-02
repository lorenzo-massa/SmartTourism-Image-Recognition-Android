package org.tensorflow.lite.examples.classification;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;

import java.util.List;
import java.util.Stack;

public class MonumentAdapter extends RecyclerView.Adapter<MonumentAdapter.ViewHolder> {

    private List<String> categories;
    private OnButtonClickListener buttonClickListener;

    public MonumentAdapter(List<String> categories, OnButtonClickListener buttonClickListener) {
        this.categories = categories;
        this.buttonClickListener = buttonClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.list_item_categories, null);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categories.get(position);

        // Load image using Glide
        String imagePath = "file:///android_asset/categories/" + category + ".jpg";
        Glide.with(holder.itemView.getContext())
                .load(Uri.parse(imagePath))
                .into(holder.imageView);

        //Set title
        holder.title.setText(category);

        // Set monument list
        List<String> monuments = DatabaseAccess.getMonumentsByCategory(category);
        holder.monumentLayout.removeAllViews();
        for (String monument : monuments) {
            View monumentView;
            if (holder.recycledMonumentViews.isEmpty()) {
                monumentView = View.inflate(holder.itemView.getContext(), R.layout.linear_layout_monument, null);
                monumentView.setTag(new MonumentViewHolder(monumentView.findViewById(R.id.tt)));
            } else {
                monumentView = holder.recycledMonumentViews.pop();
            }

            MonumentViewHolder monumentViewHolder = (MonumentViewHolder) monumentView.getTag();
            monumentViewHolder.titleTextView.setText(monument);
            holder.monumentLayout.addView(monumentView);

            // Set button click listener
            Button moreInfoButton = monumentView.findViewById(R.id.action_button);
            moreInfoButton.setOnClickListener(v -> buttonClickListener.onButtonClick(monument));
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView title;
        LinearLayout monumentLayout;
        Stack<View> recycledMonumentViews;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.media);
            title = itemView.findViewById(R.id.title);
            monumentLayout = itemView.findViewById(R.id.monument_list_by_category);
            recycledMonumentViews = new Stack<>();
        }
    }

    public static class MonumentViewHolder {
        TextView titleTextView;

        public MonumentViewHolder(@NonNull TextView titleTextView) {
            this.titleTextView = titleTextView;
        }
    }

    public interface OnButtonClickListener {
        void onButtonClick(String monument);
    }
}
