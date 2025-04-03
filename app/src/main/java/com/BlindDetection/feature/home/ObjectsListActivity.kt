package com.BlindDetection.feature.home

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.BlindDetection.R

class ObjectsListActivity : AppCompatActivity() {

    // List of all detectable objects
    private val detectableObjects = listOf(
        "person", "bicycle", "car", "motorcycle", "bus", "bench", "cat", "dog", 
        "backpack", "umbrella", "handbag", "tie", "suitcase", "bottle", "wine glass", 
        "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange", 
        "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", 
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", 
        "keyboard", "cell phone", "microwave", "oven", "toaster", "refrigerator", "book", 
        "clock", "vase"
    )

    // Map of objects to their drawable resources (you'll need to add these drawables)
    private val objectDrawables = mapOf(
        "person" to R.drawable.img_3,
        "bicycle" to R.drawable.img_1,
        "car" to R.drawable.img_2,
        "motorcycle" to R.drawable.img_4,
        "bus" to R.drawable.img,
        "bench" to R.drawable.img_5,
        "cat" to R.drawable.img_6,
        "dog" to R.drawable.img_7,
        "backpack" to R.drawable.img_8,
        "umbrella" to R.drawable.img_9,
        "tie" to R.drawable.img_10,
        "suitcase" to R.drawable.img_11,
        "bottle" to R.drawable.img_12,
        "wine" to R.drawable.img_13,
        "glass" to R.drawable.img_14,
        "cup" to R.drawable.img_15,
        "fork" to R.drawable.img_16,
        "knife" to R.drawable.img_17,
        "spoon" to R.drawable.img_18,
        "bowl" to R.drawable.img_19,
        "banana" to R.drawable.img_20,
        "apple" to R.drawable.img_21,
        "sandwich" to R.drawable.img_22,
        "orange" to R.drawable.img_23,
        "potted plant" to R.drawable.img_24,
        "bed" to R.drawable.img_25,
        "dining table" to R.drawable.img_26,
        "toilet" to R.drawable.img_27,
        "tv" to R.drawable.img_28,
        "laptop" to R.drawable.img_29,
        "mouse" to R.drawable.img_30,
        "remote" to R.drawable.img_31,
        "keyboard" to R.drawable.img_32,
        "cell phone" to R.drawable.img_33,
        "microwave" to R.drawable.img_34,
        "oven" to R.drawable.img_35,
        "toaster" to R.drawable.img_36,
        "refrigerator" to R.drawable.img_37,
        "book" to R.drawable.img_38,
        "clock" to R.drawable.img_39,
        "vase" to R.drawable.img_40,
        "broccoli" to R.drawable.img_41,
        "carrot" to R.drawable.img_42,
        "hot dog" to R.drawable.img_43,
        "pizza" to R.drawable.img_44,
        "donut" to R.drawable.img_45,
        "cake" to R.drawable.img_46,
        "chair" to R.drawable.img_47,
        "couch" to R.drawable.img_48,
        "wine glass" to R.drawable.img_49,
        "handbag" to R.drawable.img_50,


        // Add more mappings as needed, or use placeholder icons
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_objects_list)

        // Set up action bar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detectable Objects"

        // Get the grid layout
        val gridLayout = findViewById<GridLayout>(R.id.objectsGrid)
        
        // Set column count based on screen width
        gridLayout.columnCount = 3

        // Add cards for each object
        detectableObjects.forEachIndexed { index, objectName ->
            val card = createObjectCard(objectName)
            
            // Add card to grid
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(index % 3, 1f)
            params.rowSpec = GridLayout.spec(index / 3)
            params.setMargins(8, 8, 8, 8)
            
            card.layoutParams = params
            gridLayout.addView(card)
        }
    }

    private fun createObjectCard(objectName: String): CardView {
        // Create card view
        val card = CardView(this)
        card.radius = 16f
        card.cardElevation = 4f
        card.useCompatPadding = true

        // Create inner layout
        val innerLayout = GridLayout(this)
        innerLayout.columnCount = 1
        innerLayout.orientation = GridLayout.VERTICAL
        innerLayout.setPadding(16, 16, 16, 16)

        // Add image view
        val imageView = ImageView(this)
        imageView.layoutParams = GridLayout.LayoutParams().apply {
            width = GridLayout.LayoutParams.MATCH_PARENT
            height = 250  // Increased height for a larger image
            setMargins(0, 0, 0, 8)
        }

        // Set image resource
        val drawableResource = objectDrawables[objectName] ?: android.R.drawable.ic_menu_gallery
        imageView.setImageResource(drawableResource)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        // Add text view
        val textView = TextView(this)
        textView.text = objectName
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        textView.textSize = 14f  // Increased text size
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.black))

        // Add views to inner layout
        innerLayout.addView(imageView)
        innerLayout.addView(textView)

        // Add inner layout to card
        card.addView(innerLayout)

        // Increase card size by changing the LayoutParams
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
        params.setMargins(16, 16, 16, 16)
        card.layoutParams = params

        return card
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}