package com.dengdai.autograph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import java.io.*
import kotlin.math.max
import kotlin.math.min

class LinePathView : View {
    private var mContext: Context? = null

    /**
     * 笔画X坐标起点
     */
    private var mX = 0f

    /**
     * 笔画Y坐标起点
     */
    private var mY = 0f

    /**
     * 手写画笔
     */
    private val mGesturePaint = Paint()

    /**
     * 路径
     */
    private val mPath = Path()

    /**
     * 签名画笔
     */
    private var cacheCanvas: Canvas? = null

    /**
     * 签名画布
     */
    private var cachebBitmap: Bitmap? = null

    /**
     * 是否有签名
     *
     * @return
     */
    /**
     * 是否已经签名
     */
    var touched = false
        private set

    /**
     * 画笔宽度 px；
     */
    private var mPaintWidth = 10

    /**
     * 前景色
     */
    private var mPenColor = Color.BLACK

    /**
     * 背景色（指最终签名结果文件的背景颜色，默认为透明色）
     */
    private var mBackColor = Color.TRANSPARENT

    //签名开始与结束
    var touch: Touch? = null

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init(context)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context?) {
        mContext = context
        //设置抗锯齿
        mGesturePaint.isAntiAlias = true
        //设置签名笔画样式
        mGesturePaint.style = Paint.Style.STROKE
        //设置笔画宽度
        mGesturePaint.strokeWidth = mPaintWidth.toFloat()
        //设置签名颜色
        mGesturePaint.color = mPenColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //创建跟view一样大的bitmap，用来保存签名
        cachebBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        cacheCanvas = Canvas(cachebBitmap!!)
        cacheCanvas!!.drawColor(mBackColor)
        touched = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (touch != null) touch!!.OnTouch(true)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchDown(event)
            MotionEvent.ACTION_MOVE -> {
                touched = true
                if (touch != null) touch!!.OnTouch(false)
                touchMove(event)
            }
            MotionEvent.ACTION_UP -> {
                //将路径画到bitmap中，即一次笔画完成才去更新bitmap，而手势轨迹是实时显示在画板上的。
                cacheCanvas!!.drawPath(mPath, mGesturePaint)
                mPath.reset()
            }
        }
        // 更新绘制
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //画此次笔画之前的签名
        canvas.drawBitmap(cachebBitmap!!, 0f, 0f, mGesturePaint)
        // 通过画布绘制多点形成的图形
        canvas.drawPath(mPath, mGesturePaint)
    }

    // 手指点下屏幕时调用
    private fun touchDown(event: MotionEvent) {
        // 重置绘制路线
        mPath.reset()
        val x = event.x
        val y = event.y
        mX = x
        mY = y
        // mPath绘制的绘制起点
        mPath.moveTo(x, y)
    }

    // 手指在屏幕上滑动时调用
    private fun touchMove(event: MotionEvent) {
        val x = event.x
        val y = event.y
        val previousX = mX
        val previousY = mY
        val dx = Math.abs(x - previousX)
        val dy = Math.abs(y - previousY)
        // 两点之间的距离大于等于3时，生成贝塞尔绘制曲线
        if (dx >= 3 || dy >= 3) {
            // 设置贝塞尔曲线的操作点为起点和终点的一半
            val cX = (x + previousX) / 2
            val cY = (y + previousY) / 2
            // 二次贝塞尔，实现平滑曲线；previousX, previousY为操作点，cX, cY为终点
            mPath.quadTo(previousX, previousY, cX, cY)
            // 第二次执行时，第一次结束调用的坐标值将作为第二次调用的初始坐标值
            mX = x
            mY = y
        }
    }

    /**
     * 清除画板
     */
    fun clear() {
        if (cacheCanvas != null) {
            touched = false
            //更新画板信息
            mGesturePaint.color = mPenColor
            cacheCanvas!!.drawColor(mBackColor, PorterDuff.Mode.CLEAR)
            mGesturePaint.color = mPenColor
            invalidate()
        }
    }

    interface Touch {
        fun OnTouch(isTouch: Boolean)
    }

    /**
     * 保存画板
     *
     * @param path       保存到路径
     * @param clearBlank 是否清除边缘空白区域
     * @param blank      要保留的边缘空白距离
     */
    /**
     * 保存画板
     *
     * @param path 保存到路径
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun save(
        path: String?,
        clearBlank: Boolean = false,
        blank: Int = 0
    ) {
        var bitmap = cachebBitmap
        //BitmapUtil.createScaledBitmapByHeight(srcBitmap, 300);//  压缩图片
        if (clearBlank) {
            bitmap = clearBlank(bitmap, blank)
        }
        val bos = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, bos)
        val buffer = bos.toByteArray()
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        val outputStream: OutputStream = FileOutputStream(file)
        outputStream.write(buffer)
        outputStream.close()
    }

    /**
     * 获取画板的bitmap
     *
     * @return
     */
    val bitMap: Bitmap
        get() {
            isDrawingCacheEnabled = true
            buildDrawingCache()
            val bitmap = drawingCache
            isDrawingCacheEnabled = false
            return bitmap
        }

    /**
     * 逐行扫描 清楚边界空白。
     *
     * @param bp
     * @param blank 边距留多少个像素
     * @return
     */
    private fun clearBlank(bp: Bitmap?, blank: Int): Bitmap {
        var blank = blank
        val HEIGHT = bp!!.height
        val WIDTH = bp.width
        var top = 0
        var left = 0
        var right = 0
        var bottom = 0
        var pixs = IntArray(WIDTH)
        var isStop: Boolean
        //扫描上边距不等于背景颜色的第一个点
        for (y in 0 until HEIGHT) {
            bp.getPixels(pixs, 0, WIDTH, 0, y, WIDTH, 1)
            isStop = false
            for (pix in pixs) {
                if (pix != mBackColor) {
                    top = y
                    isStop = true
                    break
                }
            }
            if (isStop) {
                break
            }
        }
        //扫描下边距不等于背景颜色的第一个点
        for (y in HEIGHT - 1 downTo 0) {
            bp.getPixels(pixs, 0, WIDTH, 0, y, WIDTH, 1)
            isStop = false
            for (pix in pixs) {
                if (pix != mBackColor) {
                    bottom = y
                    isStop = true
                    break
                }
            }
            if (isStop) {
                break
            }
        }
        pixs = IntArray(HEIGHT)
        //扫描左边距不等于背景颜色的第一个点
        for (x in 0 until WIDTH) {
            bp.getPixels(pixs, 0, 1, x, 0, 1, HEIGHT)
            isStop = false
            for (pix in pixs) {
                if (pix != mBackColor) {
                    left = x
                    isStop = true
                    break
                }
            }
            if (isStop) {
                break
            }
        }
        //扫描右边距不等于背景颜色的第一个点
        for (x in WIDTH - 1 downTo 1) {
            bp.getPixels(pixs, 0, 1, x, 0, 1, HEIGHT)
            isStop = false
            for (pix in pixs) {
                if (pix != mBackColor) {
                    right = x
                    isStop = true
                    break
                }
            }
            if (isStop) {
                break
            }
        }
        if (blank < 0) {
            blank = 0
        }
        //计算加上保留空白距离之后的图像大小
        left = max(left - blank, 0)
        top = max(top - blank, 0)
        right = min(right + blank, WIDTH - 1)
        bottom = min(bottom + blank, HEIGHT - 1)
        return Bitmap.createBitmap(bp, left, top, right - left, bottom - top)
    }

    /**
     * 设置画笔宽度 默认宽度为10px
     *
     * @param mPaintWidth
     */
    fun setPaintWidth(mPaintWidth: Int) {
        mGesturePaint.strokeWidth = (if (mPaintWidth > 0) mPaintWidth else 10).toFloat()
    }

    fun setBackColor(@ColorInt backColor: Int) {
        mBackColor = backColor
    }

    /**
     * 设置画笔颜色
     *
     * @param mPenColor
     */
    fun setPenColor(mPenColor: Int) {
        mGesturePaint.color = mPenColor
    }

}