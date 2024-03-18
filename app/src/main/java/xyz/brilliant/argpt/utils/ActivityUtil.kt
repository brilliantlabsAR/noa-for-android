package xyz.brilliant.argpt.utils

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import xyz.brilliant.argpt.R

object ActivityUtil {

    /**
     * Utility function to navigate from one activity to another activity.
     *
     * @param context The context of the current activity.
     * @param targetActivityClass The class of the target activity to navigate to.
     */
    fun navigateToActivity(context: Context, targetActivityClass: Class<*>) {
        val intent = Intent(context, targetActivityClass)
        context.startActivity(intent)
    }
    /**
     * Utility function to navigate from one fragment to another fragment.
     *
     * @param fragmentManager The fragment manager of the current activity.
     * @param fragment The class of the target fragment to navigate to.
     * @param shouldAddToBackStack This params should add backstack or not.
     * @param tag This params tag of target fragment.
     */
    fun navigateToFragment(
        fragmentManager: FragmentManager,
        fragment: Fragment,
        shouldAddToBackStack: Boolean,
        tag: String?
    ) {
        val ft: FragmentTransaction = fragmentManager.beginTransaction()
        //ft.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
        ft.replace(R.id.fragmentContainer, fragment, tag)
        if (shouldAddToBackStack) {
            ft.addToBackStack("ScreenStack")
        } else {
            fragmentManager.popBackStack(null, 0)
        }
        ft.commit()
    }
}