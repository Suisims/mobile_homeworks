package com.example.hw4

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.Math.abs
import java.net.HttpURLConnection
import java.net.URL


class WallActivity : AppCompatActivity() {

    private var token: String? = null
    private var postsView: RecyclerView? = null
    private lateinit var postList: List<Post>
    private lateinit var postAdapter: PostAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wll)

        token = intent.getStringExtra("token")
        Log.e("token", token.toString())
        getPosts().execute()

        postsView = findViewById(R.id.posts)
        postsView?.layoutManager =
            StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)

        postList = ArrayList()
        postAdapter = PostAdapter(postList, applicationContext)
        postsView?.adapter = postAdapter
    }

    inner class getPosts() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: String?): String? {
            var response: String?
            try {
                response =
                    URL("https://api.vk.com/method/newsfeed.get?filters=post&count=15&access_token=$token&v=5.103")
                        .readText(Charsets.UTF_8)

            } catch (e: Exception) {
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val posts = parse(result)
            (postList as MutableList<Post>).addAll(posts)
            postAdapter.notifyDataSetChanged()
            Log.e("posts", postList.toString())
        }

        @Throws(JSONException::class)
        fun parse(r: String?): ArrayList<Post> {
            val jsonObj = JSONObject(r)
            val response = jsonObj.getJSONObject("response")
            val posts: ArrayList<Post> = ArrayList()
            val items = response.getJSONArray("items")
            val groups = response.getJSONArray("groups")
            Log.e("groups", groups.toString())
            var imgUrl: String? = "none"
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val source_id = item.getInt("source_id")
                var gName: String? = null
                var gPhoto: String? = null
                for (j in 0 until groups.length()) {
                    val group = groups.getJSONObject(j)
                    if (abs(source_id) == group.getInt("id")) {
                        gName = group.getString("name")
                        gPhoto = group.getString("photo_50")
                    }
                }
                Log.e("img", imgUrl.toString())
                val date = item.getInt("date")
                val comments = item.getJSONObject("comments")
                val commentsCount = comments.getInt("count")
                val likes = item.getJSONObject("likes")
                val likesCount = likes.getInt("count")
                val views = item.getJSONObject("views")
                val viewsCount = views.getInt("count")
                val text = item.getString("text")
                if (item.has("attachments")) {
                    Log.e("img", item.getJSONArray("attachments").getJSONObject(0).getString("type"))
                    if (item.getJSONArray("attachments").getJSONObject(0).getString("type") == "photo") {
                        imgUrl = item.getJSONArray("attachments")
                            .getJSONObject(0)
                            .getJSONObject("photo")
                            .getJSONArray("sizes")
                            .getJSONObject(2)
                            .getString("url")
                    } else {
                        imgUrl = "none"
                    }
                } else {
                    imgUrl = "none"
                }
                val post =
                    Post(
                        gName,
                        gPhoto,
                        source_id,
                        date,
                        commentsCount,
                        likesCount,
                        viewsCount,
                        text,
                        imgUrl
                    )
                posts.add(post)
            }
            return posts
        }
    }

    class PostAdapter(private var posts: List<Post>, applicationContext: Context) :
        RecyclerView.Adapter<PostAdapter.PostViewHolder>() {


        class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var gLogo: ImageView = itemView.findViewById(R.id.logo)
            var gName: TextView = itemView.findViewById(R.id.group_name)
            var text: TextView = itemView.findViewById(R.id.description)
            var image: ImageView = itemView.findViewById(R.id.post_image)
            var likes: TextView = itemView.findViewById(R.id.likesCount)
            var comments: TextView = itemView.findViewById(R.id.commentsCount)
            var views: TextView = itemView.findViewById(R.id.viewsCount)
            var layoutPost: RelativeLayout? = itemView.findViewById(R.id.layout_post)

            fun setPost(post: Post) {
                DownloadImageFromInternet(gLogo).execute(post.getGPhoto())
                DownloadImageFromInternet(image).execute(post.getImg())
                if (post.getImg() == "none") {
                    image.visibility = View.GONE
                }
                gName.text = post.getGName()
                text.text = post.getPostText()
                likes.text = post.getLikes().toString()
                comments.text = post.getComments().toString()
                views.text = post.getViews().toString()
            }

            @SuppressLint("StaticFieldLeak")
            @Suppress("DEPRECATION")
            private inner class DownloadImageFromInternet(var imageView: ImageView) : AsyncTask<String, Void, Bitmap?>() {
                override fun doInBackground(vararg params: String?): Bitmap? {
                    val imageURL = params[0]
                    var image: Bitmap? = null
                    try {
                        val `in` = java.net.URL(imageURL).openStream()
                        image = BitmapFactory.decodeStream(`in`)
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return image
                }
                override fun onPostExecute(result: Bitmap?) {
                    imageView.setImageBitmap(result)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            return PostViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.post, parent, false)
            )
        }

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            holder.setPost(posts[position])
        }

        override fun getItemCount(): Int {
            return posts.size
        }
    }


    class Post constructor(
        private val gName: String?,
        private val gPhoto: String?,
        private val source_id: Int,
        private val date: Int,
        private val commentsCount: Int,
        private val likesCount: Int,
        private val viewsCount: Int,
        private val text: String,
        val imgUrl: String?
    ) {

        fun getGName(): String? {
            return gName
        }
        fun getGPhoto(): String? {
            return gPhoto
        }
        fun getSource(): Int {
            return source_id
        }
        fun getPostDate(): Int {
            return date
        }
        fun getComments(): Int {
            return commentsCount
        }
        fun getLikes(): Int {
            return likesCount
        }
        fun getViews(): Int {
            return viewsCount
        }
        fun getPostText(): String {
            return text
        }
        fun getImg(): String? {
            return imgUrl
        }
    }
}