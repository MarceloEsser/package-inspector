package com.example.packageinspector;


import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String ACCOUNT_AFFINITY_SUFFIX = "**";
    private ListView mListView;
    private PackageManager mPackageManager;
    private List<ApplicationInfo> mApplications;
    List<Algorithim> algorithimList = new ArrayList<>();

    private Map<String, String> mPkgAuthenticators = new HashMap<>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        SearchView search = (SearchView) menu.findItem(R.id.search).getActionView();
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAndDisplayPackages(newText);
                return true;
            }
        });

        return true;
    }

    private void filterAndDisplayPackages(@Nullable final String filterText) {
        final List<String> packageNames = new ArrayList<>(mApplications.size());

        for (final ApplicationInfo applicationInfo : mApplications) {
            String packageDisplayName = applicationInfo.packageName;

            if (isAnAuthenticatorApp(applicationInfo.packageName)) {
                packageDisplayName = packageDisplayName + "**";
            }

            packageNames.add(packageDisplayName);
        }

        if (null != filterText && !filterText.isEmpty()) {
            // iterate over the package names, remove those who don't contain the filter text
            for (Iterator<String> nameItr = packageNames.iterator(); nameItr.hasNext(); ) {
                final String pkgName = nameItr.next();

                if (!pkgName.toLowerCase().contains(filterText.toLowerCase())) {
                    nameItr.remove();
                }
            }
        }

        final ArrayAdapter<String> appAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                packageNames
        );

        mListView.setAdapter(appAdapter);
        appAdapter.notifyDataSetChanged();
        mListView.setOnItemClickListener((adapterView, view, position, l) -> {
            try {
                String pkgName = packageNames.get(position);

                if (pkgName.endsWith(ACCOUNT_AFFINITY_SUFFIX)) {
                    pkgName = pkgName.replace(ACCOUNT_AFFINITY_SUFFIX, "");
                }

                final ApplicationInfo clickedAppInfo = mPackageManager.getApplicationInfo(
                        pkgName,
                        PackageManager.GET_META_DATA
                );
                final PackageInfo packageInfo = mPackageManager.getPackageInfo(
                        clickedAppInfo.packageName,
                        getPackageManagerFlag()
                );

                final Signature[] signatures = getSignatures(packageInfo);
                if (null != signatures
                        && signatures.length > 0) {
                    final Signature signature = signatures[0];

                    Set<String> algorithms = Security.getAlgorithms("MessageDigest");
                    for (String algorith : algorithms) {
                        final MessageDigest code = MessageDigest.getInstance(algorith);
                        code.update(signature.toByteArray());
                        String finalCode = Base64.encodeToString(code.digest(), Base64.NO_WRAP);
                        algorithimList.add(new Algorithim(algorith, finalCode));
                    }
                }


                LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);

                View dialogView = layoutInflater.inflate(R.layout.dialog_layout, null, false);
                RecyclerView rvAlgorithims = dialogView.findViewById(R.id.rvAlgorithims);

                rvAlgorithims.setAdapter(new AlgorithimAdapter(getBaseContext(), algorithimList));

                new AlertDialog.Builder(MainActivity.this)
                        .setView(dialogView)
                        .setTitle(pkgName)
                        .setPositiveButton(
                                MainActivity.this.getString(R.string.dismiss),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                }
                        ).show();
            } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    @SuppressLint("PackageManagerGetSignatures")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = findViewById(R.id.lv_apps);

        mPackageManager = getPackageManager();
        populateAuthenticatorsLookup(AccountManager.get(this));
        mApplications = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(mApplications, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(@NonNull final ApplicationInfo info1,
                               @NonNull final ApplicationInfo info2) {
                return info1.packageName.compareTo(info2.packageName);
            }
        });

        filterAndDisplayPackages(null);
    }

    private void populateAuthenticatorsLookup(@NonNull final AccountManager accountManager) {
        final AuthenticatorDescription[] authenticatorDescriptions = accountManager.getAuthenticatorTypes();

        for (final AuthenticatorDescription description : authenticatorDescriptions) {
            mPkgAuthenticators.put(description.packageName, description.type);
        }
    }

    private String getAuthenticatorAppMetadata(@NonNull final String pkgName) {
        return "App has account type affinity: "
                + "\n"
                + mPkgAuthenticators.get(pkgName);
    }

    private boolean isAnAuthenticatorApp(@NonNull final String pkgName) {
        return mPkgAuthenticators.containsKey(pkgName);
    }

    private Signature[] getSignatures(@Nullable final PackageInfo packageInfo) {
        if (packageInfo == null) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (packageInfo.signingInfo == null) {
                return null;
            }
            if (packageInfo.signingInfo.hasMultipleSigners()) {
                return packageInfo.signingInfo.getApkContentsSigners();
            } else {
                return packageInfo.signingInfo.getSigningCertificateHistory();
            }
        }

        return packageInfo.signatures;
    }

    private static int getPackageManagerFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return PackageManager.GET_SIGNING_CERTIFICATES;
        }

        return PackageManager.GET_SIGNATURES;
    }
}
