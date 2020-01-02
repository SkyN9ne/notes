////////////////////////////////////////////////////////////////////////////////
//
//  Notes - Notebook for Android
//
//  Copyright © 2019  Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
////////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.notes;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.ViewSwitcher;

import android.support.v4.content.FileProvider;

import org.billthefarmer.markdown.MarkdownView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.lang.ref.WeakReference;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Notes extends Activity
{
    public final static String TAG = "Notes";

    public final static String CHANGED = "changed";
    public final static String CONTENT = "content";
    public final static String MODIFIED = "modified";
    public final static String SHOWN = "shown";
    public final static String PATH = "path";

    private final static String STYLES = "file:///android_asset/styles.css";
    private final static String SCRIPT = "file:///android_asset/script.js";
    private final static String CSS_STYLES = "css/styles.css";
    private final static String TEXT_CSS = "text/css";
    private final static String JS_SCRIPT = "js/script.js";
    private final static String TEXT_JAVASCRIPT = "text/javascript";

    public final static String FOLDER = "Folder:  ";
    public final static String FILE_PROVIDER =
        "org.billthefarmer.notes.fileprovider";
    public final static String FILE_URI =
        "org.billthefarmer.notes.Uri";

    public final static String NOTES_FOLDER = "Notes";
    public final static String NOTES_FILE = "Notes.md";
    public final static String NOTES_IMAGE = "Notes.png";
    public final static String TEMPLATE_FILE = "Template.md";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String IMAGE_PNG = "image/png";
    public final static String WILD_WILD = "*/*";
    public final static String IMAGE = "image";
    public final static String AUDIO = "audio";
    public final static String VIDEO = "video";
    public final static String MEDIA_TEMPLATE = "![%s](%s)\n";
    public final static String LINK_TEMPLATE = "[%s](%s)\n";
    public final static String AUDIO_TEMPLATE =
        "<audio controls src=\"%s\"></audio>\n";
    public final static String VIDEO_TEMPLATE =
        "<video controls src=\"%s\"></video>\n";
    public final static String MAP_TEMPLATE =
        "<iframe width=\"560\" height=\"420\" " +
        "src=\"http://www.openstreetmap.org/export/embed.html?" +
        "bbox=%f,%f,%f,%f&amp;layer=mapnik\">" +
        "</iframe><br/><small>" +
        "<a href=\"http://www.openstreetmap.org/#map=16/%f/%f\">" +
        "View Larger Map</a></small>\n";

    public final static String GEO = "geo";
    public final static String OSM = "osm";
    public final static String HTTP = "http";
    public final static String TEXT = "text";
    public final static String HTTPS = "https";

    public final static Pattern GEO_PATTERN =
        Pattern.compile("geo:(-?\\d+[.]\\d+), ?(-?\\d+[.]\\d+).*");
    public final static Pattern MEDIA_PATTERN =
        Pattern.compile("!\\[(.*)\\]\\((.+)\\)", Pattern.MULTILINE);

    private final static int ADD_MEDIA = 1;
    private static final int EDIT_TEXT = 0;
    private static final int MARKDOWN = 1;
    private static final int ACCEPT = 0;
    private static final int EDIT = 1;

    private final static int REQUEST_READ = 1;
    private final static int REQUEST_SAVE = 2;
    private final static int REQUEST_OPEN = 3;
    private final static int REQUEST_TEMPLATE = 4;

    private final static int POSITION_DELAY = 128;
    private final static int MAX_PATHS = 10;

    private EditText textView;
    private ScrollView scrollView;

    private MarkdownView markdownView;
    private ViewSwitcher viewSwitcher;
    private ViewSwitcher buttonSwitcher;

    private SearchView searchView;
    private MenuItem searchItem;

    private View accept;
    private View edit;

    private Set<String> pathSet;
    private List<String> removeList;

    private String folder = NOTES_FOLDER;
    private String templateFile;

    private Uri uri;
    private File file;
    private String path;
    private Uri content;
    private String append;

    private boolean shown = true;
    private boolean changed = false;

    private boolean external = false;
    private boolean useTemplate = false;
    private boolean darkTheme = false;

    private long modified;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get preferences
        getPreferences();

        if (darkTheme)
            setTheme(R.style.AppDarkTheme);

        setContentView(R.layout.main);

        textView = findViewById(R.id.text);
        scrollView = findViewById(R.id.scroll);
        markdownView = findViewById(R.id.markdown);

        accept = findViewById(R.id.accept);
        edit = findViewById(R.id.edit);

        viewSwitcher = findViewById(R.id.view_switcher);
        buttonSwitcher = findViewById(R.id.button_switcher);

        WebSettings settings = markdownView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        if (savedInstanceState == null)
            defaultFile(null);

        setListeners();
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        markdownView.restoreState(savedInstanceState);

        path = savedInstanceState.getString(PATH);
        shown = savedInstanceState.getBoolean(SHOWN);
        changed = savedInstanceState.getBoolean(CHANGED);
        modified = savedInstanceState.getLong(MODIFIED);
        content = savedInstanceState.getParcelable(CONTENT);
        invalidateOptionsMenu();

        file = new File(path);
        uri = Uri.fromFile(file);

        String title = uri.getLastPathSegment();
        setTitle(title);

        if (file.lastModified() > modified)
            alertDialog(R.string.appName, R.string.changedReload,
                        R.string.reload, R.string.cancel, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                readNote(uri);
            }
        });
     }

    // onResume
    @Override
    protected void onResume()
    {
        super.onResume();

        boolean dark = darkTheme;

        // Get preferences
        getPreferences();

        // Recreate
        if (dark != darkTheme && Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
            recreate();

        // Clear cache
        markdownView.clearCache(true);

        if (changed)
            loadMarkdown();

        setVisibility();
    }

    // onSaveInstanceState
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        markdownView.saveState(outState);

        outState.putParcelable(CONTENT, content);
        outState.putLong(MODIFIED, modified);
        outState.putBoolean(CHANGED, changed);
        outState.putBoolean(SHOWN, shown);
        outState.putString(PATH, path);
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        searchItem = menu.findItem(R.id.search);

        // Set up search view
        if (searchItem != null)
            searchView = (SearchView) searchItem.getActionView();

        // Set up search view options and listener
        if (searchView != null)
        {
            searchView.setSubmitButtonEnabled(true);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(new QueryTextListener());
        }

        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.findItem(R.id.saveNote).setVisible(changed);

        // Get a list of recent files
        List<Long> list = new ArrayList<>();
        Map<Long, String> map = new HashMap<>();

        // Get the last modified dates
        for (String path : pathSet)
        {
            File file = new File(path);
            long last = file.lastModified();
            list.add(last);
            map.put(last, path);
        }

        // Sort in reverse order
        Collections.sort(list);
        Collections.reverse(list);

        // Get the submenu
        MenuItem item = menu.findItem(R.id.openRecent);
        SubMenu sub = item.getSubMenu();
        sub.clear();

        // Add the recent files
        for (long date : list)
        {
            String path = map.get(date);

            // Remove path prefix
            String name =
                path.replaceFirst(Environment
                                  .getExternalStorageDirectory()
                                  .getPath() + File.separator, "");
            sub.add(name);
        }

        // Add clear list item
        sub.add(Menu.NONE, R.id.clearList, Menu.NONE, R.string.clearList);

        return true;
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case android.R.id.home:
            onBackPressed();
            break;
        case R.id.newNote:
            newNote();
            break;
        case R.id.openNote:
            openNote();
            break;
        case R.id.saveNote:
            saveNote();
            break;
        case R.id.saveAs:
            saveAs();
            break;
        case R.id.clearList:
            clearList();
            break;
        case R.id.shareNote:
            shareNote();
            break;
        case R.id.addTime:
            addTime();
            break;
        case R.id.addMedia:
            addMedia();
            break;
        case R.id.editStyles:
            editStyles();
            break;
        case R.id.editScript:
            editScript();
            break;
        case R.id.settings:
            settings();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }

        // Close text search
        if (searchItem.isActionViewExpanded())
            searchItem.collapseActionView();

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        // External
        if (markdownView.canGoBack())
            markdownView.goBack();

        else
            super.onBackPressed();
    }

    // getPreferences
    private void getPreferences()
    {
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        external = preferences.getBoolean(Settings.PREF_EXTERNAL, false);
        useTemplate = preferences.getBoolean(Settings.PREF_USE_TEMPLATE, false);
        darkTheme = preferences.getBoolean(Settings.PREF_DARK_THEME, false);

        // Template file
        templateFile = preferences.getString(Settings.PREF_TEMPLATE_FILE,
                                             TEMPLATE_FILE);
        // Folder
        folder = preferences.getString(Settings.PREF_FOLDER, NOTES_FOLDER);

        Set<String> set =
            preferences.getStringSet(Settings.PREF_PATHS, null);
        pathSet = new HashSet<>();
        if (set != null)
            pathSet.addAll(set);
        removeList = new ArrayList<>();
    }

    // setListeners
    private void setListeners()
    {
        if (textView != null)
            textView.addTextChangedListener(new TextWatcher()
        {
            // afterTextChanged
            @Override
            public void afterTextChanged(Editable s)
            {
                // Text changed
                changed = true;
                invalidateOptionsMenu();
            }

            // beforeTextChanged
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after)
            {
            }

            // onTextChanged
            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count)
            {
            }
        });

        if (markdownView != null)
        {
            // On long click
            markdownView.setOnLongClickListener(v ->
            {
                // Reveal button
                edit.setVisibility(View.VISIBLE);
                return false;
            });
        }

        if (accept != null)
        {
            // On click
            accept.setOnClickListener(v ->
            {
                // Check flag
                if (changed)
                {
                    // Get text
                    loadMarkdown();
                }

                // Animation
                animateAccept();

                // Close text search
                if (searchItem.isActionViewExpanded())
                    searchItem.collapseActionView();

                shown = true;
            });

            // On long click
            accept.setOnLongClickListener(v ->
            {
                // Hide button
                v.setVisibility(View.INVISIBLE);
                return true;
            });
        }

        if (edit != null)
        {
            // On click
            edit.setOnClickListener(v ->
            {
                // Animation
                animateEdit();

                // Close text search
                if (searchItem.isActionViewExpanded())
                    searchItem.collapseActionView();

                // Scroll after delay
                edit.postDelayed(() ->
                {
                    // Get selection
                    int selection = textView.getSelectionStart();

                    // Get text position
                    int line = textView.getLayout().getLineForOffset(selection);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    int height = scrollView.getHeight();
                    scrollView.smoothScrollTo(0, position - height / 2);
                }, POSITION_DELAY);

                shown = false;
            });

            // On long click
            edit.setOnLongClickListener(v ->
            {
                // Hide button
                v.setVisibility(View.INVISIBLE);
                return true;
            });
        }

        if (textView != null)
        {
            // onFocusChange
            textView.setOnFocusChangeListener((v, hasFocus) ->
            {
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
                if (!hasFocus)
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            });

            // On long click
            textView.setOnLongClickListener(v ->
            {
                // Reveal button
                accept.setVisibility(View.VISIBLE);
                return false;
            });
        }
    }

    // animateAccept
    public void animateAccept()
    {
        // Animation
        viewSwitcher.setDisplayedChild(MARKDOWN);
        buttonSwitcher.setDisplayedChild(EDIT);
    }

    // animateEdit
    private void animateEdit()
    {
        // Animation
        viewSwitcher.setDisplayedChild(EDIT_TEXT);
        buttonSwitcher.setDisplayedChild(ACCEPT);
    }

    // loadMarkdown
    private void loadMarkdown()
    {
        CharSequence text = textView.getText();
        loadMarkdown(text);
    }

    // loadMarkdown
    private void loadMarkdown(CharSequence text)
    {
        markdownView.loadMarkdown(getBaseUrl(), markdownCheck(text),
                                  getStyles(), getScript());
    }

    // getBaseUrl
    private String getBaseUrl()
    {
        return Uri.fromFile(getHome()).toString() + File.separator;
    }

    // settings
    private void settings()
    {
        startActivity(new Intent(this, Settings.class));
    }

    // getHome
    private File getHome()
    {
        File file = new File(folder);
        if (file.isAbsolute() && file.isDirectory() && file.canWrite())
            return file;

        return new File(Environment.getExternalStorageDirectory(), folder);
    }

    // markdownCheck
    private String markdownCheck(CharSequence text)
    {
        // Check for media
        return mediaCheck(text).toString();
    }

    // getStyles
    private String getStyles()
    {
        File cssFile = new File(getHome(), CSS_STYLES);

        if (cssFile.exists())
            return Uri.fromFile(cssFile).toString();

        return STYLES;
    }

    // getScript
    private String getScript()
    {
        File jsFile = new File(getHome(), JS_SCRIPT);

        if (jsFile.exists())
            return Uri.fromFile(jsFile).toString();

        return null;
    }

    // mediaCheck
    private CharSequence mediaCheck(CharSequence text)
    {
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = MEDIA_PATTERN.matcher(text);

        // Find matches
        while (matcher.find())
        {
            File file = new File(matcher.group(2));
            String type = FileUtils.getMimeType(file);

            if (type == null)
            {
                Matcher geoMatcher = GEO_PATTERN.matcher(matcher.group(2));

                if (geoMatcher.matches())
                {
                    NumberFormat parser =
                        NumberFormat.getInstance(Locale.ENGLISH);

                    double lat;
                    double lng;

                    try
                    {
                        lat = parser.parse(geoMatcher.group(1)).doubleValue();
                        lng = parser.parse(geoMatcher.group(2)).doubleValue();
                    }

                    // Ignore parse error
                    catch (Exception e)
                    {
                        continue;
                    }

                    // Create replacement iframe
                    String replace =
                        String.format(Locale.ENGLISH, MAP_TEMPLATE,
                                      lng - 0.005, lat - 0.005,
                                      lng + 0.005, lat + 0.005,
                                      lat, lng);

                    // Append replacement
                    matcher.appendReplacement(buffer, replace);
                }
                else
                {
                }
            }
            else if (type.startsWith(IMAGE))
            {
                // Do nothing, handled by markdown view
            }
            else if (type.startsWith(AUDIO))
            {
                // Create replacement
                String replace =
                    String.format(AUDIO_TEMPLATE, matcher.group(2));

                // Append replacement
                matcher.appendReplacement(buffer, replace);
            }
            else if (type.startsWith(VIDEO))
            {
                // Create replacement
                String replace =
                    String.format(VIDEO_TEMPLATE, matcher.group(2));

                // Append replacement
                matcher.appendReplacement(buffer, replace);
            }
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer;
    }

    // setVisibility
    private void setVisibility()
    {
        buttonSwitcher.setVisibility(View.VISIBLE);
        // Check if shown
        if (shown)
        {
            viewSwitcher.setDisplayedChild(MARKDOWN);
            buttonSwitcher.setDisplayedChild(EDIT);
        }
        else
        {
            viewSwitcher.setDisplayedChild(EDIT_TEXT);
            buttonSwitcher.setDisplayedChild(ACCEPT);
        }
    }

    // clearList
    private void clearList()
    {
        for (String path : pathSet)
            removeList.add(path);

        pathSet.clear();
    }

    // shareNote
    @SuppressWarnings("deprecation")
    public void shareNote()
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.appName) + ": " +
                        getTitle().toString());
        if (shown)
        {
            intent.setType(IMAGE_PNG);
            View v = markdownView.getRootView();
            v.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v.getDrawingCache());
            v.setDrawingCacheEnabled(false);

            File image = new File(getCacheDir(), NOTES_IMAGE);
            try (FileOutputStream out = new FileOutputStream(image))
            {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            }

            catch (Exception e) {}
            Uri imageUri = FileProvider
                .getUriForFile(this, FILE_PROVIDER, image);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        }

        else
        {
            intent.setType(TEXT_PLAIN);
            intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
        }

        startActivity(Intent.createChooser(intent, null));
    }

    // addTime
    public void addTime()
    {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        String time = format.format(new Date());
        Editable editable = textView.getEditableText();
        int position = textView.getSelectionStart();
        editable.insert(position, time);
        loadMarkdown();
    }

    // addMedia
    public void addMedia()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(WILD_WILD);
        startActivityForResult(Intent.createChooser(intent, null), ADD_MEDIA);
    }

    // onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data)
    {
        // Do nothing if cancelled
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == ADD_MEDIA)
        {
            // Get uri
            Uri uri = data.getData();

            // Resolve content uri
            if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                uri = resolveContent(uri);

            if (uri != null)
            {
                String type;

                // Get type
                if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                    type = getContentResolver().getType(uri);

                else
                    type = FileUtils.getMimeType(this, uri);

                if (type == null)
                    addLink(uri, uri.getLastPathSegment());

                else if (type.startsWith(IMAGE) ||
                         type.startsWith(AUDIO) ||
                         type.startsWith(VIDEO))
                    addMedia(uri);

                else
                    addLink(uri, uri.getLastPathSegment());
            }
        }
    }

    // addMedia
    private void addMedia(Uri media)
    {
        String name = media.getLastPathSegment();
        String mediaText = String.format(MEDIA_TEMPLATE,
                                         name,
                                         media.toString());

        Editable editable = textView.getEditableText();
        int position = textView.getSelectionStart();
        editable.insert(position, mediaText);
        loadMarkdown();
    }

    // addLink
    private void addLink(Uri uri, String title)
    {
        if ((title == null) || (title.length() == 0))
            title = uri.getLastPathSegment();

        String url = uri.toString();
        String linkText = String.format(LINK_TEMPLATE, title, url);
        Editable editable = textView.getEditableText();
        int position = textView.getSelectionStart();
        editable.insert(position, linkText);
        loadMarkdown();
    }

    // addMap
    private void addMap(Uri uri)
    {
        String mapText = String.format(MEDIA_TEMPLATE,
                                       OSM,
                                       uri.toString());
        if (true)
            textView.append(mapText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, mapText);
        }

        loadMarkdown();
    }

    // editStyles
    public void editStyles()
    {
        File file = new File(getHome(), CSS_STYLES);

        // Get file provider uri
        Uri uri = FileProvider.getUriForFile
            (this, "org.billthefarmer.notes.fileprovider", file);
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Path " + uri.getPath());

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, TEXT_CSS);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        Uri extra = Uri.fromFile(file);
        intent.putExtra(FILE_URI, extra);
        startActivity(intent);
    }

    // editScript
    public void editScript()
    {
        File file = new File(getHome(), JS_SCRIPT);

        // Get file provider uri
        Uri uri = FileProvider.getUriForFile
            (this, "org.billthefarmer.notes.fileprovider", file);
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Path " + uri.getPath());

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, TEXT_JAVASCRIPT);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        Uri extra = Uri.fromFile(file);
        intent.putExtra(FILE_URI, extra);
        startActivity(intent);
    }

    // newNote
    private void newNote()
    {
        // Check if file changed
        if (changed)
            alertDialog(R.string.newNote, R.string.modified,
                        R.string.saveNote, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveNote();
                textView.setText("");
                changed = false;
                // Check template
                if (useTemplate)
                    loadTemplate();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                textView.setText("");
                changed = false;
                if (useTemplate)
                    loadTemplate();
                break;
            }
        });

        else
        {
            textView.setText("");
            changed = false;
            if (useTemplate)
                loadTemplate();
        }

        loadMarkdown();
        invalidateOptionsMenu();
    }

    // loadTemplate
    private void loadTemplate()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE},
                                   REQUEST_TEMPLATE);

                return;
            }
        }

        loadMarkdown();
    }

    // openNote
    private void openNote()
    {
        // Check if file changed
        if (changed)
            alertDialog(R.string.openNote, R.string.modified,
                        R.string.saveNote, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveNote();
                getNote();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                changed = false;
                getNote();
                break;
            }
        });

        else
            getNote();
    }

    // alertDialog
    private void alertDialog(int title, int message,
                             int positiveButton, int negativeButton,
                             DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(positiveButton, listener);
        builder.setNegativeButton(negativeButton, listener);

        // Create the AlertDialog
        builder.show();
    }

    // alertDialog
    private void alertDialog(int title, String message, int neutralButton)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setNeutralButton(neutralButton, null);

        // Create the AlertDialog
        builder.show();
    }

    // getNote
    private void getNote()
    {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_OPEN);
                return;
            }
        }

        // Open parent folder
        File dir = file.getParentFile();
        getNote(dir);
    }

    // getNote
    private void getNote(File dir)
    {
        // Get list of files
        List<File> list = getList(dir);
        if (list == null)
            return;

        // Pop up dialog
        String title = FOLDER + dir.getPath();
        openDialog(title, list, (dialog, which) ->
            {
                File selection = list.get(which);
                if (selection.isDirectory())
                    getNote(selection);

                else
                    readNote(Uri.fromFile(selection));
            });
    }

    // getList
    private List<File> getList(File dir)
    {
        List<File> list = null;
        File[] files = dir.listFiles();
        // Check files
        if (files == null)
        {
            // Create a list with just the parent folder and the
            // external storage folder
            list = new ArrayList<File>();

            if (dir.getParentFile() == null)
                list.add(dir);

            else
                list.add(dir.getParentFile());

            list.add(Environment.getExternalStorageDirectory());
            return list;
        }

        // Sort the files
        Arrays.sort(files);
        // Create a list
        list = new ArrayList<File>(Arrays.asList(files));
        // Remove hidden files
        Iterator<File> iterator = list.iterator();
        while (iterator.hasNext())
        {
            File item = iterator.next();
            if (item.getName().startsWith("."))
                iterator.remove();
        }

        // Add parent folder
        if (dir.getParentFile() == null)
            list.add(0, dir);

        else
            list.add(0, dir.getParentFile());

        return list;
    }

    // openDialog
    private void openDialog(String title, List<File> list,
                            DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Add the adapter
        FileAdapter adapter = new FileAdapter(builder.getContext(), list);
        builder.setAdapter(adapter, listener);

        // Add the button
        builder.setNegativeButton(R.string.cancel, null);

        // Create the Dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        switch (requestCode)
        {
        case REQUEST_SAVE:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .WRITE_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, save file
                    saveFile();
            break;

        case REQUEST_READ:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, read file
                    readNote(uri);
            break;

        case REQUEST_OPEN:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, open file
                    getNote();
            break;

        case REQUEST_TEMPLATE:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, load template
                    loadTemplate();
            break;
        }
    }

    // readNote
    private void readNote(Uri uri)
    {
        if (uri == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ);
                this.uri = uri;
                return;
            }
        }

        content = null;

        // Attempt to resolve content uri
        if (CONTENT.equalsIgnoreCase(uri.getScheme()))
            uri = resolveContent(uri);

        // Read into default file if unresolved
        if (CONTENT.equalsIgnoreCase(uri.getScheme()))
        {
            content = uri;
            file = getDefaultFile();
            Uri defaultUri = Uri.fromFile(file);
            path = defaultUri.getPath();

            String title = uri.getLastPathSegment();
            setTitle(title);
        }

        // Read file
        else
        {
            path = uri.getPath();
            file = new File(path);

            String title = uri.getLastPathSegment();
            setTitle(title);
        }

        textView.setText(R.string.loading);

        ReadTask read = new ReadTask(this);
        read.execute(uri);

        changed = false;
        modified = file.lastModified();
        savePath(path);
        invalidateOptionsMenu();
    }

    // resolveContent
    private Uri resolveContent(Uri uri)
    {
        String path = FileUtils.getPath(this, uri);

        if (path != null)
        {
            File file = new File(path);
            if (file.canRead())
                uri = Uri.fromFile(file);
        }

        return uri;
    }

    // getDefaultFile
    private File getDefaultFile()
    {
        return new File(getHome(), NOTES_FILE);
    }

    // defaultFile
    private void defaultFile(String text)
    {
        file = getDefaultFile();

        uri = Uri.fromFile(file);
        path = uri.getPath();

        if (file.exists())
        {
            readNote(uri);
            append = text;
        }

        else
        {
            if (text != null)
                textView.append(text);

            String title = uri.getLastPathSegment();
            setTitle(title);
        }
    }

    // savePath
    private void savePath(String path)
    {
        // Save the current position
        pathSet.add(path);

        // Get a list of files
        List<Long> list = new ArrayList<>();
        Map<Long, String> map = new HashMap<>();
        for (String name : pathSet)
        {
            File file = new File(name);
            list.add(file.lastModified());
            map.put(file.lastModified(), name);
        }

        // Sort in reverse order
        Collections.sort(list);
        Collections.reverse(list);

        int count = 0;
        for (long date : list)
        {
            String name = map.get(date);

            // Remove old files
            if (count >= MAX_PATHS)
            {
                pathSet.remove(name);
                removeList.add(name);
            }

            count++;
        }
    }

    // save
    private void saveNote()
    {
        saveFile();
    }

    // saveFile
    private void saveFile()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_SAVE);
                return;
            }
        }

        if (file.lastModified() > modified)
            alertDialog(R.string.appName, R.string.changedOverwrite,
                        R.string.overwrite, R.string.cancel, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile(file);
                break;
            }
        });

        else
        {
            if (content == null)
                saveFile(file);

            else
                saveFile(content);
        }
    }

    // saveFile
    private void saveFile(File file)
    {
        CharSequence text = textView.getText();
        write(text, file);
    }

    // saveFile
    private void saveFile(Uri uri)
    {
        CharSequence text = textView.getText();
        try (OutputStream outputStream =
             getContentResolver().openOutputStream(uri))
        {
            write(text, outputStream);
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
            return;
        }
    }

    // saveAs
    private void saveAs()
    {
        // Remove path prefix
        String name =
            path.replaceFirst(Environment
                              .getExternalStorageDirectory()
                              .getPath() + File.separator, "");

        // Open dialog
        saveAsDialog(name, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                EditText text = ((Dialog) dialog).findViewById(R.id.path_text);
                String string = text.getText().toString();

                // Ignore empty string
                if (string.isEmpty())
                    return;

                file = new File(string);

                // Check absolute file
                if (!file.isAbsolute())
                    file = new
                        File(Environment.getExternalStorageDirectory(), string);

                // Set interface title
                Uri uri = Uri.fromFile(file);
                String title = uri.getLastPathSegment();
                setTitle(title);

                path = file.getPath();
                saveFile();
            }
        });
    }

    // saveAsDialog
    private void saveAsDialog(String path,
                              DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.saveNote);
        builder.setMessage(R.string.choose);

        // Add the buttons
        builder.setPositiveButton(R.string.saveNote, listener);
        builder.setNegativeButton(R.string.cancel, listener);

        // Create edit text
        Context context = builder.getContext();
        EditText text = new EditText(context);
        text.setId(R.id.path_text);
        text.setText(path);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setView(text, 40, 0, 40, 0);
        dialog.show();
    }

    // write
    private void write(CharSequence text, File file)
    {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file))
        {
            writer.append(text);
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
            return;
        }

        changed = false;
        invalidateOptionsMenu();
        modified = file.lastModified();
        savePath(file.getPath());
    }

    // write
    private void write(CharSequence text, OutputStream os)
    {
        try (OutputStreamWriter writer = new OutputStreamWriter(os))
        {
            writer.append(text);
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
            return;
        }

        changed = false;
        invalidateOptionsMenu();
    }

    // loadText
    private void loadText(CharSequence text)
    {
        if (textView != null)
            textView.setText(text);

        if (append != null)
        {
            textView.append(append);
            append = null;
            changed = true;
        }

        else
            changed = false;

        loadMarkdown();

        // Dismiss keyboard
        textView.clearFocus();

        // Update menu
        invalidateOptionsMenu();
    }

    // QueryTextListener
    private class QueryTextListener
        implements SearchView.OnQueryTextListener
    {
        private BackgroundColorSpan span = new
        BackgroundColorSpan(Color.YELLOW);
        private Editable editable;
        private Pattern pattern;
        private Matcher matcher;
        private int index;
        private int height;

        // onQueryTextChange
        @Override
        @SuppressWarnings("deprecation")
        public boolean onQueryTextChange(String newText)
        {
            // Use web view functionality
            if (shown)
                markdownView.findAll(newText);

            // Use regex search and spannable for highlighting
            else
            {
                height = scrollView.getHeight();
                editable = textView.getEditableText();

                // Reset the index and clear highlighting
                if (newText.length() == 0)
                {
                    index = 0;
                    editable.removeSpan(span);
                }

                // Get pattern
                pattern = Pattern.compile(newText,
                                          Pattern.CASE_INSENSITIVE |
                                          Pattern.LITERAL |
                                          Pattern.UNICODE_CASE);
                // Find text
                matcher = pattern.matcher(editable);
                if (matcher.find(index))
                {
                    // Get index
                    index = matcher.start();

                    // Get text position
                    int line = textView.getLayout().getLineForOffset(index);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    scrollView.smoothScrollTo(0, position - height / 2);

                    // Highlight it
                    editable.setSpan(span, matcher.start(), matcher.end(),
                                     Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            return true;
        }

        // onQueryTextSubmit
        @Override
        public boolean onQueryTextSubmit(String query)
        {
            // Use web view functionality
            if (shown)
                markdownView.findNext(true);

            // Use regex search and spannable for highlighting
            else
            {
                // Find next text
                if (matcher.find())
                {
                    // Get index
                    index = matcher.start();

                    // Get text position
                    int line = textView.getLayout().getLineForOffset(index);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    scrollView.smoothScrollTo(0, position - height / 2);

                    // Highlight it
                    editable.setSpan(span, matcher.start(), matcher.end(),
                                     Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Reset matcher
                if (matcher.hitEnd())
                    matcher.reset();
            }

            return true;
        }
    }

    // ReadTask
    private static class ReadTask
        extends AsyncTask<Uri, Void, CharSequence>
    {
        private WeakReference<Notes> notesWeakReference;

        public ReadTask(Notes notes)
        {
            notesWeakReference = new WeakReference<>(notes);
        }

        // doInBackground
        @Override
        protected CharSequence doInBackground(Uri... uris)
        {
            StringBuilder stringBuilder = new StringBuilder();
            final Notes notes = notesWeakReference.get();
            if (notes == null)
                return stringBuilder;

            try (InputStream inputStream = notes.getContentResolver()
                 .openInputStream(uris[0]);
                 BufferedReader reader = new BufferedReader
                 (new InputStreamReader(inputStream)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    stringBuilder.append(line);
                    stringBuilder.append(System.getProperty("line.separator"));
                }
            }

            catch (Exception e)
            {
                notes.textView.post(() ->
                                     notes.alertDialog(R.string.appName,
                                                        e.getMessage(),
                                                        R.string.ok));
                e.printStackTrace();
            }

            return stringBuilder;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(CharSequence result)
        {
            final Notes notes = notesWeakReference.get();
            if (notes == null)
                return;

            notes.loadText(result);
        }
    }
}
