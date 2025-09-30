import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PermissionHelper {

    // Check if a permission is granted
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Check if multiple permissions are granted
    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    // Show explanation dialog before requesting permission
    fun showPermissionExplanation(
        context: Context,
        title: String,
        message: String,
        onPositiveClick: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it") { dialog, _ ->
                dialog.dismiss()
                onPositiveClick()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Show dialog when permission is permanently denied
    fun showPermissionPermanentlyDeniedDialog(
        context: Context,
        title: String,
        message: String
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings(context)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Open app settings for manual permission granting
    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // Handle permission result
    fun handlePermissionResult(
        context: Context,
        permission: String,
        isGranted: Boolean,
        shouldShowRequestPermissionRationale: (String) -> Boolean,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    ) {
        when {
            isGranted -> {
                onGranted()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                onDenied()
            }
            else -> {
                onPermanentlyDenied()
            }
        }
    }

    // Request permission with proper flow
    fun requestPermissionWithExplanation(
        activity: Activity,
        permissionLauncher: ActivityResultLauncher<String>,
        permission: String,
        explanationTitle: String,
        explanationMessage: String,
        permanentlyDeniedTitle: String = "Permission Required",
        permanentlyDeniedMessage: String = "This feature requires permission. Please enable it in app settings."
    ) {
        when {
            isPermissionGranted(activity, permission) -> {
                // Permission already granted
            }
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission().createIntent(activity, permission) != null -> {
                // Show explanation before requesting
                showPermissionExplanation(
                    context = activity,
                    title = explanationTitle,
                    message = explanationMessage
                ) {
                    permissionLauncher.launch(permission)
                }
            }
            else -> {
                // Permission permanently denied
                showPermissionPermanentlyDeniedDialog(
                    context = activity,
                    title = permanentlyDeniedTitle,
                    message = permanentlyDeniedMessage
                )
            }
        }
    }

    // Elderly-friendly permission explanation for common permissions
    object ElderlyExplanations {
        const val CALL_PHONE_TITLE = "Phone Call Permission"
        const val CALL_PHONE_MESSAGE = "This allows the app to make calls directly when you tap a contact. It's faster and easier for you!"

        const val CAMERA_TITLE = "Camera Permission"
        const val CAMERA_MESSAGE = "This lets you take photos for your contacts using your phone's camera."

        const val GALLERY_TITLE = "Photo Access"
        const val GALLERY_MESSAGE = "This lets you choose photos from your gallery for contact pictures."
    }
}