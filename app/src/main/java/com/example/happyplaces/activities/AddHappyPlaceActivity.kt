package com.example.happyplaces.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.example.happyplaces.BuildConfig
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.models.HappyPlaceModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var toolbarAddPlace: Toolbar
    private var cal = Calendar.getInstance()
    private lateinit var dataSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var etDate: EditText
    private lateinit var tvAddImage: TextView
    private lateinit var btnSave: Button

    private lateinit var ivPlaceImage: ImageView

    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText

    private var mHappyPlacesDetails: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)

        ivPlaceImage = findViewById(R.id.iv_place_image)
        btnSave = findViewById(R.id.btn_save)
        tvAddImage = findViewById(R.id.tv_add_image)
        etDate = findViewById(R.id.et_date)
        etTitle = findViewById(R.id.et_title)
        etDescription = findViewById(R.id.et_description)
        etLocation = findViewById(R.id.et_location)

        toolbarAddPlace = findViewById(R.id.toolbar_add_place)
        setSupportActionBar(toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbarAddPlace.setNavigationOnClickListener {
            onBackPressed()
        }

        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlacesDetails =
                intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        dataSetListener = DatePickerDialog.OnDateSetListener {
                view, year, month, dayOfMonth ->

            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        updateDateInView()

        if(mHappyPlacesDetails != null){
            supportActionBar?.title = "Edit Happy Place"
            etTitle.setText(mHappyPlacesDetails!!.title)
            etDescription.setText(mHappyPlacesDetails!!.description)
            etDate.setText(mHappyPlacesDetails!!.date)
            etLocation.setText(mHappyPlacesDetails!!.location)
            mLatitude = mHappyPlacesDetails!!.latitude
            mLongitude = mHappyPlacesDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(
                mHappyPlacesDetails!!.image
            )
            ivPlaceImage.setImageURI(saveImageToInternalStorage)

            btnSave.text = "UPDATE"
        }

        etDate.setOnClickListener(this)
        tvAddImage.setOnClickListener(this)
        btnSave.setOnClickListener(this)
//        etLocation.setOnClickListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.et_date ->{
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dataSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image ->{
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from Gallery",
                "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){
                    dialog , which ->
                    when (which) {
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save ->{
                when{
                    etTitle.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }
                    etDescription.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
                    }
                    etLocation.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null ->{
                        Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val happyPlaceModel = HappyPlaceModel(
                            if(mHappyPlacesDetails == null) 0 else mHappyPlacesDetails!!.id,
                            etTitle.text.toString(),
                            saveImageToInternalStorage.toString(),
                            etDescription.text.toString(),
                            etDate.text.toString(),
                            etLocation.text.toString(),
                            mLatitude,
                            mLongitude
                        )

                        val dbHandler = DatabaseHandler(this)

                        if(mHappyPlacesDetails == null){
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)

                            if(addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }else{
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)

                            if(updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }

                    }
                }
            }
//            R.id.et_location ->{
//                val fields = listOf(Place.Field.ID, Place.Field.NAME)
//                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
//                    .build(this)
//                startActivityForResult(intent, FROM_REQUEST_CODE)

                // Initialize the AutocompleteSupportFragment.
//                val autocompleteFragment =
//                    supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
//                            as AutocompleteSupportFragment
//                // Specify the types of place data to return.
//                autocompleteFragment.setPlaceFields(
//                    listOf(Place.Field.ID, Place.Field.NAME,
//                        Place.Field.LAT_LNG,
//                        Place.Field.ADDRESS
//                    )
//                )
//
//                // Set up a PlaceSelectionListener to handle the response.
//                autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
//                    override fun onPlaceSelected(place: Place) {
//                        etLocation.setText(place.address)
//                        mLatitude = place.latLng!!.latitude
//                        mLongitude = place.latLng!!.longitude
//                    }
//
//                    override fun onError(status: Status) {
//                        Log.i("DEBUG:-->", "An error occurred: $status")
//                    }
//                })
//            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private var galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try{
                val intent = result.data
                val photoUri: Uri = intent!!.data as Uri
                ivPlaceImage.setImageURI(photoUri)

                saveImageFromUriGallery(photoUri)

                saveImageToInternalStorage = Uri.parse(file.absolutePath);
            }catch (e: IOException){
                e.printStackTrace()
                Toast.makeText(this@AddHappyPlaceActivity, "Failed to load image from Gallery!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)
    private fun saveImageFromUriGallery(uri: Uri) {
        val source: ImageDecoder.Source =
            ImageDecoder.createSource(this.contentResolver, uri)
        val bitmap: Bitmap = ImageDecoder.decodeBitmap(source)

        createPhotoFile()
        var outputStream: OutputStream = FileOutputStream(file)

        outputStream.use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
    }
    private fun choosePhotoFromGallery() {
        Dexter.withContext(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ).withListener(object : MultiplePermissionsListener{
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onPermissionsChecked(report: MultiplePermissionsReport?){
                if(report!!.areAllPermissionsGranted()) {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    galleryActivityResultLauncher.launch(intent)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                permissionToken: PermissionToken?
            ) {
                showRationalDialogForPermission()
            }
        }).onSameThread().check()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private val openCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == RESULT_OK){
            val bitmap = getBitmap()
            var rotatedBitmap: Bitmap = needsRotating(bitmap)
            ivPlaceImage.setImageBitmap(rotatedBitmap)

            val content = createContent()
            val uri = saveFromUriCamera(content)

            clearContents(content, uri)
            Toast.makeText(this, "Imagen guardada en la galería", Toast.LENGTH_SHORT).show()

            saveImageToInternalStorage = Uri.parse(file.absolutePath);
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun takePhotoFromCamera() {
        Dexter.withContext(this).withPermission(
            Manifest.permission.CAMERA,
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(permission: PermissionGrantedResponse?) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    // packageManager -> nosotros mismo gestionamos
                    resolveActivity(packageManager).also {
                        createPhotoFile()
                        val photoUri: Uri = FileProvider.getUriForFile(
                            this@AddHappyPlaceActivity,
                            BuildConfig.APPLICATION_ID + ".fileprovider", file
                        )
                        putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    }
                }
                openCamera.launch(intent)
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                showRationalDialogForPermission()
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: com.karumi.dexter.listener.PermissionRequest?,
                p1: PermissionToken?
            ) {
                showRationalDialogForPermission()
            }
        }).onSameThread().check()
    }

    private lateinit var file: File
    private fun createPhotoFile() {
        // Dirección privada de la app
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        file = File.createTempFile("IMG${System.currentTimeMillis()}_", ".jpg", dir)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createContent(): ContentValues {
        val fileName = file.name
        val fileType = "image/jpg"
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, fileType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, 1) // Estado (no guardado)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFromUriCamera(content: ContentValues): Uri {
        var outputStream: OutputStream?
        var uri: Uri?
        contentResolver.also { resolver ->
            // Guardamos
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content)
            //
            outputStream = resolver.openOutputStream(uri!!)
        }
        outputStream.use { output ->
            var rotatedBitmap: Bitmap = needsRotating(getBitmap())
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }

        return uri!!
    }
    private fun clearContents(content: ContentValues, uri: Uri) {
        content.clear()
        content.put(MediaStore.MediaColumns.IS_PENDING, 0)
        // Borramos
        contentResolver.update(uri, content, null,null)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun needsRotating(bitmap: Bitmap): Bitmap {
        val ei = ExifInterface(file)
        val orientation: Int = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        lateinit var rotatedBitmap: Bitmap
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> rotatedBitmap = bitmap
            else -> rotatedBitmap = bitmap
        }

        return rotatedBitmap
    }
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }
    private fun getBitmap(): Bitmap{
        return BitmapFactory.decodeFile(file.toString())
    }

    private fun showRationalDialogForPermission() {
        AlertDialog.Builder(this@AddHappyPlaceActivity).setMessage("" +
            "It looks like you have turned off permission required" +
            "for this feature. It can be enabled under the" +
            "Applications Settings")
            .setPositiveButton("GO TO SETTINGS"){
                _, _ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){
                    dialog, which ->
                dialog.dismiss()
            }.show()
    }

    private fun updateDateInView(){
        val myFormat = "dd.MM.y"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        etDate.setText(sdf.format(cal.time).toString())
    }

    companion object{
        private val  FROM_REQUEST_CODE = 1
    }
}