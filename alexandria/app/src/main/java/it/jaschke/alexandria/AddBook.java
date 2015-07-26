package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import eu.livotov.zxscan.ScannerView;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText ean;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    private ScannerView mScannerView = null;
    private TextView mScanButton;


    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean =s.toString();
                //catch isbn10 numbers
                if(ean.length()==10 && !ean.startsWith("978")){
                    ean="978"+ean;
                }
                if(ean.length() == 0){
                    clearFields();
                    return;
                }
                if (Util.isNetworkAvailable(getActivity())) {
                    //Once we have an ISBN, start a book intent
                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, ean);
                    bookIntent.setAction(BookService.FETCH_BOOK);
                    getActivity().startService(bookIntent);
                    AddBook.this.restartLoader();
                } else {
                    Toast.makeText(getActivity(), R.string.book_search_no_internet, Toast.LENGTH_LONG).show();
                }
            }
        });

        mScanButton = (TextView) rootView.findViewById(R.id.scan_button);

        mScanButton.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScanButton.getText().equals(getString(R.string.scan_button))) {
                    mScanButton.setText(R.string.scan_stop_button);
                    mScannerView.setVisibility(View.VISIBLE);
                    mScannerView.startScanner();
                } else {
                    mScanButton.setText(R.string.scan_button);
                    mScannerView.stopScanner();
                    mScannerView.setVisibility(View.GONE);
                }
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView titleView = ((TextView) rootView.findViewById(R.id.bookTitle));
                Snackbar.make(view, getString(R.string.book_saved, titleView.getText()), Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, new UndoClickListener(ean.getText().toString()))
                        .show();
                ean.setText("");
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteBook(ean.getText().toString());
                ean.setText("");
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        setupScanner();

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScannerView != null) {
            mScannerView.stopScanner();
            mScannerView.setVisibility(View.GONE);
            mScanButton.setText(R.string.scan_button);
        }
    }

    private void setupScanner() {
        mScannerView = (ScannerView) rootView.findViewById(R.id.scanner);
        mScannerView.setScannerViewEventListener(new ScannerView.ScannerViewEventListener() {
            @Override
            public void onScannerReady() {
            }

            @Override
            public void onScannerFailure(int i) {
            }

            public boolean onCodeScanned(final String data) {
                mScannerView.stopScanner();
                mScannerView.setVisibility(View.GONE);
                mScanButton.setText(R.string.scan_button);
                ean.setText(data);
                return true;
            }
        });
    }

    private void deleteBook(String book) {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, book);
        bookIntent.setAction(BookService.DELETE_BOOK);
        getActivity().startService(bookIntent);
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        authors = authors == null ? "" : authors;
        String[] authorsArr = authors.split(",");
        ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
        ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    /**
     * Handles clicking undo on the snackbar after adding a book.
     */
    private class UndoClickListener implements View.OnClickListener {

        private String mBookAdded = "";

        public UndoClickListener(String book) {
            mBookAdded = book;
        }

        @Override
        public void onClick(View v) {
            deleteBook(mBookAdded);
        }
    }
}
