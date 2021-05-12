package com.example.facedetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity /*implements FrameProcessor*/ {

    private Facing cameraFacing = Facing.FRONT;
    private ImageView imageView;
    private CameraView faceDetectionCameraView;
    private RecyclerView bottomSheetRecyclerView;
    private BottomSheetBehavior bottomSheetBehavior;
    private ArrayList<FaceDetectionModel> faceDetectionModels;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        faceDetectionModels = new ArrayList<>();
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        imageView = findViewById(R.id.face_det_img_view);
        faceDetectionCameraView = findViewById(R.id.face_det_cam_view);
        //Button toggle = findViewById(R.id.face_det_cam_toggle_btn);
        FrameLayout bottomSheetButton = findViewById(R.id.bottom_sheet_button);
        bottomSheetRecyclerView = findViewById(R.id.bottom_sheet_rec_view);

//        faceDetectionCameraView.setFacing(cameraFacing);
//        faceDetectionCameraView.setLifecycleOwner(MainActivity.this);
//        faceDetectionCameraView.addFrameProcessor(MainActivity.this);
//
//        toggle.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                cameraFacing = (cameraFacing == Facing.FRONT) ? Facing.BACK : Facing.FRONT;
//                // cameraFacing = (cameraFacing == Facing.FRONT) ? Facing.BACK : Facing.FRONT;
//                faceDetectionCameraView.setFacing(cameraFacing);
//
//            }
//        });


        bottomSheetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity().start(MainActivity.this);
            }
        });

        //setting up recyclerview
        bottomSheetRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        bottomSheetRecyclerView.setAdapter(new FaceDetectionAdapter(faceDetectionModels, MainActivity.this));


    }

    //we receive an image here through an intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            
            if(resultCode == RESULT_OK){
                assert result != null;
                Uri imageUri = result.getUri();
                try {
                    analyseImage(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void analyseImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(MainActivity.this, "There was an error", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        imageView.setImageBitmap(null);
        faceDetectionModels.clear();

        Objects.requireNonNull(bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        //show Progress
        showProgress();

        //get image from device
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        //specify the options of things that will be detected in the images
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

        //initialize the face detector
        FirebaseVisionFaceDetector faceDetector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        //specify the image to be used in the face detector
        faceDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        //firebaseVisionFaces is just machine gibberish. mutableImage is the canvas that it get put on
                        Bitmap mutableImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                        detectFaces(firebaseVisionFaces, mutableImage);

                        imageView.setImageBitmap(mutableImage);

                        hideProgress();
                        Objects.requireNonNull(bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "There was some error",
                                Toast.LENGTH_SHORT).show();
                        hideProgress();
                    }
                });
    }

    //firebaseVisionFaces is a list of a bunch of numbers that outline the face
    private void detectFaces(List<FirebaseVisionFace> firebaseVisionFaces, Bitmap bitmap) {
        if (firebaseVisionFaces == null || bitmap == null) {
            Toast.makeText(this, "There was an error", Toast.LENGTH_SHORT).show();
            return;
        }

        //bitmap is where we are going to be writing
        Canvas canvas = new Canvas(bitmap);
        //this is the pencil to be used
        Paint facePaint = new Paint();
        facePaint.setColor(Color.GREEN);
        facePaint.setStyle(Paint.Style.STROKE);
        facePaint.setStrokeWidth(5f);

        //another pencil
        Paint faceTextPaint = new Paint();
        faceTextPaint.setColor(Color.BLUE);
        faceTextPaint.setTextSize(30f);
        faceTextPaint.setTypeface(Typeface.SANS_SERIF);

        //shows the landmarks. nose ears eyes mouth
        Paint landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(8f);

        //find each face and draw a square rectangle around them. Also adds the face number on top of each box
        for (int i = 0; i < firebaseVisionFaces.size(); i++) {
            canvas.drawRect(firebaseVisionFaces.get(i).getBoundingBox(), facePaint);
            canvas.drawText("Face " + i, (firebaseVisionFaces.get(i).getBoundingBox().centerX()
                            - (firebaseVisionFaces.get(i).getBoundingBox().width() >> 2) + 8f), // added >> to avoid errors when dividing with "/"
                    (firebaseVisionFaces.get(i).getBoundingBox().centerY()
                            + firebaseVisionFaces.get(i).getBoundingBox().height() >> 2) - 8F,
                    faceTextPaint);

            //gets one face at a time
            FirebaseVisionFace face = firebaseVisionFaces.get(i);
            //we look if the face has a left eye, if it does it paints a circle around eye
            if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE) != null) {
                FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
                canvas.drawCircle(Objects.requireNonNull(leftEye).getPosition().getX(),
                        leftEye.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
            }

            //does the same for the right eye as left eye
            if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE) != null) {
                FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);
                canvas.drawCircle(Objects.requireNonNull(rightEye).getPosition().getX(),
                        rightEye.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
            }

            //gets the base of the nose and also paints a circle at the base
            if (face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE) != null) {
                FirebaseVisionFaceLandmark nose = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);
                canvas.drawCircle(Objects.requireNonNull(nose).getPosition().getX(),
                        nose.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
            }
            if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR) != null) {
                FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                canvas.drawCircle(Objects.requireNonNull(leftEar).getPosition().getX(),
                        leftEar.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
            }

            if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR) != null) {
                FirebaseVisionFaceLandmark rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);
                canvas.drawCircle(Objects.requireNonNull(rightEar).getPosition().getX(),
                        rightEar.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
            }

            //we draw 2 lines. from left to bottom . then from bottom to right
            if (face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT) != null
                    && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM) != null
                    && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT) != null) {
                FirebaseVisionFaceLandmark leftMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
                FirebaseVisionFaceLandmark bottomMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
                FirebaseVisionFaceLandmark rightMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);
                canvas.drawLine(leftMouth.getPosition().getX(),
                        leftMouth.getPosition().getY(),
                        bottomMouth.getPosition().getX(),
                        bottomMouth.getPosition().getY(),
                        landmarkPaint);
                canvas.drawLine(bottomMouth.getPosition().getX(),
                        bottomMouth.getPosition().getY(),
                        rightMouth.getPosition().getX(),
                        rightMouth.getPosition().getY(), landmarkPaint);
            }
            //for the current face being analyzed, we simply call method to check eyes and mouth
            faceDetectionModels.add(new FaceDetectionModel(i, "Smiling Probability " + face.getSmilingProbability()));
            faceDetectionModels.add(new FaceDetectionModel(i, "Left Eye Open Probability " + face.getLeftEyeOpenProbability()));
            faceDetectionModels.add(new FaceDetectionModel(i, "Right Eye Open Probability " + face.getRightEyeOpenProbability()));

        }
    }

    private void showProgress() {
        findViewById(R.id.bottom_sheet_btn_img).setVisibility(View.GONE);
        findViewById(R.id.bottom_sheet_btn_progress_bar).setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        findViewById(R.id.bottom_sheet_btn_img).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_sheet_btn_progress_bar).setVisibility(View.GONE);
    }
}