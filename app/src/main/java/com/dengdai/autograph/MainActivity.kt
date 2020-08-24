package com.dengdai.autograph

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        linePathViewInit()
        cancel.setOnClickListener(this)
        save.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
           R.id.cancel->{
               linePathView.clear()
               linePathViewInit()
           }
            R.id.save->{
                if (linePathView.touched){
                    try {
//                        linePathView.save("/sdcard/qm.png", true, 10)
                        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                        setResult(100)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }else{
                    Toast.makeText(this, "您没有签名~", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private fun linePathViewInit(){
        linePathView.setBackColor(Color.WHITE)
        linePathView.setPaintWidth(20)
        linePathView.setPenColor(Color.BLACK)
    }

}