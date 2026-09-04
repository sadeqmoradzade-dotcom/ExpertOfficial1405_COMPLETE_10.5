package ir.expert.official1405

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CaseRow(val id: Long, val title: String, val court: String, val caseNo: String, val created: Long)
data class DeadlineRow(val id: Long, val title: String, val date: String, val note: String)
data class VisitRow(val id: Long, val date: String, val note: String, val photoPath: String?)
data class DocumentRow(val id: Long, val title: String, val path: String, val created: Long)

class DatabaseHelper(ctx: Context) : SQLiteOpenHelper(ctx, "expert_official.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE cases(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            court TEXT,
            case_no TEXT,
            created INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE deadlines(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            case_id INTEGER NOT NULL,
            title TEXT NOT NULL,
            date TEXT NOT NULL,
            note TEXT,
            FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE visits(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            case_id INTEGER NOT NULL,
            date TEXT NOT NULL,
            note TEXT,
            photo_path TEXT,
            FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE documents(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            case_id INTEGER NOT NULL,
            title TEXT NOT NULL,
            path TEXT NOT NULL,
            created INTEGER NOT NULL,
            FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE theories(
            case_id INTEGER PRIMARY KEY,
            body TEXT NOT NULL,
            updated INTEGER NOT NULL,
            FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE)""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addCase(title:String,court:String,caseNo:String):Long {
        val v=ContentValues().apply{put("title",title);put("court",court);put("case_no",caseNo);put("created",System.currentTimeMillis())}
        return writableDatabase.insert("cases",null,v)
    }
    fun cases():List<CaseRow>{
        val out=mutableListOf<CaseRow>()
        readableDatabase.rawQuery("SELECT id,title,court,case_no,created FROM cases ORDER BY id DESC",null).use{
            while(it.moveToNext()) out += CaseRow(it.getLong(0),it.getString(1),it.getString(2)?:"",it.getString(3)?:"",it.getLong(4))
        }
        return out
    }
    fun deleteCase(id:Long) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("deadlines","case_id=?",arrayOf(id.toString()))
            writableDatabase.delete("visits","case_id=?",arrayOf(id.toString()))
            writableDatabase.delete("documents","case_id=?",arrayOf(id.toString()))
            writableDatabase.delete("theories","case_id=?",arrayOf(id.toString()))
            writableDatabase.delete("cases","id=?",arrayOf(id.toString()))
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun getCase(id:Long):CaseRow?{
        readableDatabase.rawQuery("SELECT id,title,court,case_no,created FROM cases WHERE id=?",arrayOf(id.toString())).use{
            return if(it.moveToFirst()) CaseRow(it.getLong(0),it.getString(1),it.getString(2)?:"",it.getString(3)?:"",it.getLong(4)) else null
        }
    }
    fun addDeadline(caseId:Long,title:String,date:String,note:String){
        val v=ContentValues().apply{put("case_id",caseId);put("title",title);put("date",date);put("note",note)}
        writableDatabase.insert("deadlines",null,v)
    }
    fun allDeadlines():List<Pair<Long,DeadlineRow>>{
        val out=mutableListOf<Pair<Long,DeadlineRow>>()
        readableDatabase.rawQuery("SELECT case_id,id,title,date,note FROM deadlines ORDER BY date",null).use{
            while(it.moveToNext()) out += it.getLong(0) to DeadlineRow(it.getLong(1),it.getString(2),it.getString(3),it.getString(4)?:"")
        }
        return out
    }

    fun deadlines(caseId:Long):List<DeadlineRow>{
        val out=mutableListOf<DeadlineRow>()
        readableDatabase.rawQuery("SELECT id,title,date,note FROM deadlines WHERE case_id=? ORDER BY date",arrayOf(caseId.toString())).use{
            while(it.moveToNext()) out += DeadlineRow(it.getLong(0),it.getString(1),it.getString(2),it.getString(3)?:"")
        }
        return out
    }
    fun addVisit(caseId:Long,date:String,note:String,path:String?){
        val v=ContentValues().apply{put("case_id",caseId);put("date",date);put("note",note);put("photo_path",path)}
        writableDatabase.insert("visits",null,v)
    }
    fun visits(caseId:Long):List<VisitRow>{
        val out=mutableListOf<VisitRow>()
        readableDatabase.rawQuery("SELECT id,date,note,photo_path FROM visits WHERE case_id=? ORDER BY id DESC",arrayOf(caseId.toString())).use{
            while(it.moveToNext()) out += VisitRow(it.getLong(0),it.getString(1),it.getString(2)?:"",it.getString(3))
        }
        return out
    }
    fun addDocument(caseId:Long,title:String,path:String){
        val v=ContentValues().apply{put("case_id",caseId);put("title",title);put("path",path);put("created",System.currentTimeMillis())}
        writableDatabase.insert("documents",null,v)
    }
    fun deleteDocument(id:Long):String? {
        var path:String?=null
        readableDatabase.rawQuery("SELECT path FROM documents WHERE id=?",arrayOf(id.toString())).use{ if(it.moveToFirst()) path=it.getString(0) }
        writableDatabase.delete("documents","id=?",arrayOf(id.toString()))
        return path
    }
    fun deleteDeadline(id:Long){ writableDatabase.delete("deadlines","id=?",arrayOf(id.toString())) }

    fun documents(caseId:Long):List<DocumentRow>{
        val out=mutableListOf<DocumentRow>()
        readableDatabase.rawQuery("SELECT id,title,path,created FROM documents WHERE case_id=? ORDER BY id DESC",arrayOf(caseId.toString())).use{
            while(it.moveToNext()) out += DocumentRow(it.getLong(0),it.getString(1),it.getString(2),it.getLong(3))
        }
        return out
    }
    fun saveTheory(caseId:Long,body:String){
        val v=ContentValues().apply{put("case_id",caseId);put("body",body);put("updated",System.currentTimeMillis())}
        writableDatabase.insertWithOnConflict("theories",null,v,SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun theory(caseId:Long):String{
        readableDatabase.rawQuery("SELECT body FROM theories WHERE case_id=?",arrayOf(caseId.toString())).use{
            return if(it.moveToFirst()) it.getString(0) else ""
        }
    }
}
