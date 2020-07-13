package com.lambton.note_elite_android.database;


import com.lambton.note_elite_android.model.Note_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;

import com.lambton.note_elite_android.model.Folder;
import com.lambton.note_elite_android.model.Note;


public class NotesDAO{
	public static List<Note> getLatestNotes(Folder folder){
		if (folder == null)
			return SQLite.select().from(Note.class).orderBy(Note_Table.createdAt, false).queryList();
		else
			return FolderNoteDAO.getLatestNotes(folder);
	}

	public static Note getNote(int noteId){
		return SQLite.select().from(Note.class).where(Note_Table.id.is(noteId)).querySingle();
	}
}
