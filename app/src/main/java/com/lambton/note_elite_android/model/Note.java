package com.lambton.note_elite_android.model;

import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.SpannableString;

import com.commonsware.cwac.richtextutils.SpannableStringGenerator;
import com.commonsware.cwac.richtextutils.SpannedXhtmlGenerator;
import com.google.android.gms.maps.model.LatLng;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.data.Blob;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import com.lambton.note_elite_android.database.AppDatabase;

@Table(database = AppDatabase.class, allFields = true)
public class Note extends BaseModel {

	@PrimaryKey(autoincrement = true)
	private int id;
	private String title;
	private String body;
	private Blob drawing;
	private Blob drawingTrimmed;
	private Date createdAt;
//	private LatLng latLng;
//	private Blob image;
//	private Blob video;
//	private String recording;

/*	public LatLng getLatLng() {
		return latLng;
	}

	public void setLatLng(LatLng latLng) {
		this.latLng = latLng;
	}*/
//	public Blob getImage() {
//		return image;
//	}
//
//	public void setImage(Blob image) {
//		this.image = image;
//	}
//
//	public Blob getVideo() {
//		return video;
//	}
//
//	public void setVideo(Blob video) {
//		this.video = video;
//	}
//
//	public String getRecording() {
//		return recording;
//	}
//
//	public void setRecording(String recording) {
//		this.recording = recording;
//	}

	public Note(){}

	public int getId(){
		return id;
	}
	
	public void setId(int id){
		this.id = id;
	}

	public String getTitle(){
		return title;
	}

	public void setTitle(String title){
		this.title = title;
	}

	public String getBody(){
		return body;
	}

	public void setBody(String body){
		this.body = body;
	}

	public Blob getDrawing(){
		return drawing;
	}

	public void setDrawing(Blob drawing){
		this.drawing = drawing;
	}

	public Blob getDrawingTrimmed(){
		return drawingTrimmed;
	}

	public void setDrawingTrimmed(Blob drawingTrimmed){
		this.drawingTrimmed = drawingTrimmed;
	}

	public Date getCreatedAt(){
		return createdAt;
	}

	public void setCreatedAt(Date createdAt){
		this.createdAt = createdAt;
	}

	public Spannable getSpannedBody(){
		SpannableStringGenerator spannableStringGenerator = new SpannableStringGenerator();
		try{
			return spannableStringGenerator.fromXhtml(body);
		}catch (ParserConfigurationException e){
			e.printStackTrace();
		}catch (SAXException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}
		return new SpannableString("!ERROR!");
	}

	public void setSpannedBody(Spannable spannedBody){
		SpannedXhtmlGenerator spannedXhtmlGenerator = new SpannedXhtmlGenerator();
		body = spannedXhtmlGenerator.toXhtml(spannedBody);
		body = body.replaceAll("(?m)(^ *| +(?= |$))", "")
		           .replaceAll("(?m)^$([\r\n]+?)(^$[\r\n]+?^)+", "$1")
		           .replace("<br/>", "\n");
	}

	@Override public boolean equals(Object o){
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Note note = (Note) o;

		return id == note.id;
	}

	@Override public int hashCode(){
		return id;
	}

	@Override public String toString(){
		return "Note{" +
				"id=" + id +
				", title='" + title + '\'' +
				"} " + super.toString();
	}

	/*Comparator for sorting the list by Note title*/
	public static Comparator<Note> StuTitleComparator = new Comparator<Note>() {
		public int compare(Note s1, Note s2) {
			String noteTitle1 = s1.getTitle().toUpperCase();
			String noteTitle2 = s2.getTitle().toUpperCase();
			//ascending order
			return noteTitle1.compareTo(noteTitle2);
			//descending order
			//return StudentName2.compareTo(StudentName1);
		}};
}
