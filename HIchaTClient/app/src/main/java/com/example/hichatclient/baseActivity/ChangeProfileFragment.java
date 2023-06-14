package com.example.hichatclient.baseActivity;

import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.data.entity.User;
import com.example.hichatclient.viewModel.ChangeProfileViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import static android.app.Activity.RESULT_OK;

public class ChangeProfileFragment extends Fragment {
    private ChangeProfileViewModel changeProfileViewModel;
    private ApplicationUtil applicationUtil;
    private SharedPreferences sharedPreferences;
    private Socket socket;
    private FragmentActivity activity;

    private ImageView imageViewProfile;
    private Button buttonChangeProfile;
    private byte[] userNewProfile;



    public static ChangeProfileFragment newInstance() {
        return new ChangeProfileFragment();
    }

    //调用照相机返回图片文件
    File tempFile;
    Bitmap image = null;

    private int flag = 0;


    final int CAMEAR_REQUEST_CODE = 1;//拍照返回码
    final int ALBUM_REQUEST_CODE = 2;//相册返回码
    final int CROP_REQUEST_CODE = 3;//裁剪返回码



    public void saveImage(Bitmap bitmap) {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(Environment.getExternalStorageDirectory() + "/hear.jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.flush();
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cropPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 330);
        intent.putExtra("outputY", 330);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, 3);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
//        super.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case CAMEAR_REQUEST_CODE:
                //调用系统相机后返回
                if (resultCode == RESULT_OK) {
                    //用相机返回的照片去调用剪裁也需要对Uri进行处理
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri contentUri = FileProvider.getUriForFile(activity, "com.bw.movie", tempFile);
                        cropPhoto(contentUri);
                    } else {
                        cropPhoto(Uri.fromFile(tempFile));
                    }
                }
                break;
            case ALBUM_REQUEST_CODE:
                //调用系统相册后返回
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    cropPhoto(uri);
                }
                break;
            case CROP_REQUEST_CODE:
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    //在这里获得了剪裁后的Bitmap对象，可以用于上传
                    image = bundle.getParcelable("data");
                    //设置到ImageView上
                    if (image != null) {
                        imageViewProfile.setImageBitmap(toRoundCorner(image, 2));
                    }


//                    pic.setImageBitmap(image);
                    Log.e("TAG", "Bit==" + image.toString());
                    //也可以进行一些保存、压缩等操作后上传
                    saveImage(image);
                    File file = new File(Environment.getExternalStorageDirectory() + "/hear.jpg");
//                    getPresenter().headpic(file);
                }
                break;
        }
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.change_profile_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = requireActivity();
        changeProfileViewModel = new ViewModelProvider(this).get(ChangeProfileViewModel.class);

        imageViewProfile = activity.findViewById(R.id.imageView10);
        buttonChangeProfile = activity.findViewById(R.id.button);

        assert getArguments() != null;
        final byte[] userProfile = getArguments().getByteArray("userProfile");

        // 获取applicationUtil中的数据
        applicationUtil = (ApplicationUtil)activity.getApplication();
        final String userShortToken = applicationUtil.getUserShortToken();
        if (!applicationUtil.staticIsConnected()) {
            try {
                applicationUtil.initSocketStatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = applicationUtil.getSocketStatic();

        // 获取Share Preferences中的数据
        sharedPreferences = activity.getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
        final String userID = sharedPreferences.getString("userID", "fail");

        if (userProfile != null) {
            System.out.println("ChangeProfileFragment userProfile: " + userProfile.length);
            imageViewProfile.setImageBitmap(toRoundCorner(BitmapFactory.decodeByteArray(userProfile, 0, userProfile.length), 2));
        } else {
            imageViewProfile.setImageResource(R.drawable.head);
        }

        imageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(photoPickerIntent, ALBUM_REQUEST_CODE);
            }
        });

        buttonChangeProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (image != null) {
                    flag = 0;

                    final ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.PNG, 100, imageBytes);
                    userNewProfile = imageBytes.toByteArray();

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                flag = changeProfileViewModel.updateUserProfileToServer(userShortToken, userNewProfile, socket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (flag == 1) {
                        try {
                            User meUser;
                            meUser = changeProfileViewModel.getUserInfoByUserID(userID);
                            meUser.setUserProfile(userNewProfile);
                            changeProfileViewModel.insertUser(meUser);
                            Toast.makeText(v.getContext(), "修改成功！", Toast.LENGTH_SHORT).show();
                            NavController navController = Navigation.findNavController(v);
                            navController.navigate(R.id.action_changeProfileFragment_to_meFragment);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(v.getContext(), "修改失败！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


    }



    public static Bitmap toRoundCorner(Bitmap bitmap, float ratio) {
        System.out.println("图片是否变成圆形模式了+++++++++++++");
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, bitmap.getWidth() / ratio,
                bitmap.getHeight() / ratio, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        System.out.println("pixels+++++++" + String.valueOf(ratio));

        return output;

    }


}