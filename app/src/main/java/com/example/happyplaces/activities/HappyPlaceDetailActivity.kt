package com.example.happyplaces.activities

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.example.happyplaces.R
import com.example.happyplaces.models.HappyPlaceModel

class HappyPlaceDetailActivity : AppCompatActivity() {
    private lateinit var toolbarHappyPlaceDetail: Toolbar
    lateinit var iv_place_image: ImageView
    lateinit var tv_description: TextView
    lateinit var tv_location: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_happy_place_detail)

        var happyPlaceDetailModel: HappyPlaceModel? = null

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            happyPlaceDetailModel =
                intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        if(happyPlaceDetailModel != null){
            toolbarHappyPlaceDetail = findViewById(R.id.toolbar_happy_place_detail)
            setSupportActionBar(toolbarHappyPlaceDetail)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = happyPlaceDetailModel.title

            toolbarHappyPlaceDetail.setNavigationOnClickListener {
                onBackPressed()
            }

            iv_place_image = findViewById(R.id.iv_place_image)
            iv_place_image.setImageURI(Uri.parse(happyPlaceDetailModel.image))
            tv_description = findViewById(R.id.tv_description)
            tv_description.text = happyPlaceDetailModel.description
            tv_location = findViewById(R.id.tv_location)
            tv_location.text = happyPlaceDetailModel.location
        }
    }
}