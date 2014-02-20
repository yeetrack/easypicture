package com.yeetrack.android.easypicture;

/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.yeetrack.android.easypicture.tool.Constant;
import com.yeetrack.android.easypicture.tool.ImageTool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class ImageFolderActivity extends AbsListViewBaseActivity {

    DisplayImageOptions options;

    String[] imageUrls;
    private String uuid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ac_image_list);

        uuid = getIntent().getExtras().getString("uuid");


        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_stub)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.ic_error)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .considerExifParams(true)
                .displayer(new RoundedBitmapDisplayer(20))
                .build();

        listView = (ListView) findViewById(android.R.id.list);

        ((ListView) listView).setAdapter(new ItemAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startImagePagerActivity(position);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.item_rescan)
        {
            ImageTool.getAllImageFolderPath(true, 5);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AnimateFirstDisplayListener.displayedImages.clear();
        super.onBackPressed();
    }

    private void startImagePagerActivity(int position) {
        //扫描文件夹中的图片
        List<String> imageNames = new ArrayList<String>();
        File dir = new File(ImageTool.getAllImageFolderPath(false, 5).get(position));
        File[] files = dir.listFiles();
        for(File index : files)
        {
            if(ImageTool.isImage(index.getAbsolutePath()))
            {
                imageNames.add("file://"+index.getAbsolutePath());
            }
        }
        Intent intent = new Intent(this, ImageGridActivity.class);
        String[] names = imageNames.toArray(new String[imageNames.size()]);
        intent.putExtra(Constant.Extra.IMAGES, names);
        intent.putExtra(Constant.Extra.IMAGE_POSITION, 0);
        intent.putExtra("uuid", uuid);
        startActivity(intent);
    }

    class ItemAdapter extends BaseAdapter
    {

        private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

        private class ViewHolder {
            public TextView text;
            public ImageView image;
        }

        @Override
        public int getCount() {
            return ImageTool.getAllImageFolderPath(false, 5).size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final ViewHolder holder;
            if (convertView == null) {
                view = getLayoutInflater().inflate(R.layout.item_list_image, parent, false);
                holder = new ViewHolder();
                holder.text = (TextView) view.findViewById(R.id.text);
                holder.image = (ImageView) view.findViewById(R.id.image);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.text.setText(ImageTool.getAllImageFolderPath(false, 5).get(position));

            UILApplication.initImageLoader(ImageFolderActivity.this);
            String filePath = ImageTool.getFolderImageName().get(position);
            Log.d("debug", filePath);
            imageLoader.displayImage(filePath, holder.image, options, animateFirstListener);

            return view;
        }
    }

    private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener
    {

        static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<String>());

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }
}