package com.lambton.note_elite_android.note;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.commonsware.cwac.richedit.RichEditText;
import com.greenfrvr.hashtagview.HashtagView;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.lambton.note_elite_android.R;
import com.lambton.note_elite_android.views.RichEditWidgetView;
import com.lambton.note_elite_android.activities.AddToFoldersActivityIntentBuilder;
import com.lambton.note_elite_android.database.FolderNoteDAO;
import com.lambton.note_elite_android.database.NotesDAO;
import com.lambton.note_elite_android.actions.NoteDeletedEvent;
import com.lambton.note_elite_android.actions.NoteEditedEvent;
import com.lambton.note_elite_android.actions.NoteFoldersUpdatedEvent;
import com.lambton.note_elite_android.model.Folder;
import com.lambton.note_elite_android.model.Note;
import com.lambton.note_elite_android.model.Note_Table;
import com.lambton.note_elite_android.utils.TimeUtils;
import com.lambton.note_elite_android.utils.Utils;
import com.lambton.note_elite_android.utils.ViewUtils;

import se.emilsjolander.intentbuilder.Extra;
import se.emilsjolander.intentbuilder.IntentBuilder;

@IntentBuilder
public class NoteActivity extends AppCompatActivity{
	private static final String TAG = "NoteActivity";

	private MaterialDialog attachmentDialog;
	private boolean isRecording = false;

	@Extra @Nullable
	Integer noteId;
	Note note;

	@BindView(R.id.toolbar) Toolbar mToolbar;
	@BindView(R.id.title) EditText title;
	@BindView(R.id.body) RichEditText body;
	@BindView(R.id.folders_tag_view) HashtagView foldersTagView;
	@BindView(R.id.drawing_image) ImageView drawingImage;
	@BindView(R.id.create_time_text) TextView creationTimeTextView;
	@BindView(R.id.rich_edit_widget)
	RichEditWidgetView richEditWidgetView;
	private boolean shouldFireDeleteEvent = false;

	@Override protected void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note);
		NoteActivityIntentBuilder.inject(getIntent(), this);
		ButterKnife.bind(this);
		setSupportActionBar(mToolbar);
		mToolbar.setNavigationIcon(ViewUtils.tintDrawable(R.drawable.ic_arrow_back_white_24dp, R.color.md_blue_grey_400));
		mToolbar.setNavigationOnClickListener(new View.OnClickListener(){
			@Override public void onClick(View v){
				onBackPressed();
			}
		});

		if (noteId == null){
			note = new Note();
			Date now = new Date();
			note.setCreatedAt(now);
			note.save();
			noteId = note.getId();
		}

		richEditWidgetView.setRichEditView(body);

		bind();

		foldersTagView.addOnTagClickListener(new HashtagView.TagsClickListener(){
			@Override public void onItemClicked(Object item){
				Toast.makeText(NoteActivity.this, "Folder Clicked", Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void bind(){
		note = NotesDAO.getNote(noteId);
		if (note.getTitle() != null){
			title.setText(note.getTitle());
		}
		if (note.getBody() != null){
			body.setText(note.getSpannedBody());
		}
		foldersTagView.setData(FolderNoteDAO.getFolders(note.getId()), new HashtagView.DataTransform<Folder>(){
			@Override public CharSequence prepare(Folder item){
				return item.getName();
			}
		});
		if (note.getDrawingTrimmed() == null)
			drawingImage.setVisibility(View.GONE);
		else{
			drawingImage.setVisibility(View.VISIBLE);
			drawingImage.setImageBitmap(Utils.getImage(note.getDrawingTrimmed().getBlob()));
		}
		creationTimeTextView.setText("Created " + TimeUtils.getHumanReadableTimeDiff(note.getCreatedAt()));
	}

	@Override protected void onStop(){
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@Override protected void onStart(){
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.note_menu, menu);
		ViewUtils.tintMenu(menu, R.id.delete_note, R.color.md_blue_grey_400);
		ViewUtils.tintMenu(menu, R.id.attachments, R.color.md_blue_grey_400);
		return super.onCreateOptionsMenu(menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item){
		if (item.getItemId() == R.id.delete_note){
			SQLite.delete().from(Note.class).where(Note_Table.id.is(note.getId())).execute();
			shouldFireDeleteEvent = true;
			onBackPressed();
		}

		if (item.getItemId() == R.id.attachments){

			LayoutInflater inflater = this.getLayoutInflater();
			final View layout = inflater.inflate(R.layout.attachment_dialog, null);
			attachmentDialog = new MaterialDialog.Builder(this)
					.autoDismiss(false)
					.customView(layout, false)
					.build();
			attachmentDialog.show();


			// Camera
			android.widget.TextView cameraSelection = layout.findViewById(R.id.camera);
			cameraSelection.setOnClickListener(new AttachmentOnClickListener());

			// Audio recording
			android.widget.TextView recordingSelection = layout.findViewById(R.id.recordings);
			toggleAudioRecordingStop(recordingSelection);
			recordingSelection.setOnClickListener(new AttachmentOnClickListener());

			// Video recording
			android.widget.TextView videoSelection = layout.findViewById(R.id.video);
			videoSelection.setOnClickListener(new AttachmentOnClickListener());

			// Files
			android.widget.TextView filesSelection = layout.findViewById(R.id.files);
			filesSelection.setOnClickListener(new AttachmentOnClickListener());

			// Location
			android.widget.TextView locationSelection = layout.findViewById(R.id.location);
			locationSelection.setOnClickListener(new AttachmentOnClickListener());


		}
		return super.onOptionsItemSelected(item);
	}

	@OnClick({ R.id.edit_drawing_button, R.id.drawing_image }) void clickEditDrawingButton(){
		Intent intent = new DrawingActivityIntentBuilder(note.getId()).build(this);
		startActivity(intent);
	}

	@OnClick(R.id.edit_folders_button) void clickEditFoldersButton(){
		Intent intent = new AddToFoldersActivityIntentBuilder(note.getId()).build(this);
		startActivity(intent);
	}

	@Subscribe(threadMode = ThreadMode.MAIN) public void onNoteEditedEvent(NoteEditedEvent noteEditedEvent){
		Log.e(TAG, "onNoteEditedEvent() called with: " + "noteEditedEvent = [" + noteEditedEvent + "]");
		if (note.getId() == noteEditedEvent.getNote().getId()){
			bind();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onNoteFoldersUpdatedEvent(NoteFoldersUpdatedEvent noteFoldersUpdatedEvent){
		if (note.getId() == noteFoldersUpdatedEvent.getNoteId()){
			bind();
		}
	}

	private class AttachmentOnClickListener implements View.OnClickListener {

		@Override
		public void onClick (View v) {

			switch (v.getId()) {
				// Photo from camera
				case R.id.camera:
					takePhoto();
					break;
				case R.id.recordings:
//					if (!isRecording) {
//						startRecording(v);
//					} else {
//						stopRecording();
//						Attachment attachment = new Attachment(Uri.fromFile(new File(recordName)), MIME_TYPE_AUDIO);
//						attachment.setLength(audioRecordingTime);
//						addAttachment(attachment);
//						mAttachmentAdapter.notifyDataSetChanged();
//						mGridView.autoresize();
//					}
					break;
				case R.id.video:
					takeVideo();
					break;
				case R.id.files:
//					if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
//							PackageManager.PERMISSION_GRANTED) {
//						startGetContentAction();
//					} else {
//						askReadExternalStoragePermission();
//					}
					break;
				case R.id.location:
//					displayLocationDialog();
					break;
				default:
					break;
			}
//			if (!isRecording) {
//				attachmentDialog.dismiss();
//			}
		}
	}


	private void toggleAudioRecordingStop (View v) {
		if (isRecording) {
			((android.widget.TextView) v).setText(getString(R.string.stop));
			((android.widget.TextView) v).setTextColor(Color.parseColor("#ff0000"));
		}
	}

	private void takePhoto () {

//		Toast.makeText(this, "Camera click", Toast.LENGTH_SHORT).show();

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, 1);

	}

	private void takeVideo(){
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(intent, 2);
	}

	@Override public void onBackPressed(){
		super.onBackPressed();
		assert note != null;
		if (shouldFireDeleteEvent){
			EventBus.getDefault().postSticky(new NoteDeletedEvent(note));
		}else{
			String processedTitle = title.getText().toString().trim();
			String processedBody = body.getText().toString().trim();
			if (TextUtils.isEmpty(processedTitle) && TextUtils.isEmpty(processedBody) && note.getDrawingTrimmed() == null){
				SQLite.delete().from(Note.class).where(Note_Table.id.is(note.getId())).execute();
				return;
			}
			note.setSpannedBody(body.getText());
			note.setTitle(processedTitle);
			note.save();
			EventBus.getDefault().postSticky(new NoteEditedEvent(note.getId()));
		}
	}
}