package com.example.facedetection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FaceDetectionAdapter extends RecyclerView.Adapter<FaceDetectionAdapter.ViewHolder> {

    private List<FaceDetectionModel> faceDetectionModelList;
    private Context context;

    public FaceDetectionAdapter(List<FaceDetectionModel> faceDetectionModelList, Context context) {
        this.faceDetectionModelList = faceDetectionModelList;
        this.context = context;
    }

    @NonNull
    @Override
    public FaceDetectionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_face_detection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceDetectionAdapter.ViewHolder holder, int position) {

        FaceDetectionModel faceDetectionModel = faceDetectionModelList.get(position);
        holder.txt1.setText(String.valueOf(faceDetectionModel.getId()));
        holder.txt2.setText(faceDetectionModel.getText());
    }

    @Override
    public int getItemCount() {
        return faceDetectionModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView txt1;
        public TextView txt2;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txt1 = itemView.findViewById(R.id.item_face_det_txt_view1);
            txt2 = itemView.findViewById(R.id.item_face_det_txt_view2);
        }
    }
}
