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
package com.yeetrack.android.easypicture;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.yeetrack.android.easypicture.tool.Constant;
import com.yeetrack.android.easypicture.tool.ImageTool;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class ImagePagerActivity extends BaseActivity {

	private static final String STATE_POSITION = "STATE_POSITION";

	DisplayImageOptions options;

	ViewPager pager;
    private String uuid;

    //监控当前图片是否上传完毕
    Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            String result = msg.getData().getString("uploadResult");
            //result = 0 未登录，跳回MainActivity
            if("0".equals(result))
            {
                Toast.makeText(ImagePagerActivity.this, "登陆已失效", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ImagePagerActivity.this, MainActivity.class);
                ImagePagerActivity.this.startActivity(intent);
            }
            else if("1".equals(result)) //上传成功
            {
                Toast.makeText(ImagePagerActivity.this, "传递成功", Toast.LENGTH_SHORT).show();
            }
            else //其他错误
            {
                Toast.makeText(ImagePagerActivity.this, "发生错误，请重试", Toast.LENGTH_SHORT).show();
            }
            setProgressBarIndeterminateVisibility(false);
        }
    };

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        //注册进度条
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.ac_image_pager);

		Bundle bundle = getIntent().getExtras();
		assert bundle != null;
		final String[] imageUrls = bundle.getStringArray(Constant.Extra.IMAGES);
		int pagerPosition = bundle.getInt(Constant.Extra.IMAGE_POSITION, 0);
        uuid = bundle.getString("uuid");

		if (savedInstanceState != null) {
			pagerPosition = savedInstanceState.getInt(STATE_POSITION);
		}

		options = new DisplayImageOptions.Builder()
			.showImageForEmptyUri(R.drawable.ic_empty)
			.showImageOnFail(R.drawable.ic_error)
			.resetViewBeforeLoading(true)
			.cacheOnDisc(true)
			.imageScaleType(ImageScaleType.EXACTLY)
			.bitmapConfig(Bitmap.Config.RGB_565)
			.considerExifParams(true)
			.displayer(new FadeInBitmapDisplayer(300))
			.build();

		pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(new ImagePagerAdapter(imageUrls));
		pager.setCurrentItem(pagerPosition);

        //监听滑动手势，实现图片上传，但是第一次进入页面时，没有滑动事件，需要手动处理
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                //上传图片到服务器
                Log.d("debug", "开启线程上传图片,position-->"+position);
                uploadImage(imageUrls, position);
                setProgressBarIndeterminateVisibility(true);
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
        //手动处理第一次进入此页面时，上传图片
        Log.d("debug", "首次进入PagerActivity上传图片，position--->"+pagerPosition);
        uploadImage(imageUrls, pagerPosition);
        setProgressBarIndeterminateVisibility(true);

	}

    /**
     * 上传图片到服务器
     * @param images 图片路径数组
     * @param position 图片位置，从0开始
     */
    private void uploadImage(String[] images, int position)
    {
        final String currentFileName = images[position];
        new Thread()
        {
            @Override
            public void run()
            {
                int result = ImageTool.image2Server(currentFileName, uuid);
                Message msg = new Message();
                msg.what=1;
                Bundle bundle = new Bundle();
                bundle.putString("uploadResult", Integer.toString(result));
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }.start();
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_POSITION, pager.getCurrentItem());
	}

	private class ImagePagerAdapter extends PagerAdapter
    {

		private String[] images;
		private LayoutInflater inflater;

		ImagePagerAdapter(String[] images) {
			this.images = images;
			inflater = getLayoutInflater();
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public int getCount() {
			return images.length;
		}

        //PagerAdapter的instantiateImte第一次默认加载两张图片，后面每次加载一张，这个比较纠结
		@Override
		public Object instantiateItem(ViewGroup view, int position)
        {
            if(position>-4)
            {

            }
			View imageLayout = inflater.inflate(R.layout.item_pager_image, view, false);
			assert imageLayout != null;
			ImageView imageView = (ImageView) imageLayout.findViewById(R.id.image);
			final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading);


			imageLoader.displayImage(images[position], imageView, options, new SimpleImageLoadingListener() {
				@Override
				public void onLoadingStarted(final String imageUri, View view) {
					spinner.setVisibility(View.VISIBLE);
				}

				@Override
				public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
					String message = null;
					switch (failReason.getType()) {
						case IO_ERROR:
							message = "Input/Output error";
							break;
						case DECODING_ERROR:
							message = "Image can't be decoded";
							break;
						case NETWORK_DENIED:
							message = "Downloads are denied";
							break;
						case OUT_OF_MEMORY:
							message = "Out Of Memory error";
							break;
						case UNKNOWN:
							message = "Unknown error";
							break;
					}
					Toast.makeText(ImagePagerActivity.this, message, Toast.LENGTH_SHORT).show();

					spinner.setVisibility(View.GONE);
				}

				@Override
				public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
					spinner.setVisibility(View.GONE);
				}
			});

			view.addView(imageLayout, 0);
			return imageLayout;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}
	}
}