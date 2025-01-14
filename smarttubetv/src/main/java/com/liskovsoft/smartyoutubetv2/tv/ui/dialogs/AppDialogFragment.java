package com.liskovsoft.smartyoutubetv2.tv.ui.dialogs;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackSettingsFragment;
import androidx.preference.DialogPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.ChatPreference;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.ChatPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.CommentsPreference;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.CommentsPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.RadioListPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.StringListPreference;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other.StringListPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

public class AppDialogFragment extends LeanbackSettingsFragment
        implements DialogPreference.TargetFragment, AppDialogView {
    private static final String TAG = AppDialogFragment.class.getSimpleName();
    private AppPreferenceFragment mPreferenceFragment;
    private AppDialogPresenter mSettingsPresenter;
    private boolean mIsTransparent;
    private static final String PREFERENCE_FRAGMENT_TAG =
            "androidx.leanback.preference.LeanbackSettingsFragment.PREFERENCE_FRAGMENT";
    private int mBackStackCount;
    private final List<Integer> mMyBackStackIdx = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsPresenter = AppDialogPresenter.instance(getActivity());
        mSettingsPresenter.setView(this);
        mIsTransparent = mSettingsPresenter.isTransparent();
        getChildFragmentManager().addOnBackStackChangedListener(this::onBackStackChanged);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mSettingsPresenter.onViewDestroyed();
    }

    @Override
    public void onPreferenceStartInitialScreen() {
        // FIX: Can not perform this action after onSaveInstanceState
        // Possible fix: Unable to add window -- token android.os.BinderProxy is not valid; is your activity running?
        if (!Utils.checkActivity(getActivity())) {
            return;
        }

        try {
            // Fix mSettingsPresenter in null after init stage.
            // Seems concurrency between dialogs.
            mSettingsPresenter.setView(this);

            mSettingsPresenter.onViewInitialized();
        } catch (IllegalStateException e) {
            // NOP
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment, PreferenceScreen preferenceScreen) {
        PreferenceFragment frag = buildPreferenceFragment();
        startPreferenceFragment(frag);
        return true;
    }

    @Override
    public Preference findPreference(CharSequence charSequence) {
        return mPreferenceFragment.findPreference(charSequence);
    }

    private AppPreferenceFragment buildPreferenceFragment() {
        AppPreferenceFragment fragment = new AppPreferenceFragment();
        fragment.setIsRoot(mPreferenceFragment == null);
        return fragment;
    }

    @Override
    public void show(List<OptionCategory> categories, String title) {
        mPreferenceFragment = buildPreferenceFragment();
        mPreferenceFragment.setCategories(categories);
        mPreferenceFragment.setTitle(title);
        mPreferenceFragment.enableTransparent(mIsTransparent);
        startPreferenceFragment(mPreferenceFragment);

        if (mPreferenceFragment.isSkipBackStack()) {
            int count = getChildFragmentManager().getBackStackEntryCount();
            mMyBackStackIdx.add(count > 0 ? count + 1 : mPreferenceFragment.isRoot() ? 0 : 1);
        }
    }

    //@Override
    //public void startPreferenceFragment(@NonNull Fragment fragment) {
    //    final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
    //    final Fragment prevFragment =
    //            getChildFragmentManager().findFragmentByTag(PREFERENCE_FRAGMENT_TAG);
    //
    //    if (prevFragment != null) {
    //        if (!skipBackStack(fragment.getTargetFragment())) {
    //            transaction.addToBackStack(null);
    //        }
    //
    //        transaction
    //                .addToBackStack(null)
    //                .replace(R.id.settings_preference_fragment_container, fragment,
    //                        PREFERENCE_FRAGMENT_TAG);
    //    } else {
    //        transaction
    //                .add(R.id.settings_preference_fragment_container, fragment,
    //                        PREFERENCE_FRAGMENT_TAG);
    //    }
    //    transaction.commit();
    //}

    //private boolean skipBackStack(Fragment fragment) {
    //    return fragment instanceof AppPreferenceFragment && ((AppPreferenceFragment) fragment).isSkipBackStack();
    //}

    private void onBackStackChanged() {
        if (getChildFragmentManager() != null) {
            int currentBackStackCount = getChildFragmentManager().getBackStackEntryCount();

            if (currentBackStackCount < mBackStackCount && mMyBackStackIdx.contains(currentBackStackCount)) {
                mMyBackStackIdx.remove((Integer) currentBackStackCount);
                if (currentBackStackCount == 0) {
                    // finish dialog
                    finish();
                } else {
                    // one level back
                    getChildFragmentManager().popBackStack();
                }
            }

            mBackStackCount = currentBackStackCount;
        }
    }

    @Override
    public boolean onPreferenceDisplayDialog(@NonNull PreferenceFragment caller, Preference pref) {
        // Fix: IllegalStateException: Activity has been destroyed
        // Possible fix: Unable to add window -- token android.os.BinderProxy is not valid; is your activity running?
        if (!Utils.checkActivity(getActivity())) {
            return false;
        }

        if (pref instanceof StringListPreference) {
            StringListPreference listPreference = (StringListPreference) pref;
            StringListPreferenceDialogFragment f = StringListPreferenceDialogFragment.newInstanceStringList(listPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            startPreferenceFragment(f);

            return true;
        } else if (pref instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) pref;
            RadioListPreferenceDialogFragment f = RadioListPreferenceDialogFragment.newInstanceSingle(listPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            startPreferenceFragment(f);

            return true;
        } else if (pref instanceof ChatPreference) {
            ChatPreference chatPreference = (ChatPreference) pref;
            ChatPreferenceDialogFragment f = ChatPreferenceDialogFragment.newInstance(chatPreference.getChatReceiver(), chatPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            startPreferenceFragment(f);

            return true;
        } else if (pref instanceof CommentsPreference) {
            CommentsPreference commentsPreference = (CommentsPreference) pref;
            CommentsPreferenceDialogFragment f = CommentsPreferenceDialogFragment.newInstance(commentsPreference.getCommentsReceiver(), commentsPreference.getKey());
            f.enableTransparent(mIsTransparent);
            f.setTargetFragment(caller, 0);
            startPreferenceFragment(f);

            return true;
        }

        // NOTE: Transparent CheckedList should be placed here (just in case you'll need it).

        return super.onPreferenceDisplayDialog(caller, pref);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (mIsTransparent && view != null) {
            // Enable transparent background (this is the only place to do it)
            ViewUtil.enableTransparentDialog(getActivity(), view);
        }

        return view;
    }

    @Override
    public void finish() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void goBack() {
        if (mPreferenceFragment != null) {
            mPreferenceFragment.goBack();
        }
    }

    @Override
    public boolean isShown() {
        return isVisible() && getUserVisibleHint();
    }

    public void onFinish() {
        mSettingsPresenter.onFinish();
    }
    
    public static class AppPreferenceFragment extends LeanbackPreferenceFragment {
        private static final String TAG = AppPreferenceFragment.class.getSimpleName();
        private List<OptionCategory> mCategories;
        private Context mExtractedContext;
        private AppDialogFragmentManager mManager;
        private String mTitle;
        private boolean mIsTransparent;
        private boolean mSkipBackStack;
        private boolean mIsRoot;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            // Note, place in field with different name to avoid field overlapping
            mExtractedContext = (Context) Helpers.getField(this, "mStyledContext");
            mManager = new AppDialogFragmentManager(mExtractedContext);

            initPrefs();

            Log.d(TAG, "onCreatePreferences");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);

            if (mIsTransparent && view != null) {
                ViewUtil.enableTransparentDialog(getActivity(), view);
            }

            return view;
        }

        private void initPrefs() {
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(mExtractedContext);
            setPreferenceScreen(screen);

            screen.setTitle(mTitle);

            addPreferences(screen);

            setSingleCategoryAsRoot(screen);
        }

        private void addPreferences(PreferenceScreen screen) {
            if (mCategories != null) {
                for (OptionCategory category : mCategories) {
                    if (category.items != null) {
                        screen.addPreference(mManager.createPreference(category));
                    }
                }
            }
        }
        
        private void setSingleCategoryAsRoot(PreferenceScreen screen) {
            // Possible fix: java.lang.IllegalStateException Activity has been destroyed
            if (!Utils.checkActivity(getActivity())) {
                return;
            }

            // auto expand single list preference
            if (mCategories != null && mCategories.size() == 1 && screen.getPreferenceCount() > 0) {
                Preference preference = screen.getPreference(0);

                if (preference instanceof DialogPreference) {
                    onDisplayPreferenceDialog(preference);
                }

                // NOTE: we should avoid open simple buttons because we don't know what is hidden behind them: new dialog on action
            }
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            super.onDisplayPreferenceDialog(preference);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            return super.onPreferenceTreeClick(preference);
        }

        public void setCategories(List<OptionCategory> categories) {
            mCategories = categories;

            if (categories != null && categories.size() == 1) {
                mSkipBackStack = true;
            }
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public void enableTransparent(boolean enable) {
            mIsTransparent = enable;
        }

        public void goBack() {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        }

        public boolean isSkipBackStack() {
            return mSkipBackStack;
        }

        public void setIsRoot(boolean isRoot) {
            mIsRoot = isRoot;
        }

        public boolean isRoot() {
            return mIsRoot;
        }
    }
}