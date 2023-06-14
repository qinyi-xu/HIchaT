package com.example.hichatclient.mainActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.dataResource.AuthService;
import com.example.hichatclient.viewModel.SignUpViewModel;
import com.mordred.wordcloud.CountMap;
import com.mordred.wordcloud.WordCloud;
import com.mordred.wordcloud.WordFrequency;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import jackmego.com.jieba_android.JiebaSegmenter;

import static android.app.Activity.RESULT_OK;


public class SignUpFragment extends Fragment {
    private EditText editTextUserName;
    private EditText editTextUserPassword;
    private EditText editTextUserPasswordCheck;
    private Button buttonSignUp;
    private Button buttonToLogIn;
    private ImageButton imageButtonHeadPortrait;
    private SignUpViewModel signUpViewModel;
    private FragmentActivity activity;
    private ApplicationUtil applicationUtil;
    private Socket socket;



    public SignUpFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    public void saveImage(Bitmap bitmap) {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)){
            return;
        }
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(Environment.getExternalStorageDirectory() + "/hear.jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
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

    final int CAMEAR_REQUEST_CODE = 1;//拍照返回码
    final int ALBUM_REQUEST_CODE = 2;//相册返回码
    final int CROP_REQUEST_CODE = 3;//裁剪返回码
    //调用照相机返回图片文件
    File tempFile;
    Bitmap image = null;
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
//        super.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case CAMEAR_REQUEST_CODE:
                //调用系统相机后返回
                if (resultCode == RESULT_OK) {
                    //用相机返回的照片去调用剪裁也需要对Uri进行处理
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri contentUri = FileProvider.getUriForFile(Objects.requireNonNull(this.getContext()), "com.bw.movie", tempFile);
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
                    if (image != null){
                        imageButtonHeadPortrait.setImageBitmap(toRoundCorner(image, 2));
                    }
//                    pic.setImageBitmap(image);
                    Log.e("TAG","Bit=="+image.toString());
                    //也可以进行一些保存、压缩等操作后上传
                    saveImage(image);
                    File file = new File(Environment.getExternalStorageDirectory() + "/hear.jpg");
//                    getPresenter().headpic(file);
                }
                break;
        }
    }




    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = requireActivity();
        signUpViewModel = new ViewModelProvider(activity).get(SignUpViewModel.class);
        applicationUtil = (ApplicationUtil) activity.getApplication();
        if (!applicationUtil.staticIsConnected()) {
            try {
                applicationUtil.initSocketStatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = applicationUtil.getSocketStatic();

        editTextUserName = activity.findViewById(R.id.userName1);
        editTextUserPassword = activity.findViewById(R.id.userPasswordSign);
        editTextUserPasswordCheck = activity.findViewById(R.id.userPasswordCheck1);
        buttonSignUp = activity.findViewById(R.id.buttonSignUp);
        buttonToLogIn = activity.findViewById(R.id.buttonToLogIn);
        imageButtonHeadPortrait = activity.findViewById(R.id.imageButtonHeadPortrait);

        imageButtonHeadPortrait.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
                startActivityForResult(photoPickerIntent, ALBUM_REQUEST_CODE);
            }
        });

        buttonSignUp.setEnabled(false);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String userName = editTextUserName.getText().toString().trim();
                String userPassword = editTextUserPassword.getText().toString().trim();
                String userPasswordCheck = editTextUserPasswordCheck.getText().toString().trim();
                buttonSignUp.setEnabled(!userName.isEmpty() && !userPassword.isEmpty() && !userPasswordCheck.isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        editTextUserName.addTextChangedListener(textWatcher);
        editTextUserPassword.addTextChangedListener(textWatcher);
        editTextUserPasswordCheck.addTextChangedListener(textWatcher);

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                String userName = editTextUserName.getText().toString().trim();
                String userPassword = editTextUserPassword.getText().toString().trim();
                String userPasswordCheck = editTextUserPasswordCheck.getText().toString().trim();
                String userID;
                Bitmap bitmapImage = image;
                if (!userPassword.equals(userPasswordCheck)){
                    Toast.makeText(getActivity(), "两次输入的密码不一致！", Toast.LENGTH_SHORT).show();
                } else if(!isBitmapSizeRight(image)){
                    Toast.makeText(getActivity(), "上传的头像太大！", Toast.LENGTH_SHORT).show();
                } else if(!isPasswordLengthRight(userPassword)){
                    Toast.makeText(getActivity(), "输入的密码长度不在6到20之间！", Toast.LENGTH_SHORT).show();
                } else if(!isContainAll(userPassword)){
                    Toast.makeText(getActivity(), "输入的密码没有同时包括大小写字母和数字！", Toast.LENGTH_SHORT).show();
                } else if(!isNameLengthRight(userName)) {
                    Toast.makeText(getActivity(), "输入的昵称长度不在0到7之间！", Toast.LENGTH_SHORT).show();
                }else {
                    try {
                        userID = signUpViewModel.signUp(userName, userPassword, bitmapImage, socket);
                        AlertDialog.Builder builder= new AlertDialog.Builder(activity, R.style.Theme_AppCompat_Light_Dialog_Alert);
                        builder.setTitle("注册成功！您的ID为：" + userID);
                        builder.setPositiveButton("返回登录界面", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                NavController controller = Navigation.findNavController(v);
                                controller.navigate(R.id.action_signUpFragment_to_logInFragment);
                            }
                        });
                        builder.setNegativeButton("知道了", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        builder.create();
                        builder.show();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });


        buttonToLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController controller = Navigation.findNavController(v);
                controller.navigate(R.id.action_signUpFragment_to_logInFragment);
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

    //检测密码是否同时含有大小写字母和数字
    public static boolean isContainAll(String str) {
        boolean isDigit = false;//定义一个boolean值，用来表示是否包含数字
        boolean isLowerCase = false;//定义一个boolean值，用来表示是否包含字母
        boolean isUpperCase = false;
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {   //用char包装类中的判断数字的方法判断每一个字符
                isDigit = true;
            } else if (Character.isLowerCase(str.charAt(i))) {  //用char包装类中的判断字母的方法判断每一个字符
                isLowerCase = true;
            } else if (Character.isUpperCase(str.charAt(i))) {
                isUpperCase = true;
            }
        }
        String regex = "^[a-zA-Z0-9]+$";
        boolean isRight = isDigit && isLowerCase && isUpperCase && str.matches(regex);
        return isRight;
    }

    //检测密码长度是否在6至20之间
    public boolean isPasswordLengthRight(String str){
        if(6<=str.length() && str.length()<=20){
            return true;
        }
        else{
            return false;
        }
    }

    //检测头像大小
    public boolean isBitmapSizeRight(Bitmap bitmap){
        if(bitmap == null){
            return true;
        }
        else if(bitmap.getAllocationByteCount() <= 2097152){
            return true;
        }
        else{
            return false;
        }
    }


    //检测昵称长度是否在6至20之间
    public boolean isNameLengthRight(String str){
        if(0<=str.length() && str.length()<=7){
            return true;
        }
        else{
            return false;
        }
    }


}