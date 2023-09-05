package com.Innospectra.NanoScan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class fabricManagementActivity extends Activity {
    private List<String> data = new ArrayList<>();
    private fabricManagementAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitvity_fabric_management);

        loadData();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new fabricManagementAdapter(this, data, new fabricManagementAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                showEditDialog(position);
            }
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.add_button).setOnClickListener(v -> showAddDialog());
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加成分");
        EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("添加", (dialog, which) -> {
            String component = input.getText().toString();
            if (component.equals("")) {
                return;
            }
            data.add(component);
            adapter.notifyDataSetChanged();
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑成分");
        EditText input = new EditText(this);
        input.setText(data.get(position));
        builder.setView(input);
        builder.setPositiveButton("更新", (dialog, which) -> {
            String component = input.getText().toString();
            if (component.equals("")) {
                return;
            }
            data.set(position, component);
            adapter.notifyDataSetChanged();
        });
        builder.setNegativeButton("删除", (dialog, which) -> {
            data.remove(position);
            adapter.notifyDataSetChanged();
        });
        builder.setNeutralButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        String componentsString = sharedPreferences.getString("components", "桑蚕丝,乙纶,聚酯纤维" +
                ",棉,氨纶,动物毛纤维,芳纶,锦纶,腈纶,再生纤维素纤维,醋纤,丙纶,海藻纤维,聚酰亚胺纤维,壳聚糖纤维,其他纤维");
        System.out.println("存储数据：" + componentsString);
        if (!componentsString.equals("")){
            data = new ArrayList<>(Arrays.asList(componentsString.split(",")));
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("components", String.join(",", data));
        editor.apply();
    }
    public ArrayList<String> getComponents(){
        ArrayList<String> components = new ArrayList<>();
        for (String component : data) {
            components.add(component);
        }
        return components;
    }
}
