package com.factor.bouncy

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.factor.bouncy.util.*


class BouncyRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs)
{

    private lateinit var callBack: DragDropCallBack

    var onOverPullListener: OnOverPullListener? = null

    var overscrollAnimationSize = 0.5f

    var flingAnimationSize = 0.5f

    var viewToAnimate : View = this

    var orientation : Int? = 1
        set(value)
        {
            field = value
            setupDirection(value)

        }

    var touched: Boolean = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean
    {

        touched = when (e?.actionMasked)
        {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> false
            else -> true
        }
        return super.onTouchEvent(e)
    }



    @Suppress("MemberVisibilityCanBePrivate")
    var dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        set(value)
        {
            field = value
            this.spring.spring = SpringForce()
                .setFinalPosition(0f)
                .setDampingRatio(value)
                .setStiffness(stiffness)
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var stiffness = SpringForce.STIFFNESS_LOW
        set(value)
        {
            field = value
            this.spring.spring = SpringForce()
                .setFinalPosition(0f)
                .setDampingRatio(dampingRatio)
                .setStiffness(value)
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var longPressDragEnabled = false
        set(value)
        {
            field = value
            if (adapter is DragDropAdapter<*>) callBack.setDragEnabled(value)
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var itemSwipeEnabled = false
        set(value)
        {
            field = value
            if (adapter is DragDropAdapter<*>) callBack.setSwipeEnabled(value)
        }

    var spring: SpringAnimation = SpringAnimation(viewToAnimate, SpringAnimation.TRANSLATION_Y)
        .setSpring(
            SpringForce()
                .setFinalPosition(0f)
                .setDampingRatio(dampingRatio)
                .setStiffness(stiffness)
        )

    //**********************************************************************
    //set translationY
    fun springTranslationY(distance : Float)
    {
        viewToAnimate.translationY = distance * overscrollAnimationSize
        spring.cancel()

        if (connectedView != null)
        {
            connectedView!!.translationY = distance * overscrollAnimationSize
            connectedSpringTop?.cancel()
        }

    }

    fun springTranslateTo(distance: Float)
    {
        spring.cancel()
        viewToAnimate.animate().translationY(distance)

        if (connectedView != null)
        {
            connectedSpringTop?.cancel()
            connectedView!!.animate().translationY(distance)
        }
    }

    //manually release spring
    fun release()
    {
        spring.start()
        connectedSpringTop?.start()
        connectedSpringBottom?.start()
    }

    var connectedSpringBottom : SpringAnimation? = null

    var connectedSpringTop : SpringAnimation? = null

    var connectedView : View? = null



    override fun setAdapter(adapter: RecyclerView.Adapter<*>?)
    {
        super.setAdapter(adapter)
        if (adapter is DragDropAdapter<*>)
        {
            callBack = DragDropCallBack(adapter, longPressDragEnabled, itemSwipeEnabled)
            val touchHelper = ItemTouchHelper(callBack)
            touchHelper.attachToRecyclerView(this)
        }
    }

    inline fun <reified T : ViewHolder> RecyclerView.forEachVisibleHolder(action: (T) -> Unit)
    {
        for (i in 0 until childCount) action(getChildViewHolder(getChildAt(i)) as T)
    }


    override fun setLayoutManager(layout: LayoutManager?)
    {
        super.setLayoutManager(layout)
        if (layout is LinearLayoutManager)
        {
            orientation = layout.orientation
            setupDirection(orientation)
        }
    }

    private fun setupDirection(orientation : Int?)
    {

        if (stiffness > 0F)
        {
            when (orientation)
            {
                HORIZONTAL -> spring = SpringAnimation(viewToAnimate, SpringAnimation.TRANSLATION_X)
                    .setSpring(SpringForce()
                        .setFinalPosition(0f)
                        .setDampingRatio(dampingRatio)
                        .setStiffness(stiffness))

                VERTICAL -> spring = SpringAnimation(viewToAnimate, SpringAnimation.TRANSLATION_Y)
                    .setSpring(SpringForce()
                        .setFinalPosition(0f)
                        .setDampingRatio(dampingRatio)
                        .setStiffness(stiffness))

            }
        }
    }

    init {

        //read attributes
        context.theme.obtainStyledAttributes(attrs, R.styleable.BouncyRecyclerView, 0, 0)
            .apply{
                longPressDragEnabled = getBoolean(R.styleable.BouncyRecyclerView_allow_drag_reorder, false)
                itemSwipeEnabled = getBoolean(R.styleable.BouncyRecyclerView_allow_item_swipe, false)

                overscrollAnimationSize = getFloat(R.styleable.BouncyRecyclerView_recyclerview_overscroll_animation_size, 0.5f)
                flingAnimationSize = getFloat(R.styleable.BouncyRecyclerView_recyclerview_fling_animation_size, 0.5f)

                when (getInt(R.styleable.BouncyRecyclerView_recyclerview_damping_ratio, 0))
                {
                    0 -> dampingRatio = Bouncy.DAMPING_RATIO_NO_BOUNCY
                    1 -> dampingRatio = Bouncy.DAMPING_RATIO_LOW_BOUNCY
                    2 -> dampingRatio = Bouncy.DAMPING_RATIO_MEDIUM_BOUNCY
                    3 -> dampingRatio = Bouncy.DAMPING_RATIO_HIGH_BOUNCY
                }
                when (getInt(R.styleable.BouncyRecyclerView_recyclerview_stiffness, 1))
                {
                    0 -> stiffness = Bouncy.STIFFNESS_VERY_LOW
                    1 -> stiffness = Bouncy.STIFFNESS_LOW
                    2 -> stiffness = Bouncy.STIFFNESS_MEDIUM
                    3 -> stiffness = Bouncy.STIFFNESS_HIGH
                }
                recycle()
            }


        val rc = viewToAnimate


        //create edge effect
        this.edgeEffectFactory = object : EdgeEffectFactory()
        {
            override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int): EdgeEffect
            {
                return object : EdgeEffect(recyclerView.context)
                {
                    override fun onPull(deltaDistance: Float)
                    {
                        super.onPull(deltaDistance)
                        onPullAnimation(deltaDistance)
                    }

                    override fun onPull(deltaDistance: Float, displacement: Float)
                    {
                        super.onPull(deltaDistance, displacement)
                        onPullAnimation(deltaDistance)
                        if (direction == DIRECTION_BOTTOM)
                            onOverPullListener?.onOverPulledBottom(deltaDistance)
                        else if (direction == DIRECTION_TOP)
                            onOverPullListener?.onOverPulledTop(deltaDistance)
                    }

                    private fun onPullAnimation(deltaDistance: Float)
                    {

                        if (orientation == VERTICAL)
                        {
                            val delta: Float =
                                if (direction == DIRECTION_BOTTOM)
                                    -1 * recyclerView.width * deltaDistance * overscrollAnimationSize
                                else
                                    1 * recyclerView.width * deltaDistance * overscrollAnimationSize

                            rc.translationY += delta
                            if (connectedView != null && direction == DIRECTION_TOP)
                            {
                                connectedView!!.translationY += delta
                                connectedSpringTop?.cancel()
                            }


                            spring.cancel()
                        }
                        else
                        {
                            val delta: Float =
                                if (direction == DIRECTION_RIGHT)
                                    -1 * recyclerView.width * deltaDistance * overscrollAnimationSize
                                else
                                    1 * recyclerView.width * deltaDistance * overscrollAnimationSize

                            rc.translationX += delta
                            spring.cancel()
                        }

                        forEachVisibleHolder{holder: ViewHolder? -> if (holder is BouncyViewHolder)holder.onPulled(deltaDistance)}
                    }

                    override fun onRelease()
                    {
                        super.onRelease()

                        if (touched)
                            return
                        onOverPullListener?.onRelease()
                        spring.start()
                        connectedSpringTop?.start()

                        forEachVisibleHolder{holder: ViewHolder? -> if (holder is BouncyViewHolder)holder.onRelease()}
                    }

                    override fun onAbsorb(velocity: Int)
                    {
                        super.onAbsorb(velocity)

                        if (orientation == VERTICAL)
                        {
                            val v: Float
                            if (direction == DIRECTION_BOTTOM)
                            {
                                v = -1 * velocity * flingAnimationSize
                                connectedSpringBottom?.skipToEnd()
                                connectedSpringBottom?.setStartVelocity(-v * 0.3F)?.start()
                            }
                            else
                            {
                                v = 1 * velocity * flingAnimationSize
                                connectedSpringTop?.setStartVelocity(v)?.start()
                            }

                            spring.setStartVelocity(v).start()

                        }
                        else
                        {
                            val v: Float = if (direction == DIRECTION_RIGHT)
                                -1 * velocity * flingAnimationSize
                            else
                                1 * velocity * flingAnimationSize


                            spring.setStartVelocity(v).start()
                        }



                        forEachVisibleHolder{holder: ViewHolder? -> if (holder is BouncyViewHolder)holder.onAbsorb(velocity)}
                    }

                    override fun draw(canvas: Canvas?): Boolean
                    {
                        setSize(0, 0)
                        return super.draw(canvas)
                    }
                }
            }
        }
    }

    abstract class Adapter<T:ViewHolder>: RecyclerView.Adapter<T>(), DragDropAdapter<T>
}
