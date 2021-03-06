package com.canyie.dreamland.manager.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.canyie.dreamland.manager.R;
import com.canyie.dreamland.manager.core.Dreamland;
import com.canyie.dreamland.manager.core.ModuleInfo;
import com.canyie.dreamland.manager.ui.adapters.ModuleListAdapter;
import com.canyie.dreamland.manager.ui.widgets.CMRecyclerView;
import com.canyie.dreamland.manager.AppConstants;
import com.canyie.dreamland.manager.AppGlobals;
import com.canyie.dreamland.manager.utils.Dialogs;
import com.canyie.dreamland.manager.utils.Intents;

import java.util.List;

/**
 * @author canyie
 */
public class ModuleManagerFragment extends PageFragment implements SearchView.OnQueryTextListener, ModuleListAdapter.OnModuleStateChangedListener {
    private ModuleListAdapter mAdapter;
    private boolean mLoading;
    private ModuleInfo mCurrentSelectedModule;
    public ModuleManagerFragment() {
        super(R.string.modules);
    }

    @Override protected int getLayoutResId() {
        return R.layout.fragment_moduleslist;
    }

    @Override protected void initView(@NonNull View view) {
        Context context = requireContext();
        RecyclerView recyclerView = requireView(R.id.modules_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        mAdapter = new ModuleListAdapter(this);
        mAdapter.setOnModuleStateChangedListener(this);
        recyclerView.setAdapter(mAdapter);
        registerForContextMenu(recyclerView);
    }

    @Override protected void beforeLoadData() {
        mLoading = true;
    }

    @Override protected Object loadDataImpl() {
        List<ModuleInfo> modules = Dreamland.getModuleInfos(requireContext());
        Dreamland.getModuleManager().ensureDataLoaded();
        return modules;
    }

    @SuppressWarnings("unchecked") @Override protected void updateUIForData(Object data) {
        mAdapter.setModules((List<ModuleInfo>) data);
        super.updateUIForData(data);
        mLoading = false;
    }

    @Override public boolean onSearchViewOpen(View v) {
        if (mLoading) {
            toast(R.string.alert_wait_loading_complete);
            return true;
        }
        return super.onSearchViewOpen(v);
    }

    @Override public boolean onQueryTextChange(String newText) {
        mAdapter.getFilter().filter(newText);
        return true;
    }

    @Override public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        assert menuInfo != null;
        CMRecyclerView.CMContextMenuInfo rawMenuInfo = (CMRecyclerView.CMContextMenuInfo) menuInfo;
        mCurrentSelectedModule = mAdapter.getModuleInfoForPosition(rawMenuInfo.position);
        menu.setHeaderTitle(mCurrentSelectedModule.name);
        requireActivity().getMenuInflater().inflate(R.menu.menu_module_manage, menu);
    }

    @Override public boolean onContextItemSelectedImpl(@NonNull MenuItem item) {
        Context context = requireContext();
        switch (item.getItemId()) {
            case R.id.module_action_launch:
                if (!Intents.openAppUserInterface(context, mCurrentSelectedModule.packageName)) {
                    toast(R.string.alert_module_cannot_open);
                }
                return true;
            case R.id.module_action_info:
                Intents.openAppDetailsSettings(context, mCurrentSelectedModule.packageName);
                return true;
            case R.id.module_action_uninstall:
                Intents.uninstallApp(context, mCurrentSelectedModule.packageName);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override public void onModuleStateChanged() {
        final SharedPreferences defaultConfigSP = AppGlobals.getDefaultConfigSP();
        boolean shouldShowAlertDialog = defaultConfigSP.getBoolean(AppConstants.SP_KEY_SHOW_DIALOG_WHEN_MODULE_STATE_CHANGED, true);
        if (shouldShowAlertDialog) {
            Dialogs.create(requireActivity())
                    .message(R.string.module_state_changed_alert_content)
                    .checkbox(R.string.dont_show_again)
                    .positiveButton(R.string.ok, dialogInfo -> {
                        CheckBox checkbox = dialogInfo.checkbox;
                        assert checkbox != null;
                        if (checkbox.isChecked()) {
                            defaultConfigSP.edit()
                                    .putBoolean(AppConstants.SP_KEY_SHOW_DIALOG_WHEN_MODULE_STATE_CHANGED, false)
                                    .apply();
                        }
                    })
                    .showIfActivityActivated();
        }
    }
}
