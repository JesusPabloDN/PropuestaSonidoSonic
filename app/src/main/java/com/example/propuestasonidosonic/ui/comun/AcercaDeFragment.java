package com.example.propuestasonidosonic.ui.comun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.propuestasonidosonic.R;

public class AcercaDeFragment extends Fragment {

    public AcercaDeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_acerca_de, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvVersion = view.findViewById(R.id.tvAcercaDeVersion);
        String version = "1.0";
        try {
            if (getContext() != null) {
                version = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName;
            }
        } catch (Exception ignored) {}
        tvVersion.setText(getString(R.string.app_version_formato, version));
    }
}
