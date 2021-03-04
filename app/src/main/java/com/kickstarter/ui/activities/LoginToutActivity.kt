package com.kickstarter.ui.activities

import android.content.Intent
import android.os.Bundle
import com.facebook.AccessToken
import com.kickstarter.R
import com.kickstarter.databinding.LoginToutLayoutBinding
import com.kickstarter.libs.ActivityRequestCodes
import com.kickstarter.libs.BaseActivity
import com.kickstarter.libs.qualifiers.RequiresActivityViewModel
import com.kickstarter.libs.utils.ObjectUtils
import com.kickstarter.libs.utils.TransitionUtils
import com.kickstarter.libs.utils.ViewUtils
import com.kickstarter.services.apiresponses.ErrorEnvelope.FacebookUser
import com.kickstarter.ui.IntentKey
import com.kickstarter.ui.views.LoginPopupMenu
import com.kickstarter.viewmodels.LoginToutViewModel
import rx.Observable
import rx.android.schedulers.AndroidSchedulers

@RequiresActivityViewModel(LoginToutViewModel.ViewModel::class)
class LoginToutActivity : BaseActivity<LoginToutViewModel.ViewModel>() {

    private lateinit var binding: LoginToutLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginToutLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.loginToolbar.loginToolbar.title = getString(R.string.login_tout_navbar_title)

        viewModel.outputs.finishWithSuccessfulResult()
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { finishWithSuccessfulResult() }

        viewModel.outputs.startLoginActivity()
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { startLogin() }

        viewModel.outputs.startSignupActivity()
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { startSignup() }

        viewModel.outputs.startFacebookConfirmationActivity()
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { startFacebookConfirmationActivity(it.first, it.second) }

        viewModel.outputs.showFacebookAuthorizationErrorDialog()
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                ViewUtils.showDialog(
                    this,
                    getString(R.string.general_error_oops),
                    getString(R.string.login_tout_errors_facebook_authorization_exception_message),
                    getString(R.string.login_tout_errors_facebook_authorization_exception_button)
                )
            }

        showErrorMessageToasts()
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(ViewUtils.showToast(this))

        viewModel.outputs.startTwoFactorChallenge()
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { startTwoFactorFacebookChallenge() }

        viewModel.outputs.showUnauthorizedErrorDialog()
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { ViewUtils.showDialog(this, getString(R.string.login_tout_navbar_title), it) }

        binding.disclaimerTextView.setOnClickListener {
            disclaimerTextViewClick()
        }

        binding.facebookLoginButton.setOnClickListener {
            facebookLoginClick()
        }

        binding.loginButton.setOnClickListener {
            loginButtonClick()
        }

        binding.signUpButton.setOnClickListener {
            signupButtonClick()
        }
    }

    private fun disclaimerTextViewClick() =
        LoginPopupMenu(this, binding.loginToolbar.helpButton).show()

    private fun facebookLoginClick() =
        viewModel.inputs.facebookLoginClick(
            this,
            resources.getStringArray(R.array.facebook_permissions_array).asList()
        )

    private fun loginButtonClick() =
        viewModel.inputs.loginClick()

    private fun signupButtonClick() =
        viewModel.inputs.signupClick()

    private fun showErrorMessageToasts(): Observable<String?> {
        return viewModel.outputs.showMissingFacebookEmailErrorToast()
            .map(ObjectUtils.coalesceWith(getString(R.string.login_errors_unable_to_log_in)))
            .mergeWith(
                viewModel.outputs.showFacebookInvalidAccessTokenErrorToast()
                    .map(ObjectUtils.coalesceWith(getString(R.string.login_errors_unable_to_log_in)))
            )
    }

    private fun finishWithSuccessfulResult() {
        setResult(RESULT_OK)
        finish()
    }

    private fun startFacebookConfirmationActivity(
        facebookUser: FacebookUser,
        accessTokenString: String
    ) {
        val intent = Intent(this, FacebookConfirmationActivity::class.java)
            .putExtra(IntentKey.FACEBOOK_USER, facebookUser)
            .putExtra(IntentKey.FACEBOOK_TOKEN, accessTokenString)
        startActivityForResult(intent, ActivityRequestCodes.LOGIN_FLOW)
        TransitionUtils.transition(this, TransitionUtils.fadeIn())
    }

    private fun startLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivityForResult(intent, ActivityRequestCodes.LOGIN_FLOW)
        TransitionUtils.transition(this, TransitionUtils.fadeIn())
    }

    private fun startSignup() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivityForResult(intent, ActivityRequestCodes.LOGIN_FLOW)
        TransitionUtils.transition(this, TransitionUtils.fadeIn())
    }

    fun startTwoFactorFacebookChallenge() {
        val intent = Intent(this, TwoFactorActivity::class.java)
            .putExtra(IntentKey.FACEBOOK_LOGIN, true)
            .putExtra(IntentKey.FACEBOOK_TOKEN, AccessToken.getCurrentAccessToken().token)
        startActivityForResult(intent, ActivityRequestCodes.LOGIN_FLOW)
        TransitionUtils.transition(this, TransitionUtils.fadeIn())
    }
}
