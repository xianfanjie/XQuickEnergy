package pansong291.xposed.quickenergy.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.RelativeLayout;
import java.util.List;
import pansong291.xposed.quickenergy.R;
import pansong291.xposed.quickenergy.entity.AlipayUser;
import pansong291.xposed.quickenergy.entity.AreaCode;
import pansong291.xposed.quickenergy.entity.CooperateUser;
import pansong291.xposed.quickenergy.entity.IdAndName;
import pansong291.xposed.quickenergy.util.Config;
import pansong291.xposed.quickenergy.util.CooperationIdMap;
import pansong291.xposed.quickenergy.util.FriendIdMap;

public class ListDialog {
    static AlertDialog listDialog;
    static Button btn_find_last, btn_find_next,
            btn_select_all, btn_select_invert;
    static EditText edt_find;
    static ListView lv_list;
    static List<String> selectedList;
    static List<Integer> countList;
    static List<Integer> totalList;
    static ListAdapter.ViewHolder curViewHolder;
    static IdAndName curIdAndName;

    static AlertDialog edtDialog;
    static EditText edt_count;

    static ListType listType;

    static AlertDialog optionsDialog;
    static AlertDialog deleteDialog;

    static RelativeLayout layout_batch_process;

    public enum ListType {
        RADIO, CHECK, SHOW
    }

    public static void show(Context c, CharSequence title, List<? extends IdAndName> bl, List<String> sl,
                            List<Integer> cl, List<Integer> tl) {
        show(c, title, bl, sl, cl, tl, ListType.CHECK);
    }

    public static void show(Context c, CharSequence title, List<? extends IdAndName> bl, List<String> sl,
                            List<Integer> cl, List<Integer> tl, ListType listType) {
        selectedList = sl;
        countList = cl;
        totalList = tl;
        ListAdapter la = ListAdapter.get(c, listType);
        la.setBaseList(bl);
        la.setSelectedList(selectedList);
        showListDialog(c, title);
        ListDialog.listType = listType;
    }

    private static void showListDialog(Context c, CharSequence title) {
        try {
            getListDialog(c).show();
        } catch (Throwable t) {
            listDialog = null;
            getListDialog(c).show();
        }
        listDialog.setTitle(title);
    }

    private static AlertDialog getListDialog(Context c) {
        if (listDialog == null || listDialog.getContext() != c)
            listDialog = new AlertDialog.Builder(c)
                    .setTitle("title")
                    .setView(getListView(c))
                    .setPositiveButton(c.getString(R.string.close), null)
                    .create();
        listDialog.setOnShowListener(
                new OnShowListener() {
                    Context c;

                    public OnShowListener setContext(Context c) {
                        this.c = c;
                        return this;
                    }

                    @Override
                    public void onShow(DialogInterface p1) {
                        AlertDialog d = (AlertDialog) p1;
                        layout_batch_process = d.findViewById(R.id.layout_batch_process);
                        layout_batch_process.setVisibility(listType==ListType.CHECK&&countList==null?View.VISIBLE:View.GONE);
                        ListAdapter.get(c).notifyDataSetChanged();
                    }
                }.setContext(c));
        return listDialog;
    }

    private static View getListView(Context c) {
        View v = LayoutInflater.from(c).inflate(R.layout.dialog_list, null);

        btn_find_last = v.findViewById(R.id.btn_find_last);
        btn_find_next = v.findViewById(R.id.btn_find_next);
        btn_select_all = v.findViewById(R.id.btn_select_all);
        btn_select_invert = v.findViewById(R.id.btn_select_invert);

        OnBtnClickListener onBtnClickListener = new OnBtnClickListener();
        BatchBtnOnClickListener batchBtnOnClickListener = new BatchBtnOnClickListener();
        btn_find_last.setOnClickListener(onBtnClickListener);
        btn_find_next.setOnClickListener(onBtnClickListener);
        btn_select_all.setOnClickListener(batchBtnOnClickListener);
        btn_select_invert.setOnClickListener(batchBtnOnClickListener);

        edt_find = v.findViewById(R.id.edt_find);
        lv_list = v.findViewById(R.id.lv_list);
        lv_list.setAdapter(ListAdapter.get(c));
        lv_list.setOnItemClickListener(
                (p1, p2, p3, p4) -> {
                    if (listType == ListType.SHOW) {
                        return;
                    }
                    curIdAndName = (IdAndName) p1.getAdapter().getItem(p3);
                    curViewHolder = (ListAdapter.ViewHolder) p2.getTag();
                    if (countList == null) {
                        if (listType == ListType.RADIO) {
                            selectedList.clear();
                            if (curViewHolder.cb.isChecked()) {
                                curViewHolder.cb.setChecked(false);
                            } else {
                                for (int i = 0; i < ListAdapter.viewHolderList.size(); i++) {
                                    ListAdapter.ViewHolder viewHolder = ListAdapter.viewHolderList.get(i);
                                    viewHolder.cb.setChecked(false);
                                }
                                curViewHolder.cb.setChecked(true);
                                selectedList.add(curIdAndName.id);
                            }
                        } else {
                            if (curViewHolder.cb.isChecked()) {
                                selectedList.remove(curIdAndName.id);
                                curViewHolder.cb.setChecked(false);
                            } else {
                                if (!selectedList.contains(curIdAndName.id))
                                    selectedList.add(curIdAndName.id);
                                curViewHolder.cb.setChecked(true);
                            }
                        }
                        Config.hasChanged = true;
                    } else {
                        showEdtDialog(p1.getContext());
                    }
                });
        lv_list.setOnItemLongClickListener(
                (p1, p2, p3, p4) -> {
                    curIdAndName = (IdAndName) p1.getAdapter().getItem(p3);
                    if (curIdAndName instanceof CooperateUser) {
                        showDeleteDialog(p1.getContext());
                    } else if (!(curIdAndName instanceof AreaCode)) {
                        showOptionsDialog(p1.getContext());
                    }
                    return true;
                });
        return v;
    }

    /**
     * Show the EDT dialog and set the title, hint, and text based on the current context.
     *
     * @param  c  the context in which the dialog is shown
     */
    private static void showEdtDialog(Context c) {
        try {
            getEdtDialog(c).show();
        } catch (Throwable t) {
            edtDialog = null;
            getEdtDialog(c).show();
        }
        edtDialog.setTitle(curIdAndName.name);
        if (curIdAndName instanceof CooperateUser)
            edt_count.setHint("每天浇水克数,总需浇水克数");
        else
            edt_count.setHint("次数");
        int i = selectedList.indexOf(curIdAndName.id);
        if (i >= 0) {
            if (curIdAndName instanceof CooperateUser) {
                edt_count.setText(countList.get(i) + "," + totalList.get(i));
            }else {
                edt_count.setText(String.valueOf(countList.get(i)));
            }
        } else {
            edt_count.getText().clear();
        }
    }

    private static AlertDialog getEdtDialog(Context c) {
        if (edtDialog == null) {
            OnClickListener listener = new OnClickListener() {
                Context c;

                public OnClickListener setContext(Context c) {
                    this.c = c;
                    return this;
                }

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    if (p2 == DialogInterface.BUTTON_POSITIVE) {
                        int count = 0;
                        int total = 0;
                        if (edt_count.length() > 0)
                            try {
                                String[] parts = edt_count.getText().toString().split(",");
                                count = Integer.parseInt(parts[0]);
                                if (curIdAndName instanceof CooperateUser) {
                                    total = Integer.parseInt(parts[1]);
                                }
                            } catch (Throwable t) {
                                return;
                            }
                        int index = selectedList.indexOf(curIdAndName.id);
                        if (count > 0) {
                            if (index < 0) {
                                selectedList.add(curIdAndName.id);
                                countList.add(count);
                                if (curIdAndName instanceof CooperateUser) {
                                    totalList.add(total);
                                }
                            } else {
                                countList.set(index, count);
                                if (curIdAndName instanceof CooperateUser) {
                                    totalList.set(index, total);
                                }
                            }
                            curViewHolder.cb.setChecked(true);
                        } else {
                            if (index >= 0) {
                                selectedList.remove(index);
                                countList.remove(index);
                                if (curIdAndName instanceof CooperateUser) {
                                    totalList.remove(index);
                                }
                            }
                            curViewHolder.cb.setChecked(false);
                        }
                        Config.hasChanged = true;
                    }
                    ListAdapter.get(c).notifyDataSetChanged();
                }
            }.setContext(c);
            edt_count = new EditText(c);
            edtDialog = new AlertDialog.Builder(c)
                    .setTitle("title")
                    .setView(edt_count)
                    .setPositiveButton(c.getString(R.string.ok), listener)
                    .setNegativeButton(c.getString(R.string.cancel), null)
                    .create();
        }
        return edtDialog;
    }

    private static void showOptionsDialog(Context c) {
        try {
            getOptionsDialog(c).show();
        } catch (Throwable t) {
            optionsDialog = null;
            getOptionsDialog(c).show();
        }
    }

    private static AlertDialog getOptionsDialog(Context c) {
        if (optionsDialog == null || optionsDialog.getContext() != c) {
            optionsDialog = new AlertDialog.Builder(c)
                    .setTitle("选项")
                    .setAdapter(
                            OptionsAdapter.get(c), new OnClickListener() {
                                Context c;

                                public OnClickListener setContext(Context c) {
                                    this.c = c;
                                    return this;
                                }

                                @Override
                                public void onClick(DialogInterface p1, int p2) {
                                    String url = null;
                                    switch (p2) {
                                        case 0:
                                            url = "alipays://platformapi/startapp?saId=10000007&qrcode=https%3A%2F%2F60000002.h5app.alipay.com%2Fwww%2Fhome.html%3FuserId%3D";
                                            break;

                                        case 1:
                                            url = "alipays://platformapi/startapp?saId=10000007&qrcode=https%3A%2F%2F66666674.h5app.alipay.com%2Fwww%2Findex.htm%3Fuid%3D";
                                            break;

                                        case 2:
                                            showDeleteDialog(c);
                                    }
                                    if (url != null) {
                                        Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url + curIdAndName.id));
                                        c.startActivity(it);
                                    }
                                }
                            }.setContext(c))
                    .setNegativeButton(c.getString(R.string.cancel), null)
                    .create();
        }
        return optionsDialog;
    }

    private static void showDeleteDialog(Context c) {
        try {
            getDeleteDialog(c).show();
        } catch (Throwable t) {
            deleteDialog = null;
            getDeleteDialog(c).show();
        }
        deleteDialog.setTitle("删除 " + curIdAndName.name);
    }

    private static AlertDialog getDeleteDialog(Context c) {
        if (deleteDialog == null) {
            OnClickListener listener = new OnClickListener() {
                Context c;

                public OnClickListener setContext(Context c) {
                    this.c = c;
                    return this;
                }

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    if (p2 == DialogInterface.BUTTON_POSITIVE) {
                        if (curIdAndName instanceof AlipayUser) {
                            FriendIdMap.removeIdMap(curIdAndName.id);
                            AlipayUser.remove(curIdAndName.id);
                        } else if (curIdAndName instanceof CooperateUser) {
                            CooperationIdMap.removeIdMap(curIdAndName.id);
                            CooperateUser.remove(curIdAndName.id);
                        }
                        selectedList.remove(curIdAndName.id);
                        ListAdapter.get(c).exitFind();
                    }
                    ListAdapter.get(c).notifyDataSetChanged();
                }
            }.setContext(c);
            deleteDialog = new AlertDialog.Builder(c)
                    .setTitle("title")
                    .setPositiveButton(c.getString(R.string.ok), listener)
                    .setNegativeButton(c.getString(R.string.cancel), null)
                    .create();
        }
        return deleteDialog;
    }

    static class OnBtnClickListener implements View.OnClickListener {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View p1) {
            if (edt_find.length() <= 0)
                return;
            ListAdapter la = ListAdapter.get(p1.getContext());
            int index = -1;
            switch (p1.getId()) {
                case R.id.btn_find_last:
                    // 下面Text要转String，不然判断equals会出问题
                    index = la.findLast(edt_find.getText().toString());
                    break;

                case R.id.btn_find_next:
                    // 同上
                    index = la.findNext(edt_find.getText().toString());
                    break;
            }
            if (index < 0) {
                Toast.makeText(p1.getContext(), "未搜到", Toast.LENGTH_SHORT).show();
            } else {
                lv_list.setSelection(index);
            }
        }
    }

    static class BatchBtnOnClickListener implements View.OnClickListener {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View p1) {
            ListAdapter la = ListAdapter.get(p1.getContext());
            switch (p1.getId()) {
                case R.id.btn_select_all:
                    la.selectAll();
                    break;
                case R.id.btn_select_invert:
                    la.SelectInvert();
                    break;
            }
            Config.hasChanged = true;
        }
    }

}
