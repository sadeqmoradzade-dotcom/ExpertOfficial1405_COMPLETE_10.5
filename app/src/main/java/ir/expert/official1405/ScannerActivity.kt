package ir.expert.official1405

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class ScannerActivity:ComponentActivity(){
    private lateinit var capture:ImageCapture
    private lateinit var preview:PreviewView
    private lateinit var counter:TextView
    private val db by lazy { DatabaseHelper(this) }
    private var caseId:Long=-1
    private val pages=mutableListOf<Bitmap>()

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        setContentView(R.layout.activity_scanner)
        caseId=intent.getLongExtra("caseId",-1)
        preview=findViewById(R.id.previewView)
        counter=findViewById(R.id.pageCounter)
        findViewById<Button>(R.id.captureButton).setOnClickListener{scan()}
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finishScan() }
        })
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CAMERA),10)
        else start()
    }

    private fun start(){
        val f=ProcessCameraProvider.getInstance(this)
        f.addListener({
            val p=f.get()
            val pre=Preview.Builder().build().also{it.surfaceProvider=preview.surfaceProvider}
            capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            p.unbindAll()
            p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pre,capture)
        },ContextCompat.getMainExecutor(this))
    }

    private fun scan(){
        val dir=File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"scans")
        dir.mkdirs()
        val image=File(dir,"page_${System.currentTimeMillis()}.jpg")
        val o=ImageCapture.OutputFileOptions.Builder(image).build()
        capture.takePicture(o,ContextCompat.getMainExecutor(this),object:ImageCapture.OnImageSavedCallback{
            override fun onError(e:ImageCaptureException){ Toast.makeText(this@ScannerActivity,"خطا در اسکن: ${e.message}",Toast.LENGTH_LONG).show() }
            override fun onImageSaved(r:ImageCapture.OutputFileResults){
                val raw=BitmapFactory.decodeFile(image.path)
                if(raw==null){ Toast.makeText(this@ScannerActivity,"تصویر قابل خواندن نیست.",Toast.LENGTH_SHORT).show(); return }
                val cropped=autoCrop(raw)
                if(cropped !== raw) raw.recycle()
                pages += cropped
                image.delete()
                counter.text="صفحات اسکن‌شده: ${pages.size}"
                Toast.makeText(this@ScannerActivity,"صفحه ${pages.size} اضافه شد. برای صفحه بعد دوباره اسکن کنید.",Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Simple local document-edge crop: removes mostly-uniform margins around the paper.
    private fun autoCrop(src:Bitmap):Bitmap{
        val w=src.width; val h=src.height
        val step=(minOf(w,h)/180).coerceAtLeast(2)
        val bg=sampleBackground(src)
        var left=w; var top=h; var right=0; var bottom=0
        for(y in 0 until h step step){
            for(x in 0 until w step step){
                val c=src.getPixel(x,y)
                val d=kotlin.math.abs(Color.red(c)-Color.red(bg))+kotlin.math.abs(Color.green(c)-Color.green(bg))+kotlin.math.abs(Color.blue(c)-Color.blue(bg))
                if(d>55){ left=minOf(left,x); right=maxOf(right,x); top=minOf(top,y); bottom=maxOf(bottom,y) }
            }
        }
        val margin=(minOf(w,h)*0.025f).toInt()
        left=(left-margin).coerceAtLeast(0); top=(top-margin).coerceAtLeast(0)
        right=(right+margin).coerceAtMost(w-1); bottom=(bottom+margin).coerceAtMost(h-1)
        if(right-left<w*0.55 || bottom-top<h*0.55) return src
        return Bitmap.createBitmap(src,left,top,right-left+1,bottom-top+1)
    }

    private fun sampleBackground(src:Bitmap):Int{
        val pts=arrayOf(0 to 0, src.width-1 to 0, 0 to src.height-1, src.width-1 to src.height-1)
        var r=0;var g=0;var b=0
        pts.forEach{val c=src.getPixel(it.first,it.second);r+=Color.red(c);g+=Color.green(c);b+=Color.blue(c)}
        return Color.rgb(r/4,g/4,b/4)
    }

    private fun finishScan(){
        if(pages.isEmpty()){ Toast.makeText(this,"ابتدا حداقل یک صفحه اسکن کنید.",Toast.LENGTH_SHORT).show(); return }
        val dir=File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"scans");dir.mkdirs()
        val pdf=File(dir,"scan_${System.currentTimeMillis()}.pdf")
        val doc=PdfDocument()
        pages.forEachIndexed{index,bm->
            val maxW=1600f
            val scale=(maxW/bm.width).coerceAtMost(1f)
            val pw=(bm.width*scale).toInt(); val ph=(bm.height*scale).toInt()
            val page=doc.startPage(PdfDocument.PageInfo.Builder(pw,ph,index+1).create())
            page.canvas.drawBitmap(bm,null,android.graphics.Rect(0,0,pw,ph),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            doc.finishPage(page)
        }
        FileOutputStream(pdf).use{doc.writeTo(it)}
        doc.close()
        pages.forEach{it.recycle()}; pages.clear()
        if(caseId>0) db.addDocument(caseId,"اسکن سند (${pdf.name})",pdf.absolutePath)
        Toast.makeText(this,"PDF چندصفحه‌ای با موفقیت ذخیره شد.",Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK);finish()
    }

    override fun onDestroy(){ pages.forEach{if(!it.isRecycled)it.recycle()};pages.clear();super.onDestroy() }
}
