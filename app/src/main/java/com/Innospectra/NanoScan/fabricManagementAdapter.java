package com.Innospectra.NanoScan;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class fabricManagementAdapter extends RecyclerView.Adapter<fabricManagementAdapter.ViewHolder> {

    private List<String> data;
    private Context context;
    private OnItemClickListener listener;

    public fabricManagementAdapter(Context context, List<String> data, OnItemClickListener listener) {
        this.context = context;
        this.data = data;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(data.get(position));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void addItem(String item) {
        data.add(item);
        notifyDataSetChanged();
    }

    public void updateItem(int position, String newItem) {
        data.set(position, newItem);
        notifyItemChanged(position);
    }

    public void removeItem(int position) {
        data.remove(position);
        notifyItemRemoved(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.text_view);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

}

