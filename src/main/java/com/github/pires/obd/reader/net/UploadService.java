//package com.github.pires.obd.reader.net;
//
//import android.content.Context;
//import android.net.Uri;
//import android.view.View;
//import android.widget.ProgressBar;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//
//import com.github.pires.obd.reader.R;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.OnProgressListener;
//import com.google.firebase.storage.StorageReference;
//import com.google.firebase.storage.UploadTask;
//
//public class UploadService {
//
//    private Uri videouri;
//    private static final int REQUEST_CODE = 101;
//    private StorageReference videoref;
//    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
//
//
//    public void upload(final Context context, View view) {
//        videoref =storageRef.child("/videos" + "/userIntro.mp4");
//        if (videouri != null) {
//
//            UploadTask uploadTask = videoref.putFile(videouri);
//
//            uploadTask.addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    Toast.makeText(context,
//                            "Upload failed: " + e.getLocalizedMessage(),
//                            Toast.LENGTH_LONG).show();
//
//                }
//            }).addOnSuccessListener(
//                    new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                        @Override
//                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                            Toast.makeText(context, "Upload complete",
//                                    Toast.LENGTH_LONG).show();
//                        }
//                    }).addOnProgressListener(
//                    new OnProgressListener<UploadTask.TaskSnapshot>() {
//                        @Override
//                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                            updateProgress(taskSnapshot);
//
//                        }
//                    });
//        } else {
//            Toast.makeText(context, "Nothing to upload",
//                    Toast.LENGTH_LONG).show();
//        }
//    }
//
//    public void updateProgress(UploadTask.TaskSnapshot taskSnapshot) {
//
//        @SuppressWarnings("VisibleForTests") long fileSize =
//                taskSnapshot.getTotalByteCount();
//
//        @SuppressWarnings("VisibleForTests")
//        long uploadBytes = taskSnapshot.getBytesTransferred();
//
//        long progress = (100 * uploadBytes) / fileSize;
//
//        ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.pbar);
//        progressBar.setProgress((int) progress);
//    }
//}
