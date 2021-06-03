package jp.techacademy.cole.capri.autoslideshowapp

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import android.content.ContentUris
import kotlinx.android.synthetic.main.activity_main.*
import android.net.Uri
import java.util.*
import android.os.Looper
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 100
    private var mTimer: Timer? = null
    private var mHandler = Handler(Looper.getMainLooper())

    private var arrayImageUri = mutableListOf<Uri>()    //画像URI配列
    private var arrayMaxIndex = -1  //画像URI配列の最大インデックス
    private var arrayCurrentIndex = -1  //画像URI配列の現在インデックス

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //パーミッション確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android6.0以降
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //許可されている
                getContentsInfo()
            } else {
                //許可されていない
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
            }
        } else {
            //Android5系以外
            getContentsInfo()
        }

        //進むボタン押下時処理
        nextButton.setOnClickListener {
            //画像URI配列が初期値以外場合
            if (arrayMaxIndex > -1) {
                //最大インデックの場合は先頭インデックスを設定する。それ以外はインデックスを加算する。
                if (arrayCurrentIndex == arrayMaxIndex) {
                    arrayCurrentIndex = 0
                } else {
                    arrayCurrentIndex += 1
                }
                //画像をimageViewに設定
                imageView.setImageURI(arrayImageUri[arrayCurrentIndex])
            }
        }

        //戻るボタン押下時処理
        backButton.setOnClickListener {
            //画像URI配列が初期値以外の場合
            if (arrayMaxIndex > -1) {
                //先頭インデックスの場合は最大インデックスを設定する。それ以外はインデックスを減算する。
                if (arrayCurrentIndex == 0) {
                    arrayCurrentIndex = arrayMaxIndex
                } else {
                    arrayCurrentIndex -= 1
                }
                //画像をimageViewを設定
                imageView.setImageURI(arrayImageUri[arrayCurrentIndex])
            }
        }

        //再生/停止ボタン押下時処理
        autoButton.setOnClickListener {
            //画像URI配列が初期値以外の場合
            if (arrayMaxIndex > -1) {
                //現在表示されている文字が再生、停止で分岐
                if (autoButton.text == "再生") {
                    //進む、戻るボタン非活性
                    setEnableButton(false)
                    //表示文字を変更
                    autoButton.text = "停止"

                    //スライドショー開始処理
                    if (mTimer == null) {
                        mTimer = Timer()
                        mTimer!!.schedule(object : TimerTask() {
                            override fun run() {
                                //最大インデックの場合は先頭インデックスを設定する。それ以外はインデックスを加算する。
                                if (arrayCurrentIndex == arrayMaxIndex) {
                                    arrayCurrentIndex = 0
                                } else {
                                    arrayCurrentIndex += 1
                                }
                                mHandler.post {
                                    imageView.setImageURI(arrayImageUri[arrayCurrentIndex])
                                }
                            }
                        }, 2000, 2000)
                    }
                } else {
                    //進む、戻るボタンを活性
                    setEnableButton(true)
                    //表示文字を変更
                    autoButton.text = "再生"

                    //スライドショー終了処理
                    if (mTimer != null) {
                        mTimer!!.cancel()
                        mTimer = null
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo()
                } else {
                    notDisplay()
                }
        }
    }

    //画像のURI取得処理
    private fun getContentsInfo() {
        val resolver = contentResolver
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            sortOrder
        )

        if (cursor!!.moveToFirst()) {
            do {
                //indexからIDを取得し、そのIDから画像URIを取得する
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                //配列にURIを格納
                arrayImageUri.add(imageUri)
                Log.d("TEST", "${imageUri}")
            } while (cursor.moveToNext())

            //最大インデックスを設定、配列の先頭画像を表示
            arrayMaxIndex = arrayImageUri.size - 1
            arrayCurrentIndex = 0
            imageView.setImageURI(arrayImageUri[arrayCurrentIndex])

        }
        cursor.close()

        //画像がない場合全てのボタンを非活性
        if (arrayMaxIndex == -1) {
            notDisplay()
        }
    }

    //進む、戻るボタン活性、非活性処理
    private fun setEnableButton(boolean: Boolean) {
        nextButton.isEnabled = boolean
        backButton.isEnabled = boolean
    }

    //表示する画像がない、許可がない場合の処理
    private fun notDisplay() {
        setEnableButton(false)
        autoButton.isEnabled = false
        Toast.makeText(applicationContext, "表示できる画像がありません。", Toast.LENGTH_SHORT).show()
    }
}