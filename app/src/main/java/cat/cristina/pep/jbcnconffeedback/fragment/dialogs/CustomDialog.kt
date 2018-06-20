package cat.cristina.pep.jbcnconffeedback.fragment.dialogs

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import cat.cristina.pep.jbcnconffeedback.R
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView


class CustomDialog(context: Context, resourceImageId: Int, resourceStringId: Int = R.string.loading) : Dialog(context, R.style.TransparentProgressDialog) {

    private lateinit var imageView: ImageView
    private lateinit var textView: TextView

    init {
        val windowLayoutParams: WindowManager.LayoutParams = this.window.attributes
        windowLayoutParams.gravity = Gravity.CENTER_HORIZONTAL
        window.attributes = windowLayoutParams
        setTitle(null)
        setCancelable(true)
        setOnCancelListener(null)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        val linearLayoutLayoutParams: LinearLayout.LayoutParams
                = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        imageView = ImageView(context)
        imageView.setImageResource(resourceImageId)
        imageView.setPadding(0, 0, 0, 5)
        textView = TextView(context)
        textView.setTextColor(Color.WHITE)
        textView.setPadding(0, 5, 0, 0)
        textView.setTypeface(textView.typeface, Typeface.BOLD_ITALIC)
        textView.setText(resourceStringId)
        layout.addView(imageView, linearLayoutLayoutParams)
        layout.addView(textView, linearLayoutLayoutParams)
        addContentView(layout, linearLayoutLayoutParams)
    }

    override fun show() {
        super.show()
        val rotateAnimation = RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f)
        rotateAnimation.interpolator = LinearInterpolator(context, null)
        rotateAnimation.repeatCount = Animation.INFINITE
        rotateAnimation.duration = 3_000
        imageView.animation = rotateAnimation
        imageView.startAnimation(rotateAnimation)
    }

}