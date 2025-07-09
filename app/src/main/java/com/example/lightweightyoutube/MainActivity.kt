package com.example.lightweightyoutube

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.lightweightyoutube.databinding.ActivityMainBinding
import com.example.lightweightyoutube.ui.explore.ExploreFragment
import com.example.lightweightyoutube.ui.saved.SavedFragment
import com.google.android.material.navigation.NavigationBarView

/**
 * The main activity of the application.
 * Hosts the [ExploreFragment] and [SavedFragment] using a [BottomNavigationView].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // ViewBinding instance for activity_main.xml

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the initial fragment if the activity is newly created
        if (savedInstanceState == null) {
            navigateToFragment(ExploreFragment())
        }

        // Handle bottom navigation item selections
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_explore -> ExploreFragment()
                R.id.nav_saved -> SavedFragment()
                // TODO: Add R.id.nav_settings for a SettingsFragment when implemented
                else -> ExploreFragment() // Default to ExploreFragment
            }
            navigateToFragment(selectedFragment)
            true // Return true to indicate the item selection was handled
        }
    }

    /**
     * Replaces the content of the fragment container with the given [Fragment].
     * @param fragment The fragment to display.
     */
    private fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Programmatically selects an item in the bottom navigation view.
     * Can be used to navigate to a specific tab from other parts of the app if needed.
     *
     * @param itemId The menu item ID of the tab to select (e.g., R.id.nav_explore).
     */
    fun selectBottomNavItem(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }
}
