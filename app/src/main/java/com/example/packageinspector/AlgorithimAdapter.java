package com.example.packageinspector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlgorithimAdapter extends RecyclerView.Adapter<AlgorithimAdapter.AlgorithimViewHolder> {

    private final Context context;
    private final List<Algorithim> items;

    public AlgorithimAdapter(Context context, List<Algorithim> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public AlgorithimViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AlgorithimViewHolder(LayoutInflater.from(context).inflate(R.layout.row_algorithm, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AlgorithimViewHolder holder, int position) {
        Algorithim algorithim = items.get(position);

        holder.tvAlgorithim.setText(algorithim.getAlgorithim());
        holder.tvCode.setText(algorithim.getCode());

    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class AlgorithimViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlgorithim;
        TextView tvCode;

        public AlgorithimViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlgorithim = itemView.findViewById(R.id.tvAlgorithm);
            tvCode = itemView.findViewById(R.id.tvAlgorithmCode);
        }
    }
}
