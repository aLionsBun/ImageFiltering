package com.imagefilters;

import androidx.annotation.FloatRange;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

/**
 * Activity to filter given image
 */
public class FilterImage extends AppCompatActivity {
    /**
     * Bitmap of image to be filtered and filtered image
     */
    private Bitmap mSourceImageBitmap, mFilteredImageBitmap;

    /**
     * View of loaded image
     */
    private ImageView mSourceImageView;

    /**
     * Views to interact with buttons
     */
    private Button mInvertFilter, mGrayscaleFilter, mBlurFilter,
            mSharpFilter, mGbrFilter, mBrgFilter, mClear, mSave, mBack;

    /**
     * Write permission name in String[] format
     */
    private final String[] WRITE_PERMISSION = { Manifest.permission.WRITE_EXTERNAL_STORAGE };

    /**
     * Tag for logging
     */
    private final String TAG = "FilterImage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_image);

        //Check if MainActivity provided us with Intent
        if(getIntent().getData()==null) {
            //Load default image (required for testing)
            mSourceImageBitmap = BitmapFactory
                    .decodeResource(this.getResources(), R.drawable.ducklings);
        }
        else {
            //Load image from URI provided by MainActivity
            mSourceImageBitmap = loadImage(getIntent().getData());
        }

        mFilteredImageBitmap=mSourceImageBitmap;

        setupViews();

        mInvertFilter.setOnClickListener(e -> {
            mFilteredImageBitmap=invertFilter(mFilteredImageBitmap);
            setFilteredImageView();
        });

        mGrayscaleFilter.setOnClickListener(e -> {
            mFilteredImageBitmap=grayscaleFilter(mFilteredImageBitmap);
            setFilteredImageView();
        });

        mBlurFilter.setOnClickListener(e -> {
            mFilteredImageBitmap=blurFilter(mFilteredImageBitmap, 25f);
            setFilteredImageView();
        });

        mSharpFilter.setOnClickListener(e -> {
            mFilteredImageBitmap=sharpFilter(mFilteredImageBitmap, 1);
            setFilteredImageView();
        });

        mGbrFilter.setOnClickListener(e -> {
            mFilteredImageBitmap=rgbToGbrFilter(mFilteredImageBitmap);
            setFilteredImageView();
        });

        mBrgFilter.setOnClickListener(e -> {
            mFilteredImageBitmap=rgbToBrgFilter(mFilteredImageBitmap);
            setFilteredImageView();
        });

        mClear.setOnClickListener(e -> clearFilters());

        mSave.setOnClickListener(e -> {
            //Checking if we have permission to write to storage
            if(!MainActivity.hasPermission(this.getBaseContext(), WRITE_PERMISSION[0], TAG)) {
                Log.i(TAG, "Permission denied, asking for permission");
                MainActivity.requestPermissions(this, WRITE_PERMISSION, TAG);
            }
            else {
                Log.i(TAG, "Permission " + WRITE_PERMISSION[0] +
                        " granted, saving filtered image to storage");

                //Save image to storage
                if(saveImage(mFilteredImageBitmap)!=null) {
                    Toast.makeText(FilterImage.this,
                            getString(R.string.save_success), Toast.LENGTH_LONG).show();
                }
                else {
                    Log.w(TAG, "Error saving image");
                    Toast.makeText(FilterImage.this,
                            getString(R.string.save_error), Toast.LENGTH_LONG).show();
                }
            }
        });

        mBack.setOnClickListener(e -> {
            Log.i(TAG, "Going one step back from FilterImage Activity");
            finish();
        });
    }

    /**
     * Sets up Views by unique ID
     */
    private void setupViews() {
        Log.i(TAG, "Setting up Views to interact with elements");
        mInvertFilter = findViewById(R.id.invert_button);
        mGrayscaleFilter = findViewById(R.id.grayscale_button);
        mBlurFilter = findViewById(R.id.blur_button);
        mSharpFilter = findViewById(R.id.sharp_button);
        mGbrFilter = findViewById(R.id.change_gbr_button);
        mBrgFilter = findViewById(R.id.change_brg_button);
        mClear = findViewById(R.id.filter_clear_button);
        mSave = findViewById(R.id.filter_save_button);
        mBack = findViewById(R.id.filter_back_button);

        mSourceImageView = findViewById(R.id.filter_source);
        setFilteredImageView();
    }

    /**
     * Helper method that updates ImageView with currently filtered image
     */
    private void setFilteredImageView() {
        mSourceImageView.setImageBitmap(mFilteredImageBitmap);
    }

    /**
     * Loads image from given URI
     * @param imageSource URI of image to be loaded
     * @return Bitmap of loaded image
     */
    private Bitmap loadImage(Uri imageSource) {
        Log.i(TAG, "Loading image from gallery");
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = getContentResolver().query(imageSource,
                filePathColumn,null,
                null, null);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        return BitmapFactory.decodeFile(picturePath);
    }

    /**
     * Inverts image
     * @param src Bitmap of image to invert
     * @return Bitmap of inverted image
     */
    public Bitmap invertFilter(Bitmap src) {
        Log.i(TAG, "Inverting image");

        //Get width and height of original bitmap
        int height = src.getHeight(), width = src.getWidth();

        //Create new result bitmap with same parameters as original
        Bitmap resultBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);

        //Draw inverted image
        ColorMatrix cm = new ColorMatrix(new float[] {
                -1,  0,  0,  0, 255,
                0,  -1,  0,  0, 255,
                0,   0, -1,  0, 255,
                0,   0,  0,  1,   0 });

        ColorFilter cf = new ColorMatrixColorFilter(cm);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(cf);
        canvas.drawBitmap(src, 0, 0, paint);

        return resultBitmap;
    }

    /**
     * Applies grayscale filter to image
     * @param src Bitmap of image to filter
     * @return Bitmap of filtered image
     */
    public Bitmap grayscaleFilter(Bitmap src) {
        Log.i(TAG, "Applying grayscale filter");

        //Get width and height of original bitmap
        int height = src.getHeight(), width = src.getWidth();

        //Create new result bitmap with same parameters as original
        Bitmap resultBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);

        //Draw image in gray paint
        Canvas c = new Canvas(resultBitmap);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(src, 0, 0, paint);

        return resultBitmap;
    }

    /**
     * Applies Gaussian blur filter to image
     * @param src Bitmap of image to filter
     * @param blurRadius Radius of blur (must be [0, 25])
     * @return Bitmap of filtered image
     */
    public Bitmap blurFilter(Bitmap src,
                             @FloatRange(from = 0.0f, to = 25.0f) float blurRadius) {
        Log.i(TAG, "Applying Gaussian blur filter");

        //Get width and height of original bitmap
        int height = src.getHeight(), width = src.getWidth();

        //Create new result bitmap with same parameters as original
        Bitmap resultBitmap = Bitmap.createBitmap(width, height,
            Bitmap.Config.ARGB_8888);

        //Apply blur filter using RenderScript
        RenderScript renderScriptBlur = RenderScript.create(getBaseContext());
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(renderScriptBlur,
            Element.U8_4(renderScriptBlur));
        Allocation inAlloc = Allocation.createFromBitmap(renderScriptBlur, src);
        Allocation outAlloc = Allocation.createFromBitmap(renderScriptBlur,
            resultBitmap);
        script.setRadius(blurRadius);
        script.setInput(inAlloc);
        script.forEach(outAlloc);
        outAlloc.copyTo(resultBitmap);
        renderScriptBlur.destroy();

        return resultBitmap;
    }

    /**
     * Applies sharpening filter to image
     * @param src Bitmap of image to filter
     * @param sharpRadius Radius of sharpening
     * @return Bitmap of filtered image
     */
    public Bitmap sharpFilter(Bitmap src, float sharpRadius) {
        Log.i(TAG, "Applying sharpening filter");

        //Get width and height of original bitmap
        int height = src.getHeight(), width = src.getWidth();

        //Create new result bitmap with same parameters as original
        Bitmap resultBitmap = Bitmap.createBitmap(width, height,
            Bitmap.Config.ARGB_8888);

        //Apply sharp filter using RenderScript
        RenderScript renderScriptSharp = RenderScript.create(getBaseContext());
        ScriptIntrinsicConvolve3x3 script = ScriptIntrinsicConvolve3x3.create(
            renderScriptSharp, Element.U8_4(renderScriptSharp));
        Allocation inAlloc = Allocation.createFromBitmap(renderScriptSharp, src);
        Allocation outAlloc = Allocation.createFromBitmap(renderScriptSharp,
            resultBitmap);
        script.setInput(inAlloc);
        float[] sharpCoefficients ={ 0, -sharpRadius, 0, -sharpRadius,
            5f*sharpRadius, -sharpRadius, 0, -sharpRadius, 0};
        script.setCoefficients(sharpCoefficients);
        script.forEach(outAlloc);
        outAlloc.copyTo(resultBitmap);
        renderScriptSharp.destroy();

        return resultBitmap;
    }

    /**
     * Applies GBR filter (switch RGB colors in such way: R->G, G->B, B->R)
     * @param src Bitmap of image to filter
     * @return Bitmap of filtered image
     */
    public Bitmap rgbToGbrFilter(Bitmap src) {
        Log.i(TAG, "Applying GBR filter");

        //Get width and height of original bitmap
        int height = src.getHeight(), width = src.getWidth();

        //Create new result bitmap with same parameters as original
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, src.getConfig());

        //Draw image with switched colors: R->G, G->B, B->R
        ColorMatrix cm = new ColorMatrix(new float[] {
                0, 0, 1, 0, 0,
                1, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 0, 1, 0 });

        ColorFilter cf = new ColorMatrixColorFilter(cm);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(cf);
        canvas.drawBitmap(src, 0, 0, paint);

        return resultBitmap;
    }

    /**
     * Applies BRG filter (switch RGB colors in such way: R->B, G->R, B->G)
     * @param src Bitmap of image to filter
     * @return Bitmap of filtered image
     */
    public Bitmap rgbToBrgFilter(Bitmap src) {
        Log.i(TAG, "Applying BRG filter");

        //Get width and height of original bitmap
        int height = src.getHeight(), width = src.getWidth();

        //Create new result bitmap with same parameters as original
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, src.getConfig());

        //Draw image with switched colors: R->B, G->R, B->G
        ColorMatrix cm = new ColorMatrix(new float[] {
                0, 1, 0, 0, 0,
                0, 0, 1, 0, 0,
                1, 0, 0, 0, 0,
                0, 0, 0, 1, 0 });

        ColorFilter cf = new ColorMatrixColorFilter(cm);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(cf);
        canvas.drawBitmap(src, 0, 0, paint);

        return resultBitmap;
    }

    /**
     * Clears all filters from image and resets it back to original
     */
    private void clearFilters() {
        Log.i(TAG, "Clearing all filters from image");
        mFilteredImageBitmap=mSourceImageBitmap;
        setFilteredImageView();
    }

    /**
     * Saves given image to local storage
     * @param toSave Bitmap of image to be saved
     * @return Uri of saved image
     */
    private Uri saveImage(Bitmap toSave) {
        Log.i(TAG, "Saving image to storage");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        toSave.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(this.getContentResolver(),
                toSave, ((Long)System.currentTimeMillis()).toString(), null);
        return Uri.parse(path);
    }

}